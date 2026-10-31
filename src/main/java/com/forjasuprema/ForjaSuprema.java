package com.forjasuprema;

import com.forjasuprema.command.ForjaCommands;
import com.forjasuprema.compat.CompatManager;
import com.forjasuprema.config.ForjaConfig;
import com.forjasuprema.registry.ModBlockEntities;
import com.forjasuprema.registry.ModBlocks;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModMenus;
import com.forjasuprema.registry.ModCreativeTabs;
import com.forjasuprema.registry.ModItems;
import com.forjasuprema.registry.ModRecipes;
import com.forjasuprema.registry.ModRegistries;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Ponto de entrada do Forja Suprema.
 *
 * <p>Responsabilidade unica: ligar registros, configuracao e o gerenciador de compatibilidade.
 * Nenhuma regra de jogo vive aqui.
 */
@Mod(ForjaSuprema.MOD_ID)
public class ForjaSuprema {
    public static final String MOD_ID = "forjasuprema";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ForjaSuprema(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);

        modEventBus.addListener(ModRegistries::registerDatapackRegistries);
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.addListener(ForjaCommands::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, ForjaConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CompatManager::init);
    }
}
