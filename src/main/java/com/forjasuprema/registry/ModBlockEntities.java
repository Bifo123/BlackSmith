package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.station.AlloyStationBlockEntity;
import com.forjasuprema.station.ForgeBlockEntity;
import com.forjasuprema.station.HeatTreatmentFurnaceBlockEntity;
import com.forjasuprema.station.QuenchTankBlockEntity;
import com.forjasuprema.station.WorkpieceBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ForjaSuprema.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkpieceBlockEntity>> WORKPIECE =
            BLOCK_ENTITIES.register("workpiece_station", () -> BlockEntityType.Builder.of(
                    WorkpieceBlockEntity::new,
                    ModBlocks.WORKPIECE_STATIONS.stream().map(block -> (Block) block.get()).toArray(Block[]::new)
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeBlockEntity>> FORGE =
            BLOCK_ENTITIES.register("forge", () -> BlockEntityType.Builder.of(
                    ForgeBlockEntity::new,
                    ModBlocks.FORGES.stream().map(DeferredBlock::get).toArray(Block[]::new)
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatTreatmentFurnaceBlockEntity>> HEAT_TREATMENT =
            BLOCK_ENTITIES.register("heat_treatment_furnace", () -> BlockEntityType.Builder.of(
                    HeatTreatmentFurnaceBlockEntity::new, ModBlocks.HEAT_TREATMENT_FURNACE.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuenchTankBlockEntity>> QUENCH_TANK =
            BLOCK_ENTITIES.register("quench_tank", () -> BlockEntityType.Builder.of(
                    QuenchTankBlockEntity::new, ModBlocks.QUENCH_TANK.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloyStationBlockEntity>> ALLOY_STATION =
            BLOCK_ENTITIES.register("alloy_station", () -> BlockEntityType.Builder.of(
                    AlloyStationBlockEntity::new, ModBlocks.ALLOY_STATION.get()
            ).build(null));

    private ModBlockEntities() {
    }
}
