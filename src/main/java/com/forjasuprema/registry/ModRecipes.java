package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.metal.alloy.AlloyRecipe;
import com.forjasuprema.minigame.ForgingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ForjaSuprema.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ForjaSuprema.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyRecipe>> ALLOYING = TYPES.register("alloying",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "alloying")));

    public static final DeferredHolder<RecipeSerializer<?>, AlloyRecipe.Serializer> ALLOYING_SERIALIZER =
            SERIALIZERS.register("alloying", AlloyRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ForgingRecipe>> FORGING = TYPES.register("forging",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "forging")));

    public static final DeferredHolder<RecipeSerializer<?>, ForgingRecipe.Serializer> FORGING_SERIALIZER =
            SERIALIZERS.register("forging", ForgingRecipe.Serializer::new);

    private ModRecipes() {
    }
}
