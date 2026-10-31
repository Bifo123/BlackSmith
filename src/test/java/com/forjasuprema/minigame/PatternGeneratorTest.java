package com.forjasuprema.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PatternGeneratorTest {
    @Test
    void sameSeedGivesSamePattern() {
        assertEquals(PatternGenerator.generate(42L, 6, true), PatternGenerator.generate(42L, 6, true));
        assertNotEquals(PatternGenerator.generate(42L, 6, true), PatternGenerator.generate(43L, 6, true));
    }

    @Test
    void directionalPatternsStayInsideTheBarAndKeepSpacing() {
        for (long seed = 0; seed < 500; seed++) {
            var pattern = PatternGenerator.generate(seed, 8, true);
            assertEquals(8, pattern.size());
            assertEquals(8, pattern.directions().size());
            for (int i = 0; i < pattern.size(); i++) {
                float target = pattern.targets().get(i);
                assertTrue(target >= PatternGenerator.MIN_TARGET - 1e-6F && target <= PatternGenerator.MAX_TARGET + 1e-6F,
                        "alvo fora da faixa: " + target + " (semente " + seed + ")");
                assertNotEquals(StrikeDirection.NONE, pattern.directions().get(i));
                if (i > 0) {
                    float gap = Math.abs(target - pattern.targets().get(i - 1));
                    assertTrue(gap >= PatternGenerator.MIN_SPACING - 1e-5F, "alvos muito proximos: " + gap);
                }
            }
        }
    }

    @Test
    void nonDirectionalPatternsAimAtTheCenterWithoutDirections() {
        var pattern = PatternGenerator.generate(7L, 6, false);
        assertEquals(6, pattern.size());
        assertTrue(pattern.targets().stream().allMatch(t -> t == 0.5F));
        assertTrue(pattern.directions().stream().allMatch(d -> d == StrikeDirection.NONE));
    }

    @Test
    void zeroStrikesGivesAnEmptyPattern() {
        assertEquals(0, PatternGenerator.generate(1L, 0, true).size());
    }
}
