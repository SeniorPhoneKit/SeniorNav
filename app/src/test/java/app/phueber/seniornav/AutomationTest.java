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

    @Test public void ignoresAMalformedSide() {
        assertNull(Automation.backOnRight(null));
        assertNull(Automation.backOnRight(""));
        assertNull(Automation.backOnRight("middle"));
        assertNull(Automation.backOnRight("true"));
    }
}
