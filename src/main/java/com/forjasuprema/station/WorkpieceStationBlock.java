package com.forjasuprema.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Estacao que recebe uma peca por cima: clique com um item para apoiar, clique com a mao vazia
 * para recolher. Usada por forja, bigorna, bancadas, tanque, forno, polimento, amolar e acabamento.
 * Cada fase adiciona o comportamento proprio da estacao sobre a peca apoiada.
 */
public class WorkpieceStationBlock extends Block implements EntityBlock {
    public static final MapCodec<WorkpieceStationBlock> CODEC = simpleCodec(WorkpieceStationBlock::new);

    public WorkpieceStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorkpieceBlockEntity(pos, state);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof WorkpieceBlockEntity station)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (station.hasPiece()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            ItemStack placed = stack.copyWithCount(1);
            stack.consume(1, player);
            station.setPiece(placed);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WorkpieceBlockEntity station) || !station.hasPiece()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            player.getInventory().placeItemBackInInventory(station.takePiece());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WorkpieceBlockEntity station
                && station.hasPiece() && !level.isClientSide) {
            Block.popResource(level, pos, station.takePiece());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
