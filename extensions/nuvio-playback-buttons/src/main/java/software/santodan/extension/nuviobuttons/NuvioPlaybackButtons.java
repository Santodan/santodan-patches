package software.santodan.extension.nuviobuttons;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import java.lang.reflect.*;
import java.util.*;

/** Native actions and availability are collected only for the current composition, never retained. */
public final class NuvioPlaybackButtons {
    private static volatile ButtonLayout layout;
    private static final ThreadLocal<Frame> frames = new ThreadLocal<>();
    private static Object menuState;
    private static int menuRevision;
    private static final class Frame {
        final ButtonLayout layout;
        final Map<String, Object[]> buttons = new LinkedHashMap<>();
        Object lambda;
        boolean expanded;
        Frame(ButtonLayout layout) { this.layout = layout; }
    }
    private NuvioPlaybackButtons() {}
    private static SharedPreferences preferences() throws Exception {
        Application app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
        return app.getSharedPreferences("santodan_playback_buttons", 0);
    }
    private static ButtonLayout layout() {
        ButtonLayout value = layout;
        if (value != null) return value;
        synchronized (NuvioPlaybackButtons.class) {
            if (layout == null) try {
                SharedPreferences p = preferences();
                layout = new ButtonLayout(p.getBoolean("enabled", false), p.getString("order", ""),
                    p.getString("hidden", ""), p.getString("more", "speed,aspect,external,info,report"));
            } catch (Exception error) {
                Log.e("SantodanButtons", "Cannot read playback layout", error);
                return ButtonLayout.defaults(); // Retry after app initialization.
            }
            return layout;
        }
    }
    private static void save(ButtonLayout value) throws Exception {
        preferences().edit().putBoolean("enabled", value.enabled).putString("order", String.join(",", value.order))
            .putString("hidden", String.join(",", value.hidden)).putString("more", String.join(",", value.more)).apply();
        layout = value;
    }
    static Method method(Class<?> owner, String name, int count) throws Exception {
        Method result = null;
        for (Method candidate : owner.getDeclaredMethods()) if (candidate.getName().equals(name) && candidate.getParameterCount() == count) {
            if (result != null) throw new NoSuchMethodException("Ambiguous " + owner.getName() + '.' + name);
            result = candidate;
        }
        if (result == null) throw new NoSuchMethodException(owner.getName() + '.' + name);
        result.setAccessible(true); return result;
    }
    private static void nativeButton(Object[] args) {
        invokeNative(args.length == 9 ? "G" : "b", args, args.length == 9 ? 7 : 10);
    }
    private static void invokeNative(String name, Object[] args, int composerIndex) {
        try { method(args[composerIndex].getClass().getClassLoader().loadClass("va.yd"), name, args.length).invoke(null, args); }
        catch (Exception error) { throw new IllegalStateException("Native playback control failed", error); }
    }
    private static void capture(String id, Object[] args) {
        if (id.equals("play")) {
            frames.remove();
            ButtonLayout value = layout();
            if (value.enabled) frames.set(new Frame(value));
        }
        Frame frame = frames.get();
        if (frame == null) { nativeButton(args); return; }
        frame.buttons.put(id, args);
    }
    public static void registerMore(Object lambda) {
        Frame frame = frames.get();
        if (frame != null) frame.lambda = lambda;
    }
    public static void more(boolean visible, Object modifier, Object enter, Object exit, Object label,
            Object content, Object composer, int flags) {
        Frame frame = frames.get();
        try {
            ClassLoader loader = composer.getClass().getClassLoader();
            if (frame == null) {
                method(loader.loadClass("v.b0"), "d", 8).invoke(null, visible, modifier, enter, exit, label, content, composer, flags);
                return;
            }
            frame.expanded = visible;
            if (frame.lambda == null) throw new IllegalStateException("Missing native More content");
            // The original branch only requires a non-null empty AnimatedVisibilityScope interface.
            // Evaluate every native availability rule even while More is closed, so controls can move out of it.
            Object scope = Proxy.newProxyInstance(loader, new Class<?>[]{loader.loadClass("v.c0")},
                (proxy, called, args) -> called.getName().equals("hashCode") ? System.identityHashCode(proxy) :
                    called.getName().equals("equals") ? proxy == args[0] : "SantodanPlaybackScope");
            method(composer.getClass(), "d0", 1).invoke(composer, 0x53425001);
            try { method(frame.lambda.getClass(), "invoke", 3).invoke(frame.lambda, scope, composer, 0); }
            finally { method(composer.getClass(), "p", 1).invoke(composer, false); }
        } catch (Exception error) {
            frames.remove();
            throw new IllegalStateException("Playback More composition failed", error);
        }
    }
    private static void finishFrame(Object[] toggle) {
        Frame frame = frames.get();
        frames.remove(); // No player, callbacks or composer survive this composition, including failures.
        if (frame == null) { nativeButton(toggle); return; }
        Object composer = toggle[10];
        List<String> primary = frame.layout.visible(frame.buttons.keySet(), false);
        List<String> secondary = frame.layout.visible(frame.buttons.keySet(), true);
        secondary.removeAll(primary); // A conditional-main fallback must not duplicate Play in More.
        Object playFocus = frame.buttons.get("play")[4];
        Object infoFocus = frame.buttons.containsKey("info") ? frame.buttons.get("info")[4] : null;
        String mainFocus = primary.stream().filter(id -> !id.equals("report")).findFirst().orElse("play");
        for (String id : primary) render(id, frame.buttons.get(id), composer,
            id.equals(mainFocus) ? playFocus : null);
        String moreFocus = secondary.stream().filter(id -> !id.equals("report")).findFirst().orElse("");
        if (frame.expanded) for (String id : secondary) render(id, frame.buttons.get(id), composer,
            id.equals(moreFocus) ? infoFocus : null);
        if (!secondary.isEmpty() || frame.expanded) render("more", toggle, composer,
            frame.expanded && moreFocus.isEmpty() ? infoFocus : null);
    }
    private static void render(String id, Object[] original, Object composer, Object focus) {
        Object[] args = original.clone();
        // Native focus anchors follow actual visible destinations after hiding or regrouping.
        if (args.length == 13) {
            args[4] = focus;
            args[12] = focus == null ? ((Integer)args[12] | 16) : ((Integer)args[12] & ~16);
            args[11] = 0; // Re-evaluate changed inputs in the new stable group.
        } else { args[8] = 0; }
        try {
            method(composer.getClass(), "d0", 1).invoke(composer, 0x53425020 + ButtonLayout.IDS.indexOf(id));
            try { nativeButton(args); }
            finally { method(composer.getClass(), "p", 1).invoke(composer, false); }
        } catch (Exception error) { throw new IllegalStateException("Playback button composition failed", error); }
    }
    public static void report(Object id, boolean showId, Object click, boolean enabled, Object up, Object down,
            Object focused, Object composer, int flags) {
        capture("report", new Object[]{id, showId, click, enabled, up, down, focused, composer, flags});
    }
    public static void renderSettings(Object composer) throws Exception {
        ClassLoader loader = composer.getClass().getClassLoader();
        if (menuState == null) menuState = method(loader.loadClass("h1.h"), "s", 1).invoke(null, "");
        String page = ((String) method(menuState.getClass(), "getValue", 0).invoke(menuState)).split("\\|", 2)[0];
        ButtonLayout value = layout();
        Object toggle = ButtonsUi.callback(loader, "Function0", args -> {
            save(layout().enabled(!layout().enabled)); setPage(""); return ButtonsUi.unit(loader);
        });
        method(loader.loadClass("ab.uh"), "m", 13).invoke(null,
            "Customize playback buttons", "Choose visibility, order, and main row or More actions. Settings apply to this installation.",
            value.enabled, toggle, null, ButtonsUi.callback(loader, "Function0", args -> ButtonsUi.unit(loader)),
            false, null, 0L, false, composer, 0, 1008);
        ButtonsUi.action(composer, "Edit playback buttons", () -> setPage("list"));
        ButtonsUi.action(composer, "Reset playback buttons", () -> { save(ButtonLayout.defaults()); setPage(""); });
        if (page.equals("list")) {
            List<String> labels = new ArrayList<>();
            for (String id : value.order) labels.add(ButtonLayout.label(id) + " — " + (value.hidden.contains(id) ? "Hidden" : value.more.contains(id) ? "More actions" : "Main row"));
            ButtonsUi.picker(composer, "Playback buttons", value.order, labels, "", id -> setPage(id), () -> setPage(""));
        } else if (ButtonLayout.IDS.contains(page)) {
            List<String> actions = Arrays.asList("visibility", "group", "left", "right", "back");
            List<String> labels = Arrays.asList(value.hidden.contains(page) ? "Show button" : "Hide button",
                value.more.contains(page) ? "Move to main row" : "Move to More actions", "Move earlier in this group", "Move later in this group", "Back to buttons");
            ButtonsUi.picker(composer, ButtonLayout.label(page), actions, labels, "", action -> {
                if (!action.equals("back")) save(layout().change(page, action));
                setPage("list");
            }, () -> setPage("list"));
        }
    }
    private static void setPage(String value) throws Exception {
        // Settings actions always change the page, invalidating the native composition after edits.
        method(menuState.getClass(), "setValue", 1).invoke(menuState, value + "|" + (++menuRevision));
    }
    public static void play(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("play", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void next(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("next", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void subtitles(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("subtitles", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void audio(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("audio", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void sources(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("sources", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void engine(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("engine", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void episodes(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("episodes", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void party(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("party", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void speed(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("speed", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void aspect(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("aspect", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void external(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("external", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void info(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        capture("info", new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
    public static void finish(Object icon, Object painter, Object label, Object click, Object focus, Object up,
            boolean enabled, Object down, Object focused, long tint, Object composer, int flags, int defaults) {
        finishFrame(new Object[]{icon, painter, label, click, focus, up, enabled, down, focused, tint, composer, flags, defaults});
    }
}
