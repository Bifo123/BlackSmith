package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registro de blocos. As estacoes (fornalha, bigorna, etc.) entram na Fase 3. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ForjaSuprema.MOD_ID);

    private ModBlocks() {
    }
}
