package com.forjasuprema.station;

import com.forjasuprema.finish.FinishToolItem;
import com.forjasuprema.minigame.MinigameKind;
import com.forjasuprema.minigame.MinigameManager;
import com.forjasuprema.minigame.ProcessingMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bancada de acabamento (polimento, afiacao ou gravacao). Sem a ferramenta certa na mao, apoia/recolhe
 * a peca como qualquer estacao; com ela, abre o minigame do tipo da bancada.
 */
public class FinishStationBlock extends WorkpieceStationBlock {
    private static final Codec<MinigameKind> KIND_CODEC = Codec.STRING.comapFlatMap(name -> {
        try {
            return DataResult.success(MinigameKind.valueOf(name));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "tipo de minigame desconhecido: " + name);
        }
    }, MinigameKind::name);

    public static final MapCodec<FinishStationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            KIND_CODEC.fieldOf("kind").forGetter(FinishStationBlock::kind),
            propertiesCodec()
    ).apply(instance, FinishStationBlock::new));

    private final MinigameKind kind;

    public FinishStationBlock(MinigameKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public MinigameKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FinishToolItem tool) || tool.kind() != kind) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level.getBlockEntity(pos) instanceof WorkpieceBlockEntity station) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                if (station.hasPiece()) {
                    MinigameManager.startFinishing(serverPlayer, (ServerLevel) level, pos, station, stack, hand,
                            kind, ProcessingMode.MANUAL);
                } else {
                    player.displayClientMessage(Component.translatable("message.forjasuprema.finish.empty"), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
