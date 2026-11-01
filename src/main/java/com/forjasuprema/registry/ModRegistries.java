package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.finish.Rune;
import com.forjasuprema.metal.Metal;
import com.forjasuprema.thermal.Fuel;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Registros dinamicos (datapack) do mod. Arquivos em {@code data/<ns>/forjasuprema/<registro>/}. */
public final class ModRegistries {
    public static final ResourceKey<Registry<Metal>> METAL = key("metal");
    public static final ResourceKey<Registry<Fuel>> FUEL = key("fuel");
    public static final ResourceKey<Registry<Rune>> RUNE = key("rune");

    private ModRegistries() {
    }

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, name));
    }

    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        // O mesmo codec e usado para sincronizar os dados com o cliente.
        event.dataPackRegistry(METAL, Metal.CODEC, Metal.CODEC);
        event.dataPackRegistry(FUEL, Fuel.CODEC, Fuel.CODEC);
        event.dataPackRegistry(RUNE, Rune.CODEC, Rune.CODEC);
    }
}
