package com.forjasuprema.metal;

import com.forjasuprema.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Peca bruta de metal (chapa, barra, pregos). Um unico item serve a todos os metais: o metal fica no
 * componente {@code forjasuprema:metal_part}, que define o nome ("Chapa de Ferro") e a cor da textura.
 */
public class MetalPartItem extends Item {
    public MetalPartItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation metal = stack.get(ModDataComponents.METAL_PART.get());
        Component metalName = metal == null
                ? Component.translatable("metal.forjasuprema.unknown")
                : Component.translatableWithFallback("metal." + metal.getNamespace() + "." + metal.getPath(), metal.getPath());
        return Component.translatable(getDescriptionId(), metalName);
    }
}
