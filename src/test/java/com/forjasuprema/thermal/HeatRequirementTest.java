package com.forjasuprema.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HeatRequirementTest {
    @Test
    void weakFlameFailsEvenWhenTemperatureIsEnough() {
        assertEquals(HeatRequirement.Result.INSUFFICIENT_FLAME, HeatRequirement.check(2, 1400, 3, 1000));
    }

    @Test
    void flameIsCheckedBeforeTemperature() {
        assertEquals(HeatRequirement.Result.INSUFFICIENT_FLAME, HeatRequirement.check(1, 100, 3, 1000));
    }

    @Test
    void coldMetalFailsWithEnoughFlame() {
        assertEquals(HeatRequirement.Result.TOO_COLD, HeatRequirement.check(4, 999.9, 3, 1000));
    }

    @Test
    void exactRequirementPasses() {
        assertEquals(HeatRequirement.Result.OK, HeatRequirement.check(3, 1000, 3, 1000));
    }

    @Test
    void recipeWithoutRequirementAlwaysPasses() {
        assertEquals(HeatRequirement.Result.OK, HeatRequirement.check(0, 20, 0, 0));
    }

    @Test
    void flameLevelsClampAndHaveTranslationKeys() {
        assertEquals(FlameLevel.OFF, FlameLevel.fromLevel(-5));
        assertEquals(FlameLevel.INTENSE, FlameLevel.fromLevel(99));
        assertEquals("flame.forjasuprema.2", FlameLevel.MODERATE.translationKey());
    }
}
