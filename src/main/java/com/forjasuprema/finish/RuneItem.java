package com.forjasuprema.finish;

import com.forjasuprema.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Item de runa. A runa especifica fica no componente {@code forjasuprema:rune}. */
public class RuneItem extends Item {
    public RuneItem(Properties properties) {
        super(properties);
    }

    public static Component runeName(ResourceLocation id) {
        return Component.translatableWithFallback("rune." + id.getNamespace() + "." + id.getPath(), id.getPath());
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = stack.get(ModDataComponents.RUNE.get());
        return id == null ? super.getName(stack) : Component.translatable(getDescriptionId(), runeName(id));
    }
}
