package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BarStatusTest {
    private static final String PKG = "app.phueber.seniornav";
    private static final String CLS = "app.phueber.seniornav.NavBarService";

    @Test public void statusFromSettingAndService() {
        assertEquals(BarStatus.ON, BarStatus.of(true, true));
        assertEquals(BarStatus.OFF, BarStatus.of(false, false));
        assertEquals(BarStatus.STUCK, BarStatus.of(true, false));
        // Connected but the setting not yet updated: what is running counts.
        assertEquals(BarStatus.ON, BarStatus.of(false, true));
    }

    @Test public void findsTheServiceInBothForms() {
        assertTrue(BarStatus.listed(PKG + "/" + CLS, PKG, CLS));
        assertTrue(BarStatus.listed(PKG + "/.NavBarService", PKG, CLS));
    }

    @Test public void findsItAmongOthers() {
        assertTrue(BarStatus.listed(
                "com.google.android.marvin.talkback/.TalkBackService:" + PKG + "/.NavBarService",
                PKG, CLS));
    }

    @Test public void doesNotMatchOtherServices() {
        assertFalse(BarStatus.listed("com.example/.NavBarService", PKG, CLS));
        assertFalse(BarStatus.listed(PKG + "/.OtherService", PKG, CLS));
        assertFalse(BarStatus.listed("garbage", PKG, CLS));
    }

    @Test public void emptySettingListsNothing() {
        assertFalse(BarStatus.listed(null, PKG, CLS));
        assertFalse(BarStatus.listed("", PKG, CLS));
    }
}
