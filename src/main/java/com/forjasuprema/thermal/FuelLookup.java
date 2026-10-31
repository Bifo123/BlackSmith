package com.forjasuprema.thermal;

import com.forjasuprema.registry.ModRegistries;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

/** Descobre se um item e combustivel de forja, pelas tags declaradas nos JSONs de combustivel. */
public final class FuelLookup {
    private FuelLookup() {
    }

    public static Optional<Holder.Reference<Fuel>> find(RegistryAccess access, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Registry<Fuel> registry = access.registryOrThrow(ModRegistries.FUEL);
        return registry.holders()
                .filter(holder -> stack.is(holder.value().itemTag()))
                .findFirst();
    }

    public static boolean isFuel(RegistryAccess access, ItemStack stack) {
        return find(access, stack).isPresent();
    }
}
