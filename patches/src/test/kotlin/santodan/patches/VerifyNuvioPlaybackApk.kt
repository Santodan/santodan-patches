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
    val expectedHooks = args.getOrNull(5)?.toInt() ?: 16
    val verifier = SdkDexVerifier(File(args[2]), null)
    val verify = SdkDexVerifier::class.java.getDeclaredMethod("verifyClassHierarchy", File::class.java).apply { isAccessible = true }
    val results = args.take(2).mapIndexed { index, path ->
        val directory = File(args[3], if (index == 0) "original" else "rebuilt").apply { mkdirs() }
        val definitions = mutableSetOf<String>()
        val calls = mutableSetOf<String>()
        var hooks = 0
        ZipFile(path).use { zip ->
            for (entry in zip.entries().asSequence().filter { it.name.matches(Regex("classes\\d*\\.dex")) }) {
                val target = File(directory, entry.name)
                zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
                target.inputStream().buffered().use { input ->
                    for (owner in DexBackedDexFile.fromInputStream(null, input).classes) for (method in owner.methods) {
                        definitions += method.toString()
                        for (instruction in method.implementation?.instructions ?: emptyList()) {
                            val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                            if (!ref.definingClass.startsWith("Lsoftware/santodan/extension/")) continue
                            calls += ref.toString()
                            if (!owner.type.startsWith("Lsoftware/santodan/extension/") && ref.definingClass == extension) hooks++
                        }
                    }
                }
            }
        }
        if (index == 1) {
            check(hooks == expectedHooks) { "Rebuilt APK has $hooks hooks for $extension; expected $expectedHooks" }
            check(calls.all { it in definitions }) { "Unresolved extension signatures: ${calls - definitions}" }
        }
        val report = try { verify.invoke(verifier, directory); "" }
            catch (error: InvocationTargetException) { error.cause?.message ?: throw error }
        if (report.isEmpty()) emptyList() else report.split(Regex("\\n\\s*\\d+\\. ")).drop(1).also { check(it.isNotEmpty()) }
            .map { it.replace(Regex("\\(classes\\d*\\.dex\\)"), "(DEX)").replace(Regex("\\s+"), " ").trim() }.sorted()
    }
    check(results[0] == results[1]) { "Rebuilt APK introduced cross-DEX findings: ${results[1] - results[0].toSet()}" }
    println("PASS: CLI-rebuilt APK has all $expectedHooks hooks for $extension, resolves extension signatures and introduces no cross-DEX findings (${results[0].size} original optional-library findings)")
}
