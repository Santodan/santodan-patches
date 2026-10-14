package software.santodan.extension.nuviomovierelease;

import android.app.Application;

/** Native names proven against the original 1.1.0-beta.6 DEX. */
public final class NuvioBeta6 {
    private static volatile Boolean selected;
    private NuvioBeta6() {}
    public static boolean active() {
        Boolean value = selected;
        if (value == null) {
            try {
                Application app = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
                Object info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                java.lang.reflect.Field version = info.getClass().getDeclaredField("versionName");
                version.setAccessible(true);
                value = "1.1.0-beta.6".equals(version.get(info));
                if (!value) throw new IllegalStateException("NuvioTV extensions require 1.1.0-beta.6");
                selected = value;
            } catch (Exception error) { throw new IllegalStateException("Cannot resolve NuvioTV version", error); }
        }
        return value;
    }
    public static String name(String original) {
        if (!active()) return original;
        switch (original) {
            case "ba.d3": return "fa.q2";
            case "ba.n3": return "fa.e3";
            case "sa.eb": return "xa.ub";
            case "x5.g2": return "x5.f2";
            default: return original;
        }
    }
}
