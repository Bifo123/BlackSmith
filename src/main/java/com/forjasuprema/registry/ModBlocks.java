package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.station.AlloyStationBlock;
import com.forjasuprema.station.WorkpieceStationBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registro de blocos: minerio de estanho e as 10 estacoes de ferraria. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ForjaSuprema.MOD_ID);

    /** Estacoes que apoiam uma peca por cima (todas, exceto a Estacao de Ligas, que tem interface). */
    public static final List<DeferredBlock<WorkpieceStationBlock>> WORKPIECE_STATIONS = new ArrayList<>();

    public static final DeferredBlock<Block> TIN_ORE =
            BLOCKS.registerSimpleBlock("tin_ore", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));

    public static final DeferredBlock<WorkpieceStationBlock> BLACKSMITH_FORGE = workpiece("blacksmith_forge", stone());
    public static final DeferredBlock<WorkpieceStationBlock> BLACKSMITH_ANVIL = workpiece("blacksmith_anvil", iron());
    public static final DeferredBlock<WorkpieceStationBlock> SHAPING_BENCH = workpiece("shaping_bench", wood());
    public static final DeferredBlock<WorkpieceStationBlock> QUENCH_TANK = workpiece("quench_tank", iron());
    public static final DeferredBlock<WorkpieceStationBlock> HEAT_TREATMENT_FURNACE =
            workpiece("heat_treatment_furnace", stone());
    public static final DeferredBlock<WorkpieceStationBlock> POLISHING_STATION = workpiece("polishing_station", wood());
    public static final DeferredBlock<WorkpieceStationBlock> SHARPENING_STONE = workpiece("sharpening_stone", stone());
    public static final DeferredBlock<WorkpieceStationBlock> ENGRAVING_BENCH = workpiece("engraving_bench", wood());
    public static final DeferredBlock<WorkpieceStationBlock> FINISHING_STATION = workpiece("finishing_station", wood());

    public static final DeferredBlock<AlloyStationBlock> ALLOY_STATION =
            BLOCKS.registerBlock("alloy_station", AlloyStationBlock::new, stone());

    private ModBlocks() {
    }

    private static DeferredBlock<WorkpieceStationBlock> workpiece(String name, BlockBehaviour.Properties properties) {
        DeferredBlock<WorkpieceStationBlock> block = BLOCKS.registerBlock(name, WorkpieceStationBlock::new, properties);
        WORKPIECE_STATIONS.add(block);
        return block;
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops()
                .strength(3.0F, 6.0F).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties iron() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F).sound(SoundType.ANVIL);
    }

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)
                .ignitedByLava();
    }
}
