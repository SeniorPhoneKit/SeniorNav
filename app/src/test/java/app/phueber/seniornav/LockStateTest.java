package app.phueber.seniornav;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LockStateTest {
    @Test public void hidesTheBarOnlyWhileTheLockIsInUseAndOpen() {
        assertTrue(new LockState(true, true, false, true).hidesBar(true));
        assertFalse(new LockState(true, true, true, true).hidesBar(true));   // locked: the senior needs it
        assertFalse(new LockState(true, true, false, true).hidesBar(false)); // the helper switched it off
    }

    @Test public void aPhoneThatNeverUsesTheLockKeepsItsBar() {
        assertFalse(new LockState(true, true, false, false).hidesBar(true));
        assertFalse(LockState.NONE.hidesBar(true));
        // an older SeniorLauncher cannot say whether a PIN is set
        assertFalse(new LockState(true, false, false, false).hidesBar(true));
    }
}
