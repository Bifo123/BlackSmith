package com.forjasuprema.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThermalMathTest {
    @Test
    void flameIsFuelPlusTierBonusCappedByTier() {
        assertEquals(0, ThermalMath.flameLevel(0, ForgeTier.MASTER));
        assertEquals(1, ThermalMath.flameLevel(1, ForgeTier.APPRENTICE));
        assertEquals(2, ThermalMath.flameLevel(3, ForgeTier.APPRENTICE), "aprendiz suporta no maximo chama 2");
        assertEquals(3, ThermalMath.flameLevel(3, ForgeTier.SMITH));
        assertEquals(3, ThermalMath.flameLevel(3, ForgeTier.ADVANCED), "avancada limita em 3");
        assertEquals(4, ThermalMath.flameLevel(3, ForgeTier.MASTER), "mestre soma bonus e chega a 4");
        assertEquals(4, ThermalMath.flameLevel(4, ForgeTier.LEGENDARY));
    }

    @Test
    void targetTemperatureIsLimitedByFuelAndStructure() {
        assertEquals(900, ThermalMath.targetTemperature(1000, ForgeTier.APPRENTICE));
        assertEquals(600, ThermalMath.targetTemperature(600, ForgeTier.MASTER));
        assertEquals(1550, ThermalMath.targetTemperature(1550, ForgeTier.MASTER));
    }

    @Test
    void stricterFlameHeatsFasterAndNeverOvershoots() {
        assertTrue(ThermalMath.forgeHeatRate(4, 1.0) > ThermalMath.forgeHeatRate(1, 1.0));

        double temp = ThermalMath.AMBIENT;
        double previous = temp;
        for (int tick = 0; tick < 5000; tick++) {
            temp = ThermalMath.approach(temp, 1200, ThermalMath.forgeHeatRate(3, 1.0));
            assertTrue(temp >= previous, "aquecimento deve ser monotono");
            assertTrue(temp <= 1200.0, "nao pode ultrapassar o alvo");
            previous = temp;
        }
        assertEquals(1200.0, temp, 1.0);
    }

    @Test
    void lazyCoolingMatchesTickByTickSimulation() {
        double rate = 0.0012;
        double simulated = 1100;
        for (int i = 0; i < 300; i++) {
            simulated = ThermalMath.approach(simulated, ThermalMath.AMBIENT, rate);
        }
        assertEquals(simulated, ThermalMath.cooledAfter(1100, ThermalMath.AMBIENT, 300, rate), 1e-6);
    }

    @Test
    void coolingNeverGoesBelowAmbientAndZeroTicksChangesNothing() {
        assertEquals(500.0, ThermalMath.cooledAfter(500, ThermalMath.AMBIENT, 0, 0.01));
        assertEquals(500.0, ThermalMath.cooledAfter(500, ThermalMath.AMBIENT, -10, 0.01));
        assertTrue(ThermalMath.cooledAfter(500, ThermalMath.AMBIENT, 1_000_000, 0.01) >= ThermalMath.AMBIENT);
    }

    @Test
    void betterTiersRetainHeatLonger() {
        assertTrue(ForgeTier.LEGENDARY.coolingRate() < ForgeTier.MASTER.coolingRate());
        assertTrue(ForgeTier.MASTER.coolingRate() < ForgeTier.SMITH.coolingRate());
        assertTrue(ForgeTier.SMITH.coolingRate() < ForgeTier.APPRENTICE.coolingRate());
    }

    @Test
    void progressionGatesMatchTheShippedAlloyRecipes() {
        // Bronze: chama 3 e 1100 C. Steel: chama 4 e 1500 C. Combustivel: carvao mineral (3, ate 1550 C).
        int coalIntensity = 3;
        int coalMaxTemp = 1550;

        assertEquals(HeatRequirement.Result.OK, reach(ForgeTier.SMITH, coalIntensity, coalMaxTemp, 3, 1100));
        assertEquals(HeatRequirement.Result.INSUFFICIENT_FLAME,
                reach(ForgeTier.APPRENTICE, coalIntensity, coalMaxTemp, 3, 1100));

        assertEquals(HeatRequirement.Result.OK, reach(ForgeTier.MASTER, coalIntensity, coalMaxTemp, 4, 1500));
        assertEquals(HeatRequirement.Result.INSUFFICIENT_FLAME,
                reach(ForgeTier.ADVANCED, coalIntensity, coalMaxTemp, 4, 1500));
        assertEquals(HeatRequirement.Result.INSUFFICIENT_FLAME,
                reach(ForgeTier.SMITH, coalIntensity, coalMaxTemp, 4, 1500));
    }

    private static HeatRequirement.Result reach(ForgeTier tier, int intensity, int fuelMaxTemp,
                                                int minFlame, int minTemp) {
        int flame = ThermalMath.flameLevel(intensity, tier);
        double temp = ThermalMath.targetTemperature(fuelMaxTemp, tier);
        return HeatRequirement.check(flame, temp, minFlame, minTemp);
    }
}
