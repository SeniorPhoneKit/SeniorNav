package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import org.junit.Test;

public class NavBackupTest {
    private static final String PKG = "app.phueber.seniornav";

    private static NavBackup sample() {
        VolumeLimits[] l = {new VolumeLimits(2, 5), new VolumeLimits(1, VolumeLimits.NO_MAX),
                new VolumeLimits(3, VolumeLimits.NO_MAX)};
        return new NavBackup(true, true, false, "uudd", VolumeKeys.Mode.LIMITS, l);
    }

    private static void assertSame(NavBackup a, NavBackup b) {
        assertEquals(a.toSettings(), b.toSettings());
    }

    /** Through a whole file, as on the phone. */
    private static NavBackup viaFile(NavBackup b) throws Exception {
        String file = Backup.writeOne("2026-11-23T10:00:00Z", PKG, "0.0.3", b.toSettings());
        return NavBackup.fromSettings(Backup.settingsFor(file, PKG));
    }

    private static void assertMalformed(String settingsJson) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) Json.parse(settingsJson);
        try {
            NavBackup.fromSettings(m);
            fail("accepted: " + settingsJson);
        } catch (Backup.Failure f) {
            assertEquals(Backup.ERROR_MALFORMED, f.code);
        }
    }

    @Test public void roundTrip() throws Exception {
        NavBackup back = viaFile(sample());
        assertSame(sample(), back);
        assertTrue(back.backRight);
        assertTrue(back.lightBar);
        assertFalse(back.hideUnlocked);
        assertEquals("UUDD", back.pattern);
        assertEquals(VolumeKeys.Mode.LIMITS, back.volumeKeys);
        assertEquals(5, back.limits(VolumeLimits.Stream.RING).max);
        assertEquals(VolumeLimits.NO_MAX, back.limits(VolumeLimits.Stream.MEDIA).max);
        assertEquals(3, back.limits(VolumeLimits.Stream.ALARM).min);
    }

    @Test public void defaultsRoundTripWithoutPattern() throws Exception {
        NavBackup d = new NavBackup(false, false, true, null, VolumeKeys.Mode.NORMAL, null);
        assertFalse(d.toSettings().containsKey("pattern"));
        assertSame(d, viaFile(d));
    }

    @Test public void missingFieldsTakeTheDefaults() throws Exception {
        @SuppressWarnings("unchecked")
        NavBackup b = NavBackup.fromSettings((Map<String, Object>) Json.parse("{}"));
        assertFalse(b.backRight);
        assertFalse(b.lightBar);
        assertTrue(b.hideUnlocked);
        assertNull(b.pattern);
        assertEquals(VolumeKeys.Mode.NORMAL, b.volumeKeys);
        for (VolumeLimits.Stream s : VolumeLimits.Stream.values()) {
            assertEquals(0, b.limits(s).min);
            assertEquals(VolumeLimits.NO_MAX, b.limits(s).max);
        }
    }

    @Test public void alarmHasNoMaximum() throws Exception {
        @SuppressWarnings("unchecked")
        NavBackup b = NavBackup.fromSettings((Map<String, Object>) Json.parse(
                "{\"limits\":{\"alarm\":{\"min\":2,\"max\":4}}}"));
        assertEquals(VolumeLimits.NO_MAX, b.limits(VolumeLimits.Stream.ALARM).max);
        assertFalse(((Map<?, ?>) ((Map<?, ?>) b.toSettings().get("limits")).get("alarm")).containsKey("max"));
    }

    @Test public void wrongTypesAndInvalidValuesAreMalformed() throws Exception {
        assertMalformed("{\"backRight\":\"yes\"}");
        assertMalformed("{\"lightBar\":1}");
        assertMalformed("{\"pattern\":\"UUUU\"}");
        assertMalformed("{\"pattern\":\"UD\"}");
        assertMalformed("{\"pattern\":\"UDXD\"}");
        assertMalformed("{\"pattern\":5}");
        assertMalformed("{\"volumeKeys\":\"loud\"}");
        assertMalformed("{\"volumeKeys\":\"aus\"}");
        assertMalformed("{\"limits\":[]}");
        assertMalformed("{\"limits\":{\"ring\":3}}");
        assertMalformed("{\"limits\":{\"ring\":{\"min\":-1}}}");
        assertMalformed("{\"limits\":{\"ring\":{\"min\":5,\"max\":2}}}");
        assertMalformed("{\"limits\":{\"media\":{\"max\":-2}}}");
        assertMalformed("{\"limits\":{\"media\":{\"min\":1.5}}}");
        assertMalformed("{\"limits\":{\"media\":{\"min\":5000}}}");
    }
}
