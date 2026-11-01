package com.forjasuprema.quality;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Calculo de qualidade (ver docs/ARCHITECTURE.md, secao 5). Classe pura e SEM aleatoriedade: o mesmo
 * desempenho sempre da a mesma classe.
 *
 * <pre>
 * base = soma(peso_i * nota_i) / soma(peso_i)       (so etapas presentes)
 * teto = menor nota de ARTESANATO + 0.35            (uma etapa ruim limita o resultado)
 * Q    = min(base, teto)
 * </pre>
 *
 * O material entra na media, mas nao no teto: nao e desempenho do ferreiro.
 */
public final class QualityCalculator {
    public static final String MATERIAL = "material";
    public static final String MODELING = "modeling";
    public static final String THERMAL = "thermal";
    public static final String WELD = "weld";
    public static final String TREATMENT = "treatment";
    public static final String FINISH = "finish";

    /** Folga do teto sobre a pior etapa de artesanato. */
    public static final double CAP_MARGIN = 0.35;
    public static final double LEGENDARY_SCORE = 0.97;
    public static final double LEGENDARY_MIN_STAGE = 0.90;
    public static final int LEGENDARY_MIN_METAL_TIER = 3;
    /** Indice minimo da forja (0 Aprendiz ... 4 Lendaria): Mestre. */
    public static final int LEGENDARY_MIN_FORGE_TIER = 3;

    /** Resumo de uma peca usada na montagem. {@code weld} vazio = a peca nao foi soldada. */
    public record PartSummary(double modeling, double thermal, OptionalDouble weld, double treatment,
                              double polish, double sharpness) {
    }

    /** Condicoes extras do Lendario. */
    public record LegendaryContext(boolean recipeCapable, int metalTier, int forgeTier) {
    }

    public record Result(QualityTier tier, double score, Map<String, Double> stages) {
    }

    private QualityCalculator() {
    }

    /** Nota do material (0.5 a 1): metais mais fortes e duros dao um ponto de partida melhor. */
    public static double materialScore(double strength, double hardness) {
        double avg = (clamp01(strength) + clamp01(hardness)) / 2.0;
        return 0.5 + 0.5 * avg;
    }

    /**
     * Notas por etapa a partir das pecas. Etapas que a receita exige e que faltam valem 0; a soldagem so
     * conta se alguma peca foi soldada; o tratamento so conta se o metal admite tratamentos de endurecimento.
     */
    public static Map<String, Double> aggregateStages(List<PartSummary> parts, double material, boolean treatmentApplicable) {
        Map<String, Double> stages = new LinkedHashMap<>();
        stages.put(MATERIAL, clamp01(material));
        if (parts.isEmpty()) {
            return stages;
        }
        int n = parts.size();
        double modeling = 0.0;
        double thermal = 0.0;
        double weldSum = 0.0;
        int welded = 0;
        double treatment = 0.0;
        double polish = 0.0;
        double sharpness = 0.0;
        for (PartSummary part : parts) {
            modeling += part.modeling();
            thermal += part.thermal();
            if (part.weld().isPresent()) {
                weldSum += part.weld().getAsDouble();
                welded++;
            }
            treatment += part.treatment();
            polish += part.polish();
            sharpness = Math.max(sharpness, part.sharpness());
        }
        stages.put(MODELING, clamp01(modeling / n));
        stages.put(THERMAL, clamp01(thermal / n));
        if (welded > 0) {
            stages.put(WELD, clamp01(weldSum / welded));
        }
        if (treatmentApplicable) {
            stages.put(TREATMENT, clamp01(treatment / n));
        }
        stages.put(FINISH, clamp01((polish / n + sharpness) / 2.0));
        return stages;
    }

    public static Result evaluate(Map<String, Double> stages, Map<String, Double> weights, LegendaryContext legendary) {
        double weightSum = 0.0;
        double weighted = 0.0;
        double worstCraft = Double.MAX_VALUE;
        for (Map.Entry<String, Double> entry : stages.entrySet()) {
            double weight = weights.getOrDefault(entry.getKey(), 0.0);
            if (weight > 0.0) {
                weightSum += weight;
                weighted += weight * entry.getValue();
            }
            if (!MATERIAL.equals(entry.getKey())) {
                worstCraft = Math.min(worstCraft, entry.getValue());
            }
        }
        double base = weightSum <= 0.0 ? 0.0 : weighted / weightSum;
        double cap = worstCraft == Double.MAX_VALUE ? 1.0 : worstCraft + CAP_MARGIN;
        double score = clamp01(Math.min(base, cap));

        QualityTier tier = QualityTier.forScore(score);
        if (score >= LEGENDARY_SCORE && meetsLegendary(stages, legendary)) {
            tier = QualityTier.LEGENDARY;
        }
        return new Result(tier, score, Map.copyOf(stages));
    }

    private static boolean meetsLegendary(Map<String, Double> stages, LegendaryContext ctx) {
        if (!ctx.recipeCapable() || ctx.metalTier() < LEGENDARY_MIN_METAL_TIER
                || ctx.forgeTier() < LEGENDARY_MIN_FORGE_TIER) {
            return false;
        }
        return stages.entrySet().stream()
                .filter(entry -> !MATERIAL.equals(entry.getKey()))
                .allMatch(entry -> entry.getValue() >= LEGENDARY_MIN_STAGE);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
