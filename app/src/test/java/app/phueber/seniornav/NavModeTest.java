package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavModeTest {
    @Test public void parsesTheThreeModes() {
        assertEquals(NavMode.THREE_BUTTON, NavMode.parse(0));
        assertEquals(NavMode.TWO_BUTTON, NavMode.parse(1));
        assertEquals(NavMode.GESTURE, NavMode.parse(2));
    }

    @Test public void unreadableOrUnknownValuesAreUnknown() {
        assertEquals(NavMode.UNKNOWN, NavMode.parse(-1));
        assertEquals(NavMode.UNKNOWN, NavMode.parse(3));
    }

    @Test public void theSettingWinsOverTheResource() {
        assertEquals(NavMode.GESTURE, NavMode.of(2, 0));
    }

    @Test public void theResourceIsTheFallback() {
        assertEquals(NavMode.THREE_BUTTON, NavMode.of(-1, 0));
        assertEquals(NavMode.UNKNOWN, NavMode.of(-1, -1));
    }

    @Test public void onlyThreeButtonFitsTheBar() {
        assertTrue(NavMode.THREE_BUTTON.barFits());
        assertFalse(NavMode.GESTURE.barFits());
        assertFalse(NavMode.TWO_BUTTON.barFits());
        assertFalse(NavMode.UNKNOWN.barFits());
    }
}
