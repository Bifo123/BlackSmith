package com.forjasuprema.station;

import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.minigame.MinigameManager;
import com.forjasuprema.minigame.ProcessingMode;
import com.forjasuprema.minigame.SmithHammerItem;
import com.forjasuprema.treatment.TreatmentService;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bigorna do ferreiro. Sem martelo, comporta-se como qualquer estacao (apoia e recolhe a peca). Com o
 * martelo na mao e uma peca apoiada, abre o minigame de martelamento (agachado, o martelo troca o formato).
 */
public class AnvilStationBlock extends WorkpieceStationBlock {
    public static final MapCodec<AnvilStationBlock> CODEC = simpleCodec(AnvilStationBlock::new);

    public AnvilStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof SmithHammerItem) && !player.isShiftKeyDown()
                && level.getBlockEntity(pos) instanceof WorkpieceBlockEntity target && target.hasPiece()
                && MetalLookup.find(level.registryAccess(), stack).isPresent()) {
            if (!level.isClientSide) {
                TreatmentService.Outcome outcome = TreatmentService.weld(level.registryAccess(), target.getPiece(),
                        stack, level.getGameTime());
                if (!outcome.ok()) {
                    player.displayClientMessage(outcome.message(), true);
                } else {
                    stack.consume(1, player);
                    target.setPiece(outcome.result());
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.7F);
                    player.displayClientMessage(Component.translatable("message.forjasuprema.weld.done",
                            Math.round(outcome.quality() * 100)), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(stack.getItem() instanceof SmithHammerItem) || player.isShiftKeyDown()) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level.getBlockEntity(pos) instanceof WorkpieceBlockEntity station) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                if (station.hasPiece()) {
                    MinigameManager.startHammering(serverPlayer, (ServerLevel) level, pos, station, stack, hand,
                            ProcessingMode.MANUAL);
                } else {
                    player.displayClientMessage(Component.translatable("message.forjasuprema.anvil.empty"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
