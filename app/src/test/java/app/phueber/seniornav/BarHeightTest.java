package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BarHeightTest {
    private static final int FLOOR = 126; // 48dp at 2.625x

    @Test public void takesTheReportedInset() {
        assertEquals(144, BarHeight.choose(144, 126, FLOOR));
    }

    @Test public void fallsBackToTheResource() {
        assertEquals(140, BarHeight.choose(0, 140, FLOOR));
    }

    @Test public void neverBelowTheFloor() {
        assertEquals(FLOOR, BarHeight.choose(63, 63, FLOOR)); // gesture handle
        assertEquals(FLOOR, BarHeight.choose(0, 0, FLOOR));   // landscape, unknown
        assertEquals(FLOOR, BarHeight.choose(-1, -1, FLOOR));
    }
}
