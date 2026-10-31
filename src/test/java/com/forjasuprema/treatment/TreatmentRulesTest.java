package com.forjasuprema.treatment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TreatmentRulesTest {
    // Aco: janela de forjamento 850-1150, fusao 1450.
    private static final List<String> STEEL = List.of("forjasuprema:annealing", "forjasuprema:quench",
            "forjasuprema:tempering", "forjasuprema:surface_hardening");
    private static final double MIN = 850, MAX = 1150, MELT = 1450;

    private static TreatmentRules.Check check(TreatmentKind kind, TreatmentState state, List<String> metal,
                                              int flame, double temp) {
        return TreatmentRules.checkStart(kind, state, metal, flame, temp, MIN, MELT);
    }

    private static final TreatmentState QUENCHED = TreatmentRules.apply(TreatmentKind.QUENCH, TreatmentState.NONE, 0.9, 1.0);

    @Test
    void weakFlameFailsEvenWithPerfectTemperature() {
        // Chama 1 < 3 exigida: falha por chama, nao importa que a temperatura esteja ideal.
        assertEquals(TreatmentRules.Check.INSUFFICIENT_FLAME,
                check(TreatmentKind.SURFACE_HARDENING, TreatmentState.NONE, STEEL, 1, 1000));
        assertEquals(TreatmentRules.Check.OK,
                check(TreatmentKind.SURFACE_HARDENING, TreatmentState.NONE, STEEL, 3, 1000));
    }

    @Test
    void flameIsCheckedBeforeTemperature() {
        assertEquals(TreatmentRules.Check.INSUFFICIENT_FLAME,
                check(TreatmentKind.ANNEALING, TreatmentState.NONE, STEEL, 1, 20));
        assertEquals(TreatmentRules.Check.TOO_COLD,
                check(TreatmentKind.ANNEALING, TreatmentState.NONE, STEEL, 2, 400));
    }

    @Test
    void temperingNeedsAPriorQuenchAndASoftHeat() {
        assertEquals(TreatmentRules.Check.REQUIRES_QUENCH, check(TreatmentKind.TEMPERING, TreatmentState.NONE, STEEL, 1, 400));
        assertEquals(TreatmentRules.Check.OK, check(TreatmentKind.TEMPERING, QUENCHED, STEEL, 1, 400));
        // Quente demais para revenir (acima de 60% da temperatura minima de forjamento).
        assertEquals(TreatmentRules.Check.TOO_HOT, check(TreatmentKind.TEMPERING, QUENCHED, STEEL, 1, 900));
        assertEquals(TreatmentRules.Check.TOO_COLD, check(TreatmentKind.TEMPERING, QUENCHED, STEEL, 1, 100));
    }

    @Test
    void temperingTwiceInARowIsRefused() {
        TreatmentState tempered = TreatmentRules.apply(TreatmentKind.TEMPERING, QUENCHED, 0.9, 1.0);
        assertEquals(TreatmentRules.Check.REQUIRES_QUENCH, check(TreatmentKind.TEMPERING, tempered, STEEL, 1, 400));
    }

    @Test
    void unsupportedTreatmentsAreRefused() {
        List<String> bronze = List.of("forjasuprema:annealing");
        assertEquals(TreatmentRules.Check.UNSUPPORTED, check(TreatmentKind.QUENCH, TreatmentState.NONE, bronze, 0, 1000));
    }

    @Test
    void quenchRequiresTheMinimumPieceTemperature() {
        assertEquals(TreatmentRules.Check.TOO_COLD, check(TreatmentKind.QUENCH, TreatmentState.NONE, STEEL, 0, 700));
        assertEquals(TreatmentRules.Check.OK, check(TreatmentKind.QUENCH, TreatmentState.NONE, STEEL, 0, 900));
    }

    @Test
    void furnaceMustStayBelowMelting() {
        assertEquals(TreatmentRules.Check.TOO_HOT, check(TreatmentKind.CARBURIZING, TreatmentState.NONE,
                List.of("forjasuprema:carburizing"), 3, 1420));
    }

    @Test
    void quenchQualityPeaksInsideTheWorkWindowAndFallsWhenOverheated() {
        assertEquals(0.0, TreatmentRules.quenchQuality(800, MIN, MAX, MELT), 1e-9);
        double justAbove = TreatmentRules.quenchQuality(860, MIN, MAX, MELT);
        double warm = TreatmentRules.quenchQuality(950, MIN, MAX, MELT);
        double overheated = TreatmentRules.quenchQuality(1400, MIN, MAX, MELT);
        assertTrue(justAbove < warm);
        assertEquals(1.0, warm, 1e-9);
        assertTrue(overheated < warm);
        assertTrue(overheated >= 0.3 - 1e-9);
    }

    @Test
    void quenchHardensAndEmbrittlesMoreOnHardenableMetals() {
        TreatmentState steel = TreatmentRules.apply(TreatmentKind.QUENCH, TreatmentState.NONE, 0.9, 1.0);
        TreatmentState iron = TreatmentRules.apply(TreatmentKind.QUENCH, TreatmentState.NONE, 0.25, 1.0);
        assertTrue(steel.hardness() > iron.hardness());
        assertTrue(steel.brittleness() > iron.brittleness());
        assertTrue(steel.has(TreatmentKind.QUENCH));
    }

    @Test
    void aBetterQuenchGivesAHarderPiece() {
        double poor = TreatmentRules.apply(TreatmentKind.QUENCH, TreatmentState.NONE, 0.9, 0.3).hardness();
        double good = TreatmentRules.apply(TreatmentKind.QUENCH, TreatmentState.NONE, 0.9, 1.0).hardness();
        assertTrue(good > poor);
    }

    @Test
    void temperingTradesABitOfHardnessForMuchLessBrittleness() {
        TreatmentState tempered = TreatmentRules.apply(TreatmentKind.TEMPERING, QUENCHED, 0.9, 1.0);
        assertTrue(tempered.brittleness() < QUENCHED.brittleness() * 0.5);
        assertTrue(tempered.hardness() < QUENCHED.hardness());
        assertTrue(tempered.hardness() > QUENCHED.hardness() * 0.8);
    }

    @Test
    void annealingResetsEverything() {
        TreatmentState annealed = TreatmentRules.apply(TreatmentKind.ANNEALING, QUENCHED, 0.9, 0.2);
        assertEquals(0.0, annealed.hardness(), 1e-9);
        assertEquals(0.0, annealed.brittleness(), 1e-9);
        assertFalse(annealed.has(TreatmentKind.QUENCH));
    }

    @Test
    void carburizingHelpsIronMoreThanSteel() {
        double iron = TreatmentRules.apply(TreatmentKind.CARBURIZING, TreatmentState.NONE, 0.25, 1.0).hardness();
        double steel = TreatmentRules.apply(TreatmentKind.CARBURIZING, TreatmentState.NONE, 0.9, 1.0).hardness();
        assertTrue(iron > steel);
    }

    @Test
    void hardnessNeverExceedsTheCap() {
        TreatmentState state = TreatmentState.NONE;
        for (int i = 0; i < 10; i++) {
            state = TreatmentRules.apply(TreatmentKind.QUENCH, state, 1.0, 1.0);
        }
        assertTrue(state.hardness() <= TreatmentState.HARDNESS_CAP + 1e-9);
        assertTrue(state.brittleness() <= 1.0);
    }

    @Test
    void holdQualityIsTheFractionOfTimeInBand() {
        assertEquals(1.0, TreatmentRules.holdQuality(600, 600), 1e-9);
        assertEquals(0.5, TreatmentRules.holdQuality(300, 600), 1e-9);
        assertEquals(0.0, TreatmentRules.holdQuality(0, 0), 1e-9);
    }

    @Test
    void kindsResolveFromMetalIds() {
        assertEquals(TreatmentKind.QUENCH, TreatmentKind.fromId("forjasuprema:quench").orElseThrow());
        assertTrue(TreatmentKind.fromId("forjasuprema:unknown").isEmpty());
    }
}
