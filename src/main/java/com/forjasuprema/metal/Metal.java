package com.forjasuprema.metal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Propriedades metalurgicas de um material. Definido por datapack no registro
 * {@code forjasuprema:metal}; o id do metal e o nome do arquivo JSON.
 *
 * <p>Escalas de 0.0 a 1.0: {@code hardness}, {@code strength}, {@code malleability},
 * {@code hardenability}. Temperaturas em graus Celsius.
 *
 * @param itemTag        tag de item cujos itens representam este metal (ex.: {@code c:ingots/iron})
 * @param hardness       dureza: influencia dano e desgaste
 * @param strength       resistencia: influencia durabilidade
 * @param malleability   quanto mais alto, mais facil de modelar
 * @param hardenability  capacidade de endurecer em tratamentos termicos (tempera, etc.)
 * @param meltTemperature temperatura de fusao
 * @param workTempMin    inicio da janela ideal de forjamento
 * @param workTempMax    fim da janela ideal de forjamento (acima disso ha superaquecimento)
 * @param tier           tier de material (0 comum ... 4 lendario); usado pelas regras de qualidade
 * @param treatments     ids de tratamentos compativeis (definidos na Fase 6)
 * @param properties     propriedades especiais (ids livres, interpretadas por fases futuras)
 */
public record Metal(
        TagKey<Item> itemTag,
        float hardness,
        float strength,
        float malleability,
        float hardenability,
        int meltTemperature,
        int workTempMin,
        int workTempMax,
        int tier,
        List<ResourceLocation> treatments,
        List<ResourceLocation> properties) {

    public static final Codec<Metal> CODEC = RecordCodecBuilder.<Metal>create(instance -> instance.group(
            TagKey.codec(Registries.ITEM).fieldOf("item_tag").forGetter(Metal::itemTag),
            Codec.floatRange(0.0F, 1.0F).fieldOf("hardness").forGetter(Metal::hardness),
            Codec.floatRange(0.0F, 1.0F).fieldOf("strength").forGetter(Metal::strength),
            Codec.floatRange(0.0F, 1.0F).fieldOf("malleability").forGetter(Metal::malleability),
            Codec.floatRange(0.0F, 1.0F).fieldOf("hardenability").forGetter(Metal::hardenability),
            Codec.intRange(1, 10_000).fieldOf("melt_temperature").forGetter(Metal::meltTemperature),
            Codec.intRange(0, 10_000).fieldOf("work_temp_min").forGetter(Metal::workTempMin),
            Codec.intRange(0, 10_000).fieldOf("work_temp_max").forGetter(Metal::workTempMax),
            Codec.intRange(0, 4).fieldOf("tier").forGetter(Metal::tier),
            ResourceLocation.CODEC.listOf().optionalFieldOf("treatments", List.of()).forGetter(Metal::treatments),
            ResourceLocation.CODEC.listOf().optionalFieldOf("properties", List.of()).forGetter(Metal::properties)
    ).apply(instance, Metal::new)).validate(Metal::validate);

    private static DataResult<Metal> validate(Metal metal) {
        if (metal.workTempMin() > metal.workTempMax()) {
            return DataResult.error(() -> "work_temp_min nao pode ser maior que work_temp_max");
        }
        if (metal.workTempMax() >= metal.meltTemperature()) {
            return DataResult.error(() -> "work_temp_max deve ser menor que melt_temperature");
        }
        return DataResult.success(metal);
    }

    /** Estado termico de uma peca deste metal em uma dada temperatura. */
    public WorkState workState(int temperature) {
        if (temperature >= meltTemperature) {
            return WorkState.MOLTEN;
        }
        if (temperature > workTempMax) {
            return WorkState.OVERHEATED;
        }
        if (temperature >= workTempMin) {
            return WorkState.WORKABLE;
        }
        return WorkState.TOO_COLD;
    }

    public enum WorkState {
        TOO_COLD,
        WORKABLE,
        OVERHEATED,
        MOLTEN
    }
}
