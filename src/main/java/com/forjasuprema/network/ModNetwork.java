package com.forjasuprema.network;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.client.ClientPayloads;
import com.forjasuprema.minigame.MinigameManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registro dos pacotes do mod. Os pacotes do cliente para o servidor so descrevem intencoes ("bati",
 * "desisti"); nenhum resultado de fabricacao vem do cliente.
 */
@EventBusSubscriber(modid = ForjaSuprema.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(OpenMinigamePayload.TYPE, OpenMinigamePayload.STREAM_CODEC, ClientPayloads::onOpen);
        registrar.playToClient(StrikeResultPayload.TYPE, StrikeResultPayload.STREAM_CODEC, ClientPayloads::onResult);
        registrar.playToClient(EndMinigamePayload.TYPE, EndMinigamePayload.STREAM_CODEC, ClientPayloads::onEnd);
        registrar.playToServer(StrikePayload.TYPE, StrikePayload.STREAM_CODEC, MinigameManager::onStrike);
        registrar.playToServer(CancelMinigamePayload.TYPE, CancelMinigamePayload.STREAM_CODEC, MinigameManager::onCancel);
    }
}
