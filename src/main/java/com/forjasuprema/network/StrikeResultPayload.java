package com.forjasuprema.network;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Servidor -> cliente: resultado (autoritativo) de um golpe, para o feedback visual. */
public record StrikeResultPayload(int index, int verdict, float accuracy) implements CustomPacketPayload {
    public static final Type<StrikeResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "strike_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StrikeResultPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.index);
                buf.writeVarInt(p.verdict);
                buf.writeFloat(p.accuracy);
            },
            buf -> new StrikeResultPayload(buf.readVarInt(), buf.readVarInt(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
