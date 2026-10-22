package software.santodan.extension.nuviomerged;

import software.santodan.nuvio.NuvioNative;

/** Resolves beta4's lazy native coordinator without requiring its settings screen. */
public final class NuvioSettingsStoreResolver {
    private static volatile Object component;
    private static volatile Object store;

    private NuvioSettingsStoreResolver() {}

    public static void registerComponent(Object value) { component = value; }
    public static void registerStore(Object value) { store = value; }

    public static synchronized Object resolve() throws ReflectiveOperationException {
        if (store != null) return store;
        Object current = component;
        if (current == null) throw new IllegalStateException("Watch progress settings component is unavailable");
        Object provider = software.santodan.nuvio.NuvioAccess.trackingProvider(current);
        java.lang.reflect.Method get = provider.getClass().getDeclaredMethod("get");
        get.setAccessible(true);
        Object resolved = get.invoke(provider);
        if (resolved == null) throw new IllegalStateException("Watch progress settings provider returned null");
        store = resolved;
        return resolved;
    }
}
