package com.forjasuprema.client;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.metal.Metal;
import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.thermal.HeatData;
import com.forjasuprema.thermal.PieceThermal;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Mostra temperatura, estado de trabalho e queima das pecas aquecidas. Calculado no cliente a partir do componente. */
@EventBusSubscriber(modid = ForjaSuprema.MOD_ID, value = Dist.CLIENT)
public final class HeatTooltip {
    private HeatTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        HeatData heat = stack.get(ModDataComponents.HEAT.get());
        Level level = Minecraft.getInstance().level;
        if (heat == null || level == null) {
            return;
        }

        double temperature = heat.temperatureAt(level.getGameTime());
        List<Component> tooltip = event.getToolTip();

        if (!PieceThermal.isCooled(temperature)) {
            int rounded = (int) Math.round(temperature);
            ChatFormatting color = ChatFormatting.GRAY;
            Component state = null;
            var metal = MetalLookup.find(level.registryAccess(), stack);
            if (metal.isPresent()) {
                switch (metal.get().value().workState(rounded)) {
                    case TOO_COLD -> {
                        color = ChatFormatting.AQUA;
                        state = Component.translatable("tooltip.forjasuprema.state.too_cold");
                    }
                    case WORKABLE -> {
                        color = ChatFormatting.GOLD;
                        state = Component.translatable("tooltip.forjasuprema.state.workable");
                    }
                    case OVERHEATED -> {
                        color = ChatFormatting.RED;
                        state = Component.translatable("tooltip.forjasuprema.state.overheated");
                    }
                    case MOLTEN -> {
                        color = ChatFormatting.DARK_RED;
                        state = Component.translatable("tooltip.forjasuprema.state.molten");
                    }
                }
            }
            tooltip.add(Component.translatable("tooltip.forjasuprema.temperature", rounded).withStyle(color));
            if (state != null) {
                tooltip.add(state.copy().withStyle(color));
            }
        }

        if (heat.burn() > 0.005) {
            tooltip.add(Component.translatable("tooltip.forjasuprema.burn",
                    (int) Math.round(heat.burn() * 100.0)).withStyle(ChatFormatting.DARK_RED));
        }
    }
}
