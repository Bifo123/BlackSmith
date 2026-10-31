package com.forjasuprema.network;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cliente -> servidor: o jogador fechou a tela e desistiu da sessao. */
public record CancelMinigamePayload() implements CustomPacketPayload {
    public static final Type<CancelMinigamePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ForjaSuprema.MOD_ID, "cancel_minigame"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CancelMinigamePayload> STREAM_CODEC =
            StreamCodec.unit(new CancelMinigamePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
