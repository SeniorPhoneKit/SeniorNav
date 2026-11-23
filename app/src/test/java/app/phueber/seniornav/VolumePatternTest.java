package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VolumePatternTest {
    @Test public void acceptsFourToSixPressesInBothDirections() {
        assertEquals("UUDD", VolumePattern.normalize("UUDD"));
        assertEquals("UDUDUD", VolumePattern.normalize(" ududud "));
        assertNull(VolumePattern.normalize("UUD"));
        assertNull(VolumePattern.normalize("UUDDUUD"));
        assertNull(VolumePattern.normalize("UUUU"));   // what a senior does to turn it up
        assertNull(VolumePattern.normalize("DDDDD"));
        assertNull(VolumePattern.normalize("UUXD"));
        assertNull(VolumePattern.normalize(null));
    }

    @Test public void showsArrows() {
        assertEquals("▲ ▲ ▼ ▼", VolumePattern.arrows("UUDD"));
    }

    private static String feed(VolumePattern.Matcher<String> m, String pattern, String keys, long... times) {
        String hit = null;
        for (int i = 0; i < keys.length(); i++) {
            String r = m.press(pattern, keys.charAt(i), times[i], "before" + i);
            if (r != null) hit = r;
        }
        return hit;
    }

    @Test public void matchesInTime() {
        VolumePattern.Matcher<String> m = new VolumePattern.Matcher<>();
        assertEquals("before0", feed(m, "UUDD", "UUDD", 0, 300, 600, 900));
        assertFalse(m.pending());
    }

    @Test public void reportsOnlyOnTheLastPress() {
        VolumePattern.Matcher<String> m = new VolumePattern.Matcher<>();
        assertNull(m.press("UUDD", 'U', 0, "a"));
        assertNull(m.press("UUDD", 'U', 100, "b"));
        assertNull(m.press("UUDD", 'D', 200, "c"));
        assertTrue(m.pending());
        assertEquals("a", m.press("UUDD", 'D', 300, "d"));
    }

    @Test public void aStrayPressBeforeTheGapDoesNotSpoilIt() {
        VolumePattern.Matcher<String> m = new VolumePattern.Matcher<>();
        // U U U D D: the first U is dropped, the pattern starts at the second
        assertEquals("before1", feed(m, "UUDD", "UUUDD", 0, 200, 400, 600, 800));
    }

    @Test public void aWrongPressStartsOver() {
        VolumePattern.Matcher<String> m = new VolumePattern.Matcher<>();
        assertNull(feed(m, "UUDD", "UDDD", 0, 200, 400, 600));
        assertEquals("before0", feed(m, "UUDD", "UUDD", 800, 1000, 1200, 1400));
    }

    @Test public void tooLongAGapStartsOver() {
        VolumePattern.Matcher<String> m = new VolumePattern.Matcher<>();
        assertNull(feed(m, "UUDD", "UUDD", 0, 300, 1301, 1500));
        // exactly the gap is still fine
        m.reset();
        assertEquals("before0", feed(m, "UUDD", "UUDD", 0, 1000, 2000, 2500));
    }

    @Test public void theWholePatternMustFitInThreeSeconds() {
        VolumePattern.Matcher<String> m = new VolumePattern.Matcher<>();
        assertNull(feed(m, "UUDDUD", "UUDDUD", 0, 700, 1400, 2100, 2800, 3500));
        m.reset();
        assertEquals("before0", feed(m, "UUDDUD", "UUDDUD", 0, 600, 1200, 1800, 2400, 3000));
    }

    @Test public void resetForgets() {
        VolumePattern.Matcher<String> m = new VolumePattern.Matcher<>();
        m.press("UUDD", 'U', 0, "a");
        m.press("UUDD", 'U', 100, "b");
        m.reset();
        assertNull(m.press("UUDD", 'D', 200, "c"));
        assertNull(m.press("UUDD", 'D', 300, "d"));
    }

    @Test public void noPatternNeverMatches() {
        VolumePattern.Matcher<String> m = new VolumePattern.Matcher<>();
        assertNull(feed(m, null, "UUDD", 0, 100, 200, 300));
        assertNull(feed(m, "UUUU", "UUUU", 0, 100, 200, 300));
        assertFalse(m.pending());
    }

    @Test public void matchesAgainRightAfterAMatch() {
        VolumePattern.Matcher<String> m = new VolumePattern.Matcher<>();
        assertEquals("before0", feed(m, "UDUD", "UDUD", 0, 100, 200, 300));
        assertEquals("before0", feed(m, "UDUD", "UDUD", 400, 500, 600, 700));
    }
}
