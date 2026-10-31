package com.forjasuprema.station;

import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.treatment.TreatmentService;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Tanque de tempera. Encha com um balde de agua; depois, clique com uma peca de metal quente para tempera-la
 * na hora. Se a peca estiver abaixo da temperatura minima (ou o metal nao admitir tempera), nada acontece.
 */
public class QuenchTankBlock extends Block implements EntityBlock {
    public static final MapCodec<QuenchTankBlock> CODEC = simpleCodec(QuenchTankBlock::new);

    public QuenchTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuenchTankBlockEntity(pos, state);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof QuenchTankBlockEntity tank)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide && !tank.isFilled()) {
                tank.setFilled(true);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (MetalLookup.find(level.registryAccess(), stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (!tank.isFilled()) {
                player.displayClientMessage(Component.translatable("message.forjasuprema.quench.no_water"), true);
            } else {
                ServerLevel serverLevel = (ServerLevel) level;
                TreatmentService.Outcome outcome = TreatmentService.quench(level.registryAccess(),
                        stack, level.getGameTime());
                if (!outcome.ok()) {
                    player.displayClientMessage(outcome.message(), true);
                } else {
                    stack.consume(1, player);
                    player.getInventory().placeItemBackInInventory(outcome.result());
                    level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            10, 0.25, 0.1, 0.25, 0.02);
                    player.displayClientMessage(Component.translatable("message.forjasuprema.quench.done",
                            Math.round(outcome.quality() * 100)), true);
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof QuenchTankBlockEntity tank) {
            player.displayClientMessage(Component.translatable(tank.isFilled()
                    ? "message.forjasuprema.quench.filled" : "message.forjasuprema.quench.empty"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
