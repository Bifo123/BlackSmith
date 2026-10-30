package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.metal.Metal;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Registros dinamicos (datapack) do mod. Arquivos em {@code data/<ns>/forjasuprema/<registro>/}. */
public final class ModRegistries {
    public static final ResourceKey<Registry<Metal>> METAL =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "metal"));

    private ModRegistries() {
    }

    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        // O mesmo codec e usado para sincronizar os metais com o cliente.
        event.dataPackRegistry(METAL, Metal.CODEC, Metal.CODEC);
    }
}
