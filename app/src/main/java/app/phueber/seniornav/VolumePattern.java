package app.phueber.seniornav;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * The helper's pattern of volume-button presses that locks and unlocks
 * SeniorLauncher's shared lock, and the matcher that spots it among the
 * presses. Pure (the caller passes the clock), so it is unit tested.
 *
 * A pattern is 4 to 6 presses, written as "U" (Volume Up) and "D" (Volume
 * Down), and must contain both: four quick Volume Up presses are what a senior
 * does to make something louder, and must not toggle the lock.
 *
 * Timing: at most {@link #MAX_GAP_MS} between two presses and
 * {@link #MAX_TOTAL_MS} from the first press to the last. A slower press starts
 * over. Presses that do not fit are dropped from the front, so a stray press
 * before the pattern does not spoil it ("U U D D" is found in "U U U D D").
 */
final class VolumePattern {
    static final int MIN_LENGTH = 4;
    static final int MAX_LENGTH = 6;
    static final long MAX_GAP_MS = 1_000;
    static final long MAX_TOTAL_MS = 3_000;

    static final char UP = 'U';
    static final char DOWN = 'D';

    private VolumePattern() {}

    /** The stored form, upper-cased and trimmed, or null if it is not a usable pattern. */
    static String normalize(String s) {
        if (s == null) return null;
        String p = s.trim().toUpperCase(java.util.Locale.ROOT);
        if (p.length() < MIN_LENGTH || p.length() > MAX_LENGTH) return null;
        boolean up = false, down = false;
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == UP) up = true;
            else if (c == DOWN) down = true;
            else return null;
        }
        return up && down ? p : null;
    }

    /** "▲ ▲ ▼ ▼" for the screen. */
    static String arrows(String p) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < p.length(); i++) {
            if (i > 0) b.append(' ');
            b.append(p.charAt(i) == UP ? '▲' : '▼');
        }
        return b.toString();
    }

    /**
     * Finds the pattern among presses. Each press carries a payload (the
     * volumes just before it); on a match the payload of the pattern's first
     * press is returned, so the caller can undo what the pattern's presses did.
     */
    static final class Matcher<T> {
        private static final class Press<T> {
            final char key;
            final long at;
            final T before;

            Press(char key, long at, T before) {
                this.key = key;
                this.at = at;
                this.before = before;
            }
        }

        private final ArrayDeque<Press<T>> presses = new ArrayDeque<>();

        /** Forget the presses so far (a call, both keys held, recording, ...). */
        void reset() {
            presses.clear();
        }

        /** Whether presses are under way that may still become the pattern. */
        boolean pending() {
            return !presses.isEmpty();
        }

        /**
         * One press of {@code key} (UP/DOWN) at {@code now} (a monotonic clock,
         * ms). Returns the first press's payload if this press completes
         * {@code pattern}, else null. A null or unusable pattern never matches.
         */
        T press(String pattern, char key, long now, T before) {
            if (normalize(pattern) == null) {
                presses.clear();
                return null;
            }
            Press<T> last = presses.peekLast();
            if (last != null && now - last.at > MAX_GAP_MS) presses.clear();
            presses.addLast(new Press<>(key, now, before));
            while (!presses.isEmpty()
                    && (now - presses.peekFirst().at > MAX_TOTAL_MS || !isPrefix(pattern))) {
                presses.removeFirst();
            }
            if (presses.size() == pattern.length()) {
                T first = presses.peekFirst().before;
                presses.clear();
                return first;
            }
            return null;
        }

        private boolean isPrefix(String pattern) {
            if (presses.size() > pattern.length()) return false;
            Iterator<Press<T>> it = presses.iterator();
            for (int i = 0; it.hasNext(); i++) {
                if (it.next().key != pattern.charAt(i)) return false;
            }
            return true;
        }
    }
}
