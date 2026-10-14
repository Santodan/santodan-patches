package software.santodan.extension.nuviomerged;

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
            case "c7.a": return "a.a";
            case "la.t5": return "qa.s5";
            case "sa.eb": return "xa.ub";
            case "v9.yc": return "z9.zc";
            default: return original;
        }
    }
    public static String field(Class<?> owner, String original) {
        switch (owner.getName() + "->" + original) {
            case "qa.a4->T0": return "V0";
            case "qa.a4->V0": return "X0";
            default: return original;
        }
    }
}
