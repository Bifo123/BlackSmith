package com.forjasuprema.station;

import com.forjasuprema.metal.Metal;
import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.registry.ModBlockEntities;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModItems;
import com.forjasuprema.thermal.Boostable;
import com.forjasuprema.thermal.FlameLevel;
import com.forjasuprema.thermal.ForgeTier;
import com.forjasuprema.thermal.Fuel;
import com.forjasuprema.thermal.FuelLookup;
import com.forjasuprema.thermal.HeatData;
import com.forjasuprema.thermal.HeatSource;
import com.forjasuprema.thermal.PieceThermal;
import com.forjasuprema.thermal.ThermalMath;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Forja: queima combustivel, mantem chama e temperatura, e aquece a peca apoiada sobre ela.
 *
 * <p>Tudo e simulado apenas no servidor. A forja so "tica" enquanto ha algo acontecendo (acesa,
 * quente ou com peca fora do ambiente); em repouso nao tem custo. A temperatura da peca e mantida aqui
 * enquanto ela esta na forja e gravada no item (componente {@code heat}) quando ela e retirada.
 */
public class ForgeBlockEntity extends WorkpieceBlockEntity implements HeatSource, Boostable {
    public enum IgniteResult { OK, ALREADY_LIT, NO_FUEL }

    private static final double SNAP_DISTANCE = 0.5;

    private final ForgeTier tier;

    // Estado da forja (persistido).
    private boolean lit;
    private int burnRemaining;
    private int flame;
    private int fuelIntensity;
    private double fuelEfficiency = 1.0;
    private int fuelMaxTemperature;
    private double temperature = ThermalMath.AMBIENT;
    private ItemStack fuelStack = ItemStack.EMPTY;

    // Estado da peca apoiada (persistido).
    private double pieceTemperature = ThermalMath.AMBIENT;
    private double pieceBurn;

    // Impulso temporario dos foles (nao persistido: dura segundos).
    private double boostFactor = 1.0;
    private int boostTicks;

    // Cache nao persistido: metal da peca.
    @Nullable
    private Metal pieceMetal;
    private boolean pieceMetalResolved;

    public ForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGE.get(), pos, state);
        this.tier = state.getBlock() instanceof ForgeBlock forge ? forge.getTier() : ForgeTier.APPRENTICE;
    }

    public ForgeTier getTier() {
        return tier;
    }

    /** Capacidade de combustivel armazenado, crescente com o tier. */
    public int fuelCapacity() {
        return Math.min(64, 16 * (tier.ordinal() + 1));
    }

    @Override
    public int flameLevel() {
        return lit ? flame : 0;
    }

    @Override
    public double temperature() {
        return temperature;
    }

    @Override
    public boolean isLit() {
        return lit;
    }

    /** Foles: acelera o aquecimento da forja por um tempo. Nao eleva o teto de temperatura nem a chama. */
    @Override
    public void boost(double rateFactor, int ticks) {
        if (!lit || ticks <= 0) {
            return;
        }
        boostFactor = Math.max(1.0, rateFactor);
        boostTicks = Math.max(boostTicks, ticks);
    }

    // ---------------------------------------------------------------- combustivel e ignicao

    /** Guarda combustivel do stack na forja. Retorna quantos itens foram aceitos (0 se cheia ou outro combustivel). */
    public int addFuel(ItemStack stack) {
        int moved;
        if (fuelStack.isEmpty()) {
            moved = Math.min(stack.getCount(), fuelCapacity());
            if (moved <= 0) {
                return 0;
            }
            fuelStack = stack.copyWithCount(moved);
        } else if (ItemStack.isSameItemSameComponents(fuelStack, stack)) {
            moved = Math.min(stack.getCount(), fuelCapacity() - fuelStack.getCount());
            if (moved <= 0) {
                return 0;
            }
            fuelStack.grow(moved);
        } else {
            return 0;
        }
        setChanged();
        return moved;
    }

    public IgniteResult ignite() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return IgniteResult.NO_FUEL;
        }
        if (lit) {
            return IgniteResult.ALREADY_LIT;
        }
        if (!consumeFuel(serverLevel)) {
            return IgniteResult.NO_FUEL;
        }
        setLit(true);
        return IgniteResult.OK;
    }

    /** Consome um item de combustivel e passa a queimar com as propriedades dele. */
    private boolean consumeFuel(ServerLevel serverLevel) {
        if (fuelStack.isEmpty()) {
            return false;
        }
        Optional<Holder.Reference<Fuel>> found = FuelLookup.find(serverLevel.registryAccess(), fuelStack);
        if (found.isEmpty()) {
            // O item deixou de ser combustivel (datapack alterado): devolve ao mundo em vez de prender.
            Block.popResource(serverLevel, worldPosition.above(), fuelStack);
            fuelStack = ItemStack.EMPTY;
            return false;
        }
        Fuel fuel = found.get().value();
        fuelStack.shrink(1);
        if (fuelStack.isEmpty()) {
            fuelStack = ItemStack.EMPTY;
        }
        burnRemaining = fuel.burnTime();
        fuelIntensity = fuel.intensity();
        fuelEfficiency = fuel.efficiency();
        fuelMaxTemperature = fuel.maxTemperature();
        flame = ThermalMath.flameLevel(fuelIntensity, tier);
        return true;
    }

    private void setLit(boolean value) {
        lit = value;
        if (!value) {
            flame = 0;
            burnRemaining = 0;
        }
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(ForgeBlock.LIT) && state.getValue(ForgeBlock.LIT) != value) {
                level.setBlock(worldPosition, state.setValue(ForgeBlock.LIT, value), Block.UPDATE_ALL);
            }
        }
        setChanged();
    }

    // ---------------------------------------------------------------- tick

    public void serverTick(ServerLevel serverLevel) {
        boolean idle = !lit && temperature <= ThermalMath.AMBIENT && (!hasPiece() || pieceTemperature <= ThermalMath.AMBIENT);
        if (idle) {
            return;
        }

        if (lit) {
            if (burnRemaining > 0) {
                burnRemaining--;
            }
            if (burnRemaining <= 0 && !consumeFuel(serverLevel)) {
                setLit(false);
            }
        }

        double target;
        double rate;
        if (lit) {
            target = ThermalMath.targetTemperature(fuelMaxTemperature, tier);
            rate = ThermalMath.forgeHeatRate(flame, fuelEfficiency);
            if (boostTicks > 0) {
                rate *= boostFactor;
            }
        } else {
            target = ThermalMath.AMBIENT;
            rate = tier.coolingRate();
        }
        temperature = ThermalMath.approach(temperature, target, rate);
        if (Math.abs(target - temperature) < SNAP_DISTANCE) {
            temperature = target;
        }
        if (boostTicks > 0) {
            boostTicks--;
            if (boostTicks == 0 || !lit) {
                boostTicks = 0;
                boostFactor = 1.0;
            }
        }

        if (hasPiece()) {
            tickPiece(serverLevel);
        }

        if (serverLevel.getGameTime() % 20 == 0) {
            setChanged();
        }
    }

    private void tickPiece(ServerLevel serverLevel) {
        if (!pieceMetalResolved) {
            pieceMetal = MetalLookup.find(serverLevel.registryAccess(), getPiece()).map(Holder::value).orElse(null);
            pieceMetalResolved = true;
        }
        Metal metal = pieceMetal;
        if (metal == null) {
            return;
        }
        pieceTemperature = ThermalMath.approach(pieceTemperature, temperature, PieceThermal.heatRate(metal.malleability()));
        if (Math.abs(temperature - pieceTemperature) < SNAP_DISTANCE) {
            pieceTemperature = temperature;
        }

        double burnStep = PieceThermal.burnPerTick(pieceTemperature, metal.workTempMax(), metal.meltTemperature());
        if (burnStep > 0.0) {
            pieceBurn = Math.min(1.0, pieceBurn + burnStep);
        }
        if (pieceTemperature >= metal.meltTemperature()) {
            meltPiece(serverLevel);
        }
    }

    /** Peca alem do ponto de fusao: vira escoria. Nunca some em silencio: ha som e fumaca. */
    private void meltPiece(ServerLevel serverLevel) {
        setPiece(new ItemStack(ModItems.SLAG.get()));
        serverLevel.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                12, 0.2, 0.1, 0.2, 0.02);
    }

    // ---------------------------------------------------------------- peca apoiada

    @Override
    public void setPiece(ItemStack newPiece) {
        super.setPiece(newPiece);
        pieceMetal = null;
        pieceMetalResolved = false;
        if (newPiece.isEmpty() || level == null) {
            pieceTemperature = ThermalMath.AMBIENT;
            pieceBurn = 0.0;
            return;
        }
        HeatData heat = newPiece.get(ModDataComponents.HEAT.get());
        pieceTemperature = heat == null ? ThermalMath.AMBIENT : heat.temperatureAt(level.getGameTime());
        pieceBurn = heat == null ? 0.0 : heat.burn();
    }

    /** Retira a peca gravando nela o estado termico atual (temperatura e queima). */
    @Override
    public ItemStack takePiece() {
        double temp = pieceTemperature;
        double burn = pieceBurn;
        Metal metal = pieceMetal;
        ItemStack taken = super.takePiece();
        if (!taken.isEmpty() && metal != null && level != null) {
            if (temp > ThermalMath.AMBIENT + 1.0 || burn > 0.0) {
                taken.set(ModDataComponents.HEAT.get(), new HeatData(temp, level.getGameTime(), burn));
            } else {
                taken.remove(ModDataComponents.HEAT.get());
            }
        }
        return taken;
    }

    /** Dropa o combustivel armazenado (quando o bloco e quebrado). */
    public void dropFuel() {
        if (level != null && !fuelStack.isEmpty()) {
            Block.popResource(level, worldPosition, fuelStack);
            fuelStack = ItemStack.EMPTY;
        }
    }

    /** Linha de status mostrada ao jogador (barra de acao). */
    public Component statusMessage() {
        MutableComponent message = Component.translatable("message.forjasuprema.forge.status",
                getBlockState().getBlock().getName(),
                Component.translatable(FlameLevel.translationKey(flameLevel())),
                Math.round(temperature),
                fuelStack.getCount(),
                fuelCapacity());
        if (hasPiece()) {
            message.append(Component.translatable("message.forjasuprema.forge.status.piece", Math.round(pieceTemperature)));
        }
        return message;
    }

    // ---------------------------------------------------------------- persistencia

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Lit", lit);
        tag.putInt("BurnRemaining", burnRemaining);
        tag.putInt("Flame", flame);
        tag.putInt("FuelIntensity", fuelIntensity);
        tag.putDouble("FuelEfficiency", fuelEfficiency);
        tag.putInt("FuelMaxTemp", fuelMaxTemperature);
        tag.putDouble("Temperature", temperature);
        tag.putDouble("PieceTemperature", pieceTemperature);
        tag.putDouble("PieceBurn", pieceBurn);
        if (!fuelStack.isEmpty()) {
            tag.put("Fuel", fuelStack.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lit = tag.getBoolean("Lit");
        burnRemaining = tag.getInt("BurnRemaining");
        flame = tag.getInt("Flame");
        fuelIntensity = tag.getInt("FuelIntensity");
        fuelEfficiency = tag.contains("FuelEfficiency") ? tag.getDouble("FuelEfficiency") : 1.0;
        fuelMaxTemperature = tag.getInt("FuelMaxTemp");
        temperature = tag.contains("Temperature") ? tag.getDouble("Temperature") : ThermalMath.AMBIENT;
        pieceTemperature = tag.contains("PieceTemperature") ? tag.getDouble("PieceTemperature") : ThermalMath.AMBIENT;
        pieceBurn = tag.getDouble("PieceBurn");
        fuelStack = tag.contains("Fuel")
                ? ItemStack.parse(registries, tag.getCompound("Fuel")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        pieceMetal = null;
        pieceMetalResolved = false;
    }
}
