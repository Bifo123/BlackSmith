package com.forjasuprema.thermal;

/**
 * Condicao termica minima de um processo (liga, tempera, recozimento...). Classe pura.
 *
 * <p>Regra fundamental do documento mestre: a chama e verificada antes da temperatura e
 * independe de qualquer minigame. Chama fraca nunca vira sucesso por boa execucao.
 */
public final class HeatRequirement {
    public enum Result {
        OK,
        INSUFFICIENT_FLAME,
        TOO_COLD
    }

    private HeatRequirement() {
    }

    public static Result check(int flame, double temperature, int minFlame, double minTemperature) {
        if (flame < minFlame) {
            return Result.INSUFFICIENT_FLAME;
        }
        if (temperature < minTemperature) {
            return Result.TOO_COLD;
        }
        return Result.OK;
    }
}
