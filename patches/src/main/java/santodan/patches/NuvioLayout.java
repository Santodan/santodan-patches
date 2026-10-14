package santodan.patches;

import app.morphe.patcher.patch.AppTarget;
import java.util.List;

/** Only the latest original NuvioTV layout is supported. */
final class NuvioLayout {
    static final String BETA6 = "1.1.0-beta.6";
    static boolean beta6() { return true; }
    static boolean recent(String version) { return BETA6.equals(version); }
    static void use(String version) { modern(version); }
    static List<AppTarget> modernTargets() { return targets(); }
    static List<AppTarget> targets() { return List.of(new AppTarget(BETA6, false, null)); }
    static boolean modern(String version) {
        if (!BETA6.equals(version)) throw new IllegalStateException("Unsupported NuvioTV version: " + version);
        return true;
    }
    static boolean newer(String owner) { return true; }
}
