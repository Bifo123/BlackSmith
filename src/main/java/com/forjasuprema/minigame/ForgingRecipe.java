package com.forjasuprema.minigame;

import com.forjasuprema.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Receita de forjamento na bigorna: uma peca de metal aquecida vira o resultado depois de
 * {@code strikes} golpes. O {@code shape} (formato) e escolhido no martelo (agachar + usar).
 *
 * @param input      peca aceita (qualquer item que pertenca a um metal registrado)
 * @param result     item produzido; o metal de origem e gravado no componente {@code metal_part}
 * @param shape      identificador do formato (ex.: {@code plate}, {@code rod}, {@code nails})
 * @param strikes    quantidade de golpes do minigame
 * @param complexity complexidade da peca, de 0 a 1 (aumenta a dificuldade)
 */
public record ForgingRecipe(Ingredient input, ItemStack result, String shape, int strikes, double complexity)
        implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        return input.test(recipeInput.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput, HolderLookup.Provider registries) {
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
        return ModRecipes.FORGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FORGING.get();
    }

    public static class Serializer implements RecipeSerializer<ForgingRecipe> {
        private static final MapCodec<ForgingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ForgingRecipe::input),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ForgingRecipe::result),
                Codec.STRING.fieldOf("shape").forGetter(ForgingRecipe::shape),
                Codec.intRange(1, 12).fieldOf("strikes").forGetter(ForgingRecipe::strikes),
                Codec.doubleRange(0.0, 1.0).optionalFieldOf("complexity", 0.5).forGetter(ForgingRecipe::complexity)
        ).apply(instance, ForgingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ForgingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ForgingRecipe::input,
                ItemStack.STREAM_CODEC, ForgingRecipe::result,
                ByteBufCodecs.STRING_UTF8, ForgingRecipe::shape,
                ByteBufCodecs.VAR_INT, ForgingRecipe::strikes,
                ByteBufCodecs.DOUBLE, ForgingRecipe::complexity,
                ForgingRecipe::new
        );

        @Override
        public MapCodec<ForgingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ForgingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
