package com.forjasuprema.client;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Cor de tingimento das pecas forjadas por metal. Metais de datapack ganham uma cor derivada do id. */
public final class MetalColors {
    private static final Map<String, Integer> KNOWN = Map.of(
            "iron", 0xC8C8C8,
            "copper", 0xC87137,
            "tin", 0xD9DDE0,
            "bronze", 0xB08D57,
            "steel", 0x8A9BA8,
            "gold", 0xFFD83D);

    private MetalColors() {
    }

    public static int of(ResourceLocation metal) {
        if (metal == null) {
            return 0xB0B0B0;
        }
        Integer known = KNOWN.get(metal.getPath());
        if (known != null) {
            return known;
        }
        int hash = metal.toString().hashCode();
        return 0x80 + ((hash >> 16) & 0x7F) << 16 | 0x80 + ((hash >> 8) & 0x7F) << 8 | 0x80 + (hash & 0x7F);
    }
}
