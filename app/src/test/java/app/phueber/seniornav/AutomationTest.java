package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class AutomationTest {
    @Test public void parsesEveryAction() {
        assertEquals(Automation.Command.BAR_HIDE, Automation.parse("app.phueber.seniornav.action.BAR_HIDE"));
        assertEquals(Automation.Command.BAR_SHOW, Automation.parse("app.phueber.seniornav.action.BAR_SHOW"));
        assertEquals(Automation.Command.BAR_SIDES, Automation.parse("app.phueber.seniornav.action.BAR_SIDES"));
        assertEquals(Automation.Command.BACK, Automation.parse("app.phueber.seniornav.action.BACK"));
        assertEquals(Automation.Command.HOME, Automation.parse("app.phueber.seniornav.action.HOME"));
    }

    @Test public void constantsMatchTheParser() {
        assertEquals(Automation.Command.BAR_HIDE, Automation.parse(Automation.BAR_HIDE));
        assertEquals(Automation.Command.HOME, Automation.parse(Automation.HOME));
    }

    @Test public void isLenientAboutCaseAndSpaces() {
        assertEquals(Automation.Command.BACK, Automation.parse(" app.phueber.seniornav.action.back "));
    }

    @Test public void ignoresAnythingElse() {
        assertNull(Automation.parse(null));
        assertNull(Automation.parse(""));
        assertNull(Automation.parse("app.phueber.seniornav.action."));
        assertNull(Automation.parse("app.phueber.seniornav.action.RECENTS"));
        assertNull(Automation.parse("app.phueber.seniorlight.action.BACK"));
        assertNull(Automation.parse("BACK"));
    }

    @Test public void readsTheBackSide() {
        assertEquals(Boolean.TRUE, Automation.backOnRight("right"));
        assertEquals(Boolean.FALSE, Automation.backOnRight("left"));
        assertEquals(Boolean.TRUE, Automation.backOnRight("  RIGHT "));
        assertEquals(Boolean.FALSE, Automation.backOnRight("Left"));
    }

    @Test public void readsTheColour() {
        assertEquals(Boolean.TRUE, Automation.lightBar("light"));
        assertEquals(Boolean.FALSE, Automation.lightBar(" Dark "));
        assertEquals(Boolean.TRUE, Automation.lightBar("hell"));
        assertEquals(Boolean.FALSE, Automation.lightBar("dunkel"));
        assertNull(Automation.lightBar(null));
        assertNull(Automation.lightBar("grey"));
        assertEquals(Automation.Command.BAR_COLOUR,
                Automation.parse("app.phueber.seniornav.action.BAR_COLOUR"));
    }

    @Test public void ignoresAMalformedSide() {
        assertNull(Automation.backOnRight(null));
        assertNull(Automation.backOnRight(""));
        assertNull(Automation.backOnRight("middle"));
        assertNull(Automation.backOnRight("true"));
    }

    @Test public void parsesTheNewCommands() {
        assertEquals(Automation.Command.BAR_UNLOCKED, Automation.parse(Automation.BAR_UNLOCKED));
        assertEquals(Automation.Command.VOLUME_KEYS, Automation.parse("app.phueber.seniornav.action.VOLUME_KEYS"));
        assertEquals(Automation.Command.VOLUME_LIMIT, Automation.parse(Automation.VOLUME_LIMIT));
        assertEquals(Automation.Command.PATTERN_REMOVE, Automation.parse(Automation.PATTERN_REMOVE));
        assertEquals(Automation.Command.PULLDOWN, Automation.parse("app.phueber.seniornav.action.PULLDOWN"));
        assertNull(Automation.parse("app.phueber.seniornav.action.UNLOCK"));
    }

    @Test public void readsOnOff() {
        assertEquals(Boolean.TRUE, Automation.onOff(" On "));
        assertEquals(Boolean.TRUE, Automation.onOff("ja"));
        assertEquals(Boolean.FALSE, Automation.onOff("off"));
        assertEquals(Boolean.FALSE, Automation.onOff("aus"));
        assertNull(Automation.onOff("maybe"));
        assertNull(Automation.onOff(null));
    }

    @Test public void readsTheStream() {
        assertEquals(VolumeLimits.Stream.RING, Automation.stream("ring"));
        assertEquals(VolumeLimits.Stream.MEDIA, Automation.stream(" Media "));
        assertEquals(VolumeLimits.Stream.ALARM, Automation.stream("wecker"));
        assertNull(Automation.stream("call"));
        assertNull(Automation.stream(null));
    }

    @Test public void readsSteps() {
        assertEquals(3, Automation.step(" 3 ", false));
        assertEquals(0, Automation.step("0", false));
        assertEquals(Automation.ABSENT, Automation.step(null, false));
        assertEquals(Automation.BAD, Automation.step("-1", false));
        assertEquals(Automation.BAD, Automation.step("loud", false));
        assertEquals(Automation.BAD, Automation.step("101", false));
        assertEquals(VolumeLimits.NO_MAX, Automation.step("top", true));
        assertEquals(Automation.BAD, Automation.step("top", false));
    }
}
