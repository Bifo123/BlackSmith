package com.forjasuprema.client;

import com.forjasuprema.network.EndMinigamePayload;
import com.forjasuprema.network.OpenMinigamePayload;
import com.forjasuprema.network.StrikeResultPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Tratadores dos pacotes recebidos no cliente. So e executado em {@code Dist.CLIENT}. */
public final class ClientPayloads {
    private ClientPayloads() {
    }

    public static void onOpen(OpenMinigamePayload payload, IPayloadContext context) {
        Minecraft.getInstance().setScreen(new MinigameScreen(payload));
    }

    public static void onResult(StrikeResultPayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof MinigameScreen screen) {
            screen.onResult(payload);
        }
    }

    public static void onEnd(EndMinigamePayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof MinigameScreen screen) {
            screen.onEnd(payload);
        }
    }
}
