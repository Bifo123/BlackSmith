package com.forjasuprema.client;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.registry.ModBlockEntities;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModItems;
import com.forjasuprema.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Registros exclusivos do cliente. Esta classe so e carregada em {@link Dist#CLIENT}. */
@EventBusSubscriber(modid = ForjaSuprema.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WORKPIECE.get(), WorkpieceRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FORGE.get(), WorkpieceRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0
                        ? 0xFF000000 | MetalColors.of(stack.get(ModDataComponents.METAL_PART.get()))
                        : -1,
                ModItems.METAL_PLATE.get(), ModItems.METAL_ROD.get(), ModItems.METAL_NAILS.get());
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ALLOY_STATION.get(), AlloyStationScreen::new);
    }
}
