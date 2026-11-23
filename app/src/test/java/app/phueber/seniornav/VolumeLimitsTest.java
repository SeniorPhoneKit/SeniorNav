package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VolumeLimitsTest {
    private static final VolumeLimits NONE = new VolumeLimits(0, VolumeLimits.NO_MAX);

    @Test public void noLimitsChangeNothing() {
        assertTrue(NONE.unlimited(0, 7));
        assertEquals(0, NONE.clamp(0, 0, 7));
        assertEquals(7, NONE.clamp(7, 0, 7));
    }

    @Test public void clampsIntoRange() {
        VolumeLimits l = new VolumeLimits(3, 5);
        assertFalse(l.unlimited(0, 7));
        assertEquals(3, l.clamp(0, 0, 7));
        assertEquals(4, l.clamp(4, 0, 7));
        assertEquals(5, l.clamp(7, 0, 7));
    }

    @Test public void followsTheStreamsOwnRange() {
        // alarm cannot go below 1 on most phones; a stored max above the top is the top
        VolumeLimits l = new VolumeLimits(0, 30);
        assertEquals(1, l.low(1, 7));
        assertEquals(7, l.high(1, 7));
        assertTrue(l.unlimited(1, 7));
        // a minimum above the top (a phone with fewer steps) is the top
        assertEquals(7, new VolumeLimits(12, VolumeLimits.NO_MAX).clamp(2, 0, 7));
    }

    @Test public void highNeverBelowLow() {
        VolumeLimits l = new VolumeLimits(5, 2);
        assertEquals(5, l.low(0, 7));
        assertEquals(5, l.high(0, 7));
        assertEquals(5, l.clamp(0, 0, 7));
    }

    @Test public void stepsOnlyWithinRange() {
        VolumeLimits l = new VolumeLimits(2, 5);
        assertTrue(l.stepAllowed(4, +1, 0, 7));
        assertFalse(l.stepAllowed(5, +1, 0, 7));
        assertTrue(l.stepAllowed(3, -1, 0, 7));
        assertFalse(l.stepAllowed(2, -1, 0, 7));
        // out of range already: back towards the range is fine, further away is not
        assertTrue(l.stepAllowed(7, -1, 0, 7));
        assertFalse(l.stepAllowed(7, +1, 0, 7));
        assertTrue(l.stepAllowed(0, +1, 0, 7));
        assertFalse(l.stepAllowed(0, -1, 0, 7));
    }

    @Test public void raisingTheMinimumPushesTheMaximum() {
        VolumeLimits l = new VolumeLimits(1, 3).withMin(5, 0, 7);
        assertEquals(5, l.min);
        assertEquals(5, l.max);
        VolumeLimits open = NONE.withMin(4, 0, 7);
        assertEquals(4, open.min);
        assertEquals(VolumeLimits.NO_MAX, open.max);
        assertEquals(0, NONE.withMin(-1, 0, 7).min);
    }

    @Test public void loweringTheMaximumPushesTheMinimum() {
        VolumeLimits l = new VolumeLimits(4, VolumeLimits.NO_MAX).withMax(2, 0, 7);
        assertEquals(2, l.min);
        assertEquals(2, l.max);
        // back to the top means no maximum, so a phone with more steps is not capped
        assertEquals(VolumeLimits.NO_MAX, l.withMax(7, 0, 7).max);
        assertEquals(VolumeLimits.NO_MAX, l.withMax(9, 0, 7).max);
    }
}
