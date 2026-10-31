package com.forjasuprema.thermal;

/**
 * Intensidade da chama de uma forja (0 a 4). Classe pura: nao depende do Minecraft.
 */
public enum FlameLevel {
    OFF(0),
    WEAK(1),
    MODERATE(2),
    STRONG(3),
    INTENSE(4);

    private final int level;

    FlameLevel(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    /** Chave de traducao do nome do nivel. */
    public String translationKey() {
        return "flame.forjasuprema." + level;
    }

    public static FlameLevel fromLevel(int level) {
        int clamped = Math.max(0, Math.min(INTENSE.level, level));
        return values()[clamped];
    }

    public static String translationKey(int level) {
        return fromLevel(level).translationKey();
    }
}
