package com.forjasuprema.station;

import com.forjasuprema.registry.ModBlocks;
import com.forjasuprema.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Menu da Estacao de Ligas: 4 entradas (2x2), 1 saida e o inventario do jogador. */
public class AlloyStationMenu extends AbstractContainerMenu {
    /** Id do botao "Fundir liga", enviado pelo cliente via {@code handleInventoryButtonClick}. */
    public static final int BUTTON_ALLOY = 0;

    private static final int PLAYER_INV_START = AlloyStationBlockEntity.OUTPUT_SLOT + 1;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final AlloyStationBlockEntity station;
    private final ContainerLevelAccess access;

    /** Construtor do cliente: le a posicao enviada pelo servidor. */
    public AlloyStationMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, lookup(inventory, buffer));
    }

    public AlloyStationMenu(int containerId, Inventory inventory, AlloyStationBlockEntity station) {
        super(ModMenus.ALLOY_STATION.get(), containerId);
        this.station = station;
        this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());

        var handler = station.getItems();
        addSlot(new SlotItemHandler(handler, 0, 44, 26));
        addSlot(new SlotItemHandler(handler, 1, 62, 26));
        addSlot(new SlotItemHandler(handler, 2, 44, 44));
        addSlot(new SlotItemHandler(handler, 3, 62, 44));
        addSlot(new SlotItemHandler(handler, AlloyStationBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 152));
        }
    }

    private static AlloyStationBlockEntity lookup(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        var be = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (be instanceof AlloyStationBlockEntity station) {
            return station;
        }
        throw new IllegalStateException("Estacao de ligas nao encontrada no cliente");
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_ALLOY) {
            return false;
        }
        if (!player.level().isClientSide) {
            AlloyStationBlockEntity.AlloyOutcome outcome = station.tryAlloy();
            if (!outcome.success()) {
                player.displayClientMessage(outcome.message(), true);
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index < PLAYER_INV_START) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, AlloyStationBlockEntity.INPUT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ALLOY_STATION.get());
    }
}
