package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.thermal.HeatData;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Componentes de dados do mod. Qualidade e atributos extras entram na Fase 8. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(ForjaSuprema.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HeatData>> HEAT =
            DATA_COMPONENTS.registerComponentType("heat",
                    builder -> builder.persistent(HeatData.CODEC).networkSynchronized(HeatData.STREAM_CODEC));

    private ModDataComponents() {
    }
}
