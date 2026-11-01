package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.minigame.MinigameKind;
import com.forjasuprema.station.AlloyStationBlock;
import com.forjasuprema.station.AnvilStationBlock;
import com.forjasuprema.station.FinishStationBlock;
import com.forjasuprema.station.ForgeBlock;
import com.forjasuprema.station.HeatTreatmentFurnaceBlock;
import com.forjasuprema.station.QuenchTankBlock;
import com.forjasuprema.station.RuneStationBlock;
import com.forjasuprema.station.ShapingBenchBlock;
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
    public static final List<DeferredBlock<? extends WorkpieceStationBlock>> WORKPIECE_STATIONS = new ArrayList<>();
    /** As cinco categorias de forja, da mais simples a lendaria. */
    public static final List<DeferredBlock<ForgeBlock>> FORGES = new ArrayList<>();

    public static final DeferredBlock<Block> TIN_ORE =
            BLOCKS.registerSimpleBlock("tin_ore", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));

    public static final DeferredBlock<ForgeBlock> APPRENTICE_FORGE = forge("apprentice_forge", ForgeTier.APPRENTICE);
    public static final DeferredBlock<ForgeBlock> BLACKSMITH_FORGE = forge("blacksmith_forge", ForgeTier.SMITH);
    public static final DeferredBlock<ForgeBlock> ADVANCED_FORGE = forge("advanced_forge", ForgeTier.ADVANCED);
    public static final DeferredBlock<ForgeBlock> MASTER_FORGE = forge("master_forge", ForgeTier.MASTER);
    public static final DeferredBlock<ForgeBlock> LEGENDARY_FORGE = forge("legendary_forge", ForgeTier.LEGENDARY);

    /** Bigorna: com o martelo na mao inicia o minigame de martelamento. */
    public static final DeferredBlock<AnvilStationBlock> BLACKSMITH_ANVIL = anvil("blacksmith_anvil", iron());
    /** Bancada de Modelagem: monta equipamentos a partir das pecas forjadas. */
    public static final DeferredBlock<ShapingBenchBlock> SHAPING_BENCH =
            BLOCKS.registerBlock("shaping_bench", ShapingBenchBlock::new, wood());
    /** Tanque de tempera: encha com agua e clique com uma peca quente de metal. */
    public static final DeferredBlock<QuenchTankBlock> QUENCH_TANK =
            BLOCKS.registerBlock("quench_tank", QuenchTankBlock::new, iron());
    /** Forno de tratamento termico: usado sobre uma forja acesa. */
    public static final DeferredBlock<HeatTreatmentFurnaceBlock> HEAT_TREATMENT_FURNACE =
            BLOCKS.registerBlock("heat_treatment_furnace", HeatTreatmentFurnaceBlock::new, stone());
    public static final DeferredBlock<FinishStationBlock> POLISHING_STATION =
            finish("polishing_station", MinigameKind.POLISHING, wood());
    public static final DeferredBlock<FinishStationBlock> SHARPENING_STONE =
            finish("sharpening_stone", MinigameKind.SHARPENING, stone());
    public static final DeferredBlock<FinishStationBlock> ENGRAVING_BENCH =
            finish("engraving_bench", MinigameKind.ENGRAVING, wood());
    /** Estacao de acabamento: inscreve runas nas gravacoes. */
    public static final DeferredBlock<RuneStationBlock> FINISHING_STATION = rune("finishing_station", wood());

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

    private static DeferredBlock<FinishStationBlock> finish(String name, MinigameKind kind, BlockBehaviour.Properties properties) {
        DeferredBlock<FinishStationBlock> block =
                BLOCKS.registerBlock(name, props -> new FinishStationBlock(kind, props), properties);
        WORKPIECE_STATIONS.add(block);
        return block;
    }

    private static DeferredBlock<RuneStationBlock> rune(String name, BlockBehaviour.Properties properties) {
        DeferredBlock<RuneStationBlock> block = BLOCKS.registerBlock(name, RuneStationBlock::new, properties);
        WORKPIECE_STATIONS.add(block);
        return block;
    }

    private static DeferredBlock<AnvilStationBlock> anvil(String name, BlockBehaviour.Properties properties) {
        DeferredBlock<AnvilStationBlock> block = BLOCKS.registerBlock(name, AnvilStationBlock::new, properties);
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
