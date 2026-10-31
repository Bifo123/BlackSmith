package com.forjasuprema.quality;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Notas (0 a 1) de cada etapa de fabricacao de uma peca, guardadas no item (data component
 * {@code forjasuprema:stage_scores}). A Fase 8 combina estas notas na classificacao final de qualidade;
 * por enquanto elas ja sao gravadas pelas etapas existentes e mostradas no tooltip.
 */
public record StageScores(Map<String, Float> scores) {
    /** Precisao da modelagem (minigame de martelamento). */
    public static final String MODELING = "modeling";
    /** Controle termico: 1 menos a queima acumulada por superaquecimento. */
    public static final String THERMAL = "thermal";

    private static final int MAX_ENTRIES = 32;

    public static final Codec<StageScores> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).xmap(StageScores::new, StageScores::scores);

    public static final StreamCodec<ByteBuf, StageScores> STREAM_CODEC = StreamCodec.of(StageScores::write, StageScores::read);

    public StageScores {
        scores = Map.copyOf(scores);
    }

    private static void write(ByteBuf buf, StageScores value) {
        ByteBufCodecs.VAR_INT.encode(buf, value.scores().size());
        value.scores().forEach((stage, score) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, stage);
            ByteBufCodecs.FLOAT.encode(buf, score);
        });
    }

    private static StageScores read(ByteBuf buf) {
        int size = ByteBufCodecs.VAR_INT.decode(buf);
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("StageScores grande demais: " + size);
        }
        Map<String, Float> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String stage = ByteBufCodecs.STRING_UTF8.decode(buf);
            Float score = ByteBufCodecs.FLOAT.decode(buf);
            map.put(stage, score);
        }
        return new StageScores(map);
    }
}
