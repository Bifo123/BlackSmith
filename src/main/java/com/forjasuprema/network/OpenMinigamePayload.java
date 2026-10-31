package com.forjasuprema.network;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

/**
 * Servidor -> cliente: abre o minigame. O cliente reconstroi o padrao a partir da semente
 * ({@code PatternGenerator.generate}) e apenas desenha; o servidor decide todos os resultados.
 *
 * @param kind        ordinal de {@code MinigameKind}
 * @param seed        semente do padrao
 * @param strikes     quantidade de golpes
 * @param periodTicks periodo do marcador
 * @param halfWidth   meia-largura da zona de acerto (fracao da barra)
 * @param shape       formato sendo forjado (vazio para foles), so para exibicao
 */
public record OpenMinigamePayload(int kind, BlockPos pos, long seed, int strikes, int periodTicks,
                                  float halfWidth, String shape) implements CustomPacketPayload {
    public static final Type<OpenMinigamePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "open_minigame"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMinigamePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.kind);
                buf.writeBlockPos(p.pos);
                buf.writeLong(p.seed);
                buf.writeVarInt(p.strikes);
                buf.writeVarInt(p.periodTicks);
                buf.writeFloat(p.halfWidth);
                buf.writeUtf(p.shape, 64);
            },
            buf -> new OpenMinigamePayload(buf.readVarInt(), buf.readBlockPos(), buf.readLong(),
                    Math.max(1, Math.min(12, buf.readVarInt())), Math.max(2, Math.min(400, buf.readVarInt())),
                    buf.readFloat(), buf.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
