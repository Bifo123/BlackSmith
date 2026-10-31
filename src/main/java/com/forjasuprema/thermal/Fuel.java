package com.forjasuprema.thermal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Combustivel de forja, definido por datapack no registro {@code forjasuprema:fuel}.
 *
 * @param itemTag        tag de item dos combustiveis que seguem estas propriedades
 * @param intensity      intensidade base da chama, de 1 (fraca) a 4 (intensa)
 * @param burnTime       duracao de cada item, em ticks
 * @param efficiency     eficiencia termica: multiplica a velocidade de aquecimento da forja
 * @param maxTemperature temperatura maxima alcancavel com este combustivel, em graus Celsius
 */
public record Fuel(TagKey<Item> itemTag, int intensity, int burnTime, double efficiency, int maxTemperature) {
    public static final Codec<Fuel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(Registries.ITEM).fieldOf("item_tag").forGetter(Fuel::itemTag),
            Codec.intRange(1, 4).fieldOf("intensity").forGetter(Fuel::intensity),
            Codec.intRange(1, 1_000_000).fieldOf("burn_time").forGetter(Fuel::burnTime),
            Codec.doubleRange(0.1, 3.0).fieldOf("efficiency").forGetter(Fuel::efficiency),
            Codec.intRange(30, 10_000).fieldOf("max_temperature").forGetter(Fuel::maxTemperature)
    ).apply(instance, Fuel::new));

    /** Energia total fornecida por item: tempo de queima ponderado pela eficiencia. */
    public int energy() {
        return (int) Math.round(burnTime * efficiency);
    }
}
