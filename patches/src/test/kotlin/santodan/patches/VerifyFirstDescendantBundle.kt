package santodan.patches

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.loadPatchesFromJar
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/** Applies the shipped patch to an unsigned fixture made only from original splits. */
fun main(args: Array<String>) = runBlocking {
    val output = File(args[2]).apply { mkdirs() }
    val base = File(output, "original-base.apk")
    val split = File(output, "original-arm64.apk")
    ZipFile(args[0]).use { archive ->
        for ((name, target) in listOf("base.apk" to base, "split_config.arm64_v8a.apk" to split)) {
            archive.getInputStream(archive.getEntry(name)).use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
        }
    }
    val nativeLibraries = ZipFile(split).use { zip ->
        zip.entries().asSequence().filter { it.name.startsWith("lib/") && it.name.endsWith(".so") }
            .associate { entry -> entry.name to zip.getInputStream(entry).use { it.readBytes() } }
    }
    check(nativeLibraries.size == 7)
    val fixture = File(output, "original-merged-fixture.apk")
    ZipOutputStream(fixture.outputStream()).use { destination ->
        for (source in listOf(base, split)) ZipFile(source).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory || entry.name.startsWith("META-INF/")) continue
                if (source == split && !entry.name.startsWith("lib/")) continue
                destination.putNextEntry(ZipEntry(entry.name))
                zip.getInputStream(entry).use { it.copyTo(destination) }
                destination.closeEntry()
            }
        }
    }
    val available = loadPatchesFromJar(setOf(File(args[1])))
    check(available.none { it.name?.startsWith("TFD Companion - Suppress") == true }) {
        "Retired native suppression patch is still discoverable"
    }
    val selected = available
        .filter { it.name in setOf(TFD_VISIBILITY_NAME, FirstDescendantDiagnosticsPatch.NAME) }.toSet()
    check(selected.size == 2) { "Expected both packaged TFD patches" }
    val work = File(output, "work-${System.nanoTime()}")
    Patcher(PatcherConfig(apkFile = fixture, temporaryFilesPath = work)).use { patcher ->
        patcher += selected
        patcher().collect { result -> result.exception?.let { throw it } }
        val result = patcher.get()
        var diagnosticOwners = 0
        for (dex in result.dexFiles) dex.stream.buffered().use { input ->
            for (owner in DexBackedDexFile.fromInputStream(null, input).classes) {
                if (owner.type != FirstDescendantDiagnosticsPatch.OWNER) continue
                diagnosticOwners++
                val method = owner.methods.single { it.name == "invokeSuspend" }
                check(method.implementation!!.instructions.count {
                    (it as? ReferenceInstruction)?.reference.toString() == "TFD-NGSM"
                } == 2)
            }
        }
        check(diagnosticOwners == 1) { "Missing/duplicate diagnostic coroutine in combined DEX" }
        val resources = requireNotNull(result.resources)
        ZipFile(requireNotNull(resources.resourcesApk)).use { compiled ->
            for ((path, original) in nativeLibraries) {
                val entry = compiled.getEntry(path)
                val other = resources.otherResources?.resolve(path)
                check(resources.deleteResources.none { path == it || path.startsWith("$it/") }) {
                    "Native library scheduled for deletion: $path"
                }
                // Morphe normally retains native ZIP entries in the input APK
                // instead of extracting them. Missing output overrides therefore
                // mean reuse of the original archive entry, not a missing library.
                val retained = if (entry != null) compiled.getInputStream(entry).use { it.readBytes() }
                    else if (other?.isFile == true) other.readBytes()
                    else ZipFile(fixture).use { input -> input.getInputStream(input.getEntry(path)).use { it.readBytes() } }
                check(retained.contentEquals(original)) { "Native library changed: $path" }
            }
        }
        println("PASS: visibility and diagnostics applied together; all seven original native libraries retained; diagnostic DEX reloaded; retired suppression absent")
    }
}
