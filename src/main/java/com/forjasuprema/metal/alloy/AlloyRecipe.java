package com.forjasuprema.metal.alloy;

import com.forjasuprema.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Receita de liga: varios ingredientes (com quantidade) produzem um resultado em lotes.
 *
 * <p>{@code minFlame} (0-4) e {@code minTemperature} (graus Celsius) sao as condicoes termicas da
 * receita. Elas ja fazem parte do formato de dados, mas so passam a ser verificadas quando o
 * sistema termico existir (Fase 4); ate la, quem usa a receita nao aplica essa checagem.
 */
public record AlloyRecipe(List<Entry> inputs, ItemStack result, int minFlame, int minTemperature)
        implements Recipe<AlloyInput> {

    public record Entry(Ingredient ingredient, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Entry::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Entry::count)
        ).apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Entry::ingredient,
                ByteBufCodecs.VAR_INT, Entry::count,
                Entry::new
        );
    }

    /** Quantos lotes completos a receita consegue produzir com o conteudo informado. */
    public int maxBatches(AlloyInput input) {
        int batches = Integer.MAX_VALUE;
        for (Entry entry : inputs) {
            int available = 0;
            for (ItemStack stack : input.items()) {
                if (!stack.isEmpty() && entry.ingredient().test(stack)) {
                    available += stack.getCount();
                }
            }
            batches = Math.min(batches, available / entry.count());
        }
        return batches == Integer.MAX_VALUE ? 0 : batches;
    }

    @Override
    public boolean matches(AlloyInput input, Level level) {
        return maxBatches(input) >= 1;
    }

    @Override
    public ItemStack assemble(AlloyInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ALLOYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ALLOYING.get();
    }

    public static class Serializer implements RecipeSerializer<AlloyRecipe> {
        private static final MapCodec<AlloyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.nonEmptyList(Entry.CODEC.listOf()).fieldOf("inputs").forGetter(AlloyRecipe::inputs),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AlloyRecipe::result),
                Codec.intRange(0, 4).optionalFieldOf("min_flame", 0).forGetter(AlloyRecipe::minFlame),
                Codec.intRange(0, 10_000).optionalFieldOf("min_temperature", 0).forGetter(AlloyRecipe::minTemperature)
        ).apply(instance, AlloyRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> STREAM_CODEC = StreamCodec.composite(
                Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), AlloyRecipe::inputs,
                ItemStack.STREAM_CODEC, AlloyRecipe::result,
                ByteBufCodecs.VAR_INT, AlloyRecipe::minFlame,
                ByteBufCodecs.VAR_INT, AlloyRecipe::minTemperature,
                AlloyRecipe::new
        );

        @Override
        public MapCodec<AlloyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
