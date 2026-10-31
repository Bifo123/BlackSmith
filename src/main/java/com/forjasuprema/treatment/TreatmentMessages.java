package com.forjasuprema.treatment;

import net.minecraft.network.chat.Component;

/** Mensagens ao jogador para cada resultado de verificacao de tratamento. */
public final class TreatmentMessages {
    private TreatmentMessages() {
    }

    public static Component name(TreatmentKind kind) {
        return Component.translatable("treatment.forjasuprema." + kind.id());
    }

    /**
     * Argumentos comuns (posicionais): 1 tratamento, 2 chama minima, 3 chama atual, 4 temperatura minima,
     * 5 temperatura maxima, 6 temperatura atual.
     */
    public static Component of(TreatmentRules.Check check, TreatmentKind kind, int flame, double temperature,
                               double workMin, double melt) {
        return Component.translatable("message.forjasuprema.treatment." + check.name().toLowerCase(java.util.Locale.ROOT),
                name(kind),
                TreatmentRules.minFlame(kind),
                flame,
                Math.round(TreatmentRules.minTemperature(kind, workMin)),
                Math.round(TreatmentRules.maxTemperature(kind, workMin, melt)),
                Math.round(temperature));
    }
}
