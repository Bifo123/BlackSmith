package com.forjasuprema.thermal;

/**
 * Categorias de forja (secao 4.6 do documento mestre). Classe pura: nao depende do Minecraft.
 *
 * <ul>
 *   <li>{@code maxFlame}: teto de intensidade que a estrutura suporta;</li>
 *   <li>{@code flameBonus}: intensidade somada a do combustivel (melhor controle termico);</li>
 *   <li>{@code maxTemperature}: teto de temperatura da forja, em graus Celsius;</li>
 *   <li>{@code heatRetention}: 0 a 1, quanto maior, mais lentamente a forja esfria.</li>
 * </ul>
 */
public enum ForgeTier {
    APPRENTICE("apprentice", 2, 0, 900, 0.50),
    SMITH("blacksmith", 3, 0, 1250, 0.70),
    ADVANCED("advanced", 3, 1, 1400, 0.80),
    MASTER("master", 4, 1, 1600, 0.90),
    LEGENDARY("legendary", 4, 1, 2100, 0.95);

    private final String id;
    private final int maxFlame;
    private final int flameBonus;
    private final int maxTemperature;
    private final double heatRetention;

    ForgeTier(String id, int maxFlame, int flameBonus, int maxTemperature, double heatRetention) {
        this.id = id;
        this.maxFlame = maxFlame;
        this.flameBonus = flameBonus;
        this.maxTemperature = maxTemperature;
        this.heatRetention = heatRetention;
    }

    public String id() {
        return id;
    }

    public int maxFlame() {
        return maxFlame;
    }

    public int flameBonus() {
        return flameBonus;
    }

    public int maxTemperature() {
        return maxTemperature;
    }

    public double heatRetention() {
        return heatRetention;
    }

    /** Taxa por tick com que a forja apagada esfria em direcao a temperatura ambiente. */
    public double coolingRate() {
        return 0.004 * (1.0 - heatRetention);
    }

    public static ForgeTier byId(String id) {
        for (ForgeTier tier : values()) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Tier de forja desconhecido: " + id);
    }
}
