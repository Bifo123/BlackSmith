package com.forjasuprema.metal;

import com.forjasuprema.registry.ModRegistries;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

/** Descobre a que metal um item pertence, pelas tags declaradas nos JSONs de metal. */
public final class MetalLookup {
    private MetalLookup() {
    }

    public static Optional<Holder.Reference<Metal>> find(RegistryAccess access, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Registry<Metal> registry = access.registryOrThrow(ModRegistries.METAL);
        return registry.holders()
                .filter(holder -> stack.is(holder.value().itemTag()))
                .findFirst();
    }
}
