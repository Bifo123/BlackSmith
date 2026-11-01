package com.forjasuprema.station;

import com.forjasuprema.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Pecas apoiadas na Bancada de Modelagem (ate {@value #CAPACITY}), a espera da montagem. */
public class ShapingBenchBlockEntity extends BlockEntity {
    public static final int CAPACITY = 4;

    private final List<ItemStack> parts = new ArrayList<>();

    public ShapingBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAPING_BENCH.get(), pos, state);
    }

    public List<ItemStack> parts() {
        return List.copyOf(parts);
    }

    public boolean isFull() {
        return parts.size() >= CAPACITY;
    }

    public void add(ItemStack part) {
        parts.add(part);
        setChanged();
    }

    public ItemStack takeLast() {
        if (parts.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = parts.remove(parts.size() - 1);
        setChanged();
        return taken;
    }

    public void clear() {
        parts.clear();
        setChanged();
    }

    public void dropAll() {
        if (level != null) {
            parts.forEach(part -> Block.popResource(level, worldPosition, part));
        }
        parts.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        parts.forEach(part -> list.add(part.save(registries)));
        tag.put("Parts", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        parts.clear();
        ListTag list = tag.getList("Parts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && parts.size() < CAPACITY; i++) {
            ItemStack.parse(registries, list.getCompound(i)).ifPresent(parts::add);
        }
    }
}
