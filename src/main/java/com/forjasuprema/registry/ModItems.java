package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForjaSuprema.MOD_ID);

    /** Martelo de ferreiro. Na Fase 5 passa a ser a ferramenta do minigame de martelamento. */
    public static final DeferredItem<Item> SMITH_HAMMER =
            ITEMS.registerSimpleItem("smith_hammer", new Item.Properties().durability(250));

    private ModItems() {
    }
}
