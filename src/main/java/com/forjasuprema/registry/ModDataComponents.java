package com.forjasuprema.registry;

import com.forjasuprema.ForjaSuprema;
import com.forjasuprema.finish.FinishCodecs;
import com.forjasuprema.finish.FinishState;
import com.forjasuprema.quality.StageScores;
import com.forjasuprema.thermal.HeatData;
import com.forjasuprema.treatment.TreatmentCodecs;
import com.forjasuprema.treatment.TreatmentState;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Componentes de dados do mod. A classificacao de qualidade e os atributos extras entram na Fase 8. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(ForjaSuprema.MOD_ID);

    /** Estado termico da peca (temperatura, instante e queima). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HeatData>> HEAT =
            DATA_COMPONENTS.registerComponentType("heat",
                    builder -> builder.persistent(HeatData.CODEC).networkSynchronized(HeatData.STREAM_CODEC));

    /** Metal de uma peca bruta (chapa, barra, pregos): define nome e cor. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> METAL_PART =
            DATA_COMPONENTS.registerComponentType("metal_part",
                    builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** Notas por etapa de fabricacao, gravadas pelos minigames. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StageScores>> STAGE_SCORES =
            DATA_COMPONENTS.registerComponentType("stage_scores",
                    builder -> builder.persistent(StageScores.CODEC).networkSynchronized(StageScores.STREAM_CODEC));

    /** Formato selecionado no martelo de ferreiro. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> HAMMER_SHAPE =
            DATA_COMPONENTS.registerComponentType("hammer_shape",
                    builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** Estado de tratamento termico (dureza, fragilidade, tratamentos aplicados). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TreatmentState>> TREATMENT =
            DATA_COMPONENTS.registerComponentType("treatment",
                    builder -> builder.persistent(TreatmentCodecs.CODEC).networkSynchronized(TreatmentCodecs.STREAM_CODEC));

    /** Acabamento (polimento, afiacao, gravacoes e runas). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FinishState>> FINISH =
            DATA_COMPONENTS.registerComponentType("finish",
                    builder -> builder.persistent(FinishCodecs.CODEC).networkSynchronized(FinishCodecs.STREAM_CODEC));

    /** Qual runa (id no registro {@code forjasuprema:rune}) este item de runa representa. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> RUNE =
            DATA_COMPONENTS.registerComponentType("rune",
                    builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    private ModDataComponents() {
    }
}
