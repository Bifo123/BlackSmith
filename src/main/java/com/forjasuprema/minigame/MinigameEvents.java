package com.forjasuprema.minigame;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Liga o gerenciador de minigames aos eventos do servidor (validacao por tick, logout, parada). */
@EventBusSubscriber(modid = ForjaSuprema.MOD_ID)
public final class MinigameEvents {
    private MinigameEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinigameManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinigameManager.onLogout(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinigameManager.clearAll();
    }
}
