package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.finish.FinishToolItem;
import com.forjasuprema.finish.RuneItem;
import com.forjasuprema.metal.MetalPartItem;
import com.forjasuprema.minigame.MinigameKind;
import com.forjasuprema.minigame.SmithHammerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForjaSuprema.MOD_ID);

    /** Itens de bloco das estacoes, na ordem em que aparecem na aba criativa. */
    public static final List<DeferredItem<BlockItem>> STATION_ITEMS = new ArrayList<>();

    /** Martelo de ferreiro: ferramenta do minigame de martelamento (qualidade 0.3 reduz um pouco a dificuldade). */
    public static final DeferredItem<SmithHammerItem> SMITH_HAMMER =
            ITEMS.registerItem("smith_hammer", properties -> new SmithHammerItem(properties.durability(250), 0.3F));

    /** Foles: usados numa forja acesa, abrem o minigame que acelera o aquecimento. */
    public static final DeferredItem<Item> BELLOWS =
            ITEMS.registerItem("bellows", properties -> new Item(properties.durability(120).stacksTo(1)));

    /** Ferramentas de acabamento (Fase 7). */
    public static final DeferredItem<FinishToolItem> POLISHING_CLOTH = ITEMS.registerItem("polishing_cloth",
            properties -> new FinishToolItem(properties.durability(120), MinigameKind.POLISHING, 0.2F));
    public static final DeferredItem<FinishToolItem> METAL_FILE = ITEMS.registerItem("metal_file",
            properties -> new FinishToolItem(properties.durability(160), MinigameKind.SHARPENING, 0.2F));
    public static final DeferredItem<FinishToolItem> ENGRAVING_CHISEL = ITEMS.registerItem("engraving_chisel",
            properties -> new FinishToolItem(properties.durability(140), MinigameKind.ENGRAVING, 0.2F));
    /** Runa: o tipo vem do componente {@code rune}. */
    public static final DeferredItem<RuneItem> RUNE = ITEMS.registerItem("rune", RuneItem::new);

    /** Pecas forjadas na bigorna. O metal fica no componente {@code metal_part}. */
    public static final DeferredItem<MetalPartItem> METAL_PLATE = ITEMS.registerItem("metal_plate", MetalPartItem::new);
    public static final DeferredItem<MetalPartItem> METAL_ROD = ITEMS.registerItem("metal_rod", MetalPartItem::new);
    /** Duas pecas do mesmo metal soldadas por forjamento. */
    public static final DeferredItem<MetalPartItem> WELDED_BILLET = ITEMS.registerItem("welded_billet", MetalPartItem::new);
    public static final DeferredItem<MetalPartItem> METAL_NAILS = ITEMS.registerItem("metal_nails", MetalPartItem::new);

    public static final DeferredItem<BlockItem> TIN_ORE = ITEMS.registerSimpleBlockItem("tin_ore", ModBlocks.TIN_ORE);
    public static final DeferredItem<Item> RAW_TIN = ITEMS.registerSimpleItem("raw_tin");
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.registerSimpleItem("bronze_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    /** Ligas avancadas (Fase 6). */
    public static final DeferredItem<Item> DAMASCUS_STEEL_INGOT = ITEMS.registerSimpleItem("damascus_steel_ingot");
    public static final DeferredItem<Item> ROYAL_BRONZE_INGOT = ITEMS.registerSimpleItem("royal_bronze_ingot");
    /** Resto de uma peca que passou do ponto de fusao na forja. */
    public static final DeferredItem<Item> SLAG = ITEMS.registerSimpleItem("slag");

    static {
        station("apprentice_forge", ModBlocks.APPRENTICE_FORGE);
        station("blacksmith_forge", ModBlocks.BLACKSMITH_FORGE);
        station("advanced_forge", ModBlocks.ADVANCED_FORGE);
        station("master_forge", ModBlocks.MASTER_FORGE);
        station("legendary_forge", ModBlocks.LEGENDARY_FORGE);
        station("blacksmith_anvil", ModBlocks.BLACKSMITH_ANVIL);
        station("shaping_bench", ModBlocks.SHAPING_BENCH);
        station("quench_tank", ModBlocks.QUENCH_TANK);
        station("heat_treatment_furnace", ModBlocks.HEAT_TREATMENT_FURNACE);
        station("alloy_station", ModBlocks.ALLOY_STATION);
        station("polishing_station", ModBlocks.POLISHING_STATION);
        station("sharpening_stone", ModBlocks.SHARPENING_STONE);
        station("engraving_bench", ModBlocks.ENGRAVING_BENCH);
        station("finishing_station", ModBlocks.FINISHING_STATION);
    }

    private ModItems() {
    }

    private static void station(String name, DeferredBlock<?> block) {
        STATION_ITEMS.add(ITEMS.registerSimpleBlockItem(name, block));
    }
}
