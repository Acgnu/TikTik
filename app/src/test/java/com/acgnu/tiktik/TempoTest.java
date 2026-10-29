package com.acgnu.tiktik;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TempoTest {
    @Test public void crossingAngleSeamKeepsDirection() {
        assertEquals(2f, Tempo.angleDelta(179, -179), 0f);
        assertEquals(-2f, Tempo.angleDelta(-179, 179), 0f);
    }
    @Test public void completeTurnsHaveNoDiscontinuity() {
        float total = 0;
        float previous = 0;
        for (int step = 1; step <= 720; step++) {
            float current = (step + 180) % 360 - 180;
            total += Tempo.angleDelta(previous, current);
            previous = current;
        }
        assertEquals(720f, total, 0f);
    }
    @Test public void bpmNeverReachesZeroOrOverflowsDisplay() {
        assertEquals(1, Tempo.clamp(-20));
        assertEquals(999, Tempo.clamp(1200));
        assertEquals(140, Tempo.clamp(140));
    }
}
