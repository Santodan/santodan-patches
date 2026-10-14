package software.santodan.extension.nuviomenu;

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
            case "sa.db": return "xa.ub";
            case "sa.eb": return "xa.ub";
            case "sa.kc": return "xa.bd";
            case "sa.lb": return "xa.cc";
            case "sa.mb": return "xa.cc";
            case "sa.o3": return "xa.za";
            case "sa.p3": return "xa.za";
            case "va.l0": return "ab.l0";
            case "x5.i2": return "x5.h2";
            default: return original;
        }
    }
    public static String method(Class<?> owner, String original) {
        if (owner.getName().equals("xa.za")) {
            switch (original) {
                case "q": return "b";
                case "r": return "c";
                case "y0": return "h";
                default: break;
            }
        }
        return original;
    }
}
