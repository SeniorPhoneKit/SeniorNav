package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomationLimitTest {
    private static final long T0 = 1_000_000;

    @Test public void firstActionGoesAhead() {
        assertTrue(new AutomationLimit().allow(T0));
    }

    @Test public void atMostOnePerSecond() {
        AutomationLimit l = new AutomationLimit();
        assertTrue(l.allow(T0));
        assertFalse(l.allow(T0 + 999));
        assertTrue(l.allow(T0 + 1_000));
    }

    @Test public void refusedActionsDoNotCount() {
        AutomationLimit l = new AutomationLimit();
        assertTrue(l.allow(T0));
        for (int i = 1; i < 100; i++) assertFalse(l.allow(T0 + i));
        assertTrue(l.allow(T0 + 1_000));
    }

    @Test public void aRuleFiringNowAndThenIsNeverStopped() {
        AutomationLimit l = new AutomationLimit();
        for (int i = 0; i < 100; i++) assertTrue(l.allow(T0 + i * 10_000L)); // 6 a minute
    }

    @Test public void floodGetsTheBurstThenCooldown() {
        AutomationLimit l = new AutomationLimit();
        int allowed = 0;
        long t = T0;
        for (; t < T0 + 30_000; t += 10) {
            if (l.allow(t)) allowed++;
        }
        assertEquals(AutomationLimit.BURST, allowed);
        // the cooldown started at the 7th second-spaced attempt (T0 + 6 s)
        long coolStart = T0 + AutomationLimit.BURST * AutomationLimit.MIN_GAP_MS;
        assertFalse(l.allow(coolStart + AutomationLimit.COOLDOWN_MS - 1));
        assertTrue(l.allow(coolStart + AutomationLimit.COOLDOWN_MS));
    }

    @Test public void cooldownStartsOnlyWithinTheWindow() {
        AutomationLimit l = new AutomationLimit();
        for (int i = 0; i < AutomationLimit.BURST; i++) assertTrue(l.allow(T0 + i * 1_000L));
        // the 7th inside the minute is refused and starts the cooldown
        assertFalse(l.allow(T0 + 59_999));
        assertFalse(l.allow(T0 + 59_999 + AutomationLimit.COOLDOWN_MS - 1));
        assertTrue(l.allow(T0 + 59_999 + AutomationLimit.COOLDOWN_MS));
    }

    @Test public void oldActionsLeaveTheWindow() {
        AutomationLimit l = new AutomationLimit();
        for (int i = 0; i < AutomationLimit.BURST; i++) assertTrue(l.allow(T0 + i * 1_000L));
        assertTrue(l.allow(T0 + AutomationLimit.WINDOW_MS)); // the first has left the minute
    }

    @Test public void freshBudgetAfterCooldown() {
        AutomationLimit l = new AutomationLimit();
        long t = T0;
        for (int i = 0; i < AutomationLimit.BURST; i++, t += 1_000) assertTrue(l.allow(t));
        assertFalse(l.allow(t)); // cooldown from t
        t += AutomationLimit.COOLDOWN_MS;
        for (int i = 0; i < AutomationLimit.BURST; i++, t += 1_000) assertTrue(l.allow(t));
        assertFalse(l.allow(t));
    }
}
