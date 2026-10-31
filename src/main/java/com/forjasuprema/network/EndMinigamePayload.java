package com.forjasuprema.network;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: a sessao terminou (motivo = ordinal de {@code MinigameEnd}); fecha a tela. */
public record EndMinigamePayload(int reason, float score) implements CustomPacketPayload {
    public static final Type<EndMinigamePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "end_minigame"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EndMinigamePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.reason);
                buf.writeFloat(p.score);
            },
            buf -> new EndMinigamePayload(buf.readVarInt(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
