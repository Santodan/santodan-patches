package santodan.patches;

import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.util.*;
import kotlin.Unit;

/** Collects original native button arguments, then renders the configured order in the native row. */
public final class NuvioPlaybackButtonsPatch {
    static final String EXTENSION = "Lsoftware/santodan/extension/nuviobuttons/NuvioPlaybackButtons;";
    static final String BUTTON = "Lva/yd;->b(Lk2/e;Lj2/a;Ljava/lang/String;Lkotlin/jvm/functions/Function0;Lc2/a0;Lc2/a0;ZLkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;JLh1/o;II)V";
    static final String ANIMATION = "Lv/b0;->d(ZLx1/q;Lv/x0;Lv/y0;Ljava/lang/String;Lr1/s;Lh1/o;I)V";
    static final String REPORT = "Lva/yd;->G(Ljava/lang/String;ZLkotlin/jvm/functions/Function0;ZLc2/a0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lh1/o;I)V";
    static final String MORE_CONSTRUCTOR = "Lpa/a;-><init>(Lkotlin/jvm/functions/Function0;Lc2/a0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lcoil3/compose/AsyncImagePainter;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lc2/a0;Lva/re;ZLkotlin/jvm/functions/Function0;)V";

    @SuppressWarnings({"unchecked", "deprecation"})
    public static BytecodePatch getNuvioPlaybackButtonsPatch() {
        return PatchKt.bytecodePatch("NuvioTV - Customize playback buttons",
            "Adds optional playback button visibility, ordering and placement in the main row or More actions, with saved settings and reset.", false, builder -> {
            builder.compatibleWith(new Compatibility("com.nuvio.tv", "NuvioTV", null, ApkFileType.APK,
                null, null, List.of(new AppTarget("1.1.0-beta.7", false, null)), false));
            builder.dependsOn(NuvioSettingsMenuPatch.getMenuPatch());
            builder.extendWith(() -> NuvioStreamPreloadPatch.extension("nuvio-playback-buttons"));
            builder.execute(context -> {
                if (!"com.nuvio.tv".equals(context.getPackageMetadata().getPackageName()) ||
                    !"1.1.0-beta.7".equals(context.getPackageMetadata().getVersionName()))
                    throw new IllegalStateException("Playback buttons require NuvioTV 1.1.0-beta.7");
                MutableClass main = context.mutableClassDefBy("Lva/bd;"), more = context.mutableClassDefBy("Lpa/a;");
                validateRuntime(new MutableClass(context.classDefBy("Lva/yd;")),
                    new MutableClass(context.classDefBy("Lv/b0;")), new MutableClass(context.classDefBy("Lv/c0;")));
                for (String signature : UI_CONTRACTS) validateUiMethod(new MutableClass(context.classDefBy(
                    signature.substring(0, signature.indexOf("->")))), signature);
                validate(main, more); // Validate both owners before any mutation.
                hook(main, more);
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
    }
    static final List<String> UI_CONTRACTS = List.of(
        "Lab/uh;->d(Ljava/lang/String;Lkotlin/jvm/functions/Function0;ZZLh1/o;II)V",
        "Lab/uh;->e(Lx1/c;Lr1/s;Lh1/o;II)V",
        "Lab/uh;->j(Ljava/lang/String;Ljava/util/List;Ljava/lang/Object;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Ljava/lang/String;FFLh1/o;II)V",
        "Lab/uh;->m(Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Lx1/q;Lkotlin/jvm/functions/Function0;ZLk2/e;JZLh1/o;II)V",
        "Lab/fi;-><init>(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lm3/n;I)V",
        "Lr1/s;-><init>(ILjava/lang/Object;Z)V", "Lh1/l0;->d0(I)V", "Lh1/l0;->p(Z)V");
    static void validateUiMethod(MutableClass owner, String signature) {
        MutableMethod expected = owner.getMethods().stream().filter(m -> m.toString().equals(signature)).findFirst()
            .orElseThrow(() -> new IllegalStateException("Native playback UI contract changed: " + signature));
        if (owner.getMethods().stream().filter(m -> m.getName().equals(expected.getName()) &&
            m.getParameterTypes().size() == expected.getParameterTypes().size()).count() != 1 ||
            (((expected.getAccessFlags() & 8) != 0) != (owner.getType().equals("Lab/uh;"))))
            throw new IllegalStateException("Ambiguous or incompatible native playback UI contract: " + signature);
    }
    static String reference(Instruction instruction) {
        return instruction instanceof ReferenceInstruction ? ((ReferenceInstruction) instruction).getReference().toString() : "";
    }
    static MutableMethod invoke(MutableClass owner, int count) {
        MutableMethod method = NuvioRemainingEpisodesPatch.unique(owner, "invoke", count);
        if (!method.getReturnType().equals("Ljava/lang/Object;") || (method.getAccessFlags() & 8) != 0 ||
            method.getParameterTypes().stream().anyMatch(p -> !p.equals("Ljava/lang/Object;")) || method.getImplementation() == null)
            throw new IllegalStateException("Native playback lambda signature changed");
        return method;
    }
    static void validateRuntime(MutableClass buttons, MutableClass animation, MutableClass scope) {
        for (String signature : List.of(BUTTON, REPORT, ANIMATION)) {
            MutableClass owner = signature.startsWith("Lv/") ? animation : buttons;
            if (owner.getMethods().stream().filter(m -> m.toString().equals(signature) && (m.getAccessFlags() & 8) != 0).count() != 1)
                throw new IllegalStateException("Native playback method changed: " + signature);
        }
        if (!scope.getType().equals("Lv/c0;") || (scope.getAccessFlags() & 0x200) == 0 || !scope.getMethods().isEmpty())
            throw new IllegalStateException("Playback animation scope must be the native empty interface");
    }
    static void validate(MutableClass main, MutableClass more) {
        if (!main.getType().equals("Lva/bd;") || !more.getType().equals("Lpa/a;")) throw new IllegalStateException("Wrong playback owners");
        List<Instruction> a = NuvioAiringSeriesPatch.instructions(invoke(main, 2));
        List<Instruction> b = NuvioAiringSeriesPatch.instructions(invoke(more, 3));
        requireCalls(a, BUTTON, 9, 14); requireCalls(a, ANIMATION, 1, 8);
        requireCalls(a, MORE_CONSTRUCTOR, 1, 13);
        requireCalls(b, BUTTON, 4, 14); requireCalls(b, REPORT, 1, 9);
        if (a.stream().anyMatch(i -> reference(i).startsWith(EXTENSION)) || b.stream().anyMatch(i -> reference(i).startsWith(EXTENSION)))
            throw new IllegalStateException("Playback buttons already patched");
        // Fixed real-input register contracts; no speculative register allocation or parameter movement.
        if (invoke(main, 2).getImplementation().getRegisterCount() != 49 ||
            invoke(more, 3).getImplementation().getRegisterCount() != 78)
            throw new IllegalStateException("Native playback register layout changed");
        // Exact latest-version bodies prove availability branches, register sources and group boundaries.
        if (!fingerprint(withoutIntroControl(invoke(main, 2))).equals("2c382f8f1152d222b534cab5f4e8fc70c38cbf80515be32533ed434002e75f00") ||
            !fingerprint(invoke(more, 3)).equals("d50337b058b7289a6cbd19b54fdc8b4d88a88620f4e328fc41ff72a062aae44b"))
            throw new IllegalStateException("Native playback instruction layout changed");
    }
    static String fingerprint(MutableMethod method) {
        StringBuilder text = new StringBuilder();
        text.append(method.getImplementation().getRegisterCount());
        for (Instruction i : method.getImplementation().getInstructions()) {
            text.append('|').append(i.getOpcode()).append(':').append(reference(i));
            if (i instanceof RegisterRangeInstruction) text.append('r').append(((RegisterRangeInstruction)i).getStartRegister()).append(',').append(((RegisterRangeInstruction)i).getRegisterCount());
            if (i instanceof OneRegisterInstruction) text.append('a').append(((OneRegisterInstruction)i).getRegisterA());
            if (i instanceof TwoRegisterInstruction) text.append('b').append(((TwoRegisterInstruction)i).getRegisterB());
            if (i instanceof ThreeRegisterInstruction) text.append('c').append(((ThreeRegisterInstruction)i).getRegisterC());
            if (i instanceof FiveRegisterInstruction) {
                FiveRegisterInstruction f = (FiveRegisterInstruction)i;
                text.append('f').append(f.getRegisterCount()).append(',').append(f.getRegisterC()).append(',').append(f.getRegisterD())
                    .append(',').append(f.getRegisterE()).append(',').append(f.getRegisterF()).append(',').append(f.getRegisterG());
            }
            if (i instanceof WideLiteralInstruction) text.append('l').append(((WideLiteralInstruction)i).getWideLiteral());
            if (i instanceof OffsetInstruction) text.append('o').append(((OffsetInstruction)i).getCodeOffset());
            if (i instanceof SwitchPayload) for (SwitchElement e : ((SwitchPayload)i).getSwitchElements()) text.append('s').append(e.getKey()).append(',').append(e.getOffset());
        }
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : digest) result.append(String.format("%02x", b & 255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static MutableMethod withoutIntroControl(MutableMethod source) {
        MutableMethod copy = new MutableMethod(source);
        var code = NuvioAiringSeriesPatch.instructions(copy);
        int count = 0;
        for (int i = code.size() - 1; i >= 0; i--) if (reference(code.get(i)).equals(NuvioIntroDbPatch.RENDER)) {
            if (++count > 1 || !(code.get(i) instanceof RegisterRangeInstruction) ||
                ((RegisterRangeInstruction)code.get(i)).getStartRegister() != 46 || ((RegisterRangeInstruction)code.get(i)).getRegisterCount() != 2 ||
                i == 0 || !reference(code.get(i - 1)).equals(BUTTON)) throw new IllegalStateException("Unexpected IntroDB control hook");
            for (int later = i + 1; later < code.size(); later++) if (reference(code.get(later)).equals(BUTTON))
                throw new IllegalStateException("IntroDB control must follow the final native button");
            copy.getImplementation().removeInstruction(i);
        }
        return copy;
    }
    private static void requireCalls(List<Instruction> instructions, String signature, int count, int registers) {
        List<Instruction> matches = new ArrayList<>();
        for (Instruction i : instructions) if (reference(i).equals(signature)) matches.add(i);
        if (matches.size() != count || matches.stream().anyMatch(i -> !(i instanceof RegisterRangeInstruction) ||
            ((RegisterRangeInstruction)i).getRegisterCount() != registers))
            throw new IllegalStateException("Missing/ambiguous playback contract: " + signature);
    }
    static void hook(MutableClass main, MutableClass more) {
        validate(main, more);
        rewrite(invoke(main, 2), new String[]{"play", "next", "subtitles", "audio", "sources", "engine", "episodes", "party", "finish"});
        rewrite(invoke(more, 3), new String[]{"speed", "aspect", "external", "info"});
    }
    private static void rewrite(MutableMethod method, String[] names) {
        List<Instruction> instructions = NuvioAiringSeriesPatch.instructions(method);
        int cursor = names.length;
        for (int index = instructions.size() - 1; index >= 0; index--) {
            Instruction instruction = instructions.get(index);
            String signature = reference(instruction), name = null;
            if (signature.equals(MORE_CONSTRUCTOR)) {
                int instance = ((RegisterRangeInstruction) instruction).getStartRegister();
                method.getImplementation().addInstruction(index + 1, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, instance, 1,
                    new ImmutableMethodReference(EXTENSION, "registerMore", List.of("Ljava/lang/Object;"), "V")));
            } else if (signature.equals(BUTTON)) name = names[--cursor];
            else if (signature.equals(ANIMATION)) name = "more";
            else if (signature.equals(REPORT)) name = "report";
            if (name == null) continue;
            MethodReference nativeMethod = (MethodReference) ((ReferenceInstruction) instruction).getReference();
            List<String> parameters = new ArrayList<>();
            for (CharSequence p : nativeMethod.getParameterTypes()) {
                String type = p.toString();
                parameters.add(type.startsWith("L") ? "Ljava/lang/Object;" : type);
            }
            RegisterRangeInstruction range = (RegisterRangeInstruction) instruction;
            method.getImplementation().replaceInstruction(index, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
                range.getStartRegister(), range.getRegisterCount(), new ImmutableMethodReference(EXTENSION, name, parameters, "V")));
        }
        if (cursor != 0) throw new IllegalStateException("Unconsumed playback buttons");
    }
}
