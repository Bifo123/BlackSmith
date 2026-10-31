package com.forjasuprema.metal;

import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModRegistries;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Descobre a que metal um item pertence: pelo componente {@code metal_part} (chapas, barras, pregos
 * forjados) ou, para lingotes e afins, pelas tags declaradas nos JSONs de metal.
 */
public final class MetalLookup {
    private MetalLookup() {
    }

    public static Optional<Holder.Reference<Metal>> find(RegistryAccess access, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Registry<Metal> registry = access.registryOrThrow(ModRegistries.METAL);
        ResourceLocation part = stack.get(ModDataComponents.METAL_PART.get());
        if (part != null) {
            return registry.getHolder(ResourceKey.create(ModRegistries.METAL, part));
        }
        return registry.holders()
                .filter(holder -> stack.is(holder.value().itemTag()))
                .findFirst();
    }
}
