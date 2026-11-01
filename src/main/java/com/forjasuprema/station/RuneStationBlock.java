package com.forjasuprema.station;

import com.forjasuprema.finish.FinishRules;
import com.forjasuprema.finish.FinishState;
import com.forjasuprema.finish.Rune;
import com.forjasuprema.finish.RuneItem;
import com.forjasuprema.metal.Metal;
import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModRegistries;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
 * Estacao de acabamento: inscreve uma runa numa gravacao livre da peca apoiada. Tudo e verificado no servidor
 * (gravacao livre, tier do metal, polimento, runa repetida) e a potencia depende da nota da gravacao.
 */
public class RuneStationBlock extends WorkpieceStationBlock {
    public static final MapCodec<RuneStationBlock> CODEC = simpleCodec(RuneStationBlock::new);

    public RuneStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof RuneItem)
                || !(level.getBlockEntity(pos) instanceof WorkpieceBlockEntity station)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide) {
            Component error = inscribe(level, station, stack);
            if (error != null) {
                player.displayClientMessage(error, true);
            } else {
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Retorna mensagem de erro, ou {@code null} se a runa foi inscrita. */
    private static Component inscribe(Level level, WorkpieceBlockEntity station, ItemStack runeStack) {
        ResourceLocation runeId = runeStack.get(ModDataComponents.RUNE.get());
        if (runeId == null) {
            return Component.translatable("message.forjasuprema.rune.invalid");
        }
        Optional<Holder.Reference<Rune>> rune = level.registryAccess().registryOrThrow(ModRegistries.RUNE)
                .getHolder(ResourceKey.create(ModRegistries.RUNE, runeId));
        if (rune.isEmpty()) {
            return Component.translatable("message.forjasuprema.rune.invalid");
        }
        ItemStack piece = station.getPiece();
        if (piece.isEmpty()) {
            return Component.translatable("message.forjasuprema.finish.empty");
        }
        Optional<Holder.Reference<Metal>> metal = MetalLookup.find(level.registryAccess(), piece);
        if (metal.isEmpty() || piece.get(ModDataComponents.METAL_PART.get()) == null) {
            return Component.translatable("message.forjasuprema.finish.needs_part");
        }
        FinishState current = piece.getOrDefault(ModDataComponents.FINISH.get(), FinishState.NONE);
        Rune data = rune.get().value();
        FinishRules.Check check = FinishRules.checkRune(current, runeId.toString(), metal.get().value().tier(),
                data.minTier(), data.minPolish());
        if (check != FinishRules.Check.OK) {
            return Component.translatable("message.forjasuprema.rune." + check.name().toLowerCase(java.util.Locale.ROOT),
                    RuneItem.runeName(runeId), data.minTier(), Math.round(data.minPolish() * 100));
        }
        ItemStack result = piece.copy();
        result.set(ModDataComponents.FINISH.get(), FinishRules.inscribe(current, runeId.toString(), data.power()));
        station.setPiece(result);
        return null;
    }
}
