package com.forjasuprema.metal.alloy;

import com.forjasuprema.registry.ModRecipes;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * Logica de ligas independente de interface: a estacao da Fase 3 e as maquinas da Fase 10 reutilizam
 * {@link #find}; o consumo de itens acontece sempre no servidor.
 */
public final class AlloyService {
    /** Limite de lotes por operacao, para nao gerar pilhas absurdas. */
    public static final int MAX_BATCHES = 16;

    private AlloyService() {
    }

    public static Optional<RecipeHolder<AlloyRecipe>> find(Level level, AlloyInput input) {
        return level.getRecipeManager().getRecipeFor(ModRecipes.ALLOYING.get(), input, level);
    }

    /**
     * Consome ingredientes do inventario do jogador e entrega o resultado.
     *
     * @return quantidade de lotes produzidos (0 se nao foi possivel)
     */
    public static int craftFromInventory(ServerPlayer player, AlloyRecipe recipe) {
        Inventory inventory = player.getInventory();
        ItemStack result = recipe.result();

        int batches = recipe.maxBatches(new AlloyInput(inventory.items));
        batches = Math.min(batches, MAX_BATCHES);
        batches = Math.min(batches, result.getMaxStackSize() / Math.max(1, result.getCount()));
        if (batches <= 0) {
            return 0;
        }

        for (AlloyRecipe.Entry entry : recipe.inputs()) {
            int remaining = entry.count() * batches;
            for (ItemStack stack : inventory.items) {
                if (remaining <= 0) {
                    break;
                }
                if (!stack.isEmpty() && entry.ingredient().test(stack)) {
                    int taken = Math.min(remaining, stack.getCount());
                    stack.shrink(taken);
                    remaining -= taken;
                }
            }
        }

        inventory.placeItemBackInInventory(result.copyWithCount(result.getCount() * batches));
        return batches;
    }
}
