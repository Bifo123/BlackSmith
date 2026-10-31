package com.forjasuprema.network;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Cliente -> servidor: "bati". O instante e informado em centesimos de tick desde a abertura; o
 * servidor so o aceita dentro de uma tolerancia do proprio relogio (ver {@code StrikeSession}).
 */
public record StrikePayload(int index, long elapsed, int direction) implements CustomPacketPayload {
    public static final Type<StrikePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "strike"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StrikePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.index);
                buf.writeVarLong(p.elapsed);
                buf.writeVarInt(p.direction);
            },
            buf -> new StrikePayload(buf.readVarInt(), buf.readVarLong(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
