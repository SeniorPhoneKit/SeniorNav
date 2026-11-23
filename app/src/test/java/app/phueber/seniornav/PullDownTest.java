package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PullDownTest {
    private static final LockState LOCKED = new LockState(true, true, true, true);
    private static final LockState OPEN = new LockState(true, true, false, true);

    @Test public void blocksOnlyWhileLockedWithTheSettingOn() {
        assertTrue(PullDown.blocks(true, LOCKED, false, true, false));
        assertFalse(PullDown.blocks(false, LOCKED, false, true, false)); // setting off (default)
        assertFalse(PullDown.blocks(true, OPEN, false, true, false));    // the helper unlocked
    }

    @Test public void withoutSeniorLauncherNothingIsBlocked() {
        assertFalse(PullDown.blocks(true, LockState.NONE, false, true, false));
        assertFalse(PullDown.blocks(true, null, false, true, false));
        // an older SeniorLauncher can still say it is locked
        assertTrue(PullDown.blocks(true, new LockState(true, false, true, false), false, true, false));
    }

    @Test public void stepsAsideOnTheLockScreenScreenOffAndScreensaver() {
        assertFalse(PullDown.blocks(true, LOCKED, true, true, false));  // phone's own lock screen
        assertFalse(PullDown.blocks(true, LOCKED, false, false, false)); // screen off
        assertFalse(PullDown.blocks(true, LOCKED, false, true, true));   // screensaver
    }

    @Test public void lockInUse() {
        assertFalse(PullDown.lockInUse(LockState.NONE));
        assertFalse(PullDown.lockInUse(new LockState(true, true, false, false))); // no PIN yet
        assertTrue(PullDown.lockInUse(OPEN));
        assertTrue(PullDown.lockInUse(new LockState(true, false, false, false))); // older: cannot tell
    }

    @Test public void stripIsAsTallAsTheStatusBar() {
        assertEquals(63, PullDown.stripHeight(63, 0, 72, 60));
        assertEquals(110, PullDown.stripHeight(63, 110, 72, 60)); // a deep cutout
        assertEquals(72, PullDown.stripHeight(0, 0, 72, 60));
        assertEquals(60, PullDown.stripHeight(0, -1, 0, 60));
    }

    @Test public void closesAtMostEveryGap() {
        assertTrue(PullDown.mayClose(1000, 0));
        assertFalse(PullDown.mayClose(1100, 1000));
        assertTrue(PullDown.mayClose(1000 + PullDown.CLOSE_GAP_MS, 1000));
    }

    @Test public void onlySystemUiEventsCount() {
        assertTrue(PullDown.fromSystemUi("com.android.systemui"));
        assertFalse(PullDown.fromSystemUi("com.android.systemui.x"));
        assertFalse(PullDown.fromSystemUi(null));
        assertFalse(PullDown.fromSystemUi("app.phueber.seniorlauncher"));
    }
}
