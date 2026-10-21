package santodan.patches

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import javax.tools.ToolProvider
import software.santodan.extension.nuviobuttons.ButtonLayout

/** Runs the production extension, including callbacks and persistence, against native-signature fixtures. */
fun main(args: Array<String>) {
    val root = Files.createTempDirectory(File(args[1]).apply { mkdirs() }.toPath(), "buttons-runtime-")
    val sources = mapOf(
        "android.content.SharedPreferences" to """package android.content;
            public class SharedPreferences {
                public java.util.Map<String,Object> values=new java.util.HashMap<>();
                public boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}
                public String getString(String k,String d){return (String)values.getOrDefault(k,d);}
                public Editor edit(){return new Editor();}
                public class Editor { public Editor putBoolean(String k,boolean v){values.put(k,v);return this;}
                    public Editor putString(String k,String v){values.put(k,v);return this;} public void apply(){} }
            }""",
        "android.app.Application" to """package android.app; public class Application {
            public android.content.SharedPreferences prefs=new android.content.SharedPreferences();
            public android.content.SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
        }""",
        "android.app.ActivityThread" to """package android.app; public class ActivityThread {
            public static Application app=new Application(); public static Application currentApplication(){return app;}
        }""",
        "android.util.Log" to """package android.util; public class Log {public static int e(String tag,String message,Throwable error){throw new AssertionError(message,error);}}""",
        "h1.l0" to """package h1; public class l0 { public int depth; public void d0(int key){depth++;} public void p(boolean node){if(--depth<0)throw new AssertionError("Unbalanced groups");} }""",
        "h1.State" to """package h1; public class State { private Object value; public State(Object value){this.value=value;} public Object getValue(){return value;} public void setValue(Object v){value=v;} }""",
        "h1.h" to """package h1; public class h { public static State s(Object v){return new State(v);} }""",
        "v.c0" to """package v; public interface c0 {}""",
        "v.b0" to """package v; public class b0 { public static int nativeCalls; public static void d(boolean visible,Object modifier,Object enter,Object exit,Object label,Object content,Object composer,int flags){nativeCalls++;} }""",
        "r1.s" to """package r1; public class s { public Object content; public s(int key,Object content,boolean tracked){this.content=content;} }""",
        "ab.fi" to """package ab; public class fi {public Object id; public String label; public fi(Object id,String label,String sub,String icon,Object meta,int flags){this.id=id;this.label=label;}}""",
        "ab.uh" to """package ab; import kotlin.jvm.functions.*; public class uh {
            public static Function0 toggle,dismiss; public static Function1 choose; public static String picker;
            public static java.util.Map<String,Function0> actions=new java.util.HashMap<>();
            public static void m(String title,String description,boolean enabled,Function0 click,Object modifier,Function0 noop,boolean disabled,Object icon,long tint,boolean busy,Object composer,int flags,int defaults){toggle=click;}
            public static void e(Object alignment,r1.s content,Object composer,int flags,int defaults){((Function3)content.content).invoke(null,composer,0);}
            public static void d(String title,Function0 click,boolean busy,boolean enabled,Object composer,int flags,int defaults){actions.put(title,click);}
            public static void j(String title,java.util.List options,Object selected,Function1 select,Function0 close,String description,float width,float height,Object composer,int flags,int defaults){picker=title;choose=select;dismiss=close;}
            public static void action(String title){actions.get(title).invoke();} public static void select(String id){choose.invoke(id);}
        }""",
        "va.yd" to """package va; public class yd {
            public static java.util.List<Object[]> rendered=new java.util.ArrayList<>(); public static boolean fail;
            public static void b(Object icon,Object painter,Object label,Object click,Object focus,Object up,boolean enabled,Object down,Object focused,long tint,Object composer,int flags,int defaults){
                if(fail)throw new IllegalStateException("render failure"); rendered.add(new Object[]{label,focus,click,up,defaults});}
            public static void G(Object id,boolean show,Object click,boolean enabled,Object up,Object down,Object focused,Object composer,int flags){rendered.add(new Object[]{"report",null,click,up,0});}
        }""",
        "fixture.Player" to """package fixture; import software.santodan.extension.nuviobuttons.NuvioPlaybackButtons;
            public class Player {
                public static h1.l0 composer=new h1.l0(); public static boolean expanded,subtitles=true,report=true;
                public static void run(){va.yd.rendered.clear();
                    NuvioPlaybackButtons.play(null,null,"play","play-click","play-focus","up",true,"down","focused",0L,composer,0,0);
                    if(subtitles)NuvioPlaybackButtons.subtitles(null,null,"subtitles","sub-click",null,"up",true,"down","focused",0L,composer,0,16);
                    NuvioPlaybackButtons.registerMore(new More());
                    NuvioPlaybackButtons.more(expanded,null,null,null,null,new Object(),composer,0);
                    NuvioPlaybackButtons.finish(null,null,"more","toggle",null,"up",true,"down","focused",0L,composer,0,16);
                }
                public static class More {
                    public Object invoke(Object scope,Object composer,Object flags){if(!(scope instanceof v.c0))throw new AssertionError();
                        NuvioPlaybackButtons.speed(null,null,"speed","speed-click",null,"up",true,"down","focused",0L,composer,0,16);
                        NuvioPlaybackButtons.info(null,null,"info","info-click","info-focus","up",true,"down","focused",0L,composer,0,0);
                        if(report)NuvioPlaybackButtons.report("report-id",false,"report-click",true,"report-up","down","focused",composer,0);
                        return null;
                    }
                }
            }"""
    )
    val files = sources.map { (name, source) -> root.resolve(name.replace('.', '/') + ".java").also {
        Files.createDirectories(it.parent); Files.write(it, source.toByteArray(Charsets.UTF_8))
    }.toString() }
    val production = File(args[0]).listFiles()!!.filter { it.extension == "java" }.map { it.absolutePath }
    check(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-classpath", System.getProperty("java.class.path"),
        "-d", root.toString(), *(files + production).toTypedArray()) == 0)
    val isolated = object : URLClassLoader(arrayOf(root.toUri().toURL())) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (sources.keys.any { name == it || name.startsWith(it + "$" ) } ||
                name.startsWith("software.santodan.extension.nuviobuttons.NuvioPlaybackButtons") ||
                name.startsWith("software.santodan.extension.nuviobuttons.ButtonsUi")) {
                synchronized(this) {
                    val type = findLoadedClass(name) ?: findClass(name)
                    if (resolve) resolveClass(type)
                    return type
                }
            }
            return super.loadClass(name, resolve)
        }
    }
    isolated.use { loader ->
        val runtime = loader.loadClass("software.santodan.extension.nuviobuttons.NuvioPlaybackButtons")
        val player = loader.loadClass("fixture.Player")
        val native = loader.loadClass("va.yd")
        val ui = loader.loadClass("ab.uh")
        val layout = runtime.getDeclaredField("layout").apply { isAccessible = true }
        val frames = runtime.getDeclaredField("frames").apply { isAccessible = true }.get(null) as ThreadLocal<*>
        val composer = player.getField("composer").get(null)
        fun rendered(): List<Array<*>> = (native.getField("rendered").get(null) as List<*>).map { it as Array<*> }
        fun run(): List<String> {
            player.getMethod("run").invoke(null)
            check(frames.get() == null && composer.javaClass.getField("depth").getInt(composer) == 0)
            return rendered().map { it[0] as String }
        }
        fun settings() = runtime.getMethod("renderSettings", Any::class.java).invoke(null, composer)
        fun action(name: String) = ui.getMethod("action", String::class.java).invoke(null, name)
        fun select(id: String) = ui.getMethod("select", String::class.java).invoke(null, id)
        check(run() == listOf("play", "subtitles", "more"))
        check(loader.loadClass("v.b0").getField("nativeCalls").getInt(null) == 1)
        check(rendered()[0][1] == "play-focus" && rendered()[0][2] == "play-click")
        settings(); (ui.getField("toggle").get(null) as kotlin.jvm.functions.Function0<*>).invoke()
        check((layout.get(null) as ButtonLayout).enabled)
        settings(); action("Edit playback buttons"); settings()
        check(ui.getField("picker").get(null) == "Playback buttons")
        select("speed"); settings(); select("group"); settings()
        check(!(layout.get(null) as ButtonLayout).more.contains("speed"))
        check(run() == listOf("play", "subtitles", "speed", "more"))
        // Reload from persisted preferences as if the process restarted.
        layout.set(null, null)
        check(run() == listOf("play", "subtitles", "speed", "more"))
        player.getField("expanded").setBoolean(null, true)
        check(run() == listOf("play", "subtitles", "speed", "info", "report", "more"))
        val saved = layout.get(null) as ButtonLayout
        layout.set(null, saved.change("subtitles", "group"))
        player.getField("expanded").setBoolean(null, false)
        check(run() == listOf("play", "speed", "more"))
        player.getField("expanded").setBoolean(null, true)
        check(run() == listOf("play", "speed", "subtitles", "info", "report", "more"))
        // Conditional primary controls are absent: temporary Play fallback appears exactly once.
        layout.set(null, ButtonLayout(true, "", "subtitles", "play,speed,info,report"))
        check(run() == listOf("play", "speed", "info", "report", "more"))
        layout.set(null, saved)
        layout.set(null, (layout.get(null) as ButtonLayout).change("play", "visibility").change("info", "visibility"))
        check(run() == listOf("subtitles", "speed", "report", "more"))
        check(rendered()[0][1] == "play-focus" && rendered().last()[1] == "info-focus")
        check(rendered().single { it[0] == "report" }[3] == "report-up")
        player.getField("subtitles").setBoolean(null, false)
        check(run() == listOf("speed", "report", "more"))
        // A rendering failure cannot keep a native callback/composer alive in the collector.
        native.getField("fail").setBoolean(null, true)
        check(runCatching { run() }.isFailure && frames.get() == null)
        native.getField("fail").setBoolean(null, false)
        settings(); action("Reset playback buttons"); settings()
        check(!(layout.get(null) as ButtonLayout).enabled)
        check(run() == listOf("play", "more"))
        check(composer.javaClass.getField("depth").getInt(composer) == 0)
    }
    println("PASS: production callbacks, persistence/reload/reset, disabled native path, expanded/collapsed regrouping, visibility, native action and focus preservation, balanced groups, and failure cleanup")
}
