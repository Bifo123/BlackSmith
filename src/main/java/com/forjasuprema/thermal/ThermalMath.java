package com.forjasuprema.thermal;

/**
 * Matematica termica do mod. Classe pura (testavel sem iniciar o jogo).
 *
 * <p>Todas as taxas sao "por tick". A forja e simulada tick a tick por {@link #approach}; a peca
 * fora da forja esfria de forma preguicosa por {@link #cooledAfter}. As duas formas sao
 * matematicamente equivalentes (uma e a forma fechada da outra), o que mantem o resfriamento
 * consistente sem precisar de tick por item.
 */
public final class ThermalMath {
    /** Temperatura ambiente, em graus Celsius. */
    public static final double AMBIENT = 20.0;

    /** Taxa base de aquecimento da forja por tick, antes de eficiencia e chama. */
    public static final double FORGE_HEAT_BASE = 0.006;

    private ThermalMath() {
    }

    /**
     * Intensidade efetiva da chama: combustivel + bonus da estrutura, limitada pelo teto do tier.
     * Combustivel de intensidade 0 (ou negativa) nao produz chama.
     */
    public static int flameLevel(int fuelIntensity, ForgeTier tier) {
        if (fuelIntensity <= 0) {
            return 0;
        }
        return Math.min(fuelIntensity + tier.flameBonus(), tier.maxFlame());
    }

    /** Temperatura que a forja tende a atingir: limitada pelo combustivel e pela estrutura. */
    public static int targetTemperature(int fuelMaxTemperature, ForgeTier tier) {
        return Math.min(fuelMaxTemperature, tier.maxTemperature());
    }

    /** Velocidade de aquecimento da forja: chama mais intensa aquece mais depressa. */
    public static double forgeHeatRate(int flame, double fuelEfficiency) {
        return FORGE_HEAT_BASE * fuelEfficiency * (0.5 + 0.25 * flame);
    }

    /** Um tick de aproximacao exponencial de {@code current} em direcao a {@code target}. */
    public static double approach(double current, double target, double rate) {
        return current + (target - current) * (1.0 - Math.exp(-rate));
    }

    /** Temperatura de uma peca depois de {@code ticks} ticks esfriando em direcao ao ambiente. */
    public static double cooledAfter(double temperature, double ambient, long ticks, double rate) {
        if (ticks <= 0) {
            return temperature;
        }
        return ambient + (temperature - ambient) * Math.exp(-rate * ticks);
    }
}
