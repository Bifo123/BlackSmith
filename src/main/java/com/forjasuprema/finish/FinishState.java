package com.forjasuprema.finish;

import java.util.ArrayList;
import java.util.List;

/**
 * Acabamento de uma peca: polimento e afiacao (0 a 1), notas das gravacoes (cada uma abre um encaixe
 * para uma runa) e runas inscritas, em ordem. Imutavel e pura.
 */
public record FinishState(double polish, double sharpness, List<Double> engravings, List<RuneMark> runes) {
    public static final FinishState NONE = new FinishState(0.0, 0.0, List.of(), List.of());

    /** Runa inscrita: {@code id} do registro de runas e potencia final (0 a 1). */
    public record RuneMark(String id, double power) {
    }

    public FinishState {
        polish = clamp01(polish);
        sharpness = clamp01(sharpness);
        engravings = List.copyOf(engravings);
        runes = List.copyOf(runes);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public boolean isUntouched() {
        return polish <= 0.0 && sharpness <= 0.0 && engravings.isEmpty() && runes.isEmpty();
    }

    public int freeSlots() {
        return engravings.size() - runes.size();
    }

    public boolean hasRune(String id) {
        return runes.stream().anyMatch(rune -> rune.id().equals(id));
    }

    public FinishState withPolish(double value) {
        return new FinishState(value, sharpness, engravings, runes);
    }

    public FinishState withSharpness(double value) {
        return new FinishState(polish, value, engravings, runes);
    }

    public FinishState withEngraving(double score) {
        List<Double> next = new ArrayList<>(engravings);
        next.add(score);
        return new FinishState(polish, sharpness, next, runes);
    }

    public FinishState withRune(RuneMark mark) {
        List<RuneMark> next = new ArrayList<>(runes);
        next.add(mark);
        return new FinishState(polish, sharpness, engravings, next);
    }
}
