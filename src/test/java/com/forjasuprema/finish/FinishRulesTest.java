package com.forjasuprema.finish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FinishRulesTest {
    @Test
    void slotsGrowWithMetalTier() {
        assertEquals(1, FinishRules.engravingSlots(0));
        assertEquals(1, FinishRules.engravingSlots(1));
        assertEquals(2, FinishRules.engravingSlots(2));
        assertEquals(2, FinishRules.engravingSlots(3));
        assertEquals(3, FinishRules.engravingSlots(4));
    }

    @Test
    void hotPiecesCannotBeFinished() {
        assertEquals(FinishRules.Check.TOO_HOT, FinishRules.checkSurfaceWork(300));
        assertEquals(FinishRules.Check.OK, FinishRules.checkSurfaceWork(25));
        FinishState polished = FinishState.NONE.withPolish(0.8);
        assertEquals(FinishRules.Check.TOO_HOT, FinishRules.checkEngraving(polished, 2, 300));
    }

    @Test
    void engravingNeedsAPolishedSurfaceAndAFreeSlot() {
        assertEquals(FinishRules.Check.NEEDS_POLISH, FinishRules.checkEngraving(FinishState.NONE, 2, 20));
        FinishState polished = FinishState.NONE.withPolish(0.5);
        assertEquals(FinishRules.Check.OK, FinishRules.checkEngraving(polished, 0, 20));
        FinishState engraved = FinishRules.engrave(polished, 0.9);
        assertEquals(FinishRules.Check.NO_SLOT, FinishRules.checkEngraving(engraved, 0, 20));
        assertEquals(FinishRules.Check.OK, FinishRules.checkEngraving(engraved, 2, 20));
    }

    @Test
    void polishingAndSharpeningNeverMakeThingsWorse() {
        FinishState state = FinishRules.polish(FinishState.NONE, 0.9);
        assertEquals(0.9, FinishRules.polish(state, 0.4).polish(), 1e-9);
        FinishState sharp = FinishRules.sharpen(FinishState.NONE, 1.0, 0.8, 0.0);
        assertEquals(sharp.sharpness(), FinishRules.sharpen(sharp, 0.2, 0.8, 0.0).sharpness(), 1e-9);
    }

    @Test
    void harderAndLessBrittleBladesTakeABetterEdge() {
        double soft = FinishRules.sharpen(FinishState.NONE, 1.0, 0.2, 0.0).sharpness();
        double hard = FinishRules.sharpen(FinishState.NONE, 1.0, 0.9, 0.0).sharpness();
        double brittle = FinishRules.sharpen(FinishState.NONE, 1.0, 0.9, 0.9).sharpness();
        assertTrue(hard > soft);
        assertTrue(brittle < hard);
    }

    @Test
    void aPoorStrokeGivesAPoorEdge() {
        double poor = FinishRules.sharpen(FinishState.NONE, 0.2, 0.9, 0.0).sharpness();
        double good = FinishRules.sharpen(FinishState.NONE, 1.0, 0.9, 0.0).sharpness();
        assertTrue(good > poor * 3);
    }

    @Test
    void runesNeedAnEngravingTheTierAndThePolish() {
        FinishState polished = FinishState.NONE.withPolish(0.9);
        assertEquals(FinishRules.Check.NO_ENGRAVING, FinishRules.checkRune(polished, "keen", 3, 1, 0.3));
        FinishState engraved = FinishRules.engrave(polished, 1.0);
        assertEquals(FinishRules.Check.TIER_TOO_LOW, FinishRules.checkRune(engraved, "keen", 0, 2, 0.3));
        assertEquals(FinishRules.Check.NEEDS_POLISH,
                FinishRules.checkRune(FinishState.NONE.withPolish(0.1).withEngraving(1.0), "keen", 3, 1, 0.5));
        assertEquals(FinishRules.Check.OK, FinishRules.checkRune(engraved, "keen", 3, 1, 0.3));
    }

    @Test
    void eachEngravingHoldsOneRuneAndRunesDoNotRepeat() {
        FinishState engraved = FinishRules.engrave(FinishState.NONE.withPolish(0.9), 1.0);
        FinishState inscribed = FinishRules.inscribe(engraved, "keen", 0.8);
        assertEquals(FinishRules.Check.NO_SLOT, FinishRules.checkRune(inscribed, "swift", 3, 0, 0.0));
        FinishState twoSlots = FinishRules.engrave(inscribed, 1.0);
        assertEquals(FinishRules.Check.DUPLICATE_RUNE, FinishRules.checkRune(twoSlots, "keen", 3, 0, 0.0));
        assertEquals(FinishRules.Check.OK, FinishRules.checkRune(twoSlots, "swift", 3, 0, 0.0));
    }

    @Test
    void runePowerFollowsTheEngravingQuality() {
        FinishState sloppy = FinishState.NONE.withEngraving(0.0);
        FinishState careful = FinishState.NONE.withEngraving(1.0);
        assertEquals(0.4, FinishRules.runePower(sloppy, 0.8), 1e-9);
        assertEquals(0.8, FinishRules.runePower(careful, 0.8), 1e-9);
    }
}
