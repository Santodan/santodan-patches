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
import app.morphe.patcher.dex.SdkDexVerifier
import java.lang.reflect.InvocationTargetException

/** Verify packaged standalone and combined patches against the original beta7 APK. */
fun main(args: Array<String>) = runBlocking {
    NuvioLayout.use("1.1.0-beta.7")
    val originals = mutableMapOf<String, ClassDef>()
    val pickerCallers = mutableListOf<String>()
    val originalDex = File(args[2], "original-dex").apply { mkdirs() }
    ZipFile(args[0]).use { zip ->
        for (entry in zip.entries().asSequence().filter { it.name.matches(Regex("classes\\d*\\.dex")) }) {
            zip.getInputStream(entry).use { input -> File(originalDex, entry.name).outputStream().use { input.copyTo(it) } }
            zip.getInputStream(entry).buffered().use { input ->
                for (owner in DexBackedDexFile.fromInputStream(null, input).classes) {
                    if (owner.type in setOf("Lva/ub;", "Lva/ue;", "Lva/k8;")) originals[owner.type] = owner
                    for (method in owner.methods) for (instruction in method.implementation?.instructions ?: emptyList()) {
                        val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                        if (ref.toString() == "Lva/ub;->D(Lva/k8;Lcom/nuvio/tv/domain/model/Video;Z)V")
                            pickerCallers += "${owner.type}->${method.name}"
                    }
                }
            }
        }
    }
    check(pickerCallers.size == 3 && pickerCallers.all { it == "Lva/mb;->invokeSuspend" })
    val original = originals.getValue("Lva/ub;")
    check(originals.getValue("Lva/ue;").fields.single { it.name == "Y" }.type == "Lva/k8;")
    check(originals.getValue("Lva/k8;").fields.single { it.name == "T" }.type == "Ljava/lang/String;")
    check(original.methods.single { it.name == "q" }.implementation!!.instructions.any {
        (it as? ReferenceInstruction)?.reference?.toString() == "Lva/k8;->T:Ljava/lang/String;"
    })
    fun rejected(owner: MutableClass) {
        var failed = false
        try { NuvioStreamPreloadPatch.hookNextEpisodeRoutesBeta7(owner) }
        catch (expected: IllegalStateException) { failed = true }
        check(failed) { "Invalid or already patched next-episode routes accepted" }
    }
    val missing = MutableClass(original)
    missing.methods.removeAll { it.name == "x" }
    rejected(missing)
    val changed = MutableClass(original)
    changed.methods.single { it.name == "x" }.implementation!!.replaceInstruction(0, BuilderInstruction10x(Opcode.NOP))
    rejected(changed)
    check(changed.methods.single { it.name == "D" }.implementation!!.instructions.first().opcode != Opcode.INVOKE_STATIC_RANGE)
    val ambiguous = MutableClass(original)
    val picker = original.methods.single { it.name == "D" }
    ambiguous.methods.add(MutableMethod(ImmutableMethod(original.type, "D",
        listOf(ImmutableMethodParameter("Ljava/lang/Object;", emptySet(), null)) + picker.parameters.drop(1),
        picker.returnType, picker.accessFlags, picker.annotations, picker.hiddenApiRestrictions, picker.implementation)))
    rejected(ambiguous)
    val installed = MutableClass(original)
    NuvioStreamPreloadPatch.hookNextEpisodeRoutesBeta7(installed)
    rejected(installed)
    println("PASS: beta7 picker callers are exclusively next-episode transitions; missing, changed, ambiguous and duplicate route hooks rejected")
    val available = loadPatchesFromJar(setOf(File(args[1])))
    val hierarchy = SdkDexVerifier(File(args[3]), null)
    val hierarchyMethod = SdkDexVerifier::class.java.getDeclaredMethod("verifyClassHierarchy", File::class.java).apply { isAccessible = true }
    fun hierarchyIssues(directory: File): List<String> {
        val report = try { hierarchyMethod.invoke(hierarchy, directory); return emptyList() }
        catch (error: InvocationTargetException) { error.cause?.message ?: throw error }
        val issues = report.split(Regex("\\n\\s*\\d+\\. ")).drop(1)
        check(issues.isNotEmpty()) { "Unexpected hierarchy verification failure: $report" }
        return issues.map { it.replace(Regex("\\(classes\\d*\\.dex\\)"), "(DEX)").replace(Regex("\\s+"), " ").trim() }.sorted()
    }
    val baselineIssues = hierarchyIssues(originalDex)
    val nextName = "NuvioTV - Preload streams for next episode"
    check(available.count { it.name == nextName } == 1)
    for (mode in listOf("next", "merged", "buttons", "introdb", "current-stream", "combined")) {
        val combined = mode == "combined"
        val mergeOnly = mode == "merged"
        val buttonsOnly = mode == "buttons"
        val introOnly = mode == "introdb"
        val currentOnly = mode == "current-stream"
        val selected = available.filter {
            if (combined) it.name?.startsWith("NuvioTV - ") == true && it.name != "NuvioTV - Side-by-side installation"
            else it.name == if (currentOnly) "NuvioTV - Indicate current stream" else if (introOnly) "NuvioTV - Submit segments to IntroDB" else if (buttonsOnly) "NuvioTV - Customize playback buttons" else if (mergeOnly) "NuvioTV - Merge tracking progress" else nextName
        }.toSet()
        check(selected.size == if (combined) 12 else 1)
        val output = File(args[2], mode).apply { mkdirs() }
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
            val mergeHooks = mutableMapOf<String, Int>()
            var introHooks = 0
            var currentLabels = 0
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
                            if (owner.type == "Lva/z;" && method.name == "q") {
                                val body = instructions.toList()
                                currentLabels += body.count { (it as? ReferenceInstruction)?.reference?.toString() == NuvioCurrentStreamPatch.LABEL_METHOD }
                                if (currentOnly || combined) {
                                    check(body[142].opcode == Opcode.INVOKE_STATIC)
                                    val call = body[142] as com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
                                    check(call.registerCount == 2 && call.registerC == 7 && call.registerD == 8)
                                    check((body[142] as ReferenceInstruction).reference.toString() == NuvioCurrentStreamPatch.LABEL_METHOD)
                                    check(body[143].opcode == Opcode.MOVE_RESULT_OBJECT && (body[143] as OneRegisterInstruction).registerA == 7)
                                }
                            }
                            if ((combined || mergeOnly) && owner.type == "Lba/ze;" && method.name in setOf("C", "s")) {
                                val entry = instructions.toList()
                                check((entry[0] as ReferenceInstruction).reference.toString().endsWith("->syncLocalProgress(Z)Z"))
                                check((entry[0] as RegisterRangeInstruction).startRegister == method.implementation!!.registerCount - 1)
                                check(entry[1].opcode == Opcode.MOVE_RESULT &&
                                    (entry[1] as OneRegisterInstruction).registerA == method.implementation!!.registerCount - 1)
                            }
                            if ((combined || mergeOnly) && owner.type == "Lba/hd;" && method.name == "invokeSuspend") {
                                val entry = instructions.toList()
                                val returns = entry.indices.filter { entry[it].opcode == Opcode.RETURN_OBJECT }
                                check(returns.size == 2)
                                check((entry[returns.first() - 3] as? ReferenceInstruction)?.reference?.toString()?.contains("->mergedProvider(") != true) { "Watch-party branch was modified" }
                                for (at in listOf(returns.last())) {
                                    val register = (entry[at] as OneRegisterInstruction).registerA
                                    check((entry[at - 3] as ReferenceInstruction).reference.toString().contains("->mergedProvider("))
                                    check((entry[at - 3] as RegisterRangeInstruction).startRegister == register)
                                    check(entry[at - 2].opcode == Opcode.MOVE_RESULT_OBJECT && entry[at - 1].opcode == Opcode.CHECK_CAST)
                                }
                            }
                            for (instruction in instructions) {
                                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                                if (ref.definingClass == NuvioIntroDbPatch.EXT && !owner.type.startsWith("Lsoftware/santodan/extension/")) introHooks++
                                if ((owner.type.startsWith("Lsoftware/santodan/extension/nuviostreams/") ||
                                    owner.type.startsWith("Lsoftware/santodan/extension/nuviomerged/")) &&
                                    ref.definingClass.startsWith("Lkotlinx/coroutines/")) coroutineCalls += ref.toString()
                                if (ref.definingClass == "Lsoftware/santodan/extension/nuviomerged/NuvioMergedProgress;" &&
                                    !owner.type.startsWith("Lsoftware/santodan/extension/")) {
                                    mergeHooks[ref.name] = (mergeHooks[ref.name] ?: 0) + 1
                                }
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
            check(currentLabels == if (currentOnly || combined) 1 else 0)
            check((NuvioCurrentStreamPatch.EXTENSION in classes) == (currentOnly || combined))
            if (!mergeOnly && !buttonsOnly && !introOnly && !currentOnly) {
            for (hook in listOf("registerComponent", "observeNextEpisode", "stopNextEpisode", "nextEpisodePluginPause"))
                check(nativeHooks[hook] == 1) { "Expected one $hook hook, found ${nativeHooks[hook]}" }
            check(nativeHooks["nextEpisodePickerRefresh"] == 1) { "Next-episode picker entry must reuse the warmed search" }
            check(nativeHooks["useNativeNextEpisodeThresholdPreload"] == 1)
            // Inspect the assembled entry points, not just the presence of extension calls.
            for (dex in output.listFiles()!!.filter { it.name.matches(Regex("classes\\d*\\.dex")) }) {
                dex.inputStream().buffered().use { input ->
                    for (owner in DexBackedDexFile.fromInputStream(null, input).classes.filter { it.type == "Lva/ub;" }) {
                        val entry = owner.methods.single { it.name == "D" }.implementation!!
                        val instructions = entry.instructions.toList()
                        check((instructions[0] as RegisterRangeInstruction).startRegister == entry.registerCount - 1)
                        check(instructions[1].opcode == Opcode.MOVE_RESULT &&
                            (instructions[1] as OneRegisterInstruction).registerA == entry.registerCount - 1)
                        val threshold = owner.methods.single { it.name == "x" }.implementation!!.instructions.toList()
                        check(threshold[0].opcode == Opcode.INVOKE_STATIC_RANGE && threshold[1].opcode == Opcode.MOVE_RESULT)
                        check(threshold[2].opcode == Opcode.IF_NEZ && threshold[3].opcode == Opcode.RETURN_VOID)
                        check((threshold[2] as OffsetInstruction).codeOffset == threshold[2].codeUnits + threshold[3].codeUnits)
                        check(threshold[4].opcode == Opcode.CONST_STRING)
                    }
                }
            }
            }
            if (combined || mergeOnly) {
                for (hook in listOf("registerNavigation", "activityResumed", "activityPaused", "activityDestroyed", "playbackScrobblers"))
                    check(mergeHooks[hook] == 1) { "Expected one merge $hook hook: $mergeHooks" }
                check(mergeHooks["syncLocalProgress"] == 2 && mergeHooks["mergedProvider"] == 1)
                check("setPlaybackPaused" !in mergeHooks) { "Search pause must not control merged sync" }
                check("Lsoftware/santodan/extension/nuviomerged/NuvioMergePolicy;" in classes)
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
            check(("Lsoftware/santodan/extension/nuvionextstreams/NuvioNextEpisodeStreams;" in classes) == (!mergeOnly && !buttonsOnly && !introOnly && !currentOnly))
            check(("Lsoftware/santodan/extension/nuviobuttons/NuvioPlaybackButtons;" in classes) == (buttonsOnly || combined))
            check(("Lsoftware/santodan/extension/nuviocwstreams/NuvioContinueWatchingStreams;" in classes) == combined)
            check(("Lsoftware/santodan/extension/nuviodetailstreams/NuvioDetailStreams;" in classes) == combined)
            if (buttonsOnly || combined) {
                val buttonHooks = extensionCalls.filter { it.startsWith(NuvioPlaybackButtonsPatch.EXTENSION) }
                check(buttonHooks.size >= 16) { "Playback button hooks missing from packaged DEX: $buttonHooks" }
            }
            if (introOnly || combined) {
                check("Lsoftware/santodan/extension/nuviointro/Submission;" in classes)
                check(introHooks == 3) { "Expected three native IntroDB hooks, found $introHooks" }
            }
            println("PASS: ${if (combined) "all twelve Nuvio behavior patches coexist" else if (currentOnly) "standalone current-stream indicator" else if (introOnly) "standalone IntroDB submission patch" else if (buttonsOnly) "standalone playback-buttons patch" else if (mergeOnly) "standalone merged-progress patch" else "standalone next-episode patch"}; assembled DEX reloads and hooks resolve")
            check(hierarchyIssues(output) == baselineIssues) { "Patched cross-DEX hierarchy findings differ from original" }
            println("PASS: $mode cross-DEX hierarchy has no new findings (${baselineIssues.size} original optional-library findings)")
        }
    }
}
