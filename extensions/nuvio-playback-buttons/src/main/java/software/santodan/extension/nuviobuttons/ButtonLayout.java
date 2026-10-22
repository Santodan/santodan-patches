package software.santodan.extension.nuviobuttons;

import java.util.*;

/** Immutable, app-wide preferences. IDs are independent of translated button labels. */
public final class ButtonLayout {
    public static final List<String> IDS = Collections.unmodifiableList(Arrays.asList(
        "play", "next", "subtitles", "audio", "sources", "engine", "episodes", "party",
        "chapters", "speed", "aspect", "external", "info", "report", "introdb"));
    public static final List<String> LABELS = Collections.unmodifiableList(Arrays.asList(
        "Play / pause", "Next episode", "Subtitles", "Audio tracks", "Sources", "Player engine",
        "Episodes", "Watch party", "Chapters", "Playback speed", "Aspect ratio", "External player", "Stream info", "Report playback issue", "Submit segment to IntroDB"));
    public final List<String> order;
    public final Set<String> hidden, more;
    public final boolean enabled;

    public ButtonLayout(boolean enabled, String order, String hidden, String more) {
        this.enabled = enabled;
        LinkedHashSet<String> sorted = parse(order);
        sorted.addAll(IDS);
        this.order = Collections.unmodifiableList(new ArrayList<>(sorted));
        LinkedHashSet<String> invisible = parse(hidden), secondary = parse(more);
        // The progress bar's native down-focus target must always have a main-row destination.
        if (this.order.stream().noneMatch(id -> !invisible.contains(id) && !secondary.contains(id))) {
            invisible.remove("play"); secondary.remove("play");
        }
        this.hidden = Collections.unmodifiableSet(invisible);
        this.more = Collections.unmodifiableSet(secondary);
    }
    public static ButtonLayout defaults() { return new ButtonLayout(false, "", "", "speed,aspect,engine,external,info,report"); }
    private static LinkedHashSet<String> parse(String text) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String id : text.split(",")) if (IDS.contains(id)) values.add(id);
        return values;
    }
    public ButtonLayout change(String id, String action) {
        if (!IDS.contains(id)) throw new IllegalArgumentException(id);
        List<String> sorted = new ArrayList<>(order);
        Set<String> invisible = new LinkedHashSet<>(hidden), secondary = new LinkedHashSet<>(more);
        if (action.equals("visibility")) { if (!invisible.remove(id)) invisible.add(id); }
        else if (action.equals("group")) { if (!secondary.remove(id)) secondary.add(id); }
        else if (action.equals("left") || action.equals("right")) {
            int at = sorted.indexOf(id), step = action.equals("left") ? -1 : 1;
            for (int to = at + step; to >= 0 && to < sorted.size(); to += step) {
                String candidate = sorted.get(to);
                if (secondary.contains(candidate) == secondary.contains(id)) { Collections.swap(sorted, at, to); break; }
            }
        } else throw new IllegalArgumentException(action);
        return new ButtonLayout(enabled, String.join(",", sorted), String.join(",", invisible), String.join(",", secondary));
    }
    public ButtonLayout enabled(boolean value) {
        return new ButtonLayout(value, String.join(",", order), String.join(",", hidden), String.join(",", more));
    }
    public List<String> visible(Collection<String> available, boolean secondary) {
        List<String> result = new ArrayList<>();
        for (String id : order) if (available.contains(id) && !hidden.contains(id) && more.contains(id) == secondary) result.add(id);
        // A conditional button can disappear between videos. Always retain an available focus target.
        if (!secondary && result.stream().noneMatch(id -> !id.equals("report")) && available.contains("play")) result.add(0, "play");
        return result;
    }
    public static String label(String id) { return LABELS.get(IDS.indexOf(id)); }
}
