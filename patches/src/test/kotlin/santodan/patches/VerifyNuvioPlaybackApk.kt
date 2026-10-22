package santodan.patches

import app.morphe.patcher.dex.SdkDexVerifier
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.zip.ZipFile

/** Check the actual CLI-rebuilt APK, including its stripped/relocated DEX classes. */
fun main(args: Array<String>) {
    val extension = args.getOrNull(4) ?: NuvioPlaybackButtonsPatch.EXTENSION
    val expectedHooks = args.getOrNull(5)?.toInt() ?: 17
    val verifier = SdkDexVerifier(File(args[2]), null)
    val verify = SdkDexVerifier::class.java.getDeclaredMethod("verifyClassHierarchy", File::class.java).apply { isAccessible = true }
    val results = args.take(2).mapIndexed { index, path ->
        val directory = File(args[3], if (index == 0) "original" else "rebuilt").apply { mkdirs() }
        val definitions = mutableSetOf<String>()
        val calls = mutableSetOf<String>()
        val parents = mutableMapOf<String, String?>()
        var hooks = 0
        ZipFile(path).use { zip ->
            for (entry in zip.entries().asSequence().filter { it.name.matches(Regex("classes\\d*\\.dex")) }) {
                val target = File(directory, entry.name)
                zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
                target.inputStream().buffered().use { input ->
                    for (owner in DexBackedDexFile.fromInputStream(null, input).classes) {
                        parents[owner.type] = owner.superclass
                        if (index == 1) check(owner.type != "Lsoftware/santodan/nuvio/NuvioNative;")
                        for (method in owner.methods) {
                            definitions += method.toString()
                            if (index == 1 && owner.type == NuvioAccessPatch.TYPE && method.name in NuvioAccessPatch.FIELDS) {
                                val body = method.implementation!!.instructions.toList()
                                check(body.size == 6 && body[0].opcode == com.android.tools.smali.dexlib2.Opcode.IF_NEZ &&
                                    body[4].opcode == com.android.tools.smali.dexlib2.Opcode.IGET_OBJECT)
                                check((body[4] as ReferenceInstruction).reference.toString() == NuvioAccessPatch.FIELDS.getValue(method.name))
                            }
                            if (index == 1 && extension == NuvioPlaybackButtonsPatch.EXTENSION && owner.type == "Lab/cj;" && method.name == "invoke") {
                                val code = method.implementation?.instructions?.toList() ?: emptyList()
                                val intro = code.indexOfFirst { NuvioPlaybackButtonsPatch.reference(it) == NuvioIntroDbPatch.RENDER }
                                if (intro >= 0) check(code.getOrNull(intro + 1)?.let { NuvioPlaybackButtonsPatch.reference(it) }
                                    ?.startsWith(NuvioPlaybackButtonsPatch.EXTENSION + "->finish(") == true) {
                                    "IntroDB must be collected immediately before the customized playback row renders"
                                }
                            }
                            for (instruction in method.implementation?.instructions ?: emptyList()) {
                                val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                                if (index == 1 && owner.type.startsWith("Lsoftware/santodan/extension/nuviobuttons/")) {
                                    check(!(ref.definingClass == "Ljava/util/Optional;" && ref.name == "orElseThrow" && ref.parameterTypes.isEmpty())) {
                                        "Playback editor requires an unavailable Android API: $ref in $method"
                                    }
                                }
                                if (!ref.definingClass.startsWith("Lsoftware/santodan/extension/") &&
                                    !ref.definingClass.startsWith("Lsoftware/santodan/nuvio/")) continue
                                calls += ref.toString()
                                if (!owner.type.startsWith("Lsoftware/santodan/extension/") && ref.definingClass == extension) hooks++
                            }
                    }
                    }
                }
            }
        }
        if (index == 1) {
            check(NuvioAccessPatch.TYPE in parents) { "Shared direct native accessors are missing from the rebuilt APK" }
            check(hooks == expectedHooks) { "Rebuilt APK has $hooks hooks for $extension; expected $expectedHooks" }
            val frameworkContext = "Lsoftware/santodan/extension/nuviolauncher/LauncherArtworkProvider;->getContext()Landroid/content/Context;"
            check(calls.all { it in definitions || (it == frameworkContext &&
                parents["Lsoftware/santodan/extension/nuviolauncher/LauncherArtworkProvider;"] == "Landroid/content/ContentProvider;") }) {
                "Unresolved extension signatures: ${calls - definitions}"
            }
        }
        val report = try { verify.invoke(verifier, directory); "" }
            catch (error: InvocationTargetException) { error.cause?.message ?: throw error }
        if (report.isEmpty()) emptyList() else report.split(Regex("\\n\\s*\\d+\\. ")).drop(1).also { check(it.isNotEmpty()) }
            .map { it.replace(Regex("\\(classes\\d*\\.dex\\)"), "(DEX)").replace(Regex("\\s+"), " ").trim() }.sorted()
    }
    check(results[0] == results[1]) { "Rebuilt APK introduced cross-DEX findings: ${results[1] - results[0].toSet()}" }
    println("PASS: CLI-rebuilt APK has all $expectedHooks hooks for $extension, resolves extension signatures and introduces no cross-DEX findings (${results[0].size} original optional-library findings)")
}
