package com.forjasuprema.treatment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Codecs de {@link TreatmentState} (separados da classe pura para ela continuar testavel sem Minecraft). */
public final class TreatmentCodecs {
    public static final Codec<TreatmentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("hardness").forGetter(TreatmentState::hardness),
            Codec.DOUBLE.fieldOf("brittleness").forGetter(TreatmentState::brittleness),
            Codec.STRING.listOf().optionalFieldOf("applied", java.util.List.of()).forGetter(TreatmentState::applied)
    ).apply(instance, TreatmentState::new));

    public static final StreamCodec<ByteBuf, TreatmentState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, TreatmentState::hardness,
            ByteBufCodecs.DOUBLE, TreatmentState::brittleness,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), TreatmentState::applied,
            TreatmentState::new);

    private TreatmentCodecs() {
    }
}
