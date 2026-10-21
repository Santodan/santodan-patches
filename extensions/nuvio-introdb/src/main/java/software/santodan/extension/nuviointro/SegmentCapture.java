package software.santodan.extension.nuviointro;

/** One explicit two-press capture; identity includes the controller, episode and stream cut. */
public final class SegmentCapture {
    public long start = -1, end = -1;
    public boolean mark(long position) {
        if (position < 0 || position > 86400000L) throw new IllegalArgumentException("Playback position is unavailable.");
        if (start < 0) { start = position; return false; }
        if (position <= start) throw new IllegalArgumentException("End must be after start. Seek forward, or cancel and start again.");
        end = position; return true;
    }
    public static String time(long ms) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d.%03d", ms / 3600000, ms / 60000 % 60, ms / 1000 % 60, ms % 1000);
    }
}
