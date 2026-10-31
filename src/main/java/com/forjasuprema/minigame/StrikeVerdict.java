package com.forjasuprema.minigame;

/** Avaliacao qualitativa de um golpe, mostrada ao jogador. Classe pura. */
public enum StrikeVerdict {
    PERFECT,
    GOOD,
    POOR,
    MISS,
    WRONG_DIRECTION;

    public static StrikeVerdict of(double accuracy, boolean directionOk) {
        if (!directionOk) {
            return WRONG_DIRECTION;
        }
        if (accuracy >= 0.95) {
            return PERFECT;
        }
        if (accuracy >= 0.60) {
            return GOOD;
        }
        return accuracy > 0.0 ? POOR : MISS;
    }

    public static StrikeVerdict fromOrdinal(int ordinal) {
        StrikeVerdict[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal))];
    }
}
