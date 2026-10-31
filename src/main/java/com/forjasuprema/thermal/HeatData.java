package com.forjasuprema.thermal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Estado termico de uma peca de metal, guardado no ItemStack (data component {@code forjasuprema:heat}).
 *
 * <p>A temperatura nunca e atualizada por tick: guarda-se o valor e o instante em que foi medido, e
 * o valor atual e calculado sob demanda por {@link #temperatureAt(long)}. Assim pecas em baus ou
 * no inventario esfriam corretamente sem custo de processamento e sem sincronizacao.
 *
 * @param temperature temperatura, em graus Celsius, no instante {@code gameTime}
 * @param gameTime    tempo de jogo (ticks) da medicao
 * @param burn        dano acumulado por superaquecimento, de 0.0 a 1.0 (usado na qualidade, Fase 8)
 */
public record HeatData(double temperature, long gameTime, double burn) {
    public static final Codec<HeatData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("temperature").forGetter(HeatData::temperature),
            Codec.LONG.fieldOf("time").forGetter(HeatData::gameTime),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("burn", 0.0).forGetter(HeatData::burn)
    ).apply(instance, HeatData::new));

    public static final StreamCodec<ByteBuf, HeatData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, HeatData::temperature,
            ByteBufCodecs.VAR_LONG, HeatData::gameTime,
            ByteBufCodecs.DOUBLE, HeatData::burn,
            HeatData::new
    );

    /** Temperatura da peca no tempo de jogo {@code now}, considerando o resfriamento ao ar. */
    public double temperatureAt(long now) {
        return ThermalMath.cooledAfter(temperature, ThermalMath.AMBIENT, now - gameTime, PieceThermal.COOLING_RATE);
    }

    /** Dados sem informacao util (peca fria e sem queima) podem ser descartados. */
    public boolean isIrrelevantAt(long now) {
        return burn <= 0.0 && PieceThermal.isCooled(temperatureAt(now));
    }
}
