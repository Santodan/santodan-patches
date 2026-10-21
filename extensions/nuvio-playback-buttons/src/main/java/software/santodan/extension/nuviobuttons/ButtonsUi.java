package software.santodan.extension.nuviobuttons;

import java.lang.reflect.Proxy;
import java.util.*;

/** Original beta.7 picker and action controls; no dependency on another optional patch. */
final class ButtonsUi {
    interface Action { void run() throws Exception; }
    interface Select { void accept(String value) throws Exception; }
    interface Callback { Object invoke(Object[] args) throws Exception; }
    static Object unit(ClassLoader loader) throws Exception { return loader.loadClass("kotlin.Unit").getField("INSTANCE").get(null); }
    static Object callback(ClassLoader loader, String name, Callback action) throws Exception {
        return Proxy.newProxyInstance(loader, new Class<?>[]{loader.loadClass("kotlin.jvm.functions." + name)}, (proxy, called, args) -> {
            if (called.getName().equals("invoke")) return action.invoke(args);
            if (called.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (called.getName().equals("equals")) return proxy == args[0];
            return "SantodanPlaybackSettings";
        });
    }
    static void action(Object composer, String title, Action action) throws Exception {
        ClassLoader loader = composer.getClass().getClassLoader();
        Object content = loader.loadClass("r1.s").getDeclaredConstructor(int.class, Object.class, boolean.class).newInstance(
            0x53425501, callback(loader, "Function3", args -> {
                NuvioPlaybackButtons.method(loader.loadClass("ab.uh"), "d", 7).invoke(null, title,
                    callback(loader, "Function0", ignored -> { action.run(); return unit(loader); }), false, true, args[1], 0, 0);
                return unit(loader);
            }), true);
        NuvioPlaybackButtons.method(loader.loadClass("ab.uh"), "e", 5).invoke(null, null, content, composer, 0, 1);
    }
    static void picker(Object composer, String title, List<String> ids, List<String> labels, String selected,
            Select select, Action dismiss) throws Exception {
        ClassLoader loader = composer.getClass().getClassLoader();
        java.lang.reflect.Constructor<?> constructor = Arrays.stream(loader.loadClass("ab.fi").getDeclaredConstructors())
            .filter(c -> c.getParameterCount() == 6).reduce((a,b) -> { throw new IllegalStateException("Ambiguous picker option"); }).orElseThrow();
        constructor.setAccessible(true);
        List<Object> options = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) options.add(constructor.newInstance(ids.get(i), labels.get(i), null, null, null, 28));
        NuvioPlaybackButtons.method(loader.loadClass("ab.uh"), "j", 11).invoke(null, title, options, selected,
            callback(loader, "Function1", args -> { select.accept((String)args[0]); return unit(loader); }),
            callback(loader, "Function0", args -> { dismiss.run(); return unit(loader); }), null, 560f, 420f, composer, 0, 32);
    }
}
