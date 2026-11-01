package com.forjasuprema.finish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Codecs de {@link FinishState} (separados da classe pura, que nao depende do Minecraft). */
public final class FinishCodecs {
    private static final Codec<FinishState.RuneMark> RUNE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(FinishState.RuneMark::id),
            Codec.DOUBLE.fieldOf("power").forGetter(FinishState.RuneMark::power)
    ).apply(instance, FinishState.RuneMark::new));

    public static final Codec<FinishState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("polish", 0.0).forGetter(FinishState::polish),
            Codec.DOUBLE.optionalFieldOf("sharpness", 0.0).forGetter(FinishState::sharpness),
            Codec.DOUBLE.listOf().optionalFieldOf("engravings", java.util.List.of()).forGetter(FinishState::engravings),
            RUNE_CODEC.listOf().optionalFieldOf("runes", java.util.List.of()).forGetter(FinishState::runes)
    ).apply(instance, FinishState::new));

    private static final StreamCodec<ByteBuf, FinishState.RuneMark> RUNE_STREAM = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FinishState.RuneMark::id,
            ByteBufCodecs.DOUBLE, FinishState.RuneMark::power,
            FinishState.RuneMark::new);

    public static final StreamCodec<ByteBuf, FinishState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, FinishState::polish,
            ByteBufCodecs.DOUBLE, FinishState::sharpness,
            ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list(8)), FinishState::engravings,
            RUNE_STREAM.apply(ByteBufCodecs.list(8)), FinishState::runes,
            FinishState::new);

    private FinishCodecs() {
    }
}
