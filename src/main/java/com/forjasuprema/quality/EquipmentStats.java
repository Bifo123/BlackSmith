package com.forjasuprema.quality;

/** Atributos finais do equipamento a partir do metal, dos tratamentos, do acabamento e da classe. Classe pura. */
public final class EquipmentStats {
    private EquipmentStats() {
    }

    /** Fator do material (0.7 a 1.3): forca e dureza total (metal + tratamento). Ferro vale 1.0. */
    public static double materialFactor(double strength, double totalHardness) {
        double avg = (clamp01(strength) + clamp01(totalHardness)) / 2.0;
        return 0.7 + 0.6 * avg;
    }

    /** Dano final: base da receita x material x classe x fio (0.8 sem fio, 1.2 com fio perfeito). */
    public static double attackDamage(double base, double materialFactor, QualityTier tier, double sharpness) {
        return base * materialFactor * tier.multiplier() * (0.8 + 0.4 * clamp01(sharpness));
    }

    /** Durabilidade: base x forca x classe; a fragilidade tira ate 35%. */
    public static int durability(int base, double strength, QualityTier tier, double brittleness) {
        double value = base * (0.6 + 0.8 * clamp01(strength)) * tier.multiplier() * (1.0 - 0.35 * clamp01(brittleness));
        return Math.max(1, (int) Math.round(value));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
