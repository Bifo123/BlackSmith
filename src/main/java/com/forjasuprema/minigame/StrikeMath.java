package com.forjasuprema.minigame;

import java.util.List;

/**
 * Matematica dos minigames de golpes. Classe pura (testavel sem iniciar o jogo).
 *
 * <p>O marcador percorre uma barra de 0.0 a 1.0 e volta (onda triangular) com um periodo em ticks.
 * Cliente e servidor calculam a mesma posicao a partir do tempo decorrido, entao o servidor consegue
 * conferir cada golpe sem confiar no resultado enviado pelo cliente.
 */
public final class StrikeMath {
    /** Erro (fracao da meia-largura da zona) ate o qual o golpe e perfeito. */
    public static final double PERFECT_FRACTION = 0.35;
    /** Erro (fracao da meia-largura da zona) a partir do qual o golpe e errado. */
    public static final double MISS_FRACTION = 1.6;

    /** Faixa abaixo da janela de forjamento em que a eficiencia cai de 100% a 0%. */
    public static final double COLD_SPAN = 250.0;
    /** Quanto abaixo da janela de forjamento a peca pode ficar antes de a sessao ser interrompida. */
    public static final double ABORT_MARGIN = 200.0;
    /** Eficiencia minima de um golpe em peca superaquecida (quase derretendo). */
    public static final double OVERHEATED_FLOOR = 0.6;

    /** Nota a partir da qual um golpe conta para a sequencia (ritmo) de acertos. */
    public static final double STREAK_THRESHOLD = 0.7;

    private StrikeMath() {
    }

    /** Posicao do marcador (0 a 1) {@code elapsedTicks} ticks apos o inicio, com periodo completo de {@code periodTicks}. */
    public static double cursor(double elapsedTicks, int periodTicks) {
        if (periodTicks <= 0) {
            return 0.5;
        }
        double phase = (elapsedTicks % periodTicks) / periodTicks;
        if (phase < 0) {
            phase += 1.0;
        }
        return phase < 0.5 ? phase * 2.0 : 2.0 - phase * 2.0;
    }

    /**
     * Precisao de um golpe, de 0 a 1: perfeita ate {@link #PERFECT_FRACTION} da meia-largura da zona,
     * caindo linearmente ate zero em {@link #MISS_FRACTION}.
     */
    public static double accuracy(double error, double zoneHalfWidth) {
        if (zoneHalfWidth <= 0) {
            return 0.0;
        }
        double ratio = Math.abs(error) / zoneHalfWidth;
        if (ratio <= PERFECT_FRACTION) {
            return 1.0;
        }
        if (ratio >= MISS_FRACTION) {
            return 0.0;
        }
        return 1.0 - (ratio - PERFECT_FRACTION) / (MISS_FRACTION - PERFECT_FRACTION);
    }

    /**
     * Eficiencia do golpe conforme a temperatura da peca: 1.0 dentro da janela de forjamento, caindo para
     * zero abaixo dela e ate {@link #OVERHEATED_FLOOR} acima dela (quanto mais perto de derreter, pior).
     */
    public static double temperatureFactor(double temperature, double workMin, double workMax, double melt) {
        if (temperature < workMin) {
            return Math.max(0.0, 1.0 - (workMin - temperature) / COLD_SPAN);
        }
        if (temperature <= workMax) {
            return 1.0;
        }
        if (melt <= workMax) {
            return OVERHEATED_FLOOR;
        }
        double over = Math.min(1.0, (temperature - workMax) / (melt - workMax));
        return 1.0 - (1.0 - OVERHEATED_FLOOR) * over;
    }

    /** Peca fria demais para continuar: a sessao deve ser interrompida. */
    public static boolean shouldAbortCold(double temperature, double workMin) {
        return temperature < workMin - ABORT_MARGIN;
    }

    /** Maior sequencia seguida de golpes com nota de pelo menos {@link #STREAK_THRESHOLD}. */
    public static int longestStreak(List<Double> scores) {
        int best = 0;
        int current = 0;
        for (double score : scores) {
            current = score >= STREAK_THRESHOLD ? current + 1 : 0;
            best = Math.max(best, current);
        }
        return best;
    }

    /**
     * Nota final da sessao, de 0 a 1: 85% a media das notas dos golpes (precisao) e 15% a maior
     * sequencia de acertos em relacao ao total (ritmo mantido).
     */
    public static double sessionScore(List<Double> scores) {
        if (scores.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double score : scores) {
            sum += score;
        }
        double mean = sum / scores.size();
        double streak = (double) longestStreak(scores) / scores.size();
        return Math.max(0.0, Math.min(1.0, 0.85 * mean + 0.15 * streak));
    }

    /**
     * Tempo decorrido a usar para um golpe: o valor informado pelo cliente se estiver dentro de
     * {@code maxSkew} ticks do relogio do servidor; caso contrario, o do servidor. Limita o quanto um
     * cliente adulterado consegue "adiantar" ou "atrasar" o proprio golpe.
     */
    public static long trustedElapsed(long claimed, long serverElapsed, long maxSkew) {
        long chosen = Math.abs(claimed - serverElapsed) <= maxSkew ? claimed : serverElapsed;
        return Math.max(0L, chosen);
    }
}
