package santodan.patches

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.loadPatchesFromJar
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val output = File(args[2]).apply { mkdirs() }
    val base = File(output, "original-base.apk")
    ZipFile(args[0]).use { zip ->
        zip.getInputStream(zip.getEntry("base.apk")).use { input -> base.outputStream().use { input.copyTo(it) } }
    }
    val originals = mutableMapOf<String, ClassDef>()
    ZipFile(base).use { zip ->
        for (entry in zip.entries().asSequence().filter { it.name.matches(Regex("classes\\d*\\.dex")) }) {
            zip.getInputStream(entry).buffered().use { input ->
                DexBackedDexFile.fromInputStream(null, input).classes.forEach { originals[it.type] = it }
            }
        }
    }
    val original = originals.getValue(FirstDescendantDiagnosticsPatch.OWNER)
    fun reject(owner: MutableClass) {
        var rejected = false
        try { FirstDescendantDiagnosticsPatch.hook(owner) } catch (_: IllegalStateException) { rejected = true }
        check(rejected) { "Invalid diagnostic fixture accepted" }
    }
    val missing = MutableClass(original)
    missing.methods.removeAll { it.name == "invokeSuspend" }
    reject(missing)
    val already = MutableClass(original)
    FirstDescendantDiagnosticsPatch.hook(already)
    reject(already)
    val changed = MutableClass(original)
    val implementation = changed.methods.single { it.name == "invokeSuspend" }.implementation!!
    val anchor = implementation.instructions.indexOfFirst {
        (it as? ReferenceInstruction)?.reference.toString() == "checkNgsmValidation fail : "
    }
    implementation.removeInstruction(anchor)
    reject(changed)
    val selected = loadPatchesFromJar(setOf(File(args[1])))
        .filter { it.name == FirstDescendantDiagnosticsPatch.NAME }.toSet()
    check(selected.size == 1)
    val rewritten = mutableMapOf<String, ClassDef>()
    Patcher(PatcherConfig(apkFile = base, temporaryFilesPath = File(output, "work-${System.nanoTime()}"))).use { patcher ->
        patcher += selected
        patcher().collect { it.exception?.let { error -> throw error } }
        for (dex in patcher.get().dexFiles) {
            val file = File(output, dex.name)
            dex.stream.use { input -> file.outputStream().use { input.copyTo(it) } }
            file.inputStream().buffered().use { input ->
                DexBackedDexFile.fromInputStream(null, input).classes.forEach { rewritten[it.type] = it }
            }
        }
    }
    val target = rewritten.getValue(original.type).methods.single { it.name == "invokeSuspend" }
    check(target.implementation!!.registerCount == 13)
    val instructions = target.implementation!!.instructions.toList()
    val references = instructions.mapNotNull { (it as? ReferenceInstruction)?.reference?.toString() }
    check(references.count { it == "TFD-NGSM" } == 2)
    check(references.count { it == "Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I" } == 1)
    check(references.count { it == "Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I" } == 1)
    val before = original.methods.single { it.name == "invokeSuspend" }.implementation!!.instructions.toList()
    check(instructions.size == before.size + 5)
    val branchOpcodes = setOf(Opcode.IF_EQ, Opcode.IF_NE, Opcode.IF_EQZ, Opcode.IF_NEZ,
        Opcode.GOTO, Opcode.GOTO_16, Opcode.GOTO_32, Opcode.THROW, Opcode.RETURN_OBJECT)
    check(before.filter { it.opcode in branchOpcodes }.map { it.opcode } ==
        instructions.filter { it.opcode in branchOpcodes }.map { it.opcode })
    for (ref in listOf(
        "Lkr/co/nexon/npaccount/NPAccount;->getNgsmToken(Lkr/co/nexon/npaccount/listener/NXPGetNgsmTokenListener;)V",
        "Lcom/nexon/tfdc/network/TC2AppApi;->d(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;")) {
        check(references.count { it == ref } == 1) { "Validation call changed" }
    }
    for ((type, owner) in rewritten) {
        if (type == original.type) continue
        val prior = originals.getValue(type)
        check(owner.methods.map { it.toString() } == prior.methods.map { it.toString() })
        for (method in owner.methods) {
            val previous = prior.methods.single { it.toString() == method.toString() }
            check(method.implementation?.instructions?.map { it.opcode } ==
                previous.implementation?.instructions?.map { it.opcode })
        }
    }
    println("PASS: packaged diagnostics applied, DEX written/reloaded; two non-token logs; validation calls/branches retained; incompatible and already-patched rejection")
}
