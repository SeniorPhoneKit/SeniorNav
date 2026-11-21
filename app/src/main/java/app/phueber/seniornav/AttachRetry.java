package app.phueber.seniornav;

/**
 * How long to wait before trying again to attach the bar's window after the
 * window manager refused it: 1 s, 2 s, 4 s, ... up to one minute, so a refusal
 * that lasts is retried rarely instead of in a tight loop. Pure, unit tested.
 */
final class AttachRetry {
    static final long FIRST_MS = 1_000;
    static final long MAX_MS = 60_000;

    private AttachRetry() {}

    /** The wait after {@code failures} refusals in a row (at least 1). */
    static long delayMs(int failures) {
        int doublings = Math.max(0, Math.min(failures - 1, 6));
        return Math.min(MAX_MS, FIRST_MS << doublings);
    }
}
