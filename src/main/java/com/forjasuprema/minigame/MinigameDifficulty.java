package com.forjasuprema.minigame;

/**
 * Dificuldade progressiva dos minigames (secao 5.3 do documento mestre). Classe pura.
 *
 * <p>A dificuldade (0 a 1) cresce com a rigidez do material, a complexidade da peca e o tier do metal, e
 * diminui com a qualidade da ferramenta e com a experiencia do jogador. Ela define o tamanho da zona
 * de acerto e a velocidade do marcador.
 */
public final class MinigameDifficulty {
    /** Reducao maxima de dificuldade vinda da experiencia do ferreiro. */
    public static final double MAX_SKILL_BONUS = 0.20;
    /** Reducao de dificuldade por nivel do ferreiro (50 niveis chegam ao maximo). */
    public static final double SKILL_BONUS_PER_LEVEL = 0.004;
    /** Reducao maxima de dificuldade vinda da ferramenta (qualidade 1.0). */
    public static final double MAX_TOOL_BONUS = 0.10;

    /** Dificuldade base dos foles (nao dependem de material). */
    private static final double BELLOWS_BASE = 0.20;

    private static final double POLISHING_BASE = 0.15;
    private static final double SHARPENING_BASE = 0.30;
    private static final double ENGRAVING_BASE = 0.40;

    private MinigameDifficulty() {
    }

    /**
     * @param zoneHalfWidth meia-largura da zona de acerto, em fracao da barra
     * @param periodTicks   ticks para o marcador ir e voltar
     * @param difficulty    dificuldade final, de 0 a 1
     */
    public record Params(double zoneHalfWidth, int periodTicks, double difficulty) {
    }

    public static Params forHammering(double malleability, double complexity, int metalTier,
                                      double toolQuality, int playerLevel) {
        double base = 0.35 * (1.0 - clamp01(malleability))
                + 0.30 * clamp01(complexity)
                + 0.05 * (Math.max(0, Math.min(4, metalTier)) / 4.0);
        return build(MinigameKind.HAMMERING, base, toolQuality, playerLevel);
    }

    public static Params forBellows(double toolQuality, int playerLevel) {
        return build(MinigameKind.BELLOWS, BELLOWS_BASE, toolQuality, playerLevel);
    }

    /** Acabamento: polimento, afiacao ou gravacao. A gravacao e mais dificil em metais de tier alto. */
    public static Params forFinishing(MinigameKind kind, int metalTier, double toolQuality, int playerLevel) {
        double base = switch (kind) {
            case POLISHING -> POLISHING_BASE;
            case SHARPENING -> SHARPENING_BASE;
            case ENGRAVING -> ENGRAVING_BASE + 0.10 * (Math.max(0, Math.min(4, metalTier)) / 4.0);
            default -> throw new IllegalArgumentException("nao e um minigame de acabamento: " + kind);
        };
        return build(kind, base, toolQuality, playerLevel);
    }

    private static Params build(MinigameKind kind, double base, double toolQuality, int playerLevel) {
        double skillBonus = Math.min(MAX_SKILL_BONUS, Math.max(0, playerLevel) * SKILL_BONUS_PER_LEVEL);
        double toolBonus = MAX_TOOL_BONUS * clamp01(toolQuality);
        double difficulty = clamp01(base - skillBonus - toolBonus);

        double halfWidth = lerp(0.130, 0.045, difficulty);
        double period = lerp(64.0, 30.0, difficulty);
        if (kind == MinigameKind.BELLOWS || kind == MinigameKind.POLISHING) {
            // Foles sao mais permissivos: zona maior e marcador mais lento.
            halfWidth += 0.020;
            period += 10.0;
        }
        return new Params(halfWidth, (int) Math.round(period), difficulty);
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
