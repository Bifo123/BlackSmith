package com.forjasuprema.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StrikeMathTest {
    @Test
    void cursorSweepsThereAndBackWithinBounds() {
        int period = 40;
        assertEquals(0.0, StrikeMath.cursor(0, period), 1e-9);
        assertEquals(0.5, StrikeMath.cursor(10, period), 1e-9);
        assertEquals(1.0, StrikeMath.cursor(20, period), 1e-9);
        assertEquals(0.5, StrikeMath.cursor(30, period), 1e-9);
        assertEquals(0.0, StrikeMath.cursor(40, period), 1e-9);
        for (double t = 0; t < 400; t += 0.37) {
            double c = StrikeMath.cursor(t, period);
            assertTrue(c >= 0.0 && c <= 1.0, "fora da barra: " + c);
        }
    }

    @Test
    void cursorIsPeriodicAndTolerantToNegativeTime() {
        assertEquals(StrikeMath.cursor(7, 40), StrikeMath.cursor(47, 40), 1e-9);
        assertEquals(StrikeMath.cursor(-5, 40), StrikeMath.cursor(35, 40), 1e-9);
        assertEquals(0.5, StrikeMath.cursor(12, 0), 1e-9);
    }

    @Test
    void accuracyIsPerfectNearCenterAndZeroFarAway() {
        double hw = 0.10;
        assertEquals(1.0, StrikeMath.accuracy(0.0, hw));
        assertEquals(1.0, StrikeMath.accuracy(0.035, hw), 1e-9);
        assertEquals(0.0, StrikeMath.accuracy(0.16, hw), 1e-9);
        assertEquals(0.0, StrikeMath.accuracy(0.5, hw));
        double mid = StrikeMath.accuracy(0.10, hw);
        assertTrue(mid > 0.0 && mid < 1.0);
        assertTrue(StrikeMath.accuracy(0.06, hw) > StrikeMath.accuracy(0.12, hw));
        assertEquals(StrikeMath.accuracy(0.07, hw), StrikeMath.accuracy(-0.07, hw));
        assertEquals(0.0, StrikeMath.accuracy(0.0, 0.0));
    }

    @Test
    void temperatureFactorHasAFullWindowAndPenalizesBothSides() {
        double min = 900;
        double max = 1250;
        double melt = 1538;
        assertEquals(1.0, StrikeMath.temperatureFactor(900, min, max, melt));
        assertEquals(1.0, StrikeMath.temperatureFactor(1250, min, max, melt));
        assertEquals(0.0, StrikeMath.temperatureFactor(650, min, max, melt), 1e-9);
        assertEquals(0.0, StrikeMath.temperatureFactor(20, min, max, melt));
        assertTrue(StrikeMath.temperatureFactor(800, min, max, melt) < 1.0);
        assertTrue(StrikeMath.temperatureFactor(800, min, max, melt) > StrikeMath.temperatureFactor(700, min, max, melt));
        assertEquals(StrikeMath.OVERHEATED_FLOOR, StrikeMath.temperatureFactor(1538, min, max, melt), 1e-9);
        assertTrue(StrikeMath.temperatureFactor(1400, min, max, melt) < 1.0);
    }

    @Test
    void sessionAbortsOnlyWhenWayTooCold() {
        assertFalse(StrikeMath.shouldAbortCold(800, 900));
        assertFalse(StrikeMath.shouldAbortCold(700, 900));
        assertTrue(StrikeMath.shouldAbortCold(699, 900));
    }

    @Test
    void perfectSessionScoresOneAndMissesScoreZero() {
        assertEquals(1.0, StrikeMath.sessionScore(List.of(1.0, 1.0, 1.0, 1.0)), 1e-9);
        assertEquals(0.0, StrikeMath.sessionScore(List.of(0.0, 0.0, 0.0)), 1e-9);
        assertEquals(0.0, StrikeMath.sessionScore(List.of()), 1e-9);
    }

    @Test
    void keepingTheRhythmBeatsScatteredHitsWithTheSameAverage() {
        // Os dois conjuntos tem media 0.8, mas so o primeiro mantem a sequencia de acertos.
        double steady = StrikeMath.sessionScore(List.of(0.8, 0.8, 0.8, 0.8));
        double scattered = StrikeMath.sessionScore(List.of(1.0, 0.6, 1.0, 0.6));
        assertEquals(0.85 * 0.8 + 0.15, steady, 1e-9);
        assertTrue(steady > scattered);
    }

    @Test
    void longestStreakCountsConsecutiveGoodStrikes() {
        assertEquals(0, StrikeMath.longestStreak(List.of()));
        assertEquals(3, StrikeMath.longestStreak(List.of(0.9, 0.8, 0.7, 0.2, 1.0)));
        assertEquals(2, StrikeMath.longestStreak(List.of(0.9, 0.1, 0.9, 0.9, 0.69)));
    }

    @Test
    void trustedElapsedBoundsClientClockCheating() {
        // Dentro da tolerancia: vale o relogio do cliente (compensa a latencia).
        assertEquals(98, StrikeMath.trustedElapsed(98, 100, 8));
        assertEquals(108, StrikeMath.trustedElapsed(108, 100, 8));
        // Fora da tolerancia: vale o relogio do servidor.
        assertEquals(100, StrikeMath.trustedElapsed(60, 100, 8));
        assertEquals(100, StrikeMath.trustedElapsed(500, 100, 8));
        // Nunca negativo.
        assertEquals(0, StrikeMath.trustedElapsed(-4, 2, 8));
    }

    @Test
    void verdictsFollowAccuracyAndDirection() {
        assertEquals(StrikeVerdict.PERFECT, StrikeVerdict.of(1.0, true));
        assertEquals(StrikeVerdict.GOOD, StrikeVerdict.of(0.7, true));
        assertEquals(StrikeVerdict.POOR, StrikeVerdict.of(0.2, true));
        assertEquals(StrikeVerdict.MISS, StrikeVerdict.of(0.0, true));
        assertEquals(StrikeVerdict.WRONG_DIRECTION, StrikeVerdict.of(1.0, false));
    }

    @Test
    void networkOrdinalsAreSafeForInvalidValues() {
        assertEquals(StrikeDirection.NONE, StrikeDirection.fromOrdinal(-1));
        assertEquals(StrikeDirection.NONE, StrikeDirection.fromOrdinal(99));
        assertEquals(StrikeDirection.RIGHT, StrikeDirection.fromOrdinal(4));
        assertEquals(MinigameKind.BELLOWS, MinigameKind.fromOrdinal(99));
        assertEquals(MinigameKind.HAMMERING, MinigameKind.fromOrdinal(-3));
    }

    @Test
    void automatedProcessesNeverAllowMinigames() {
        assertTrue(ProcessingMode.MANUAL.allowsMinigames());
        assertFalse(ProcessingMode.AUTOMATED.allowsMinigames());
    }
}
