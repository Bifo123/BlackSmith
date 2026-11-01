package com.forjasuprema.craft;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Pecas sobre a bancada de montagem. */
public record AssemblyInput(List<ItemStack> parts) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return parts.get(index);
    }

    @Override
    public int size() {
        return parts.size();
    }
}
