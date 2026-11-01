package com.forjasuprema.station;

import com.forjasuprema.craft.AssemblyRecipe;
import com.forjasuprema.craft.AssemblyService;
import com.forjasuprema.minigame.SmithHammerItem;
import com.forjasuprema.quality.QualityTier;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Bancada de Modelagem: aqui as pecas forjadas (chapa, barra, pregos) viram equipamento. Clique com uma peca
 * para apoia-la (todas do mesmo metal), com o martelo para montar, com a mao vazia para recolher a ultima
 * (agachado, mostra o conteudo). A qualidade e calculada no servidor a partir das notas gravadas nas pecas.
 */
public class ShapingBenchBlock extends Block implements EntityBlock {
    public static final MapCodec<ShapingBenchBlock> CODEC = simpleCodec(ShapingBenchBlock::new);

    public ShapingBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShapingBenchBlockEntity(pos, state);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ShapingBenchBlockEntity bench)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (AssemblyService.shapeOf(stack) != null) {
            if (!level.isClientSide) {
                ResourceLocation metal = stack.get(ModDataComponents.METAL_PART.get());
                if (bench.isFull()) {
                    player.displayClientMessage(Component.translatable("message.forjasuprema.bench.full"), true);
                } else if (!bench.parts().isEmpty()
                        && !java.util.Objects.equals(metal, bench.parts().get(0).get(ModDataComponents.METAL_PART.get()))) {
                    player.displayClientMessage(Component.translatable("message.forjasuprema.bench.mixed_metals"), true);
                } else {
                    bench.add(stack.copyWithCount(1));
                    stack.consume(1, player);
                    player.displayClientMessage(Component.translatable("message.forjasuprema.bench.count",
                            bench.parts().size(), ShapingBenchBlockEntity.CAPACITY), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof SmithHammerItem && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                List<RecipeHolder<AssemblyRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY.get());
                AssemblyService.Outcome outcome = AssemblyService.assemble(level.registryAccess(), level, bench.parts(), recipes);
                if (!outcome.ok()) {
                    player.displayClientMessage(outcome.message(), true);
                } else {
                    bench.clear();
                    player.getInventory().placeItemBackInInventory(outcome.result());
                    stack.hurtAndBreak(2, player, LivingEntity.getSlotForHand(hand));
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    QualityTier tier = outcome.quality().tier();
                    player.displayClientMessage(Component.translatable("message.forjasuprema.bench.done",
                            Component.translatable("quality.forjasuprema." + tier.id()),
                            Math.round(outcome.quality().score() * 100)), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ShapingBenchBlockEntity bench)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!player.isShiftKeyDown() && !bench.parts().isEmpty()) {
                player.getInventory().placeItemBackInInventory(bench.takeLast());
            } else {
                player.displayClientMessage(Component.translatable("message.forjasuprema.bench.count",
                        bench.parts().size(), ShapingBenchBlockEntity.CAPACITY), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ShapingBenchBlockEntity bench) {
            bench.dropAll();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
