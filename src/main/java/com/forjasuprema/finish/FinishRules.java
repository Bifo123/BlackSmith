package com.forjasuprema.finish;

/**
 * Regras do acabamento (polimento, afiacao, gravacao, runas). Classe pura e deterministica: nada depende
 * de sorte; a nota vem do minigame (validado no servidor) e as exigencias vem do estado da peca.
 */
public final class FinishRules {
    /** Acima desta temperatura (graus C) a peca nao pode ser trabalhada nas bancadas de acabamento. */
    public static final double MAX_WORK_TEMPERATURE = 60.0;
    /** Polimento minimo para gravar (a superficie precisa estar lisa). */
    public static final double MIN_POLISH_TO_ENGRAVE = 0.2;

    public enum Check {
        OK,
        TOO_HOT,
        NEEDS_POLISH,
        NO_SLOT,
        TIER_TOO_LOW,
        NO_ENGRAVING,
        DUPLICATE_RUNE
    }

    private FinishRules() {
    }

    /** Encaixes de gravacao que o metal comporta: 1 + tier/2 (tiers 0-1: 1, 2-3: 2, 4: 3). */
    public static int engravingSlots(int metalTier) {
        return 1 + Math.max(0, Math.min(4, metalTier)) / 2;
    }

    /** Verifica se um minigame de acabamento pode comecar ({@code engrave} = gravacao). */
    public static Check checkEngraving(FinishState state, int metalTier, double pieceTemperature) {
        if (pieceTemperature > MAX_WORK_TEMPERATURE) {
            return Check.TOO_HOT;
        }
        if (state.polish() < MIN_POLISH_TO_ENGRAVE) {
            return Check.NEEDS_POLISH;
        }
        if (state.engravings().size() >= engravingSlots(metalTier)) {
            return Check.NO_SLOT;
        }
        return Check.OK;
    }

    public static Check checkSurfaceWork(double pieceTemperature) {
        return pieceTemperature > MAX_WORK_TEMPERATURE ? Check.TOO_HOT : Check.OK;
    }

    /** Polir de novo nunca piora: vale a melhor passada. */
    public static FinishState polish(FinishState state, double score) {
        return state.withPolish(Math.max(state.polish(), score));
    }

    /**
     * Fio da lamina: depende da nota, da dureza total da peca (base + tratamento) e perde ate 25% com a
     * fragilidade (gume que lasca). Afiar de novo nunca piora.
     */
    public static FinishState sharpen(FinishState state, double score, double totalHardness, double brittleness) {
        double hardness = Math.max(0.0, Math.min(1.0, totalHardness));
        double edge = score * (0.55 + 0.45 * hardness) * (1.0 - 0.25 * Math.max(0.0, Math.min(1.0, brittleness)));
        return state.withSharpness(Math.max(state.sharpness(), edge));
    }

    public static FinishState engrave(FinishState state, double score) {
        return state.withEngraving(score);
    }

    /** Verifica a inscricao de uma runa (ordem fixa: gravacao livre, tier, polimento, duplicata). */
    public static Check checkRune(FinishState state, String runeId, int metalTier, int runeMinTier, double runeMinPolish) {
        if (state.engravings().isEmpty() || state.freeSlots() <= 0) {
            return state.engravings().isEmpty() ? Check.NO_ENGRAVING : Check.NO_SLOT;
        }
        if (metalTier < runeMinTier) {
            return Check.TIER_TOO_LOW;
        }
        if (state.polish() < runeMinPolish) {
            return Check.NEEDS_POLISH;
        }
        if (state.hasRune(runeId)) {
            return Check.DUPLICATE_RUNE;
        }
        return Check.OK;
    }

    /** Potencia final da runa: de 50% a 100% da potencia base, conforme a nota da gravacao do encaixe usado. */
    public static double runePower(FinishState state, double basePower) {
        int slot = state.runes().size();
        double engraving = slot < state.engravings().size() ? state.engravings().get(slot) : 0.0;
        return Math.max(0.0, Math.min(1.0, basePower * (0.5 + 0.5 * engraving)));
    }

    public static FinishState inscribe(FinishState state, String runeId, double basePower) {
        return state.withRune(new FinishState.RuneMark(runeId, runePower(state, basePower)));
    }
}
