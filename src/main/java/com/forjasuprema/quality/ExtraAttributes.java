package com.forjasuprema.quality;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Atributos extras de itens Perfeito (1) e Lendario (2). A escolha usa uma semente DETERMINISTICA derivada
 * da receita, do metal e das notas por etapa: o mesmo desempenho da o mesmo item, e nao ha como "rerrolar".
 */
public final class ExtraAttributes {
    /** Atributos possiveis, em ordem fixa. */
    public static final List<String> POOL = List.of("attack_speed", "movement_speed", "max_health", "knockback_resistance");

    /** Um bonus escolhido: atributo e magnitude (ja ajustada pela classe). */
    public record Bonus(String attribute, double amount) {
    }

    private ExtraAttributes() {
    }

    public static int count(QualityTier tier) {
        return switch (tier) {
            case PERFECT -> 1;
            case LEGENDARY -> 2;
            default -> 0;
        };
    }

    /** Magnitude base de cada atributo (Lendario multiplica por 1.5). */
    public static double baseAmount(String attribute) {
        return switch (attribute) {
            case "attack_speed" -> 0.20;
            case "movement_speed" -> 0.05;
            case "max_health" -> 2.0;
            case "knockback_resistance" -> 0.10;
            default -> 0.0;
        };
    }

    /** Semente estavel: receita + metal + notas arredondadas (milesimos), em ordem alfabetica de etapa. */
    public static long seed(String recipeId, String metalId, Map<String, Double> stages) {
        StringBuilder sb = new StringBuilder(recipeId).append('|').append(metalId);
        new TreeMap<>(stages).forEach((stage, score) -> sb.append('|').append(stage).append('=').append(Math.round(score * 1000.0)));
        long h = 1125899906842597L;
        for (int i = 0; i < sb.length(); i++) {
            h = 31 * h + sb.charAt(i);
        }
        return h;
    }

    public static List<Bonus> select(long seed, QualityTier tier) {
        int wanted = count(tier);
        List<Bonus> chosen = new ArrayList<>();
        if (wanted == 0) {
            return chosen;
        }
        Random random = new Random(seed);
        List<String> remaining = new ArrayList<>(POOL);
        double scale = tier == QualityTier.LEGENDARY ? 1.5 : 1.0;
        for (int i = 0; i < wanted && !remaining.isEmpty(); i++) {
            String attribute = remaining.remove(random.nextInt(remaining.size()));
            chosen.add(new Bonus(attribute, baseAmount(attribute) * scale));
        }
        return chosen;
    }
}
