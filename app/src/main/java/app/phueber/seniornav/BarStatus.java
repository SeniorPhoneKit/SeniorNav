package app.phueber.seniornav;

/**
 * What the main screen says about the bar. Pure, so it is unit tested.
 *
 * "Turned on" is Android's list of enabled accessibility services;
 * "running" is whether the service is actually connected in this process.
 * They differ when the system has stopped the service (for example after it
 * crashed, or a battery saver killed it), and the helper then has to switch it
 * off and on again.
 */
enum BarStatus {
    ON, OFF, STUCK;

    static BarStatus of(boolean turnedOn, boolean running) {
        if (running) return ON;
        return turnedOn ? STUCK : OFF;
    }

    /**
     * Whether Settings.Secure "enabled_accessibility_services" (colon-separated
     * flattened component names, "pkg/.Cls" or "pkg/pkg.Cls") lists this service.
     */
    static boolean listed(String setting, String pkg, String cls) {
        if (setting == null || setting.isEmpty()) return false;
        for (String entry : setting.split(":")) {
            int slash = entry.indexOf('/');
            if (slash <= 0) continue;
            String p = entry.substring(0, slash).trim();
            String c = entry.substring(slash + 1).trim();
            if (c.startsWith(".")) c = p + c;
            if (p.equalsIgnoreCase(pkg) && c.equalsIgnoreCase(cls)) return true;
        }
        return false;
    }
}
