package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import app.phueber.seniornav.Palette.Role;

import org.junit.Test;

/**
 * The combinations the screens and the bar actually draw must stay readable:
 * 7:1 (WCAG AAA) for body text and for the bar, 4.5:1 (AA) for bold button
 * text and secondary text.
 */
public class PaletteTest {
    private static double c(Role fg, Role bg) {
        return Palette.contrast(Palette.color(fg), Palette.color(bg));
    }

    private static void atLeast(double min, double actual, String what) {
        assertTrue(what + " contrast " + actual + " < " + min, actual >= min);
    }

    @Test public void everyRoleHasAColour() {
        assertEquals(Role.values().length, Palette.count());
    }

    @Test public void textIsReadableOnPageAndCards() {
        atLeast(7, c(Role.TEXT, Role.PAGE), "text/page");
        atLeast(7, c(Role.TEXT, Role.CARD), "text/card");
        atLeast(7, c(Role.TEXT, Role.GREEN_LIGHT), "text/green card");
        atLeast(7, c(Role.TEXT, Role.RED_LIGHT), "text/red card");
        atLeast(4.5, c(Role.TEXT_MUTED, Role.PAGE), "muted/page");
        atLeast(4.5, c(Role.TEXT_MUTED, Role.CARD), "muted/card");
    }

    @Test public void whiteButtonTextIsReadable() {
        for (Role r : new Role[] {Role.GREEN, Role.BLUE, Role.RED}) {
            atLeast(4.5, Palette.contrast(Palette.ON_COLOR, Palette.color(r)), "white/" + r);
        }
    }

    @Test public void theBarIsHighContrast() {
        atLeast(7, c(Role.ON_BAR, Role.BAR), "bar");
        atLeast(3, c(Role.BAR_DIVIDER, Role.BAR), "bar divider");
        atLeast(7, c(Role.ON_BAR_LIGHT, Role.BAR_LIGHT), "light bar");
        atLeast(3, c(Role.BAR_DIVIDER_LIGHT, Role.BAR_LIGHT), "light bar divider");
    }
}
