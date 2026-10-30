package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForjaSuprema.MOD_ID);

    /** Martelo de ferreiro. Na Fase 5 passa a ser a ferramenta do minigame de martelamento. */
    public static final DeferredItem<Item> SMITH_HAMMER =
            ITEMS.registerSimpleItem("smith_hammer", new Item.Properties().durability(250));

    public static final DeferredItem<BlockItem> TIN_ORE = ITEMS.registerSimpleBlockItem("tin_ore", ModBlocks.TIN_ORE);
    public static final DeferredItem<Item> RAW_TIN = ITEMS.registerSimpleItem("raw_tin");
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.registerSimpleItem("bronze_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");

    private ModItems() {
    }
}
