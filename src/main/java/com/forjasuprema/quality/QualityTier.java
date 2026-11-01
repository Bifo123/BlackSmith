package com.forjasuprema.quality;

import java.util.Optional;

/** As 7 classes de qualidade, da pior a melhor. Classe pura. */
public enum QualityTier {
    DEFECTIVE("defective", 0.60),
    RUDIMENTARY("rudimentary", 0.80),
    COMMON("common", 1.00),
    GOOD("good", 1.10),
    EXCELLENT("excellent", 1.20),
    PERFECT("perfect", 1.35),
    LEGENDARY("legendary", 1.55);

    private final String id;
    private final double multiplier;

    QualityTier(String id, double multiplier) {
        this.id = id;
        this.multiplier = multiplier;
    }

    public String id() {
        return id;
    }

    /** Multiplicador de dano e durabilidade desta classe. */
    public double multiplier() {
        return multiplier;
    }

    /** Classe pela nota (sem considerar as condicoes do Lendario: ver {@link QualityCalculator}). */
    public static QualityTier forScore(double score) {
        if (score < 0.20) {
            return DEFECTIVE;
        }
        if (score < 0.40) {
            return RUDIMENTARY;
        }
        if (score < 0.60) {
            return COMMON;
        }
        if (score < 0.78) {
            return GOOD;
        }
        if (score < 0.90) {
            return EXCELLENT;
        }
        return PERFECT;
    }

    public static Optional<QualityTier> fromId(String id) {
        for (QualityTier tier : values()) {
            if (tier.id.equals(id)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }
}
