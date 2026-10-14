package santodan.patches

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.loadPatchesFromJar
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking

/** Verify packaged standalone and combined patches against the original beta6 APK. */
fun main(args: Array<String>) = runBlocking {
    NuvioLayout.use("1.1.0-beta.6")
    val originals = mutableMapOf<String, ClassDef>()
    val pickerCallers = mutableListOf<String>()
    ZipFile(args[0]).use { zip ->
        for (entry in zip.entries().asSequence().filter { it.name.matches(Regex("classes\\d*\\.dex")) }) {
            zip.getInputStream(entry).buffered().use { input ->
                for (owner in DexBackedDexFile.fromInputStream(null, input).classes) {
                    if (owner.type in setOf("Lsa/ec;", "Lsa/df;", "Lsa/p8;")) originals[owner.type] = owner
                    for (method in owner.methods) for (instruction in method.implementation?.instructions ?: emptyList()) {
                        val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                        if (ref.toString() == "Lsa/ec;->C(Lsa/p8;Lcom/nuvio/tv/domain/model/Video;Z)V")
                            pickerCallers += "${owner.type}->${method.name}"
                    }
                }
            }
        }
    }
    check(pickerCallers.size == 3 && pickerCallers.all { it == "Lsa/wb;->invokeSuspend" })
    val original = originals.getValue("Lsa/ec;")
    check(originals.getValue("Lsa/df;").fields.single { it.name == "Y" }.type == "Lsa/p8;")
    check(originals.getValue("Lsa/p8;").fields.single { it.name == "S" }.type == "Ljava/lang/String;")
    check(original.methods.single { it.name == "p" }.implementation!!.instructions.any {
        (it as? ReferenceInstruction)?.reference?.toString() == "Lsa/p8;->S:Ljava/lang/String;"
    })
    fun rejected(owner: MutableClass) {
        var failed = false
        try { NuvioStreamPreloadPatch.hookNextEpisodeRoutesBeta6(owner) }
        catch (expected: IllegalStateException) { failed = true }
        check(failed) { "Invalid or already patched next-episode routes accepted" }
    }
    val missing = MutableClass(original)
    missing.methods.removeAll { it.name == "w" }
    rejected(missing)
    val changed = MutableClass(original)
    changed.methods.single { it.name == "w" }.implementation!!.replaceInstruction(0, BuilderInstruction10x(Opcode.NOP))
    rejected(changed)
    check(changed.methods.single { it.name == "C" }.implementation!!.instructions.first().opcode != Opcode.INVOKE_STATIC_RANGE)
    val ambiguous = MutableClass(original)
    val picker = original.methods.single { it.name == "C" }
    ambiguous.methods.add(MutableMethod(ImmutableMethod(original.type, "C",
        listOf(ImmutableMethodParameter("Ljava/lang/Object;", emptySet(), null)) + picker.parameters.drop(1),
        picker.returnType, picker.accessFlags, picker.annotations, picker.hiddenApiRestrictions, picker.implementation)))
    rejected(ambiguous)
    val installed = MutableClass(original)
    NuvioStreamPreloadPatch.hookNextEpisodeRoutesBeta6(installed)
    rejected(installed)
    println("PASS: beta6 picker callers are exclusively next-episode transitions; missing, changed, ambiguous and duplicate route hooks rejected")
    val available = loadPatchesFromJar(setOf(File(args[1])))
    val nextName = "NuvioTV - Preload streams for next episode"
    check(available.count { it.name == nextName } == 1)
    for (combined in listOf(false, true)) {
        val selected = available.filter {
            if (combined) it.name?.startsWith("NuvioTV - ") == true && it.name != "NuvioTV - Side-by-side installation"
            else it.name == nextName
        }.toSet()
        check(selected.size == if (combined) 9 else 1)
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
            check(nativeHooks["nextEpisodePickerRefresh"] == 1) { "Next-episode picker entry must reuse the warmed search" }
            check(nativeHooks["useNativeNextEpisodeThresholdPreload"] == 1)
            // Inspect the assembled entry points, not just the presence of extension calls.
            for (dex in output.listFiles()!!.filter { it.name.matches(Regex("classes\\d*\\.dex")) }) {
                dex.inputStream().buffered().use { input ->
                    for (owner in DexBackedDexFile.fromInputStream(null, input).classes.filter { it.type == "Lsa/ec;" }) {
                        val entry = owner.methods.single { it.name == "C" }.implementation!!
                        val instructions = entry.instructions.toList()
                        check((instructions[0] as RegisterRangeInstruction).startRegister == entry.registerCount - 1)
                        check(instructions[1].opcode == Opcode.MOVE_RESULT &&
                            (instructions[1] as OneRegisterInstruction).registerA == entry.registerCount - 1)
                        val threshold = owner.methods.single { it.name == "w" }.implementation!!.instructions.toList()
                        check(threshold[0].opcode == Opcode.INVOKE_STATIC_RANGE && threshold[1].opcode == Opcode.MOVE_RESULT)
                        check(threshold[2].opcode == Opcode.IF_NEZ && threshold[3].opcode == Opcode.RETURN_VOID)
                        check((threshold[2] as OffsetInstruction).codeOffset == threshold[2].codeUnits + threshold[3].codeUnits)
                        check(threshold[4].opcode == Opcode.CONST_STRING)
                    }
                }
            }
            val frameworkAccessor = "Lsoftware/santodan/extension/nuviolauncher/LauncherArtworkProvider;->getContext()Landroid/content/Context;"
            check(extensionCalls.all { it in definitions || (it == frameworkAccessor &&
                parents["Lsoftware/santodan/extension/nuviolauncher/LauncherArtworkProvider;"]?.contains("Landroid/content/ContentProvider;") == true) }) {
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
            println("PASS: ${if (combined) "all nine Nuvio runtime patches coexist" else "standalone next-episode patch"}; assembled DEX reloads and hooks resolve")
        }
    }
}
