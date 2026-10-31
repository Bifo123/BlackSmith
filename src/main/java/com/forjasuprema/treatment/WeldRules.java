package com.forjasuprema.treatment;

/**
 * Regras da soldagem por forjamento (pura). As duas pecas precisam estar na "temperatura de solda", perto do
 * topo da janela de forjamento; abaixo disso a solda falha, sem excecao.
 */
public final class WeldRules {
    /** Fracao do topo da janela de forjamento a partir da qual as pecas soldam. */
    public static final double WELD_FRACTION = 0.92;

    private WeldRules() {
    }

    public static double minTemperature(double workMax) {
        return WELD_FRACTION * workMax;
    }

    public static boolean canWeld(double tempA, double tempB, double workMax) {
        double min = minTemperature(workMax);
        return tempA >= min && tempB >= min;
    }

    /** Nota da solda (0.5 a 1): sobe conforme a peca mais fria passa do minimo, ate o topo da janela. */
    public static double quality(double tempA, double tempB, double workMax) {
        double min = minTemperature(workMax);
        if (workMax <= min) {
            return 0.5;
        }
        double coldest = Math.min(tempA, tempB);
        double margin = Math.max(0.0, Math.min(1.0, (coldest - min) / (workMax - min)));
        return 0.5 + 0.5 * margin;
    }
}
