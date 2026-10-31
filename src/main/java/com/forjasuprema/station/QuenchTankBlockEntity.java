package com.forjasuprema.station;

import com.forjasuprema.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Guarda apenas se o tanque de tempera tem agua (a tempera exige agua). */
public class QuenchTankBlockEntity extends BlockEntity {
    private boolean filled;

    public QuenchTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUENCH_TANK.get(), pos, state);
    }

    public boolean isFilled() {
        return filled;
    }

    public void setFilled(boolean value) {
        filled = value;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Filled", filled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filled = tag.getBoolean("Filled");
    }
}
