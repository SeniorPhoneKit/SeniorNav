package app.phueber.seniornav;

/**
 * The volume range the helper allows while the shared lock is on, for one
 * stream, in the stream's own steps (as the system's slider has them). Pure,
 * so it is unit tested. {@link #NO_MAX} means "up to the top".
 */
final class VolumeLimits {
    static final int NO_MAX = -1;

    /** The limited streams; the alarm has a minimum only. */
    enum Stream {
        RING("ring", true), MEDIA("media", true), ALARM("alarm", false);

        final String id;
        final boolean hasMax;

        Stream(String id, boolean hasMax) {
            this.id = id;
            this.hasMax = hasMax;
        }
    }

    final int min;
    final int max;

    VolumeLimits(int min, int max) {
        this.min = min;
        this.max = max;
    }

    /** The lowest allowed step on a stream that goes from streamMin to streamMax. */
    int low(int streamMin, int streamMax) {
        return Math.max(streamMin, Math.min(min, streamMax));
    }

    /** The highest allowed step; never below {@link #low}. */
    int high(int streamMin, int streamMax) {
        int h = max == NO_MAX ? streamMax : Math.max(streamMin, Math.min(max, streamMax));
        return Math.max(h, low(streamMin, streamMax));
    }

    /** The volume brought into range. */
    int clamp(int volume, int streamMin, int streamMax) {
        return Math.max(low(streamMin, streamMax), Math.min(volume, high(streamMin, streamMax)));
    }

    /** Whether nothing is limited on this stream. */
    boolean unlimited(int streamMin, int streamMax) {
        return low(streamMin, streamMax) == streamMin && high(streamMin, streamMax) == streamMax;
    }

    /**
     * Whether one step up (direction +1) or down (-1) from {@code volume} stays
     * in range. A step that moves an out-of-range volume back towards the range
     * is allowed too.
     */
    boolean stepAllowed(int volume, int direction, int streamMin, int streamMax) {
        int lo = low(streamMin, streamMax), hi = high(streamMin, streamMax);
        if (direction > 0) return volume < hi;
        if (direction < 0) return volume > lo;
        return true;
    }

    /** New limits with the lowest step set; the highest follows if it would be passed. */
    VolumeLimits withMin(int newMin, int streamMin, int streamMax) {
        int lo = Math.max(streamMin, Math.min(newMin, streamMax));
        return new VolumeLimits(lo, max == NO_MAX ? NO_MAX : Math.max(high(streamMin, streamMax), lo));
    }

    /** New limits with the highest step set; the lowest follows if it would be passed. The top means NO_MAX. */
    VolumeLimits withMax(int newMax, int streamMin, int streamMax) {
        int hi = Math.max(streamMin, Math.min(newMax, streamMax));
        int lo = Math.min(low(streamMin, streamMax), hi);
        return new VolumeLimits(lo, hi == streamMax ? NO_MAX : hi);
    }
}
