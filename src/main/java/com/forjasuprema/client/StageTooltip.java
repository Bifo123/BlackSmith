package com.forjasuprema.client;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.quality.StageScores;
import com.forjasuprema.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Mostra as notas por etapa gravadas na peca. A classificacao final de qualidade chega na Fase 8. */
@EventBusSubscriber(modid = ForjaSuprema.MOD_ID, value = Dist.CLIENT)
public final class StageTooltip {
    private StageTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        StageScores scores = event.getItemStack().get(ModDataComponents.STAGE_SCORES.get());
        if (scores == null) {
            return;
        }
        scores.scores().forEach((stage, value) -> event.getToolTip().add(
                Component.translatable("tooltip.forjasuprema.stage." + stage, Math.round(value * 100))
                        .withStyle(ChatFormatting.GRAY)));
    }
}
