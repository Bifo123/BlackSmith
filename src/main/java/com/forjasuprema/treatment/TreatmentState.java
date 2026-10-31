package com.forjasuprema.treatment;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado de tratamento de uma peca: bonus de dureza (0 a {@link #HARDNESS_CAP}), fragilidade (0 a 1) e a
 * lista, em ordem, dos tratamentos aplicados desde o ultimo recozimento. Imutavel e pura.
 */
public record TreatmentState(double hardness, double brittleness, List<String> applied) {
    public static final double HARDNESS_CAP = 0.5;
    public static final TreatmentState NONE = new TreatmentState(0.0, 0.0, List.of());

    public TreatmentState {
        hardness = Math.max(0.0, Math.min(HARDNESS_CAP, hardness));
        brittleness = Math.max(0.0, Math.min(1.0, brittleness));
        applied = List.copyOf(applied);
    }

    public boolean has(TreatmentKind kind) {
        return applied.contains(kind.id());
    }

    /** Ultimo tratamento aplicado, se houver. */
    public String last() {
        return applied.isEmpty() ? "" : applied.get(applied.size() - 1);
    }

    public TreatmentState with(TreatmentKind kind, double newHardness, double newBrittleness) {
        List<String> next = new ArrayList<>(applied);
        next.add(kind.id());
        return new TreatmentState(newHardness, newBrittleness, next);
    }

    public boolean isUntreated() {
        return applied.isEmpty() && hardness <= 0.0 && brittleness <= 0.0;
    }
}
