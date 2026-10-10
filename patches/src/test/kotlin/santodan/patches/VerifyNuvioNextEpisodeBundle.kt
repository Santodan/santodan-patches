package santodan.patches

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.loadPatchesFromJar
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.io.File
import kotlinx.coroutines.runBlocking

/** Verify packaged standalone and combined patches against the original beta5 APK. */
fun main(args: Array<String>) = runBlocking {
    val available = loadPatchesFromJar(setOf(File(args[1])))
    val nextName = "NuvioTV - Preload streams for next episode"
    check(available.count { it.name == nextName } == 1)
    for (combined in listOf(false, true)) {
        val selected = available.filter {
            if (combined) it.name?.startsWith("NuvioTV - ") == true && it.name != "NuvioTV - Side-by-side installation"
            else it.name == nextName
        }.toSet()
        check(selected.size == if (combined) 8 else 1)
        val output = File(args[2], if (combined) "combined" else "standalone").apply { mkdirs() }
        Patcher(PatcherConfig(apkFile = File(args[0]), temporaryFilesPath = File(output, "work"))).use { patcher ->
            patcher += selected
            patcher().collect { result ->
                result.exception?.let { throw it }
                println("PASS: packaged patch applied: ${result.patch.name}")
            }
            val definitions = mutableSetOf<String>()
            val parents = mutableMapOf<String, List<String>>()
            val extensionCalls = mutableSetOf<String>()
            val coroutineCalls = mutableSetOf<String>()
            val classes = mutableSetOf<String>()
            val nativeHooks = mutableMapOf<String, Int>()
            for (dex in patcher.get().dexFiles) {
                val target = File(output, dex.name)
                dex.stream.use { input -> target.outputStream().use { input.copyTo(it) } }
                target.inputStream().buffered().use { input ->
                    for (owner in DexBackedDexFile.fromInputStream(null, input).classes) {
                        classes += owner.type
                        parents[owner.type] = listOfNotNull(owner.superclass) + owner.interfaces
                        for (method in owner.methods) {
                            definitions += method.toString()
                            val instructions = method.implementation?.instructions ?: continue
                            for (instruction in instructions) {
                                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                                if (owner.type.startsWith("Lsoftware/santodan/extension/nuviostreams/") &&
                                    ref.definingClass.startsWith("Lkotlinx/coroutines/")) coroutineCalls += ref.toString()
                                if (!ref.definingClass.startsWith("Lsoftware/santodan/extension/")) continue
                                extensionCalls += ref.toString()
                                if (ref.definingClass == NuvioStreamPreloadPatch.EXTENSION &&
                                    !owner.type.startsWith("Lsoftware/santodan/extension/")) {
                                    nativeHooks[ref.name] = (nativeHooks[ref.name] ?: 0) + 1
                                }
                            }
                        }
                    }
                }
            }
            for (hook in listOf("registerComponent", "observeNextEpisode", "stopNextEpisode", "nextEpisodePluginPause"))
                check(nativeHooks[hook] == 1) { "Expected one $hook hook, found ${nativeHooks[hook]}" }
            check(nativeHooks["nextEpisodePickerRefresh"] == 3) { "Next-episode manual picker must reuse the warmed search" }
            check(extensionCalls.all { it in definitions }) {
                "Missing extension signatures: ${extensionCalls.filter { it !in definitions }}"
            }
            fun resolves(reference: String): Boolean {
                val signature = reference.substringAfter("->")
                val visited = mutableSetOf<String>()
                fun find(type: String): Boolean = visited.add(type) &&
                    ("$type->$signature" in definitions || parents[type].orEmpty().any { find(it) })
                return find(reference.substringBefore("->"))
            }
            check(coroutineCalls.all { resolves(it) }) {
                "Missing native coroutine APIs: ${coroutineCalls.filter { !resolves(it) }}"
            }
            check("Lsoftware/santodan/extension/nuvionextstreams/NuvioNextEpisodeStreams;" in classes)
            check(("Lsoftware/santodan/extension/nuviocwstreams/NuvioContinueWatchingStreams;" in classes) == combined)
            check(("Lsoftware/santodan/extension/nuviodetailstreams/NuvioDetailStreams;" in classes) == combined)
            println("PASS: ${if (combined) "all eight Nuvio runtime patches coexist" else "standalone next-episode patch"}; assembled DEX reloads and hooks resolve")
        }
    }
}
