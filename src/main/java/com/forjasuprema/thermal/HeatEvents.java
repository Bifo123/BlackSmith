package com.forjasuprema.thermal;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.registry.ModDataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Limpeza periodica: remove o componente de calor de pecas que ja esfriaram e nao tem queima, para
 * que voltem a empilhar com lingotes comuns. Roda no servidor a cada 2 s, apenas no inventario do jogador.
 */
@EventBusSubscriber(modid = ForjaSuprema.MOD_ID)
public final class HeatEvents {
    private static final int INTERVAL_TICKS = 40;

    private HeatEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % INTERVAL_TICKS != 0) {
            return;
        }
        long now = player.level().getGameTime();
        player.getInventory().items.forEach(stack -> clean(stack, now));
        player.getInventory().offhand.forEach(stack -> clean(stack, now));
    }

    private static void clean(ItemStack stack, long now) {
        if (stack.isEmpty()) {
            return;
        }
        HeatData heat = stack.get(ModDataComponents.HEAT.get());
        if (heat != null && heat.isIrrelevantAt(now)) {
            stack.remove(ModDataComponents.HEAT.get());
        }
    }
}
