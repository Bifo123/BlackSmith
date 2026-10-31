package com.forjasuprema.station;

import com.forjasuprema.metal.alloy.AlloyInput;
import com.forjasuprema.metal.alloy.AlloyRecipe;
import com.forjasuprema.metal.alloy.AlloyService;
import com.forjasuprema.registry.ModBlockEntities;
import com.forjasuprema.thermal.FlameLevel;
import com.forjasuprema.thermal.HeatRequirement;
import com.forjasuprema.thermal.HeatSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Estacao de Criacao de Ligas: 4 slots de entrada e 1 de saida. A fusao e executada no servidor
 * pelo botao da interface, usando as receitas ({@code forjasuprema:alloying}) da Fase 2.
 *
 * <p>A estacao e um cadinho: precisa estar <b>diretamente sobre uma fonte de calor</b> (uma forja
 * acesa). A receita exige chama e temperatura minimas; se a forja nao as atingir, a liga falha
 * antes de consumir qualquer item.
 */
public class AlloyStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOT = 4;
    private static final String ITEMS_KEY = "Items";

    /** Resultado de uma tentativa de fundir liga; {@code message} e mostrado ao jogador em caso de falha. */
    public record AlloyOutcome(boolean success, Component message) {
        static AlloyOutcome ok() {
            return new AlloyOutcome(true, Component.empty());
        }

        static AlloyOutcome fail(Component message) {
            return new AlloyOutcome(false, message);
        }
    }

    private final ItemStackHandler items = new ItemStackHandler(INPUT_SLOTS + 1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS;
        }
    };

    public AlloyStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_STATION.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    /** Fonte de calor logo abaixo da estacao, se houver. */
    @Nullable
    private HeatSource findHeatSource() {
        if (level == null) {
            return null;
        }
        return level.getBlockEntity(worldPosition.below()) instanceof HeatSource source ? source : null;
    }

    /** Tenta fundir uma liga com o conteudo dos slots de entrada, respeitando as condicoes termicas. */
    public AlloyOutcome tryAlloy() {
        if (level == null || level.isClientSide) {
            return AlloyOutcome.fail(Component.empty());
        }
        List<ItemStack> inputs = new ArrayList<>(INPUT_SLOTS);
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            inputs.add(items.getStackInSlot(slot));
        }
        AlloyInput input = new AlloyInput(inputs);

        Optional<RecipeHolder<AlloyRecipe>> found = AlloyService.find(level, input);
        if (found.isEmpty()) {
            return AlloyOutcome.fail(Component.translatable("commands.forjasuprema.alloy.none"));
        }
        AlloyRecipe recipe = found.get().value();

        // Condicao termica: verificada antes de qualquer consumo, sem depender de nada que o jogador faca.
        if (recipe.minFlame() > 0 || recipe.minTemperature() > 0) {
            HeatSource source = findHeatSource();
            if (source == null) {
                return AlloyOutcome.fail(Component.translatable("message.forjasuprema.alloy.no_heat_source"));
            }
            HeatRequirement.Result heat = HeatRequirement.check(
                    source.flameLevel(), source.temperature(), recipe.minFlame(), recipe.minTemperature());
            if (heat == HeatRequirement.Result.INSUFFICIENT_FLAME) {
                return AlloyOutcome.fail(Component.translatable("message.forjasuprema.alloy.flame_too_weak",
                        Component.translatable(FlameLevel.translationKey(recipe.minFlame())),
                        Component.translatable(FlameLevel.translationKey(source.flameLevel()))));
            }
            if (heat == HeatRequirement.Result.TOO_COLD) {
                return AlloyOutcome.fail(Component.translatable("message.forjasuprema.alloy.too_cold",
                        recipe.minTemperature(), Math.round(source.temperature())));
            }
        }

        ItemStack result = recipe.result();
        ItemStack output = items.getStackInSlot(OUTPUT_SLOT);

        int room;
        if (output.isEmpty()) {
            room = result.getMaxStackSize();
        } else if (ItemStack.isSameItemSameComponents(output, result)) {
            room = output.getMaxStackSize() - output.getCount();
        } else {
            return AlloyOutcome.fail(Component.translatable("message.forjasuprema.alloy.output_blocked"));
        }

        int batches = Math.min(recipe.maxBatches(input), AlloyService.MAX_BATCHES);
        batches = Math.min(batches, room / Math.max(1, result.getCount()));
        if (batches <= 0) {
            return AlloyOutcome.fail(Component.translatable("message.forjasuprema.alloy.output_blocked"));
        }

        for (AlloyRecipe.Entry entry : recipe.inputs()) {
            int remaining = entry.count() * batches;
            for (int slot = 0; slot < INPUT_SLOTS && remaining > 0; slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                if (!stack.isEmpty() && entry.ingredient().test(stack)) {
                    ItemStack taken = items.extractItem(slot, Math.min(remaining, stack.getCount()), false);
                    remaining -= taken.getCount();
                }
            }
        }

        int produced = result.getCount() * batches;
        if (output.isEmpty()) {
            items.setStackInSlot(OUTPUT_SLOT, result.copyWithCount(produced));
        } else {
            ItemStack grown = output.copy();
            grown.grow(produced);
            items.setStackInSlot(OUTPUT_SLOT, grown);
        }
        return AlloyOutcome.ok();
    }

    /** Dropa todo o conteudo no mundo (usado quando o bloco e quebrado). */
    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, worldPosition, stack.copy());
                items.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ITEMS_KEY, items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(ITEMS_KEY)) {
            items.deserializeNBT(registries, tag.getCompound(ITEMS_KEY));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.forjasuprema.alloy_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AlloyStationMenu(containerId, inventory, this);
    }
}
