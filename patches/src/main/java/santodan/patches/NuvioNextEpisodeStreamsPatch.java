package santodan.patches;
import app.morphe.patcher.patch.BytecodePatch;

public final class NuvioNextEpisodeStreamsPatch {
    public static BytecodePatch getNuvioNextEpisodeStreamsPatch() {
        return NuvioStreamPreloadPatch.create("NuvioTV - Preload streams for next episode",
            "Adds an opt-in Streams setting to fetch the next episode's source list after 30 seconds of playback, reusing Nuvio's native search cache.", "next_episode");
    }
}
