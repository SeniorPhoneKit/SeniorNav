package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VolumeKeysTest {
    @Test public void parsesModes() {
        assertEquals(VolumeKeys.Mode.NORMAL, VolumeKeys.parse("normal"));
        assertEquals(VolumeKeys.Mode.LIMITS, VolumeKeys.parse(" Limits "));
        assertEquals(VolumeKeys.Mode.OFF, VolumeKeys.parse("OFF"));
        assertEquals(VolumeKeys.Mode.OFF, VolumeKeys.parse("aus"));
        assertEquals(VolumeKeys.Mode.LIMITS, VolumeKeys.parse("grenzen"));
        assertNull(VolumeKeys.parse("loud"));
        assertNull(VolumeKeys.parse(null));
        for (VolumeKeys.Mode m : VolumeKeys.Mode.values()) assertEquals(m, VolumeKeys.parse(m.id));
    }

    @Test public void unlockedEverythingPasses() {
        for (VolumeKeys.Mode m : VolumeKeys.Mode.values()) {
            assertEquals(VolumeKeys.Action.PASS, VolumeKeys.decide(m, false, false));
        }
    }

    @Test public void lockedFollowsTheMode() {
        assertEquals(VolumeKeys.Action.PASS, VolumeKeys.decide(VolumeKeys.Mode.NORMAL, true, false));
        assertEquals(VolumeKeys.Action.LIMIT, VolumeKeys.decide(VolumeKeys.Mode.LIMITS, true, false));
        assertEquals(VolumeKeys.Action.BLOCK, VolumeKeys.decide(VolumeKeys.Mode.OFF, true, false));
        assertEquals(VolumeKeys.Action.PASS, VolumeKeys.decide(null, true, false));
    }

    @Test public void aRingingPhoneOrACallIsNeverTouched() {
        for (VolumeKeys.Mode m : VolumeKeys.Mode.values()) {
            assertEquals(VolumeKeys.Action.PASS, VolumeKeys.decide(m, true, true));
        }
    }

    @Test public void limitsAreEnforcedOnlyLockedInLimitsMode() {
        assertTrue(VolumeKeys.enforceLimits(VolumeKeys.Mode.LIMITS, true));
        assertFalse(VolumeKeys.enforceLimits(VolumeKeys.Mode.LIMITS, false));
        assertFalse(VolumeKeys.enforceLimits(VolumeKeys.Mode.OFF, true));
        assertFalse(VolumeKeys.enforceLimits(VolumeKeys.Mode.NORMAL, true));
    }

    @Test public void keysAreFilteredOnlyWhenNeeded() {
        assertFalse(VolumeKeys.needKeys(null, VolumeKeys.Mode.NORMAL));
        assertFalse(VolumeKeys.needKeys("UUUU", VolumeKeys.Mode.NORMAL));
        assertTrue(VolumeKeys.needKeys("UUDD", VolumeKeys.Mode.NORMAL));
        assertTrue(VolumeKeys.needKeys(null, VolumeKeys.Mode.OFF));
        assertTrue(VolumeKeys.needKeys(null, VolumeKeys.Mode.LIMITS));
    }
}
