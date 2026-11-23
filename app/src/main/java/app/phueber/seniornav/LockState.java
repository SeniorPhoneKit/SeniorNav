package app.phueber.seniornav;

/**
 * SeniorLauncher's shared lock as SeniorNav sees it, and what follows from
 * it. Pure, so it is unit tested; {@link LauncherLock} reads it.
 */
final class LockState {
    /** SeniorLauncher is not installed or did not answer: no lock. */
    static final LockState NONE = new LockState(false, false, false, false);

    /** SeniorLauncher answered. */
    final boolean available;
    /** It is new enough to say whether a PIN is set and to lock for the pattern. */
    final boolean supportsNav;
    final boolean locked;
    /** A PIN is stored: the lock is in use, also while it is open. */
    final boolean hasPin;

    LockState(boolean available, boolean supportsNav, boolean locked, boolean hasPin) {
        this.available = available;
        this.supportsNav = supportsNav;
        this.locked = locked;
        this.hasPin = hasPin;
    }

    /**
     * Whether the bar steps aside because the helper has unlocked: the
     * helper's setting is on, the lock is in use (a PIN is set) and open right
     * now. A phone that never uses the lock, or has no SeniorLauncher, keeps
     * its bar.
     */
    boolean hidesBar(boolean hideWhileUnlocked) {
        return hideWhileUnlocked && available && supportsNav && hasPin && !locked;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof LockState)) return false;
        LockState s = (LockState) o;
        return available == s.available && supportsNav == s.supportsNav
                && locked == s.locked && hasPin == s.hasPin;
    }

    @Override public int hashCode() {
        return (available ? 8 : 0) | (supportsNav ? 4 : 0) | (locked ? 2 : 0) | (hasPin ? 1 : 0);
    }
}
