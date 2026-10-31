package com.forjasuprema.minigame;

import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModRecipes;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * Martelo de ferreiro: ferramenta do minigame de martelamento. Clicar com ele na bigorna inicia o
 * martelamento; agachar + usar troca o formato a forjar (chapa, barra, pregos...). A qualidade do martelo
 * reduz a dificuldade do minigame (martelos melhores entram em fases futuras).
 */
public class SmithHammerItem extends Item {
    private final float quality;

    /** @param quality qualidade da ferramenta, de 0 a 1 */
    public SmithHammerItem(Properties properties, float quality) {
        super(properties);
        this.quality = quality;
    }

    public float quality() {
        return quality;
    }

    /** Formatos disponiveis, na ordem alfabetica, a partir das receitas de forjamento carregadas. */
    public static List<String> shapesOf(List<RecipeHolder<ForgingRecipe>> recipes) {
        return recipes.stream().map(holder -> holder.value().shape()).distinct().sorted().toList();
    }

    /** Formato selecionado no martelo; se nao houver (ou for invalido), o primeiro da lista. */
    public static String currentShape(ItemStack hammer, List<String> shapes) {
        String selected = hammer.get(ModDataComponents.HAMMER_SHAPE.get());
        if (selected != null && shapes.contains(selected)) {
            return selected;
        }
        return shapes.isEmpty() ? "" : shapes.get(0);
    }

    public static Component shapeName(String shape) {
        return Component.translatableWithFallback("shape.forjasuprema." + shape, shape);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            cycleShape(level, player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide) {
            cycleShape(level, player, context.getItemInHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void cycleShape(Level level, Player player, ItemStack stack) {
        List<String> shapes = shapesOf(level.getRecipeManager().getAllRecipesFor(ModRecipes.FORGING.get()));
        if (shapes.isEmpty()) {
            return;
        }
        String current = currentShape(stack, shapes);
        String next = shapes.get((shapes.indexOf(current) + 1) % shapes.size());
        stack.set(ModDataComponents.HAMMER_SHAPE.get(), next);
        player.displayClientMessage(Component.translatable("message.forjasuprema.hammer.shape", shapeName(next)), true);
    }
}
