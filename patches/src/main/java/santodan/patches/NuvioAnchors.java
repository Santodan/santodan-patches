package santodan.patches;

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.util.List;
import java.util.function.IntPredicate;

/** Structural anchors return instruction/register identities, never guessed offsets. */
final class NuvioAnchors {
    private NuvioAnchors() {}

    static int unique(List<Instruction> instructions, IntPredicate matches, String description) {
        int result = -1;
        for (int i = 0; i < instructions.size(); i++) if (matches.test(i)) {
            if (result >= 0) throw new IllegalStateException("Ambiguous Nuvio anchor: " + description);
            result = i;
        }
        if (result < 0) throw new IllegalStateException("Missing Nuvio anchor: " + description);
        return result;
    }

    static int reference(List<Instruction> instructions, String reference, String description) {
        return unique(instructions, i -> NuvioPlaybackButtonsPatch.reference(instructions.get(i)).equals(reference), description);
    }

    static int parameter(MutableMethod method, int index) {
        if (index < 0 || index >= method.getParameterTypes().size()) throw new IllegalArgumentException("Parameter index");
        int words = (method.getAccessFlags() & 8) == 0 ? 1 : 0;
        for (CharSequence type : method.getParameterTypes()) words += width(type);
        int register = method.getImplementation().getRegisterCount() - words;
        if ((method.getAccessFlags() & 8) == 0) register++;
        for (int i = 0; i < index; i++) register += width(method.getParameterTypes().get(i));
        return register;
    }

    private static int width(CharSequence type) { return type.charAt(0) == 'J' || type.charAt(0) == 'D' ? 2 : 1; }

    static int branchTarget(List<Instruction> instructions, int branch) {
        int address = ((OffsetInstruction) instructions.get(branch)).getCodeOffset();
        for (int i = 0; i < branch; i++) address += instructions.get(i).getCodeUnits();
        int cursor = 0;
        for (int i = 0; i < instructions.size(); i++) {
            if (cursor == address) return i;
            cursor += instructions.get(i).getCodeUnits();
        }
        throw new IllegalStateException("Nuvio branch target does not point to an instruction");
    }

    static int fieldReceiver(List<Instruction> instructions, String field, int before) {
        int register = -1;
        for (int i = 0; i < before; i++) {
            Instruction instruction = instructions.get(i);
            if (instruction.getOpcode() != Opcode.IGET_BOOLEAN ||
                !NuvioPlaybackButtonsPatch.reference(instruction).equals(field)) continue;
            int candidate = ((TwoRegisterInstruction) instruction).getRegisterB();
            if (register >= 0 && register != candidate) throw new IllegalStateException("Ambiguous Nuvio field receiver: " + field);
            register = candidate;
        }
        if (register < 0) throw new IllegalStateException("Missing Nuvio field receiver: " + field);
        return register;
    }
}
