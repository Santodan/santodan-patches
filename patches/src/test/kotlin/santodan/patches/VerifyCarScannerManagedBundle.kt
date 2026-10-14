package santodan.patches

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.loadPatchesFromJar
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val selected = loadPatchesFromJar(setOf(File(args[1])))
        .filter { it.name == "Car Scanner - Premium status" }.toSet()
    check(selected.size == 1)
    val output = File(args[2]).apply { mkdirs() }
    val original = ZipFile(args[0]).use { apk ->
        apk.getInputStream(apk.getEntry(CarScannerAssemblyStore.ENTRY)).use { it.readBytes() }
    }
    Patcher(PatcherConfig(apkFile = File(args[0]), temporaryFilesPath = File(output, "work"))).use { patcher ->
        patcher += selected
        patcher().collect { result -> result.exception?.let { throw it } }
        val result = patcher.get()
        check(result.dexFiles.isEmpty()) { "Raw patch unexpectedly produced DEX edits" }
        val candidates = File(output, "work").walkTopDown()
            .filter { it.isFile && it.name == "libassemblies.arm64-v8a.blob.so" }
            .filter { it.length() != original.size.toLong() }.toList()
        check(candidates.size == 1) { "Expected one rewritten store; found $candidates" }
        val rewritten = candidates.single().readBytes()
        VerifyCarScannerManaged.verify(original, rewritten)
        File(output, "patched-store.so").writeBytes(rewritten)
        println("PASS: packaged Car Scanner patch discovered and applied; managed store reloads; no DEX changes")
    }
}
