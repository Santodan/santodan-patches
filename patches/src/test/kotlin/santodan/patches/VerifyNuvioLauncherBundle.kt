package santodan.patches

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.loadPatchesFromJar
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.io.File
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val selected = loadPatchesFromJar(setOf(File(args[1])))
        .filter { it.name == "NuvioTV - Android TV launcher channels" }.toSet()
    check(selected.size == 1)
    val output = File(args[2]).apply { mkdirs() }
    Patcher(PatcherConfig(apkFile = File(args[0]), temporaryFilesPath = File(output, "work"))).use { patcher ->
        patcher += selected
        patcher().collect { result -> result.exception?.let { throw it } }
        val manifest = File(output, "work/apk/AndroidManifest.xml")
        val document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifest)
        val providers = document.getElementsByTagName("provider")
        val artworkProvider = (0 until providers.length).map { providers.item(it) as org.w3c.dom.Element }
            .single { it.getAttribute("android:name") == "software.santodan.extension.nuviolauncher.LauncherArtworkProvider" }
        check(artworkProvider.getAttribute("android:exported") == "true")
        check(artworkProvider.getAttribute("android:authorities") == "com.nuvio.tv.santodan.launcher.artwork")
        transformNuvioManifest(document, "com.nuvio.santodan", "NuvioTV Patched")
        check(artworkProvider.getAttribute("android:authorities") == "com.nuvio.santodan.santodan.launcher.artwork")
        val definitions = mutableSetOf<String>()
        val parents = mutableMapOf<String, List<String>>()
        val calls = mutableSetOf<String>()
        val hooks = mutableMapOf<String, Int>()
        for (dex in patcher.get().dexFiles) {
            val file = File(output, dex.name)
            dex.stream.use { input -> file.outputStream().use { input.copyTo(it) } }
            file.inputStream().buffered().use { input ->
                for (owner in DexBackedDexFile.fromInputStream(null, input).classes) {
                    parents[owner.type] = listOfNotNull(owner.superclass) + owner.interfaces
                    for (method in owner.methods) {
                        definitions += method.toString()
                        for (instruction in method.implementation?.instructions ?: emptyList()) {
                            val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                            if (owner.type.startsWith("Lsoftware/santodan/extension/nuviolauncher/") &&
                                (ref.definingClass.startsWith("Lkotlinx/coroutines/") || ref.definingClass.startsWith("Lsoftware/santodan/extension/"))) calls += ref.toString()
                            if (!owner.type.startsWith("Lsoftware/santodan/extension/") && ref.definingClass == NuvioLauncherChannelsPatch.EXTENSION)
                                hooks[ref.name] = (hooks[ref.name] ?: 0) + 1
                        }
                    }
                }
            }
        }
        fun resolves(reference: String): Boolean {
            val visited = mutableSetOf<String>()
            val signature = reference.substringAfter("->")
            fun find(type: String): Boolean = visited.add(type) &&
                ("$type->$signature" in definitions ||
                    (type == "Landroid/content/ContentProvider;" && signature == "getContext()Landroid/content/Context;") ||
                    parents[type].orEmpty().any { find(it) })
            return find(reference.substringBefore("->"))
        }
        check(calls.all { resolves(it) }) { "Unresolved launcher runtime APIs: ${calls.filter { !resolves(it) }}" }
        check(hooks == mapOf("registerComponent" to 1, "observeHome" to 1, "stopHome" to 1, "observeLibrary" to 1,
            "effectiveCachedHasAired" to 1)) { "Incorrect standalone hooks: $hooks" }
        check("Lsoftware/santodan/extension/nuviolauncher/LauncherArtworkProvider;" in parents)
        check(parents.keys.none { it.startsWith("Lsoftware/santodan/extension/nuviostreams/") })
        println("PASS: packaged standalone launcher patch applies to beta5; merged DEX reloads, five hooks, artwork provider and native coroutine calls resolve")
    }
}
