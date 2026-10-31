package com.forjasuprema.thermal;

/**
 * Termica das pecas de metal (aquecimento na forja, resfriamento ao ar e queima por
 * superaquecimento). Classe pura: nao depende do Minecraft.
 */
public final class PieceThermal {
    /** Taxa por tick de resfriamento da peca ao ar livre (constante de tempo de ~20 s). */
    public static final double COOLING_RATE = 0.0025;

    /** Dano de queima por tick quando a peca esta na iminencia de derreter. */
    public static final double BURN_PER_TICK_AT_MELT = 0.01;

    private PieceThermal() {
    }

    /** Taxa por tick com que a peca acompanha a temperatura da forja; metais maleaveis aquecem mais depressa. */
    public static double heatRate(double malleability) {
        return 0.006 * (0.5 + malleability);
    }

    /**
     * Queima acumulada por tick acima da janela de forjamento: proporcional a quanto a peca
     * esta perto de derreter. Dentro da janela (ou abaixo) nao ha queima.
     */
    public static double burnPerTick(double temperature, double workTempMax, double meltTemperature) {
        if (temperature <= workTempMax || meltTemperature <= workTempMax) {
            return 0.0;
        }
        double fraction = Math.min(1.0, (temperature - workTempMax) / (meltTemperature - workTempMax));
        return BURN_PER_TICK_AT_MELT * fraction;
    }

    /** Peca considerada fria (indistinguivel do ambiente). */
    public static boolean isCooled(double temperature) {
        return temperature <= ThermalMath.AMBIENT + 1.0;
    }
}
