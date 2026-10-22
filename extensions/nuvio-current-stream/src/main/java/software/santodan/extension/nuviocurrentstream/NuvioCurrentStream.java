package software.santodan.extension.nuviocurrentstream;

import software.santodan.nuvio.NuvioNative;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import java.lang.reflect.*;

/** Installation-local setting; the playing source itself remains native session state. */
public final class NuvioCurrentStream {
    private static volatile Boolean enabled;
    private static Object menuState;
    private NuvioCurrentStream() {}

    private static SharedPreferences preferences() throws Exception {
        Application app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
        return app.getSharedPreferences("santodan_current_stream", 0);
    }
    private static boolean enabled() {
        Boolean value = enabled;
        if (value != null) return value;
        try { enabled = value = preferences().getBoolean("enabled", true); }
        catch (Exception error) {
            Log.e("SantodanCurrentStream", "Cannot read current stream setting", error);
            return true; // Retry when the application is available.
        }
        return value;
    }
    public static String label(String title, boolean current) {
        return current && enabled() ? "[Playing] " + title : title;
    }
    public static void renderSettings(Object composer) throws Exception {
        ClassLoader loader = composer.getClass().getClassLoader();
        if (menuState == null) menuState = loader.loadClass(NuvioNative.COMPOSE_FACTORY_NAME).getDeclaredMethod(NuvioNative.STATE_FACTORY_METHOD, Object.class).invoke(null, enabled());
        boolean value = (Boolean) menuState.getClass().getMethod("getValue").invoke(menuState);
        Object toggle = callback(loader, true);
        Method row = null;
        for (Method candidate : loader.loadClass(NuvioNative.SETTINGS_ROWS_NAME).getDeclaredMethods())
            if (candidate.getName().equals("m") && candidate.getParameterCount() == 13) {
                if (row != null) throw new NoSuchMethodException("Ambiguous current stream settings row");
                row = candidate;
            }
        if (row == null) throw new NoSuchMethodException("Current stream settings row");
        row.setAccessible(true);
        row.invoke(null, "Indicate current stream", "Show [Playing] on the active source when changing streams.",
            value, toggle, null, callback(loader, false), false, null, 0L, false, composer, 0, 1008);
    }
    private static Object callback(ClassLoader loader, boolean toggle) throws Exception {
        return Proxy.newProxyInstance(loader, new Class<?>[]{loader.loadClass("kotlin.jvm.functions.Function0")}, (proxy, called, args) -> {
            if (called.getName().equals("invoke")) {
                if (toggle) {
                    boolean next = !enabled();
                    preferences().edit().putBoolean("enabled", next).apply();
                    enabled = next;
                    menuState.getClass().getMethod("setValue", Object.class).invoke(menuState, next);
                }
                return loader.loadClass("kotlin.Unit").getField("INSTANCE").get(null);
            }
            if (called.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (called.getName().equals("equals")) return proxy == args[0];
            return "SantodanCurrentStreamCallback";
        });
    }
}
