package software.santodan.extension.nuvionextstreams;

/** Presence of this bridge exposes this independently selected patch's setting. */
public final class NuvioNextEpisodeStreams {
    public static void renderSettings(Object composer) throws Exception {
        Class.forName("software.santodan.extension.nuviostreams.NuvioStreamPreload")
            .getMethod("renderSettings", Object.class, String.class)
            .invoke(null, composer, "next_episode");
    }
}
