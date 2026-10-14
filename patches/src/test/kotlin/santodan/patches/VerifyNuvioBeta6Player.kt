package santodan.patches

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import javax.tools.ToolProvider
import software.santodan.extension.nuviostreams.engine.StreamTargets

/** Exercise the production readers with beta.6's actual class and field identities. */
fun main(args: Array<String>) {
    val directory = Files.createTempDirectory(File(args[0]).toPath(), "beta6-player-")
    val sources = mapOf(
        "sa.e2" to """package sa; public class e2 {
            public String a="tt1:3:1",g="2026-01-01";
            public int b=3,c=1; public boolean h=true; public Boolean i=true;
        }""",
        "sa.af" to """package sa; public class af {
            public String z0="tt1:2:9",c1=null,g="SERIES";
            public e2 o1=new e2(); public boolean a=true,b=false,c=false;
            public Object d=null;
        }""",
        "sa.p8" to """package sa; public class p8 { public String S="TV"; }""",
        "sa.df" to """package sa; public class df { public p8 Y=new p8(); }""",
        "p8.e" to """package p8; public class e { public Object B="profile",V2="streams"; }""",
    )
    val files = sources.map { (name, source) ->
        directory.resolve(name.replace('.', '/') + ".java").also {
            Files.createDirectories(it.parent)
            Files.write(it, source.toByteArray(Charsets.UTF_8))
        }.toString()
    }
    check(ToolProvider.getSystemJavaCompiler().run(null, null, null,
        "-d", directory.toString(), *files.toTypedArray()) == 0)
    URLClassLoader(arrayOf(directory.toUri().toURL())).use { loader ->
        val state = loader.loadClass("sa.af").getConstructor().newInstance()
        fun snapshot() = StreamTargets.player(state, "profile", true, true, "TV")
        val ready = snapshot()
        check(ready.playing && ready.currentVideoId == "tt1:2:9")
        check(ready.next == StreamTargets.Media("series", "tt1:3:1", 3, 1))
        state.javaClass.getField("b").setBoolean(state, true)
        check(!snapshot().playing)
        state.javaClass.getField("b").setBoolean(state, false)
        state.javaClass.getField("c1").set(state, "playback failed")
        check(!snapshot().playing)
        state.javaClass.getField("c1").set(state, null)
        val next = state.javaClass.getField("o1").get(state)
        next.javaClass.getField("h").setBoolean(next, false)
        check(snapshot().next == null)
        val model = loader.loadClass("sa.df").getConstructor().newInstance()
        val controller = StreamTargets.field(model, "W")!!
        check(StreamTargets.field(controller, "Q") == "TV")
        val component = loader.loadClass("p8.e").getConstructor().newInstance()
        check(StreamTargets.field(component, "y") == "profile")
        check(StreamTargets.field(component, "K2") == "streams")
    }
    println("PASS: beta.6 player IDs, cross-season target, buffering/errors, unaired rejection, controller and Hilt readers")
}
