package com.forjasuprema.finish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * Runa definida por datapack (registro {@code forjasuprema:rune}; o id e o nome do arquivo). A runa e
 * inscrita numa gravacao da peca; o efeito de jogo e aplicado pelo sistema de equipamentos (Fase 8).
 *
 * @param power      potencia base (0 a 1); a potencia final depende da nota da gravacao
 * @param minTier    tier minimo do metal da peca
 * @param minPolish  polimento minimo da peca (0 a 1)
 * @param effect     id do efeito/atributo que a Fase 8 aplicara (livre; datapacks externos podem definir os seus)
 */
public record Rune(float power, int minTier, float minPolish, ResourceLocation effect) {
    public static final Codec<Rune> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("power").forGetter(Rune::power),
            Codec.intRange(0, 4).optionalFieldOf("min_tier", 0).forGetter(Rune::minTier),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("min_polish", 0.0F).forGetter(Rune::minPolish),
            ResourceLocation.CODEC.fieldOf("effect").forGetter(Rune::effect)
    ).apply(instance, Rune::new));
}
