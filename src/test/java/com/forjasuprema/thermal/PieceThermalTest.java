package com.forjasuprema.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PieceThermalTest {
    @Test
    void noBurnInsideOrBelowTheWorkWindow() {
        assertEquals(0.0, PieceThermal.burnPerTick(20, 1250, 1538));
        assertEquals(0.0, PieceThermal.burnPerTick(1250, 1250, 1538));
    }

    @Test
    void burnGrowsTowardsTheMeltingPoint() {
        double slightlyOver = PieceThermal.burnPerTick(1300, 1250, 1538);
        double nearMelt = PieceThermal.burnPerTick(1500, 1250, 1538);
        assertTrue(slightlyOver > 0.0);
        assertTrue(nearMelt > slightlyOver);
        assertEquals(PieceThermal.BURN_PER_TICK_AT_MELT, PieceThermal.burnPerTick(1538, 1250, 1538), 1e-12);
        assertEquals(PieceThermal.BURN_PER_TICK_AT_MELT, PieceThermal.burnPerTick(2000, 1250, 1538), 1e-12);
    }

    @Test
    void invalidWindowNeverBurns() {
        assertEquals(0.0, PieceThermal.burnPerTick(900, 800, 800));
    }

    @Test
    void malleableMetalsHeatFaster() {
        assertTrue(PieceThermal.heatRate(0.95) > PieceThermal.heatRate(0.45));
    }

    @Test
    void ironPieceReachesWorkWindowInsideBlacksmithForgeInReasonableTime() {
        double temp = ThermalMath.AMBIENT;
        int ticks = 0;
        while (temp < 900 && ticks < 20_000) {
            temp = ThermalMath.approach(temp, 1250, PieceThermal.heatRate(0.60));
            ticks++;
        }
        assertTrue(ticks < 600, "peca deve aquecer em menos de 30 s: " + ticks + " ticks");
    }

    @Test
    void coolingFromForgeHeatTakesSecondsNotMinutes() {
        double after20s = ThermalMath.cooledAfter(1000, ThermalMath.AMBIENT, 400, PieceThermal.COOLING_RATE);
        assertTrue(after20s < 400 && after20s > ThermalMath.AMBIENT);
        assertFalse(PieceThermal.isCooled(after20s));
        assertTrue(PieceThermal.isCooled(ThermalMath.cooledAfter(1000, ThermalMath.AMBIENT, 5000, PieceThermal.COOLING_RATE)));
    }
}
