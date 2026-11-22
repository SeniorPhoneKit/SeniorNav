package app.phueber.seniornav;

/**
 * Bounds how often automation may act, so that an app flooding
 * {@link AutomationReceiver} cannot keep the senior or the helper out of
 * Settings. At most one action per {@link #MIN_GAP_MS}, at most
 * {@link #BURST} in any {@link #WINDOW_MS}; the next one after that starts a
 * {@link #COOLDOWN_MS} in which every action is refused. Only allowed actions
 * count. A rule that fires on an app being opened or closed stays far below
 * this; a flood gets {@link #BURST} actions, then the phone is left alone.
 *
 * Pure (the caller passes the clock), so it is unit tested. Not thread-safe on
 * its own: the receiver calls it on the main thread only.
 */
final class AutomationLimit {
    static final long MIN_GAP_MS = 1_000;
    static final int BURST = 6;
    static final long WINDOW_MS = 60_000;
    static final long COOLDOWN_MS = 120_000;

    /** When the last {@link #BURST} allowed actions happened, oldest overwritten first. */
    private final long[] allowed = new long[BURST];
    private int count;      // allowed actions recorded, at most BURST
    private int next;       // where the next one goes in allowed
    private long last;      // the latest allowed action
    private long cooldownUntil;
    private boolean cooling;

    /** Whether an action at {@code now} (a monotonic clock, ms) may go ahead; records it if so. */
    boolean allow(long now) {
        if (cooling) {
            if (now < cooldownUntil) return false;
            cooling = false;
            count = 0; // a fresh budget after the cooldown
        }
        if (count > 0 && now - last < MIN_GAP_MS) return false;
        if (count == BURST && now - allowed[next] < WINDOW_MS) {
            cooling = true;
            cooldownUntil = now + COOLDOWN_MS;
            return false;
        }
        allowed[next] = now;
        next = (next + 1) % BURST;
        if (count < BURST) count++;
        last = now;
        return true;
    }
}
