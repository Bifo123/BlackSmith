package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registro de blocos. As estacoes (fornalha, bigorna, etc.) entram na Fase 3. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ForjaSuprema.MOD_ID);

    public static final DeferredBlock<Block> TIN_ORE =
            BLOCKS.registerSimpleBlock("tin_ore", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE));

    private ModBlocks() {
    }
}
