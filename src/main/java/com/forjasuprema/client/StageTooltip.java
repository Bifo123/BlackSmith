package com.forjasuprema.client;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.quality.StageScores;
import com.forjasuprema.finish.FinishState;
import com.forjasuprema.finish.RuneItem;
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
        FinishState finish = event.getItemStack().get(ModDataComponents.FINISH.get());
        if (finish != null && !finish.isUntouched()) {
            if (finish.polish() > 0.0) {
                event.getToolTip().add(Component.translatable("tooltip.forjasuprema.finish.polish",
                        Math.round(finish.polish() * 100)).withStyle(ChatFormatting.WHITE));
            }
            if (finish.sharpness() > 0.0) {
                event.getToolTip().add(Component.translatable("tooltip.forjasuprema.finish.sharpness",
                        Math.round(finish.sharpness() * 100)).withStyle(ChatFormatting.WHITE));
            }
            if (!finish.engravings().isEmpty()) {
                event.getToolTip().add(Component.translatable("tooltip.forjasuprema.finish.engravings",
                        finish.runes().size(), finish.engravings().size()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            finish.runes().forEach(mark -> event.getToolTip().add(Component.translatable("tooltip.forjasuprema.finish.rune",
                    RuneItem.runeName(net.minecraft.resources.ResourceLocation.parse(mark.id())),
                    Math.round(mark.power() * 100)).withStyle(ChatFormatting.LIGHT_PURPLE)));
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
