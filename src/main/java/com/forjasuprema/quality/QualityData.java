package com.forjasuprema.quality;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Classe e nota final de um equipamento, guardadas no item (componente {@code forjasuprema:quality}). */
public record QualityData(String tier, double score) {
    public static final Codec<QualityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("tier").forGetter(QualityData::tier),
            Codec.DOUBLE.fieldOf("score").forGetter(QualityData::score)
    ).apply(instance, QualityData::new));

    public static final StreamCodec<ByteBuf, QualityData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QualityData::tier,
            ByteBufCodecs.DOUBLE, QualityData::score,
            QualityData::new);

    public QualityTier tierOrCommon() {
        return QualityTier.fromId(tier).orElse(QualityTier.COMMON);
    }
}
