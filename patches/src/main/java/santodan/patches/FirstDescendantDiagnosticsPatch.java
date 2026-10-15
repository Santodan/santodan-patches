package santodan.patches;

import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference;
import java.util.*;
import kotlin.Unit;

/** Diagnostic logging only: no token values or changed validation results. */
public final class FirstDescendantDiagnosticsPatch {
    public static final String NAME = "TFD Companion - NGSM validation diagnostics - Troubleshooting purpose";
    public static final String OWNER = "Lcom/nexon/tfdc/activity/base/TCSdkActivity$getNgsmToken$2;";
    private FirstDescendantDiagnosticsPatch() {}

    public static BytecodePatch getFirstDescendantDiagnosticsPatch() {
        return PatchKt.bytecodePatch(NAME,
            "Logs NGSM token-result codes and failed validation values under TFD-NGSM. "
                + "Does not log tokens or change authentication results.", false, builder -> {
                builder.compatibleWith(FirstDescendantCompatibility.create());
                builder.execute(context -> {
                    hook(context.mutableClassDefBy(OWNER));
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
    }

    public static void hook(MutableClass owner) {
        if (!OWNER.equals(owner.getType())) throw new IllegalStateException("Unexpected TFD diagnostic owner");
        List<MutableMethod> matches = new ArrayList<>();
        for (MutableMethod method : owner.getMethods()) {
            if (method.getName().equals("invokeSuspend")
                && method.getParameterTypes().equals(Collections.singletonList("Ljava/lang/Object;"))
                && method.getReturnType().equals("Ljava/lang/Object;")) matches.add(method);
        }
        if (matches.size() != 1) throw new IllegalStateException("Missing/ambiguous TFD token coroutine");
        MutableMethod method = matches.get(0);
        if (method.getImplementation() == null || method.getImplementation().getRegisterCount() != 13)
            throw new IllegalStateException("Changed TFD token coroutine registers");
        List<? extends Instruction> instructions = method.getImplementation().getInstructions();
        int result = -1, failure = -1, anchor = -1, fields = 0, anchors = 0;
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            String ref = reference(instruction);
            if (ref.equals("TFD-NGSM")) throw new IllegalStateException("TFD diagnostics already applied");
            if (ref.equals("Lcom/nexon/core/requestpostman/result/NXToyResult;->errorCode:I")) {
                fields++;
                if (result < 0) result = i;
            }
            if (ref.equals("checkNgsmValidation fail : ")) { anchor = i; anchors++; }
        }
        if (fields != 2 || anchors != 1 || result < 0 || anchor < 0)
            throw new IllegalStateException("Changed TFD token-result contract");
        // The first read is v1 <- p1.errorCode; v9/v10 are dead until their
        // following assignments. p1 is v12 with the unchanged 13-register layout.
        Instruction read = instructions.get(result);
        if (read.getOpcode() != Opcode.IGET || !(read instanceof TwoRegisterInstruction)
            || ((TwoRegisterInstruction) read).getRegisterA() != 1
            || ((TwoRegisterInstruction) read).getRegisterB() != 12
            || instructions.get(result + 1).getOpcode() != Opcode.SGET_OBJECT)
            throw new IllegalStateException("Changed TFD token-result register sources");
        for (int i = anchor + 1; i < Math.min(anchor + 15, instructions.size()); i++) {
            Instruction instruction = instructions.get(i);
            if (reference(instruction).equals("Lcom/nexon/tfdc/util/NXLog;->b(Ljava/lang/String;)V")) {
                if (!(instruction instanceof FiveRegisterInstruction)
                    || ((FiveRegisterInstruction) instruction).getRegisterCount() != 1
                    || ((FiveRegisterInstruction) instruction).getRegisterC() != 12)
                    throw new IllegalStateException("Changed TFD failure message register");
                if (failure >= 0) throw new IllegalStateException("Ambiguous TFD failure logger");
                failure = i;
            }
        }
        if (failure < result) throw new IllegalStateException("Missing TFD validation-failure logger");
        // Mutate from the end so earlier indices remain stable. v9 is unused
        // on this failure branch before its cancellation exception is thrown.
        method.getImplementation().replaceInstruction(failure, new BuilderInstruction35c(
            Opcode.INVOKE_STATIC, 2, 9, 12, 0, 0, 0, log("e")));
        method.getImplementation().addInstruction(failure, new BuilderInstruction21c(
            Opcode.CONST_STRING, 9, new ImmutableStringReference("TFD-NGSM")));
        int at = result + 1;
        method.getImplementation().addInstruction(at++, new BuilderInstruction35c(
            Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0, new ImmutableMethodReference(
                "Ljava/lang/String;", "valueOf", Collections.singletonList("I"), "Ljava/lang/String;")));
        method.getImplementation().addInstruction(at++, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 9));
        method.getImplementation().addInstruction(at++, new BuilderInstruction21c(
            Opcode.CONST_STRING, 10, new ImmutableStringReference("TFD-NGSM")));
        method.getImplementation().addInstruction(at, new BuilderInstruction35c(
            Opcode.INVOKE_STATIC, 2, 10, 9, 0, 0, 0, log("i")));
    }

    private static ImmutableMethodReference log(String level) {
        return new ImmutableMethodReference("Landroid/util/Log;", level,
            Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;"), "I");
    }

    private static String reference(Instruction instruction) {
        return instruction instanceof ReferenceInstruction
            ? ((ReferenceInstruction) instruction).getReference().toString() : "";
    }
}
