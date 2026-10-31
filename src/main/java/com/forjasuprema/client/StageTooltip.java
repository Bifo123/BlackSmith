package com.forjasuprema.client;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.quality.StageScores;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.treatment.TreatmentState;
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
        TreatmentState treatment = event.getItemStack().get(ModDataComponents.TREATMENT.get());
        if (treatment != null && !treatment.isUntreated()) {
            event.getToolTip().add(Component.translatable("tooltip.forjasuprema.treatment.hardness",
                    Math.round(treatment.hardness() * 100)).withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(Component.translatable("tooltip.forjasuprema.treatment.brittleness",
                    Math.round(treatment.brittleness() * 100)).withStyle(ChatFormatting.DARK_AQUA));
            if (!treatment.applied().isEmpty()) {
                event.getToolTip().add(Component.translatable("tooltip.forjasuprema.treatment.applied",
                        treatment.applied().stream()
                                .map(id -> Component.translatable("treatment.forjasuprema." + id).getString())
                                .collect(java.util.stream.Collectors.joining(" > "))).withStyle(ChatFormatting.GRAY));
            }
        }
        StageScores scores = event.getItemStack().get(ModDataComponents.STAGE_SCORES.get());
        if (scores == null) {
            return;
        }
        scores.scores().forEach((stage, value) -> event.getToolTip().add(
                Component.translatable("tooltip.forjasuprema.stage." + stage, Math.round(value * 100))
                        .withStyle(ChatFormatting.GRAY)));
    }
}
