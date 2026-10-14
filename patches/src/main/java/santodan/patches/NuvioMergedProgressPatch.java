package santodan.patches;

import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.*;
import kotlin.Unit;

/** Adds an opt-in merged tracking-progress snapshot to NuvioTV. */
public final class NuvioMergedProgressPatch {
    private static final String NAME = "NuvioTV - Merge tracking progress";
    private static final String EXT = "Lsoftware/santodan/extension/nuviomerged/NuvioMergedProgress;";
    private NuvioMergedProgressPatch() {}

    @SuppressWarnings({"unchecked", "deprecation"})
    public static BytecodePatch getNuvioMergedProgressPatch() {
        return PatchKt.bytecodePatch(NAME,
            "Merges Nuvio Sync and connected tracking-provider progress, preserving the previous snapshot while providers refresh.",
            false, builder -> {
                builder.compatibleWith(new Compatibility("com.nuvio.tv", "NuvioTV", null, ApkFileType.APK,
                    null, null, NuvioLayout.targets(), false));
                builder.dependsOn(NuvioSettingsMenuPatch.getMenuPatch());
                builder.extendWith(NuvioMergedProgressPatch::extensionStream);
                builder.execute(context -> {
                    String version = context.getPackageMetadata().getVersionName();
                NuvioLayout.use(version);
                    NuvioLayout.modern(version);
                    MutableClass repository = context.mutableClassDefBy("Lz9/zc;");
                    hookRepository(repository);
                    {
                        NuvioRemainingEpisodesPatch.hookCard(context.mutableClassDefBy("Lfa/v1;"),
                            "Lc7/a;", EXT, "prepareProviderBadge", "renderProviderBadge");
                        hookPlaybackPause(context.mutableClassDefBy("Lz9/k4;"));
                        hookInlinedCutoff(context.mutableClassDefBy("Lqa/g5;"));
                        hookInlinedCutoff(context.mutableClassDefBy("Lqa/y1;"));
                        hookBadgeCacheHit(context.mutableClassDefBy("Ln9/k0;"));
                        hookBadgeGroupProgress(context.mutableClassDefBy("Lqa/s5;"));
                    }
                    hookInlinedNextUpSeedPolicy(context.mutableClassDefBy("Lqa/s5;"));
                    hookMergedProvider(context.mutableClassDefBy("Lz9/sb;"));
                    hookEffectiveSource(context.mutableClassDefBy("Lio/sentry/o;"));
                    hookWatchProgressEnum(context.mutableClassDefBy("Lcom/nuvio/tv/data/local/dc;"));

                    {
                        hookSettingsStore(context.mutableClassDefBy("Lo9/a1;"));
                        hookSettingsComponent(context.mutableClassDefBy("Lp8/e;"));
                    }
                    hookWatchProgressSelection(context.mutableClassDefBy("Lxa/o7;"));
                    hookWatchProgressSummary(context.mutableClassDefBy("Lxa/bh;"), "z");
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
    }

    /** Observe the native signal used to suspend source searches during playback. */
    static void hookPlaybackPause(MutableClass owner) {
        List<MutableMethod> matches = new ArrayList<>();
        for (MutableMethod candidate : owner.getMethods()) {
            if (!candidate.getParameterTypes().equals(List.of("Z")) || !candidate.getReturnType().equals("V")
                || candidate.getImplementation() == null) continue;
            for (Instruction instruction : instructions(candidate)) {
                if (instruction instanceof ReferenceInstruction && ((ReferenceInstruction) instruction).getReference()
                    .toString().equals(owner.getType() + ("->l:") + "Lkotlinx/coroutines/flow/MutableStateFlow;")) {
                    matches.add(candidate);
                    break;
                }
            }
        }
        if (matches.size() != 1) throw unsupported("Native playback pause setter changed");
        MutableMethod target = matches.get(0);
        target.getImplementation().addInstruction(0, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
            target.getImplementation().getRegisterCount() - 1, 1,
            method(EXT, "setPlaybackPaused", List.of("Z"), "V")));
    }

    static void hookSettingsStore(MutableClass owner) {
        MutableMethod constructor = unique(owner, "<init>", 10);
        List<Instruction> instructions = instructions(constructor);
        int returns = 0;
        for (int i = instructions.size() - 1; i >= 0; i--) {
            if (instructions.get(i).getOpcode() != Opcode.RETURN_VOID) continue;
            constructor.getImplementation().addInstruction(i,
                new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
                    constructor.getImplementation().getRegisterCount() - 11, 1,
                    method(EXT, "registerSettingsStore", List.of("Ljava/lang/Object;"), "V")));
            returns++;
        }
        if (returns != 1) throw unsupported("Watch progress settings store constructor changed");
    }

    /** A cancelled metadata batch must not make unchanged watched IDs skip retries. */
    static void hookBadgeCacheHit(MutableClass owner) {
        MutableMethod target = unique(owner, "invokeSuspend", 1);
        List<Instruction> ins = instructions(target);
        int anchor = -1;
        for (int i = 0; i + 2 < ins.size(); i++) {
            if (ins.get(i).getOpcode() != Opcode.IGET_OBJECT || !(ins.get(i) instanceof ReferenceInstruction) || !((ReferenceInstruction) ins.get(i)).getReference()
                .toString().equals("Lqa/a4;->X0:Ljava/util/Set;")) continue;
            if (anchor >= 0 || !calls(ins.get(i + 1), "Lkotlin/jvm/internal/Intrinsics;", "areEqual")
                || ins.get(i + 2).getOpcode() != Opcode.MOVE_RESULT)
                throw unsupported("Badge ID cache comparison changed");
            anchor = i + 3;
        }
        if (anchor < 0) throw unsupported("Badge ID cache comparison missing");
        int result = ((OneRegisterInstruction) ins.get(anchor - 1)).getRegisterA();
        target.getImplementation().addInstruction(anchor, new BuilderInstruction3rc(
            Opcode.INVOKE_STATIC_RANGE, result, 1, method(EXT, "allowBadgeCacheHit", List.of("Z"), "Z")));
        target.getImplementation().addInstruction(anchor + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT, result));
        int home = ((TwoRegisterInstruction) ins.get(anchor - 3)).getRegisterB();
        target.getImplementation().addInstruction(anchor - 3, new BuilderInstruction3rc(
            Opcode.INVOKE_STATIC_RANGE, home, 1,
            method(EXT, "prepareBadgeValidation", List.of("Ljava/lang/Object;"), "V")));
    }

    /** Publish each completed metadata group before the entire bulk batch finishes. */
    static void hookBadgeGroupProgress(MutableClass owner) {
        MutableMethod target = unique(owner, "i", 3);
        List<Instruction> ins = instructions(target);
        int home = -1;
        int anchor = -1;
        for (int i = 0; i < ins.size(); i++) {
            Instruction instruction = ins.get(i);
            if (instruction instanceof ReferenceInstruction && ((ReferenceInstruction) instruction).getReference()
                .toString().equals("Lqa/a4;->V0:Ljava/util/Map;")) {
                int receiver = ((TwoRegisterInstruction) instruction).getRegisterB();
                if (home >= 0 && home != receiver) throw unsupported("Badge group Home register changed");
                home = receiver;
            }
            if (calls(instruction, "Ljava/util/Iterator;", "hasNext")) {
                if (anchor >= 0 || ins.get(i + 1).getOpcode() != Opcode.MOVE_RESULT)
                    throw unsupported("Badge group loop changed");
                anchor = i + 2;
            }
        }
        if (home < 0 || anchor < 0) throw unsupported("Badge group progress anchors missing");
        target.getImplementation().addInstruction(anchor, new BuilderInstruction3rc(
            Opcode.INVOKE_STATIC_RANGE, home, 1,
            method(EXT, "publishCachedWatchedBadges", List.of("Ljava/lang/Object;"), "V")));
    }

    /** The coordinator is lazy: Layout must be able to resolve its native provider. */
    static void hookSettingsComponent(MutableClass owner) {
        MutableMethod constructor = unique(owner, "<init>", 1);
        List<Instruction> ins = instructions(constructor);
        int returns = 0;
        for (int i = ins.size() - 1; i >= 0; i--) {
            if (ins.get(i).getOpcode() != Opcode.RETURN_VOID) continue;
            constructor.getImplementation().addInstruction(i, new BuilderInstruction3rc(
                Opcode.INVOKE_STATIC_RANGE, parameterStart(constructor), 1,
                method(EXT, "registerSettingsComponent", List.of("Ljava/lang/Object;"), "V")));
            returns++;
        }
        if (returns != 1) throw unsupported("Settings component constructor changed");
    }

    /** Beta4 inlines the selected provider's cutoff into two Home coroutines. */
    private static void hookInlinedCutoff(MutableClass owner) {
        MutableMethod target = unique(owner, "invokeSuspend", 1);
        List<Instruction> ins = instructions(target);
        int hook = -1;
        int register = -1;
        for (int i = 0; i + 1 < ins.size(); i++) {
            if (!calls(ins.get(i), "Lo9/z;", "x")) continue;
            if (hook >= 0 || ins.get(i + 1).getOpcode() != Opcode.MOVE_RESULT_OBJECT)
                throw unsupported("Inlined cutoff anchor changed: " + owner.getType());
            hook = i + 2;
            register = ((OneRegisterInstruction) ins.get(i + 1)).getRegisterA();
        }
        if (hook < 0) throw unsupported("Inlined cutoff anchor missing: " + owner.getType());
        target.getImplementation().addInstruction(hook, new BuilderInstruction3rc(
            Opcode.INVOKE_STATIC_RANGE, register, 1,
            method(EXT, "adjustContinueWatchingCutoff", List.of("Ljava/lang/Long;"), "Ljava/lang/Long;")));
        target.getImplementation().addInstruction(hook + 1,
            new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, register));
    }

    /** Replaces Nuvio's selected progress provider with our live aggregate provider. */
    private static void hookMergedProvider(MutableClass owner) {
        MutableMethod target = unique(owner, "invokeSuspend", 1);
        List<Instruction> ins = instructions(target);
        int hook = -1;
        int result = -1;
        for (int i = 0; i + 1 < ins.size(); i++) {
            if (!calls(ins.get(i), "Lo9/a0;", "a") || !(ins.get(i + 1) instanceof OneRegisterInstruction)) continue;
            if (hook >= 0) throw unsupported("Multiple provider-registry lookup anchors found");
            hook = i + 2;
            result = ((OneRegisterInstruction) ins.get(i + 1)).getRegisterA();
        }
        if (hook < 0) throw unsupported("Provider-registry lookup anchor not found");
        target.getImplementation().addInstruction(hook++, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1,
            result, 0, 0, 0, 0,
            method(EXT, "mergedProvider", List.of("Ljava/lang/Object;"), "Ljava/lang/Object;")));
        target.getImplementation().addInstruction(hook++, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, result));
        target.getImplementation().addInstruction(hook, new BuilderInstruction21c(Opcode.CHECK_CAST, result,
            new com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference("Lo9/z;")));
    }

    private static void hookRepository(MutableClass owner) {
        MutableMethod ctor = unique(owner, "<init>", 14);
        int instance = parameterStart(ctor);
        List<Instruction> ins = instructions(ctor);
        for (int i = 0; i < ins.size(); i++) if (ins.get(i).getOpcode() == Opcode.RETURN_VOID) {
            ctor.getImplementation().addInstruction(i, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, instance, 1,
                method(EXT, "registerRepository", List.of("Ljava/lang/Object;"), "V")));
            return;
        }
        throw unsupported("Watch progress repository constructor changed");
    }

    /** Prevents the Trakt carrier from imposing its policy on other providers' merged seeds. */


    /** beta.6 inlines shouldUseAsNextUpSeed into the Home pipeline helper. */
    private static void hookInlinedNextUpSeedPolicy(MutableClass owner) {
        MutableMethod target = unique(owner, "C", 2);
        if (!"Z".equals(target.getReturnType())
            || !"Lcom/nuvio/tv/domain/model/WatchProgress;".contentEquals(target.getParameterTypes().get(1)))
            throw unsupported("Inlined next-up seed policy signature changed");
        int progress = target.getImplementation().getRegisterCount() - 1;
        List<Instruction> ins = instructions(target);
        int returns = 0;
        for (int i = ins.size() - 1; i >= 0; i--) {
            Instruction instruction = ins.get(i);
            if (instruction.getOpcode() != Opcode.RETURN || !(instruction instanceof OneRegisterInstruction)) continue;
            int result = ((OneRegisterInstruction) instruction).getRegisterA();
            if (progress > 15 || result > 15) throw unsupported("Inlined next-up seed policy register layout changed");
            target.getImplementation().addInstruction(i, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2,
                progress, result, 0, 0, 0,
                method(EXT, "adjustNextUpSeedDecision", List.of("Ljava/lang/Object;", "Z"), "Z")));
            target.getImplementation().addInstruction(i + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT, result));
            returns++;
        }
        if (returns == 0) throw unsupported("Inlined next-up seed policy return anchors not found");
    }

    private static void hookEffectiveSource(MutableClass owner) {
        MutableMethod target = null;
        for (MutableMethod method : owner.getMethods()) {
            if (method.getParameterTypes().size() == 2 && "Lcom/nuvio/tv/data/local/dc;".equals(method.getReturnType())) {
                if (target != null) throw unsupported("Multiple effective source methods found");
                target = method;
            }
        }
        if (target == null) throw unsupported("Effective source method not found");
        int first = target.getImplementation().getRegisterCount() - 2;
        target.getImplementation().addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, first, first + 1, 0, 0, 0,
            method(EXT, "effectiveSource", List.of("Ljava/lang/Object;", "Ljava/lang/Object;"), "Ljava/lang/Object;")));
        target.getImplementation().addInstruction(1, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, first));
        target.getImplementation().addInstruction(2, new BuilderInstruction21c(Opcode.CHECK_CAST, first,
            new com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference("Lcom/nuvio/tv/data/local/dc;")));
    }

    private static void hookWatchProgressEnum(MutableClass owner) {
        MutableMethod clinit = unique(owner, "<clinit>", 0);
        List<Instruction> ins = instructions(clinit);
        int valuesStore = -1;
        for (int i = 0; i < ins.size(); i++) {
            if (ins.get(i).getOpcode() == Opcode.SPUT_OBJECT && ins.get(i) instanceof ReferenceInstruction) {
                Object reference = ((ReferenceInstruction) ins.get(i)).getReference();
                if (reference instanceof FieldReference && "n".equals(((FieldReference) reference).getName())) {
                    valuesStore = i;
                    break;
                }
            }
        }
        if (valuesStore < 0) throw unsupported("Watch progress enum values anchor not found");
        int at = valuesStore;
        at = addEnumValue(owner, clinit, at, "MERGED_HIGHEST");
        addEnumValue(owner, clinit, at, "MERGED_RECENT");
    }

    private static int addEnumValue(MutableClass owner, MutableMethod clinit, int at, String name) {
        String type = "Lcom/nuvio/tv/data/local/dc;";
        clinit.getImplementation().addInstruction(at++, new BuilderInstruction21c(Opcode.NEW_INSTANCE, 1,
            new com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference(type)));
        clinit.getImplementation().addInstruction(at++, new BuilderInstruction21c(Opcode.CONST_STRING, 2,
            new com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference(name)));
        // Deliberately share NUVIO_SYNC's ordinal. Nuvio's generated when-mapping arrays
        // therefore treat the synthetic choices as Nuvio Sync without needing resizing.
        clinit.getImplementation().addInstruction(at++, new BuilderInstruction11n(Opcode.CONST_4, 3, 3));
        clinit.getImplementation().addInstruction(at++, new BuilderInstruction35c(Opcode.INVOKE_DIRECT, 3, 1, 2, 3, 0, 0,
            method("Ljava/lang/Enum;", "<init>", List.of("Ljava/lang/String;", "I"), "V")));
        clinit.getImplementation().addInstruction(at++, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 0, 1, 0, 0, 0,
            method(EXT, "appendEnum", List.of("Ljava/lang/Object;", "Ljava/lang/Object;"), "Ljava/lang/Object;")));
        clinit.getImplementation().addInstruction(at++, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        clinit.getImplementation().addInstruction(at++, new BuilderInstruction21c(Opcode.CHECK_CAST, 0,
            new com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference("[" + type)));
        return at;
    }



    private static void hookWatchProgressSelection(MutableClass owner) {
        MutableMethod target = unique(owner, "invokeSuspend", 1);
        List<Instruction> ins = instructions(target);
        int hook = -1;
        int source = -1;
        for (int i = 0; i < ins.size(); i++) {
            if (!calls(ins.get(i), "Lo9/a1;", "f") || !(ins.get(i) instanceof FiveRegisterInstruction)) continue;
            FiveRegisterInstruction call = (FiveRegisterInstruction) ins.get(i);
            if (hook >= 0) throw unsupported("Multiple watch progress persistence anchors found");
            hook = i;
            source = call.getRegisterD();
        }
        if (hook < 0) throw unsupported("Watch progress persistence anchor not found");
        target.getImplementation().addInstruction(hook, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1,
            source, 0, 0, 0, 0,
            method(EXT, "selectSource", List.of("Ljava/lang/Object;"), "Ljava/lang/Object;")));
        target.getImplementation().addInstruction(hook + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, source));
        target.getImplementation().addInstruction(hook + 2, new BuilderInstruction21c(Opcode.CHECK_CAST, source,
            new com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference("Lcom/nuvio/tv/data/local/dc;")));
    }

    private static void hookWatchProgressSummary(MutableClass owner, String labelMethod) {
        MutableMethod target = unique(owner, "invoke", 3);
        List<Instruction> ins = instructions(target);
        int hook = -1;
        int source = -1;
        int label = -1;
        for (int i = 0; i + 1 < ins.size(); i++) {
            if (!calls(ins.get(i), "Lxa/za;", labelMethod) || !(ins.get(i) instanceof FiveRegisterInstruction)
                || !(ins.get(i + 1) instanceof OneRegisterInstruction)) continue;
            if (hook >= 0) throw unsupported("Multiple Watch Progress summary labels found");
            hook = i + 2;
            source = ((FiveRegisterInstruction) ins.get(i)).getRegisterC();
            label = ((OneRegisterInstruction) ins.get(i + 1)).getRegisterA();
        }
        if (hook < 0) throw unsupported("Watch Progress summary label not found");
        target.getImplementation().addInstruction(hook, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2,
            source, label, 0, 0, 0,
            method(EXT, "summaryLabel", List.of("Ljava/lang/Object;", "Ljava/lang/String;"), "Ljava/lang/String;")));
        target.getImplementation().addInstruction(hook + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, label));
    }

    private static MutableMethod unique(MutableClass owner, String name, int params) {
        MutableMethod result = null;
        for (MutableMethod method : owner.getMethods()) if (name.equals(method.getName()) && method.getParameterTypes().size() == params && method.getImplementation() != null) {
            if (result != null) throw unsupported("Multiple method matches"); result = method;
        }
        if (result == null) throw unsupported(owner.getType() + "->" + name + " not found"); return result;
    }
    private static int parameterStart(MutableMethod method) {
        int words = 1; for (CharSequence type : method.getParameterTypes()) words += type.charAt(0) == 'J' || type.charAt(0) == 'D' ? 2 : 1;
        return method.getImplementation().getRegisterCount() - words;
    }
    private static List<Instruction> instructions(MutableMethod method) { List<Instruction> out = new ArrayList<>(); for (Instruction i : method.getImplementation().getInstructions()) out.add(i); return out; }
    private static boolean calls(Instruction i, String owner, String name) { if (!(i instanceof ReferenceInstruction)) return false; Object r=((ReferenceInstruction)i).getReference(); return r instanceof MethodReference && owner.equals(((MethodReference)r).getDefiningClass()) && name.equals(((MethodReference)r).getName()); }
    private static ImmutableMethodReference method(String owner,String name,List<String> params,String result){return new ImmutableMethodReference(owner,name,params,result);}
    private static IllegalStateException unsupported(String reason){return new IllegalStateException("Unsupported NuvioTV bytecode: "+reason+". Use an original NuvioTV 1.1.0-beta.6 APK.");}
    private static InputStream extensionStream() {
        String path="extensions/nuvio-merged-progress.mpe"; InputStream resource=NuvioMergedProgressPatch.class.getClassLoader().getResourceAsStream(path); if(resource!=null)return resource;
        try { URI source=NuvioMergedProgressPatch.class.getProtectionDomain().getCodeSource().getLocation().toURI(); try(ZipFile zip=new ZipFile(new File(source))){ZipEntry entry=zip.getEntry(path);if(entry==null)throw new FileNotFoundException(path);try(InputStream input=zip.getInputStream(entry)){return new ByteArrayInputStream(input.readAllBytes());}} } catch(Exception error){throw new IllegalStateException("Cannot load merged-progress extension",error);}
    }
}
