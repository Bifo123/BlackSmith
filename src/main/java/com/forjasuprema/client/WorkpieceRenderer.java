package com.forjasuprema.client;

import com.forjasuprema.station.WorkpieceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Desenha a peca apoiada, deitada sobre o topo da estacao. */
public class WorkpieceRenderer implements BlockEntityRenderer<WorkpieceBlockEntity> {
    private final ItemRenderer itemRenderer;

    public WorkpieceRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(WorkpieceBlockEntity station, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack piece = station.getPiece();
        if (piece.isEmpty() || station.getLevel() == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.02D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.7F, 0.7F, 0.7F);
        int light = LevelRenderer.getLightColor(station.getLevel(), station.getBlockPos().above());
        itemRenderer.renderStatic(piece, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, station.getLevel(), (int) station.getBlockPos().asLong());
        poseStack.popPose();
    }
}
