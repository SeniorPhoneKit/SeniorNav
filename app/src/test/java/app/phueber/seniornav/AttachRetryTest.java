package app.phueber.seniornav;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AttachRetryTest {
    @Test public void doublesFromOneSecond() {
        assertEquals(1_000, AttachRetry.delayMs(1));
        assertEquals(2_000, AttachRetry.delayMs(2));
        assertEquals(4_000, AttachRetry.delayMs(3));
        assertEquals(32_000, AttachRetry.delayMs(6));
    }

    @Test public void neverMoreThanAMinute() {
        assertEquals(60_000, AttachRetry.delayMs(7));
        assertEquals(60_000, AttachRetry.delayMs(1_000));
    }

    @Test public void oddInputStaysSane() {
        assertEquals(1_000, AttachRetry.delayMs(0));
        assertEquals(1_000, AttachRetry.delayMs(-5));
    }
}
