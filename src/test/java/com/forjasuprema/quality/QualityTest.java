package com.forjasuprema.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class QualityTest {
    private static final Map<String, Double> WEIGHTS = Map.of(
            "material", 0.10, "modeling", 0.30, "thermal", 0.15, "weld", 0.10, "treatment", 0.20, "finish", 0.25);
    private static final QualityCalculator.LegendaryContext RARE_MASTER =
            new QualityCalculator.LegendaryContext(true, 3, 3);
    private static final QualityCalculator.LegendaryContext COMMON_CTX =
            new QualityCalculator.LegendaryContext(true, 1, 1);

    private static Map<String, Double> stages(double material, double modeling, double thermal, double treatment, double finish) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("material", material);
        m.put("modeling", modeling);
        m.put("thermal", thermal);
        m.put("treatment", treatment);
        m.put("finish", finish);
        return m;
    }

    @Test
    void scoresMapToTheSevenClasses() {
        assertEquals(QualityTier.DEFECTIVE, QualityTier.forScore(0.1));
        assertEquals(QualityTier.RUDIMENTARY, QualityTier.forScore(0.3));
        assertEquals(QualityTier.COMMON, QualityTier.forScore(0.5));
        assertEquals(QualityTier.GOOD, QualityTier.forScore(0.7));
        assertEquals(QualityTier.EXCELLENT, QualityTier.forScore(0.85));
        assertEquals(QualityTier.PERFECT, QualityTier.forScore(0.93));
        assertEquals(7, QualityTier.values().length);
    }

    @Test
    void oneBadStageCapsTheWholeItem() {
        // Tudo 1.0 menos o acabamento 0.2: a media seria alta, mas o teto e 0.2 + 0.35 = 0.55.
        var result = QualityCalculator.evaluate(stages(1, 1, 1, 1, 0.2), WEIGHTS, RARE_MASTER);
        assertEquals(0.55, result.score(), 1e-9);
        assertEquals(QualityTier.COMMON, result.tier());
    }

    @Test
    void aFlawlessRunReachesPerfectAtLeast() {
        var result = QualityCalculator.evaluate(stages(0.8, 1, 1, 1, 1), WEIGHTS, COMMON_CTX);
        assertTrue(result.score() >= 0.90);
        assertEquals(QualityTier.PERFECT, result.tier());
    }

    @Test
    void legendaryNeedsEveryCondition() {
        var flawless = stages(1, 1, 1, 1, 1);
        assertEquals(QualityTier.LEGENDARY, QualityCalculator.evaluate(flawless, WEIGHTS, RARE_MASTER).tier());
        // Metal comum, forja simples, receita sem permissao: cai para Perfeito mesmo com nota 1.0.
        assertEquals(QualityTier.PERFECT, QualityCalculator.evaluate(flawless, WEIGHTS, COMMON_CTX).tier());
        assertEquals(QualityTier.PERFECT, QualityCalculator.evaluate(flawless, WEIGHTS,
                new QualityCalculator.LegendaryContext(false, 3, 3)).tier());
        assertEquals(QualityTier.PERFECT, QualityCalculator.evaluate(flawless, WEIGHTS,
                new QualityCalculator.LegendaryContext(true, 3, 2)).tier());
    }

    @Test
    void legendaryNeedsEveryStageAtNinety() {
        var result = QualityCalculator.evaluate(stages(1, 1, 1, 1, 0.89), WEIGHTS, RARE_MASTER);
        assertTrue(result.tier() != QualityTier.LEGENDARY);
    }

    @Test
    void theSamePerformanceAlwaysGivesTheSameResult() {
        var a = QualityCalculator.evaluate(stages(0.7, 0.9, 0.8, 0.6, 0.7), WEIGHTS, RARE_MASTER);
        var b = QualityCalculator.evaluate(stages(0.7, 0.9, 0.8, 0.6, 0.7), WEIGHTS, RARE_MASTER);
        assertEquals(a, b);
    }

    @Test
    void missingStagesAreIgnoredAndWeightsRenormalised() {
        Map<String, Double> only = new LinkedHashMap<>();
        only.put("material", 1.0);
        only.put("modeling", 1.0);
        assertEquals(1.0, QualityCalculator.evaluate(only, WEIGHTS, COMMON_CTX).score(), 1e-9);
    }

    @Test
    void aggregationAveragesPartsAndTakesTheBestEdge() {
        var parts = List.of(
                new QualityCalculator.PartSummary(0.8, 1.0, OptionalDouble.empty(), 0.5, 0.6, 0.0),
                new QualityCalculator.PartSummary(0.6, 0.8, OptionalDouble.of(0.9), 0.3, 0.8, 0.9));
        var stages = QualityCalculator.aggregateStages(parts, 0.75, true);
        assertEquals(0.7, stages.get("modeling"), 1e-9);
        assertEquals(0.9, stages.get("thermal"), 1e-9);
        assertEquals(0.9, stages.get("weld"), 1e-9);
        assertEquals(0.4, stages.get("treatment"), 1e-9);
        // finish = (media do polimento 0.7 + melhor fio 0.9) / 2
        assertEquals(0.8, stages.get("finish"), 1e-9);
        assertTrue(!QualityCalculator.aggregateStages(parts, 0.75, false).containsKey("treatment"));
    }

    @Test
    void anUnfinishedPieceScoresZeroInFinish() {
        var raw = List.of(new QualityCalculator.PartSummary(1, 1, OptionalDouble.empty(), 1, 0, 0));
        assertEquals(0.0, QualityCalculator.aggregateStages(raw, 1.0, true).get("finish"), 1e-9);
    }

    @Test
    void strongerMaterialStartsHigher() {
        assertTrue(QualityCalculator.materialScore(0.8, 0.75) > QualityCalculator.materialScore(0.55, 0.45));
    }

    @Test
    void betterClassesGiveMoreDamageAndDurability() {
        double base = 5.0;
        double mf = EquipmentStats.materialFactor(0.55, 0.45);
        assertEquals(1.0, mf, 1e-9);
        assertTrue(EquipmentStats.attackDamage(base, mf, QualityTier.EXCELLENT, 0.5)
                > EquipmentStats.attackDamage(base, mf, QualityTier.COMMON, 0.5));
        assertTrue(EquipmentStats.attackDamage(base, mf, QualityTier.COMMON, 1.0)
                > EquipmentStats.attackDamage(base, mf, QualityTier.COMMON, 0.0));
        assertTrue(EquipmentStats.durability(250, 0.55, QualityTier.LEGENDARY, 0.0)
                > EquipmentStats.durability(250, 0.55, QualityTier.DEFECTIVE, 0.0));
        assertTrue(EquipmentStats.durability(250, 0.55, QualityTier.COMMON, 0.9)
                < EquipmentStats.durability(250, 0.55, QualityTier.COMMON, 0.0));
    }

    @Test
    void extraAttributesAreDeterministicAndOnlyForTopClasses() {
        Map<String, Double> st = stages(0.9, 0.95, 0.9, 0.9, 0.95);
        long seed = ExtraAttributes.seed("forjasuprema:sword", "forjasuprema:steel", st);
        assertEquals(seed, ExtraAttributes.seed("forjasuprema:sword", "forjasuprema:steel", new LinkedHashMap<>(st)));
        assertEquals(ExtraAttributes.select(seed, QualityTier.PERFECT), ExtraAttributes.select(seed, QualityTier.PERFECT));
        assertEquals(0, ExtraAttributes.select(seed, QualityTier.EXCELLENT).size());
        assertEquals(1, ExtraAttributes.select(seed, QualityTier.PERFECT).size());
        var legendary = ExtraAttributes.select(seed, QualityTier.LEGENDARY);
        assertEquals(2, legendary.size());
        assertTrue(!legendary.get(0).attribute().equals(legendary.get(1).attribute()));
    }

    @Test
    void aDifferentPerformanceChangesTheSeed() {
        long a = ExtraAttributes.seed("r", "m", stages(0.9, 0.95, 0.9, 0.9, 0.95));
        long b = ExtraAttributes.seed("r", "m", stages(0.9, 0.96, 0.9, 0.9, 0.95));
        assertTrue(a != b);
    }
}
