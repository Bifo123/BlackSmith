package com.forjasuprema.treatment;

import com.forjasuprema.thermal.HeatRequirement;
import java.util.Collection;

/**
 * Regras dos tratamentos. Classe pura e deterministica (sem sorte): a nota vem de quao bem as condicoes
 * foram mantidas. Regra do documento mestre: chama ou temperatura insuficientes FALHAM o tratamento,
 * sempre, antes de qualquer outra consideracao de desempenho.
 */
public final class TreatmentRules {
    /** Margem abaixo do ponto de fusao em que fornos de tratamento ja sao perigosos para a peca. */
    public static final double MELT_MARGIN = 50.0;

    public enum Check {
        OK,
        UNSUPPORTED,
        REQUIRES_QUENCH,
        INSUFFICIENT_FLAME,
        TOO_COLD,
        TOO_HOT
    }

    private TreatmentRules() {
    }

    public static boolean supports(Collection<String> metalTreatments, TreatmentKind kind) {
        return metalTreatments.contains(kind.fullId()) || metalTreatments.contains(kind.id());
    }

    /** Chama minima (0 a 4) exigida do forno. A tempera nao usa forno: o calor ja esta na peca. */
    public static int minFlame(TreatmentKind kind) {
        return switch (kind) {
            case QUENCH -> 0;
            case TEMPERING -> 1;
            case ANNEALING -> 2;
            case CARBURIZING, SURFACE_HARDENING -> 3;
        };
    }

    /** Temperatura minima do processo (da peca, na tempera; do forno, nos demais). */
    public static double minTemperature(TreatmentKind kind, double workMin) {
        return switch (kind) {
            case TEMPERING -> 0.30 * workMin;
            case ANNEALING -> 0.80 * workMin;
            case QUENCH, CARBURIZING, SURFACE_HARDENING -> workMin;
        };
    }

    /** Temperatura maxima do processo. O revenimento exige calor suave; os demais, abaixo da fusao. */
    public static double maxTemperature(TreatmentKind kind, double workMin, double melt) {
        return kind == TreatmentKind.TEMPERING ? 0.60 * workMin : melt - MELT_MARGIN;
    }

    /** Ticks de permanencia em condicao correta ate concluir (0 para a tempera, que e imediata). */
    public static int holdTicks(TreatmentKind kind) {
        return switch (kind) {
            case QUENCH -> 0;
            case TEMPERING -> 400;
            case ANNEALING -> 600;
            case CARBURIZING -> 1200;
            case SURFACE_HARDENING -> 300;
        };
    }

    /**
     * Verifica se o tratamento pode comecar. A ordem e fixa: suporte do metal, pre-requisito, chama,
     * temperatura minima, temperatura maxima.
     */
    public static Check checkStart(TreatmentKind kind, TreatmentState state, Collection<String> metalTreatments,
                                   int flame, double temperature, double workMin, double melt) {
        if (!supports(metalTreatments, kind)) {
            return Check.UNSUPPORTED;
        }
        if (kind == TreatmentKind.TEMPERING && (!state.has(TreatmentKind.QUENCH) || "tempering".equals(state.last()))) {
            return Check.REQUIRES_QUENCH;
        }
        HeatRequirement.Result heat = HeatRequirement.check(flame, temperature, minFlame(kind), minTemperature(kind, workMin));
        if (heat == HeatRequirement.Result.INSUFFICIENT_FLAME) {
            return Check.INSUFFICIENT_FLAME;
        }
        if (heat == HeatRequirement.Result.TOO_COLD) {
            return Check.TOO_COLD;
        }
        if (temperature > maxTemperature(kind, workMin, melt)) {
            return Check.TOO_HOT;
        }
        return Check.OK;
    }

    /**
     * Qualidade da tempera (0 a 1) pela temperatura da peca ao ser imersa: ligeiramente acima do minimo e
     * dentro da janela de forjamento e o ideal; acima da janela cai ate 0.3 perto da fusao.
     */
    public static double quenchQuality(double temperature, double workMin, double workMax, double melt) {
        if (temperature < workMin) {
            return 0.0;
        }
        if (temperature <= workMax) {
            return 0.7 + 0.3 * Math.min(1.0, (temperature - workMin) / 50.0);
        }
        if (melt <= workMax) {
            return 0.3;
        }
        double over = Math.min(1.0, (temperature - workMax) / (melt - workMax));
        return 1.0 - 0.7 * over;
    }

    /** Estabilidade de um tratamento de forno: fracao do tempo decorrido em que as condicoes foram mantidas. */
    public static double holdQuality(long inBandTicks, long elapsedTicks) {
        if (elapsedTicks <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) inBandTicks / elapsedTicks));
    }

    /** Efeito do tratamento sobre a peca, com qualidade {@code q} (0 a 1) e a temperabilidade do metal. */
    public static TreatmentState apply(TreatmentKind kind, TreatmentState state, double hardenability, double q) {
        double quality = Math.max(0.0, Math.min(1.0, q));
        double h = state.hardness();
        double b = state.brittleness();
        switch (kind) {
            case QUENCH -> {
                h += 0.5 * hardenability * (0.4 + 0.6 * quality);
                b += 0.30 + 0.50 * hardenability * quality;
            }
            case ANNEALING -> {
                return new TreatmentState(0.0, 0.0, java.util.List.of(kind.id()));
            }
            case TEMPERING -> {
                b *= 1.0 - 0.70 * quality;
                h *= 1.0 - 0.15 * quality;
            }
            case CARBURIZING -> {
                h += (0.15 + 0.25 * (1.0 - hardenability)) * quality;
                b += 0.08;
            }
            case SURFACE_HARDENING -> {
                h += 0.35 * hardenability * quality;
                b += 0.05;
            }
        }
        return state.with(kind, h, b);
    }
}
