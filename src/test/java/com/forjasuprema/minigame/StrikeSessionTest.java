package com.forjasuprema.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StrikeSessionTest {
    private static final long START = 1_000L;
    private static final int PERIOD = 40;

    private static StrikeSession session(MinigameKind kind, int strikes) {
        return new StrikeSession(kind, 99L, strikes, new MinigameDifficulty.Params(0.10, PERIOD, 0.5), START);
    }

    /** Instante (em ticks, na subida do marcador, na volta {@code lap}) em que o marcador esta no alvo. */
    private static double hitTime(StrikeSession s, int index, int lap) {
        return s.pattern().targets().get(index) * PERIOD / 2.0 + lap * PERIOD;
    }

    private static long scaled(double ticks) {
        return Math.round(ticks * StrikeSession.TIME_SCALE);
    }

    @Test
    void aStrikeOnTheTargetIsPerfect() {
        StrikeSession s = session(MinigameKind.BELLOWS, 3);
        double t = hitTime(s, 0, 1);
        var result = s.submit(0, scaled(t), StrikeDirection.NONE, START + Math.round(t), 1.0);
        assertTrue(result.isPresent());
        assertEquals(StrikeVerdict.PERFECT, result.get().verdict());
        assertEquals(1, s.nextIndex());
    }

    @Test
    void aStrikeFarFromTheTargetMisses() {
        StrikeSession s = session(MinigameKind.BELLOWS, 3);
        // Alvo em 0.5 -> o marcador esta no extremo (0.0) no instante 0 do ciclo.
        var result = s.submit(0, 0L, StrikeDirection.NONE, START + 1, 1.0);
        assertTrue(result.isPresent());
        assertEquals(StrikeVerdict.MISS, result.get().verdict());
    }

    @Test
    void outOfOrderIndexIsRefused() {
        StrikeSession s = session(MinigameKind.BELLOWS, 3);
        double t = hitTime(s, 0, 1);
        assertTrue(s.submit(1, scaled(t), StrikeDirection.NONE, START + Math.round(t), 1.0).isEmpty());
        assertEquals(0, s.nextIndex());
    }

    @Test
    void strikesCloserThanTheMinimumGapAreRefused() {
        StrikeSession s = session(MinigameKind.BELLOWS, 3);
        double t0 = hitTime(s, 0, 1);
        assertTrue(s.submit(0, scaled(t0), StrikeDirection.NONE, START + Math.round(t0), 1.0).isPresent());
        // Mesmo tick do servidor: recusado pelo limite de taxa.
        assertTrue(s.submit(1, scaled(t0 + 0.5), StrikeDirection.NONE, START + Math.round(t0), 1.0).isEmpty());
        // Instante informado quase igual ao anterior: recusado pelo intervalo minimo.
        assertTrue(s.submit(1, scaled(t0 + 1), StrikeDirection.NONE, START + Math.round(t0) + 5, 1.0).isEmpty());
        assertEquals(1, s.nextIndex());
    }

    @Test
    void aClaimFarFromTheServerClockFallsBackToTheServerClock() {
        StrikeSession s = session(MinigameKind.BELLOWS, 3);
        double perfect = hitTime(s, 0, 1);
        // O cliente diz que bateu no ponto perfeito, mas o servidor so viu passar 1 tick: a alegacao e ignorada.
        var result = s.submit(0, scaled(perfect), StrikeDirection.NONE, START + 1, 1.0);
        assertTrue(result.isPresent());
        assertEquals(StrikeVerdict.MISS, result.get().verdict());
    }

    @Test
    void wrongDirectionScoresZeroEvenOnTarget() {
        StrikeSession s = session(MinigameKind.HAMMERING, 3);
        double t = hitTime(s, 0, 1);
        StrikeDirection wanted = s.pattern().directions().get(0);
        StrikeDirection wrong = wanted == StrikeDirection.UP ? StrikeDirection.DOWN : StrikeDirection.UP;
        var result = s.submit(0, scaled(t), wrong, START + Math.round(t), 1.0);
        assertTrue(result.isPresent());
        assertEquals(StrikeVerdict.WRONG_DIRECTION, result.get().verdict());
        assertEquals(0.0, s.scores().get(0), 1e-9);
    }

    @Test
    void aColdPieceDoesNotRescueAPerfectStrike() {
        StrikeSession s = session(MinigameKind.HAMMERING, 3);
        double t = hitTime(s, 0, 1);
        var result = s.submit(0, scaled(t), s.pattern().directions().get(0), START + Math.round(t), 0.4);
        assertTrue(result.isPresent());
        assertEquals(0.4, result.get().accuracy(), 0.02);
        assertEquals(StrikeVerdict.POOR, result.get().verdict());
    }

    @Test
    void theSessionFinishesAfterTheLastStrikeAndScoresIt() {
        StrikeSession s = session(MinigameKind.BELLOWS, 2);
        double previous = -100;
        for (int i = 0; i < 2; i++) {
            int lap = 1;
            double t = hitTime(s, i, lap);
            // Avanca voltas ate respeitar o intervalo minimo entre golpes.
            while (t - previous < 6) {
                lap++;
                t = hitTime(s, i, lap);
            }
            var result = s.submit(i, scaled(t), StrikeDirection.NONE, START + Math.round(t), 1.0);
            assertTrue(result.isPresent(), "golpe " + i + " recusado");
            assertEquals(i == 1, result.get().finished());
            previous = t;
        }
        assertTrue(s.isFinished());
        assertTrue(s.finalScore() > 0.9);
        // Depois de terminada, nada mais e aceito.
        assertTrue(s.submit(2, 0L, StrikeDirection.NONE, START + 500, 1.0).isEmpty());
    }

    @Test
    void sessionsExpireOnTheirOwn() {
        StrikeSession s = session(MinigameKind.BELLOWS, 3);
        assertFalse(s.isExpired(START + 100));
        assertTrue(s.isExpired(START + 3L * PERIOD * 4L + 201L));
    }
}
