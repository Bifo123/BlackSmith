package com.forjasuprema.station;

import com.forjasuprema.metal.Metal;
import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.registry.ModBlockEntities;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.thermal.HeatData;
import com.forjasuprema.thermal.HeatSource;
import com.forjasuprema.thermal.ThermalMath;
import com.forjasuprema.treatment.TreatmentKind;
import com.forjasuprema.treatment.TreatmentMessages;
import com.forjasuprema.treatment.TreatmentRules;
import com.forjasuprema.treatment.TreatmentService;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Forno de tratamento termico: colocado sobre uma forja acesa, mantem a peca em condicoes controladas por um
 * tempo (recozimento, revenimento, cementacao, endurecimento superficial). O servidor verifica chama e
 * temperatura da fonte de calor logo abaixo (via {@link HeatSource}); condicao insuficiente recusa o
 * inicio e, durante o processo, simplesmente nao conta tempo: nenhuma habilidade compensa isso.
 */
public class HeatTreatmentFurnaceBlockEntity extends WorkpieceBlockEntity {
    /** Tratamentos que o jogador alterna no forno (a tempera acontece no tanque). */
    public static final List<TreatmentKind> SELECTABLE = List.of(TreatmentKind.ANNEALING, TreatmentKind.TEMPERING,
            TreatmentKind.CARBURIZING, TreatmentKind.SURFACE_HARDENING);
    private static final int MAX_CARBON = 4;
    private static final int TIMEOUT_FACTOR = 3;

    private TreatmentKind selected = TreatmentKind.ANNEALING;
    private int carbon;
    private boolean active;
    private int progress;
    private int elapsed;

    @Nullable
    private Metal metal;

    public HeatTreatmentFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_TREATMENT.get(), pos, state);
    }

    public TreatmentKind selected() {
        return selected;
    }

    public boolean isActive() {
        return active;
    }

    public void cycleSelected() {
        int next = (SELECTABLE.indexOf(selected) + 1) % SELECTABLE.size();
        selected = SELECTABLE.get(next);
        setChanged();
    }

    /** Guarda uma unidade de carbono (carvao). Retorna se foi aceita. */
    public boolean addCarbon() {
        if (carbon >= MAX_CARBON) {
            return false;
        }
        carbon++;
        setChanged();
        return true;
    }

    @Nullable
    private HeatSource heatSource() {
        return level != null && level.getBlockEntity(worldPosition.below()) instanceof HeatSource source ? source : null;
    }

    /**
     * Tenta iniciar o tratamento selecionado com a peca (que o chamador consome em caso de sucesso).
     *
     * @return mensagem de erro, ou {@code null} se o tratamento comecou
     */
    @Nullable
    public Component tryStart(ServerLevel serverLevel, ItemStack stack) {
        Optional<Holder.Reference<Metal>> found = MetalLookup.find(serverLevel.registryAccess(), stack);
        if (found.isEmpty()) {
            return Component.translatable("message.forjasuprema.minigame.not_metal");
        }
        Metal found1 = found.get().value();
        HeatSource source = heatSource();
        if (source == null) {
            return Component.translatable("message.forjasuprema.furnace.no_heat_source");
        }
        int flame = source.flameLevel();
        double temperature = source.temperature();
        TreatmentRules.Check check = TreatmentRules.checkStart(selected, TreatmentService.stateOf(stack),
                TreatmentService.treatmentIds(found1), flame, temperature, found1.workTempMin(), found1.meltTemperature());
        if (check != TreatmentRules.Check.OK) {
            return TreatmentMessages.of(check, selected, flame, temperature, found1.workTempMin(), found1.meltTemperature());
        }
        if (selected == TreatmentKind.CARBURIZING) {
            if (carbon <= 0) {
                return Component.translatable("message.forjasuprema.furnace.needs_carbon");
            }
            carbon--;
        }
        setPiece(stack.copyWithCount(1));
        metal = found1;
        active = true;
        progress = 0;
        elapsed = 0;
        setChanged();
        return null;
    }

    public void serverTick(ServerLevel serverLevel) {
        if (!active || !hasPiece()) {
            return;
        }
        if (metal == null) {
            metal = MetalLookup.find(serverLevel.registryAccess(), getPiece()).map(Holder::value).orElse(null);
            if (metal == null) {
                active = false;
                return;
            }
        }
        HeatSource source = heatSource();
        elapsed++;
        if (source != null) {
            boolean inBand = source.flameLevel() >= TreatmentRules.minFlame(selected)
                    && source.temperature() >= TreatmentRules.minTemperature(selected, metal.workTempMin())
                    && source.temperature() <= TreatmentRules.maxTemperature(selected, metal.workTempMin(), metal.meltTemperature());
            if (inBand) {
                progress++;
            }
        }
        int hold = TreatmentRules.holdTicks(selected);
        if (progress >= hold) {
            complete(serverLevel, source);
        } else if (elapsed > hold * TIMEOUT_FACTOR) {
            abort(serverLevel);
        } else if (elapsed % 20 == 0) {
            setChanged();
        }
    }

    private void complete(ServerLevel serverLevel, @Nullable HeatSource source) {
        double quality = TreatmentRules.holdQuality(progress, elapsed);
        ItemStack piece = getPiece();
        ItemStack result = TreatmentService.finish(piece, selected, metal, quality);
        // A peca sai na temperatura do forno.
        HeatData old = piece.get(ModDataComponents.HEAT.get());
        double temperature = source == null ? ThermalMath.AMBIENT : source.temperature();
        result.set(ModDataComponents.HEAT.get(), new HeatData(temperature, serverLevel.getGameTime(), old == null ? 0.0 : old.burn()));
        active = false;
        setPiece(result);
        serverLevel.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.5F);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                worldPosition.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.0);
    }

    /** Tempo esgotado sem manter as condicoes: o processo falha e a peca fica como estava. */
    private void abort(ServerLevel serverLevel) {
        active = false;
        setChanged();
        serverLevel.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.8F);
        serverLevel.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                worldPosition.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
    }

    @Override
    public ItemStack takePiece() {
        active = false;
        return super.takePiece();
    }

    public Component statusMessage() {
        int percent = active ? Math.min(100, progress * 100 / Math.max(1, TreatmentRules.holdTicks(selected))) : 0;
        return Component.translatable("message.forjasuprema.furnace.status",
                TreatmentMessages.name(selected), carbon, active ? percent : Component.translatable("message.forjasuprema.furnace.idle"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Selected", selected.id());
        tag.putInt("Carbon", carbon);
        tag.putBoolean("Active", active);
        tag.putInt("Progress", progress);
        tag.putInt("Elapsed", elapsed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selected = TreatmentKind.fromId(tag.getString("Selected")).filter(SELECTABLE::contains).orElse(TreatmentKind.ANNEALING);
        carbon = tag.getInt("Carbon");
        active = tag.getBoolean("Active");
        progress = tag.getInt("Progress");
        elapsed = tag.getInt("Elapsed");
        metal = null;
    }
}
