package software.santodan.extension.nuviostreams;

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
            case "ka.d1": return "pa.z0";
            case "ka.e1": return "pa.z0";
            case "sa.db": return "xa.ub";
            case "sa.eb": return "xa.ub";
            default: return original;
        }
    }
    public static String field(Class<?> owner, String original) {
        switch (owner.getName() + "->" + original) {
            case "p8.e->K2": return "V2";
            case "p8.e->y": return "B";
            case "z9.k4->k": return "l";
            case "sa.df->W": return "Y";
            case "sa.p8->Q": return "S";
            case "sa.af->w0": return "z0";
            case "sa.af->k1": return "o1";
            case "sa.af->Y0": return "c1";
            default: return original;
        }
    }
}
