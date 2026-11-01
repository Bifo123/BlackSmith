package com.forjasuprema.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinigameDifficultyTest {
    @Test
    void stifferMaterialsAndMoreComplexPiecesAreHarder() {
        var easy = MinigameDifficulty.forHammering(0.95, 0.2, 0, 0.5, 0);
        var stiff = MinigameDifficulty.forHammering(0.45, 0.2, 0, 0.5, 0);
        var complex = MinigameDifficulty.forHammering(0.95, 0.9, 0, 0.5, 0);
        assertTrue(stiff.difficulty() > easy.difficulty());
        assertTrue(complex.difficulty() > easy.difficulty());
        assertTrue(stiff.zoneHalfWidth() < easy.zoneHalfWidth());
        assertTrue(stiff.periodTicks() < easy.periodTicks());
    }

    @Test
    void higherMetalTierIsSlightlyHarder() {
        var common = MinigameDifficulty.forHammering(0.6, 0.3, 0, 0.5, 0);
        var rare = MinigameDifficulty.forHammering(0.6, 0.3, 4, 0.5, 0);
        assertTrue(rare.difficulty() > common.difficulty());
    }

    @Test
    void experienceAndBetterToolsMakeItEasier() {
        var novice = MinigameDifficulty.forHammering(0.6, 0.5, 1, 0.0, 0);
        var veteran = MinigameDifficulty.forHammering(0.6, 0.5, 1, 0.0, 50);
        var toolAssisted = MinigameDifficulty.forHammering(0.6, 0.5, 1, 1.0, 0);
        assertTrue(veteran.difficulty() < novice.difficulty());
        assertTrue(veteran.zoneHalfWidth() > novice.zoneHalfWidth());
        assertTrue(toolAssisted.difficulty() < novice.difficulty());
    }

    @Test
    void skillBonusIsCappedAndNeverMakesDifficultyNegative() {
        var capped = MinigameDifficulty.forHammering(0.6, 0.5, 1, 0.0, 50);
        var hugeLevel = MinigameDifficulty.forHammering(0.6, 0.5, 1, 0.0, 10_000);
        assertEquals(capped.difficulty(), hugeLevel.difficulty(), 1e-9);
        var trivial = MinigameDifficulty.forHammering(1.0, 0.0, 0, 1.0, 10_000);
        assertEquals(0.0, trivial.difficulty(), 1e-9);
    }

    @Test
    void parametersStayInPlayableRanges() {
        for (double mall = 0; mall <= 1.0; mall += 0.25) {
            for (double complexity = 0; complexity <= 1.0; complexity += 0.25) {
                for (int tier = 0; tier <= 4; tier++) {
                    var p = MinigameDifficulty.forHammering(mall, complexity, tier, 0.0, 0);
                    assertTrue(p.zoneHalfWidth() >= 0.045 && p.zoneHalfWidth() <= 0.130 + 1e-9);
                    assertTrue(p.periodTicks() >= 30 && p.periodTicks() <= 64);
                    assertTrue(p.difficulty() >= 0.0 && p.difficulty() <= 1.0);
                }
            }
        }
    }

    @Test
    void bellowsAreMoreForgivingThanHammering() {
        var bellows = MinigameDifficulty.forBellows(0.5, 0);
        var hammer = MinigameDifficulty.forHammering(0.65, 0.35, 1, 0.5, 0);
        assertTrue(bellows.zoneHalfWidth() > hammer.zoneHalfWidth());
        assertTrue(bellows.periodTicks() > hammer.periodTicks());
    }

    @Test
    void finishingGetsHarderFromPolishingToEngraving() {
        var polish = MinigameDifficulty.forFinishing(MinigameKind.POLISHING, 2, 0.0, 0);
        var sharpen = MinigameDifficulty.forFinishing(MinigameKind.SHARPENING, 2, 0.0, 0);
        var engrave = MinigameDifficulty.forFinishing(MinigameKind.ENGRAVING, 2, 0.0, 0);
        assertTrue(polish.difficulty() < sharpen.difficulty());
        assertTrue(sharpen.difficulty() < engrave.difficulty());
        assertTrue(MinigameDifficulty.forFinishing(MinigameKind.ENGRAVING, 4, 0.0, 0).difficulty() > engrave.difficulty());
    }
}
