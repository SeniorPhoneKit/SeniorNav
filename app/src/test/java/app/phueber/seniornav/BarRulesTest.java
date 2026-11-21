package app.phueber.seniornav;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BarRulesTest {
    @Test public void showsWhenNothingIsInTheWay() {
        assertTrue(BarRules.show(false, false));
    }

    @Test public void hidesOnTheLockScreen() {
        assertFalse(BarRules.show(true, false));
    }

    @Test public void hidesWhileAKeyboardIsUp() {
        assertFalse(BarRules.show(false, true));
    }

    @Test public void anyReasonIsEnoughToHide() {
        assertFalse(BarRules.show(true, true));
    }
}
