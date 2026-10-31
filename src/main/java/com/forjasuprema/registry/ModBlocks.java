package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.station.AlloyStationBlock;
import com.forjasuprema.station.ForgeBlock;
import com.forjasuprema.station.WorkpieceStationBlock;
import com.forjasuprema.thermal.ForgeTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registro de blocos: minerio de estanho, as 5 forjas e as demais estacoes de ferraria. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ForjaSuprema.MOD_ID);

    /** Estacoes que apenas apoiam uma peca por cima (sem comportamento proprio ate as proximas fases). */
    public static final List<DeferredBlock<WorkpieceStationBlock>> WORKPIECE_STATIONS = new ArrayList<>();
    /** As cinco categorias de forja, da mais simples a lendaria. */
    public static final List<DeferredBlock<ForgeBlock>> FORGES = new ArrayList<>();

    public static final DeferredBlock<Block> TIN_ORE =
            BLOCKS.registerSimpleBlock("tin_ore", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));

    public static final DeferredBlock<ForgeBlock> APPRENTICE_FORGE = forge("apprentice_forge", ForgeTier.APPRENTICE);
    public static final DeferredBlock<ForgeBlock> BLACKSMITH_FORGE = forge("blacksmith_forge", ForgeTier.SMITH);
    public static final DeferredBlock<ForgeBlock> ADVANCED_FORGE = forge("advanced_forge", ForgeTier.ADVANCED);
    public static final DeferredBlock<ForgeBlock> MASTER_FORGE = forge("master_forge", ForgeTier.MASTER);
    public static final DeferredBlock<ForgeBlock> LEGENDARY_FORGE = forge("legendary_forge", ForgeTier.LEGENDARY);

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

    private static DeferredBlock<ForgeBlock> forge(String name, ForgeTier tier) {
        DeferredBlock<ForgeBlock> block = BLOCKS.registerBlock(name, properties -> new ForgeBlock(tier, properties),
                stone().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0));
        FORGES.add(block);
        return block;
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
