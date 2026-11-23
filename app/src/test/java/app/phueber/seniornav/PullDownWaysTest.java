package app.phueber.seniornav;

import static app.phueber.seniornav.PullDownWays.ABSENT;
import static app.phueber.seniornav.PullDownWays.UNREADABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import app.phueber.seniornav.PullDownWays.Note;
import org.junit.Test;

public class PullDownWaysTest {
    private static final NavMode BUTTONS = NavMode.THREE_BUTTON;
    private static final NavMode GESTURE = NavMode.GESTURE;

    @Test public void namesTheFingerprintSwipeWhenItIsOn() {
        assertEquals(Note.FINGERPRINT, PullDownWays.note(true, 1, 35, BUTTONS));
        assertEquals(Note.FINGERPRINT, PullDownWays.note(true, 1, 35, GESTURE));
        // a clear reading counts on other phones too
        assertEquals(Note.FINGERPRINT, PullDownWays.note(false, 1, 30, NavMode.UNKNOWN));
    }

    @Test public void stockPhoneWithAllOffShowsNothing() {
        assertEquals(Note.NONE, PullDownWays.note(true, 0, 35, BUTTONS));
        // never stored is the AOSP default, off (the emulator has no such key)
        assertEquals(Note.NONE, PullDownWays.note(true, ABSENT, 35, BUTTONS));
        assertEquals(Note.NONE, PullDownWays.note(true, 0, 35, NavMode.TWO_BUTTON));
        // before Android 12 there is no One-handed mode
        assertEquals(Note.NONE, PullDownWays.note(true, 0, 30, GESTURE));
    }

    @Test public void oneHandedModeCannotBeReadSoGestureNavigationGetsTheGeneralLine() {
        assertEquals(Note.GENERIC, PullDownWays.note(true, 0, 35, GESTURE));
        assertEquals(Note.GENERIC, PullDownWays.note(true, 0, 31, NavMode.UNKNOWN));
        assertTrue(PullDownWays.oneHandedPossible(31, GESTURE));
        assertFalse(PullDownWays.oneHandedPossible(35, BUTTONS));
        assertFalse(PullDownWays.oneHandedPossible(30, GESTURE));
    }

    @Test public void unknownGivesTheGeneralLine() {
        assertEquals(Note.GENERIC, PullDownWays.note(true, UNREADABLE, 35, BUTTONS));
        assertEquals(Note.GENERIC, PullDownWays.note(true, 2, 35, BUTTONS)); // a value we do not know
    }

    @Test public void otherPhonesMayHaveTheirOwnGestures() {
        assertEquals(Note.GENERIC, PullDownWays.note(false, 0, 35, BUTTONS));
        assertEquals(Note.GENERIC, PullDownWays.note(false, ABSENT, 30, BUTTONS));
    }
}
