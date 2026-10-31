package com.forjasuprema.station;

import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Forno de tratamento termico. Interacoes: agachado com a mao vazia alterna o tratamento; carvao/carvao
 * vegetal abastece carbono (cementacao); uma peca de metal inicia o tratamento selecionado (se a chama e a
 * temperatura da forja de baixo permitirem); mao vazia recolhe a peca.
 */
public class HeatTreatmentFurnaceBlock extends WorkpieceStationBlock {
    public static final MapCodec<HeatTreatmentFurnaceBlock> CODEC = simpleCodec(HeatTreatmentFurnaceBlock::new);

    public HeatTreatmentFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatTreatmentFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.HEAT_TREATMENT.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                ((HeatTreatmentFurnaceBlockEntity) blockEntity).serverTick((ServerLevel) tickLevel);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HeatTreatmentFurnaceBlockEntity furnace)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ItemTags.COALS)) {
            if (!level.isClientSide && furnace.addCarbon()) {
                stack.consume(1, player);
                player.displayClientMessage(furnace.statusMessage(), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!furnace.hasPiece() && MetalLookup.find(level.registryAccess(), stack).isPresent()) {
            if (!level.isClientSide) {
                Component error = furnace.tryStart((ServerLevel) level, stack);
                if (error == null) {
                    stack.consume(1, player);
                    player.displayClientMessage(furnace.statusMessage(), true);
                } else {
                    player.displayClientMessage(error, true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HeatTreatmentFurnaceBlockEntity furnace)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown() && !furnace.hasPiece()) {
                furnace.cycleSelected();
                player.displayClientMessage(furnace.statusMessage(), true);
            } else if (furnace.hasPiece() && !player.isShiftKeyDown()) {
                player.getInventory().placeItemBackInInventory(furnace.takePiece());
            } else {
                player.displayClientMessage(furnace.statusMessage(), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
