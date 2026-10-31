package com.forjasuprema.treatment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeldRulesTest {
    @Test
    void bothPiecesMustReachWeldingHeat() {
        // Ferro: topo da janela 1250 -> minimo 1150.
        assertTrue(WeldRules.canWeld(1200, 1250, 1250));
        assertFalse(WeldRules.canWeld(1200, 1100, 1250));
        assertFalse(WeldRules.canWeld(900, 1250, 1250));
    }

    @Test
    void qualityRisesWithTheColdestPiece() {
        double low = WeldRules.quality(1150, 1250, 1250);
        double high = WeldRules.quality(1250, 1250, 1250);
        assertEquals(0.5, low, 1e-9);
        assertEquals(1.0, high, 1e-9);
        assertTrue(WeldRules.quality(1200, 1250, 1250) > low);
    }
}
