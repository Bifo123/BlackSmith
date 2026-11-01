package com.forjasuprema.craft;

import com.forjasuprema.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Receita de montagem de equipamento na Bancada de Modelagem: as pecas (por formato) viram um item cujo
 * tipo depende do metal. O resultado e dinamico (atributos vem da qualidade), por isso {@code assemble}
 * nao e usado: quem monta e o {@link AssemblyService}.
 *
 * @param parts           pecas exigidas, por formato (a bancada precisa conter exatamente isto)
 * @param results         metal (id) -> item resultante; metais ausentes nao fazem este equipamento
 * @param attackDamage    dano base (antes de material, classe e fio)
 * @param attackSpeed     velocidade de ataque
 * @param durability      durabilidade base
 * @param legendaryCapable se a receita admite a classe Lendario
 * @param weights         peso de cada etapa no calculo de qualidade
 */
public record AssemblyRecipe(List<PartRequirement> parts, Map<ResourceLocation, ResourceLocation> results,
                             double attackDamage, double attackSpeed, int durability, boolean legendaryCapable,
                             Map<String, Double> weights) implements Recipe<AssemblyInput> {

    public record PartRequirement(String shape, int count) {
        public static final Codec<PartRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("shape").forGetter(PartRequirement::shape),
                Codec.intRange(1, 4).fieldOf("count").forGetter(PartRequirement::count)
        ).apply(instance, PartRequirement::new));
    }

    @Override
    public boolean matches(AssemblyInput input, Level level) {
        Map<String, Integer> have = new HashMap<>();
        for (ItemStack part : input.parts()) {
            String shape = AssemblyService.shapeOf(part);
            if (shape == null) {
                return false;
            }
            have.merge(shape, 1, Integer::sum);
        }
        Map<String, Integer> need = new HashMap<>();
        parts.forEach(req -> need.merge(req.shape(), req.count(), Integer::sum));
        return have.equals(need);
    }

    @Override
    public ItemStack assemble(AssemblyInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /** Montagem e feita na bancada, nao no livro de receitas. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ASSEMBLY.get();
    }

    public static class Serializer implements RecipeSerializer<AssemblyRecipe> {
        private static final MapCodec<AssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PartRequirement.CODEC.listOf().fieldOf("parts").forGetter(AssemblyRecipe::parts),
                Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).fieldOf("results").forGetter(AssemblyRecipe::results),
                Codec.DOUBLE.fieldOf("attack_damage").forGetter(AssemblyRecipe::attackDamage),
                Codec.DOUBLE.fieldOf("attack_speed").forGetter(AssemblyRecipe::attackSpeed),
                Codec.intRange(1, 100_000).fieldOf("durability").forGetter(AssemblyRecipe::durability),
                Codec.BOOL.optionalFieldOf("legendary_capable", false).forGetter(AssemblyRecipe::legendaryCapable),
                Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("weights").forGetter(AssemblyRecipe::weights)
        ).apply(instance, AssemblyRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

        @Override
        public MapCodec<AssemblyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
