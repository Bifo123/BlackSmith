package com.forjasuprema.station;

import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.minigame.MinigameManager;
import com.forjasuprema.minigame.ProcessingMode;
import com.forjasuprema.registry.ModBlockEntities;
import com.forjasuprema.registry.ModItems;
import com.forjasuprema.thermal.ForgeTier;
import com.forjasuprema.thermal.FuelLookup;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Forja de uma das cinco categorias ({@link ForgeTier}). Interacoes:
 * <ul>
 *   <li>item de combustivel: abastece a forja;</li>
 *   <li>isqueiro ou bola de fogo: acende (precisa de combustivel);</li>
 *   <li>peca de metal: apoia sobre a forja para aquecer;</li>
 *   <li>mao vazia: retira a peca; agachado (ou sem peca) mostra o estado da forja.</li>
 * </ul>
 */
public class ForgeBlock extends WorkpieceStationBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final Codec<ForgeTier> TIER_CODEC = Codec.STRING.comapFlatMap(id -> {
        try {
            return DataResult.success(ForgeTier.byId(id));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }, ForgeTier::id);

    public static final MapCodec<ForgeBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TIER_CODEC.fieldOf("tier").forGetter(ForgeBlock::getTier),
            propertiesCodec()
    ).apply(instance, ForgeBlock::new));

    private final ForgeTier tier;

    public ForgeBlock(ForgeTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    public ForgeTier getTier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.FORGE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                ((ForgeBlockEntity) blockEntity).serverTick((ServerLevel) tickLevel);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ForgeBlockEntity forge)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
            if (!level.isClientSide) {
                switch (forge.ignite()) {
                    case OK -> {
                        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        if (stack.is(Items.FLINT_AND_STEEL)) {
                            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                        } else {
                            stack.consume(1, player);
                        }
                    }
                    case NO_FUEL -> player.displayClientMessage(
                            Component.translatable("message.forjasuprema.forge.no_fuel"), true);
                    case ALREADY_LIT -> player.displayClientMessage(
                            Component.translatable("message.forjasuprema.forge.already_lit"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(ModItems.BELLOWS.get())) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                MinigameManager.startBellows(serverPlayer, (ServerLevel) level, pos, forge, stack, hand,
                        ProcessingMode.MANUAL);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (FuelLookup.isFuel(level.registryAccess(), stack)) {
            if (!level.isClientSide) {
                int moved = forge.addFuel(stack);
                if (moved > 0) {
                    stack.consume(moved, player);
                    player.displayClientMessage(Component.translatable("message.forjasuprema.forge.fuel_added", moved), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.forjasuprema.forge.fuel_full"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!forge.hasPiece() && MetalLookup.find(level.registryAccess(), stack).isPresent()) {
            if (!level.isClientSide) {
                ItemStack placed = stack.copyWithCount(1);
                stack.consume(1, player);
                forge.setPiece(placed);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ForgeBlockEntity forge)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!player.isShiftKeyDown() && forge.hasPiece()) {
                player.getInventory().placeItemBackInInventory(forge.takePiece());
            } else {
                player.displayClientMessage(forge.statusMessage(), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ForgeBlockEntity forge) {
            forge.dropFuel();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.05, z, 0.0, 0.04, 0.0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 1.02, z, 0.0, 0.01, 0.0);
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F, false);
        }
    }
}
