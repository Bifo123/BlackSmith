package com.forjasuprema.treatment;

import com.forjasuprema.metal.Metal;
import com.forjasuprema.metal.MetalLookup;
import com.forjasuprema.quality.StageScores;
import com.forjasuprema.registry.ModDataComponents;
import com.forjasuprema.registry.ModItems;
import com.forjasuprema.thermal.HeatData;
import com.forjasuprema.thermal.ThermalMath;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Aplica tratamentos a pecas (ItemStack), sempre no servidor. A matematica vive em {@link TreatmentRules}. */
public final class TreatmentService {
    /** Resultado: {@code message} nao nulo = falhou (nada mudou); caso contrario {@code result} e a peca tratada. */
    public record Outcome(Component message, ItemStack result, double quality) {
        public boolean ok() {
            return message == null;
        }
    }

    private TreatmentService() {
    }

    public static List<String> treatmentIds(Metal metal) {
        return metal.treatments().stream().map(Object::toString).toList();
    }

    public static TreatmentState stateOf(ItemStack piece) {
        return piece.getOrDefault(ModDataComponents.TREATMENT.get(), TreatmentState.NONE);
    }

    public static double temperatureOf(ItemStack piece, long now) {
        HeatData heat = piece.get(ModDataComponents.HEAT.get());
        return heat == null ? ThermalMath.AMBIENT : heat.temperatureAt(now);
    }

    /** Tempera: a peca em {@code piece} (1 item) e imersa na temperatura em que esta agora. */
    public static Outcome quench(RegistryAccess access, ItemStack piece, long now) {
        Optional<Holder.Reference<Metal>> found = MetalLookup.find(access, piece);
        if (found.isEmpty()) {
            return new Outcome(Component.translatable("message.forjasuprema.minigame.not_metal"), ItemStack.EMPTY, 0.0);
        }
        Metal metal = found.get().value();
        double temperature = temperatureOf(piece, now);
        TreatmentRules.Check check = TreatmentRules.checkStart(TreatmentKind.QUENCH, stateOf(piece),
                treatmentIds(metal), 0, temperature, metal.workTempMin(), metal.meltTemperature());
        if (check != TreatmentRules.Check.OK) {
            return new Outcome(TreatmentMessages.of(check, TreatmentKind.QUENCH, 0, temperature,
                    metal.workTempMin(), metal.meltTemperature()), ItemStack.EMPTY, 0.0);
        }
        double quality = TreatmentRules.quenchQuality(temperature, metal.workTempMin(), metal.workTempMax(),
                metal.meltTemperature());
        ItemStack result = finish(piece, TreatmentKind.QUENCH, metal, quality);
        // A agua leva a peca ao ambiente; so a queima acumulada permanece.
        HeatData heat = piece.get(ModDataComponents.HEAT.get());
        if (heat != null && heat.burn() > 0.0) {
            result.set(ModDataComponents.HEAT.get(), new HeatData(ThermalMath.AMBIENT, now, heat.burn()));
        } else {
            result.remove(ModDataComponents.HEAT.get());
        }
        return new Outcome(null, result, quality);
    }

    /**
     * Solda por forjamento: a peca da bigorna e a da mao, do mesmo metal e ambas na temperatura de solda,
     * viram um lingote soldado (billet). Falha, sem consumir nada, se alguma estiver fria.
     */
    public static Outcome weld(RegistryAccess access, ItemStack anvilPiece, ItemStack handStack, long now) {
        Optional<Holder.Reference<Metal>> a = MetalLookup.find(access, anvilPiece);
        Optional<Holder.Reference<Metal>> b = MetalLookup.find(access, handStack);
        if (a.isEmpty() || b.isEmpty() || !a.get().key().equals(b.get().key())) {
            return new Outcome(Component.translatable("message.forjasuprema.weld.different"), ItemStack.EMPTY, 0.0);
        }
        if (anvilPiece.getCount() != 1) {
            return new Outcome(Component.translatable("message.forjasuprema.weld.single"), ItemStack.EMPTY, 0.0);
        }
        Metal metal = a.get().value();
        double tempA = temperatureOf(anvilPiece, now);
        double tempB = temperatureOf(handStack, now);
        if (!WeldRules.canWeld(tempA, tempB, metal.workTempMax())) {
            return new Outcome(Component.translatable("message.forjasuprema.weld.too_cold",
                    Math.round(WeldRules.minTemperature(metal.workTempMax())), Math.round(tempA), Math.round(tempB)),
                    ItemStack.EMPTY, 0.0);
        }
        double quality = WeldRules.quality(tempA, tempB, metal.workTempMax());
        ItemStack result = new ItemStack(ModItems.WELDED_BILLET.get());
        result.set(ModDataComponents.METAL_PART.get(), a.get().key().location());
        HeatData heatA = anvilPiece.get(ModDataComponents.HEAT.get());
        HeatData heatB = handStack.get(ModDataComponents.HEAT.get());
        double burn = Math.max(heatA == null ? 0.0 : heatA.burn(), heatB == null ? 0.0 : heatB.burn());
        result.set(ModDataComponents.HEAT.get(), new HeatData(Math.min(tempA, tempB), now, burn));
        result.set(ModDataComponents.STAGE_SCORES.get(), StageScores.empty().with(StageScores.WELD, (float) quality));
        return new Outcome(null, result, quality);
    }

    /** Copia de uma unidade da peca com o tratamento aplicado e a nota da etapa gravada. */
    public static ItemStack finish(ItemStack piece, TreatmentKind kind, Metal metal, double quality) {
        ItemStack result = piece.copyWithCount(1);
        TreatmentState next = TreatmentRules.apply(kind, stateOf(piece), metal.hardenability(), quality);
        result.set(ModDataComponents.TREATMENT.get(), next);
        // Aquecer forma carepa: o acabamento (polimento, afiacao, gravacoes) precisa ser refeito.
        result.remove(ModDataComponents.FINISH.get());
        StageScores scores = piece.getOrDefault(ModDataComponents.STAGE_SCORES.get(), StageScores.empty());
        result.set(ModDataComponents.STAGE_SCORES.get(), scores.with(kind.id(), (float) quality));
        return result;
    }
}
