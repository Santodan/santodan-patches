package santodan.patches;

import app.morphe.patcher.patch.AppTarget;
import java.util.List;

/** Exact compatibility for the latest workspace NuvioTV release. */
final class NuvioLayout {
    static final String BETA7 = "1.1.0-beta.7";
    static boolean recent(String version) { return BETA7.equals(version); }
    static void use(String version) {
        if (!recent(version)) throw new IllegalStateException("Unsupported NuvioTV version: " + version);
    }
    static List<AppTarget> targets() { return List.of(new AppTarget(BETA7, false, null)); }
    static List<AppTarget> modernTargets() { return targets(); }
    static boolean modern(String version) { use(version); return true; }
}
