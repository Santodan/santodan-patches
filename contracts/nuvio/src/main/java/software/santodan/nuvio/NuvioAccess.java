package software.santodan.nuvio;

/**
 * Stable runtime API. The shared settings patch replaces these host implementations
 * with validated check-cast/iget-object accessors in the APK before any caller runs.
 */
public final class NuvioAccess {
    private NuvioAccess() {}
    public static Object currentVideo(Object value) throws ReflectiveOperationException { return read(value, NuvioNative.STATE_VIDEO_FIELD); }
    public static Object currentSeason(Object value) throws ReflectiveOperationException { return read(value, NuvioNative.STATE_SEASON_FIELD); }
    public static Object currentEpisode(Object value) throws ReflectiveOperationException { return read(value, NuvioNative.STATE_EPISODE_FIELD); }
    public static Object nextEpisode(Object value) throws ReflectiveOperationException { return read(value, NuvioNative.STATE_NEXT_FIELD); }
    public static Object playbackError(Object value) throws ReflectiveOperationException { return read(value, NuvioNative.STATE_ERROR_FIELD); }
    public static Object profileProvider(Object value) throws ReflectiveOperationException { return read(value, NuvioNative.PROFILE_PROVIDER_FIELD); }
    public static Object streamProvider(Object value) throws ReflectiveOperationException { return read(value, NuvioNative.STREAM_PROVIDER_FIELD); }
    public static Object trackingProvider(Object value) throws ReflectiveOperationException { return read(value, NuvioNative.TRACKING_PROVIDER_FIELD); }

    private static Object read(Object value, String name) throws ReflectiveOperationException {
        if (value == null) return null;
        java.lang.reflect.Field field = value.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(value);
    }
}
