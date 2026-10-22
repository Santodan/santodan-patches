package santodan.patches

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import javax.tools.ToolProvider
import software.santodan.extension.nuviostreams.engine.StreamTargets

/** Exercise the production readers with beta.8's actual class and field identities. */
fun main(args: Array<String>) {
    val directory = Files.createTempDirectory(File(args[0]).toPath(), "beta8-player-")
    val sources = mapOf(
        "ab.s4" to """package ab; public class s4 {
            public String a="tt1:3:1",g="2026-01-01";
            public int b=3,c=1; public boolean h=true; public Boolean i=true;
        }""",
        "ab.nm" to """package ab; public class nm {
            public String C0="tt1:2:9",j1=null,g="SERIES";
            public ab.s4 v1=new ab.s4(); public boolean a=true,b=false,c=false;
            public Object d=null;
        }""",
        "ab.mc" to """package ab; public class mc { public String T="TV"; }""",
        "ab.qm" to """package ab; public class qm { public ab.mc Y=new ab.mc(); }""",
        "q8.e" to """package q8; public class e { public Object B="profile",e3="streams"; }""",
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
        val state = loader.loadClass("ab.nm").getConstructor().newInstance()
        fun snapshot() = StreamTargets.player(state, "profile", true, true, "TV")
        val ready = snapshot()
        check(ready.playing && ready.currentVideoId == "tt1:2:9")
        check(ready.next == StreamTargets.Media("series", "tt1:3:1", 3, 1))
        state.javaClass.getField("b").setBoolean(state, true)
        check(!snapshot().playing)
        state.javaClass.getField("b").setBoolean(state, false)
        state.javaClass.getField("j1").set(state, "playback failed")
        check(!snapshot().playing)
        state.javaClass.getField("j1").set(state, null)
        val next = state.javaClass.getField("v1").get(state)
        next.javaClass.getField("h").setBoolean(next, false)
        check(snapshot().next == null)
        val model = loader.loadClass("ab.qm").getConstructor().newInstance()
        val controller = StreamTargets.field(model, "Y")!!
        check(StreamTargets.field(controller, "T") == "TV")
        val component = loader.loadClass("q8.e").getConstructor().newInstance()
        check(StreamTargets.field(component, "B") == "profile")
        check(StreamTargets.field(component, "e3") == "streams")
    }
    println("PASS: beta.8 player IDs, cross-season target, buffering/errors, unaired rejection, controller and Hilt readers")
}
