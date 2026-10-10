package santodan.patches;

import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.io.*;
import java.util.*;
import kotlin.Unit;

/** Common runtime and Hilt registration, installed once for the stream-preloading patches. */
final class NuvioStreamPreloadPatch {
    static final String EXTENSION = "Lsoftware/santodan/extension/nuviostreams/NuvioStreamPreload;";
    private static BytecodePatch shared;
    private NuvioStreamPreloadPatch() {}

    private static synchronized BytecodePatch shared() {
        if (shared == null) shared = PatchKt.bytecodePatch(null, null, false, builder -> {
            builder.dependsOn(NuvioSettingsMenuPatch.getMenuPatch());
            builder.extendWith(() -> extension("nuvio-stream-preload"));
            builder.execute(context -> {
                validateTarget(context.getPackageMetadata().getPackageName(), context.getPackageMetadata().getVersionName());
                hookComponent(context.mutableClassDefBy("Lp8/e;"));
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
        return shared;
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    static BytecodePatch create(String name, String description, boolean details) {
        return create(name, description, details ? "details" : "continue_watching");
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    static BytecodePatch create(String name, String description, String mode) {
        return PatchKt.bytecodePatch(name, description, false, builder -> {
            builder.compatibleWith(new Compatibility("com.nuvio.tv", "NuvioTV", null, ApkFileType.APK,
                null, null, NuvioLayout.modernTargets(), false));
            builder.dependsOn(shared());
            builder.extendWith(() -> extension("next_episode".equals(mode) ? "nuvio-next-episode-streams"
                : "details".equals(mode) ? "nuvio-detail-streams" : "nuvio-cw-streams"));
            builder.execute(context -> {
                validateTarget(context.getPackageMetadata().getPackageName(), context.getPackageMetadata().getVersionName());
                if ("next_episode".equals(mode)) {
                    hookPlayer(context.mutableClassDefBy(NuvioLayout.current("Lna/le;")));
                    hookNextEpisodePluginPause(context.mutableClassDefBy(NuvioLayout.current("Lv9/d4;")));
                    hookNextEpisodePicker(context.mutableClassDefBy(NuvioLayout.current("Lna/cb;")));
                }
                else if ("details".equals(mode)) hookDetails(context.mutableClassDefBy(NuvioLayout.current("Lka/l9;")));
                else hookContinueWatching(context.mutableClassDefBy(NuvioLayout.current("Lba/e2;")));
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
    }

    static void validateTarget(String name, String version) {
        if (!"com.nuvio.tv".equals(name) || (!NuvioLayout.BETA4.equals(version) && !NuvioLayout.BETA5.equals(version)))
            throw NuvioRemainingEpisodesPatch.unsupported("Stream preloading requires com.nuvio.tv beta.4 or beta.5");
        NuvioLayout.use(version);
    }

    static void hookComponent(MutableClass owner) {
        hookReturn(NuvioRemainingEpisodesPatch.unique(owner, "<init>", 1), "registerComponent");
    }

    static void hookContinueWatching(MutableClass owner) {
        MutableMethod invoke = NuvioRemainingEpisodesPatch.unique(owner, "invoke", 3);
        insert(invoke, 0, "onContinueWatching");
    }

    static void hookDetails(MutableClass owner) {
        hookReturn(NuvioRemainingEpisodesPatch.unique(owner, "<init>", 28), "observeDetails");
        insert(NuvioRemainingEpisodesPatch.unique(owner, "onCleared", 0), 0, "stopDetails");
    }

    static void hookPlayer(MutableClass owner) {
        hookReturn(NuvioRemainingEpisodesPatch.unique(owner, "<init>", 50), "observeNextEpisode");
        insert(NuvioRemainingEpisodesPatch.unique(owner, "onCleared", 0), 0, "stopNextEpisode");
    }

    /** Substitute only this search's pause flow; never unpause the singleton repository. */
    static void hookNextEpisodePluginPause(MutableClass owner) {
        MutableMethod target = NuvioRemainingEpisodesPatch.unique(owner, "invokeSuspend", 1);
        List<Instruction> instructions = NuvioRemainingEpisodesPatch.instructions(target);
        String pause = NuvioLayout.current("Lv9/i4;") + "->k:Lkotlinx/coroutines/flow/MutableStateFlow;";
        int index = -1;
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (NuvioRemainingEpisodesPatch.calls(instruction, EXTENSION, "nextEpisodePluginPause"))
                throw NuvioRemainingEpisodesPatch.unsupported("Next-episode plugin pause hook already installed");
            if (instruction.getOpcode() == Opcode.IGET_OBJECT && instruction instanceof ReferenceInstruction
                && ((ReferenceInstruction) instruction).getReference().toString().equals(pause)) {
                if (index >= 0) throw NuvioRemainingEpisodesPatch.unsupported("Ambiguous plugin pause read");
                index = i;
            }
        }
        if (index < 0) throw NuvioRemainingEpisodesPatch.unsupported("Missing plugin pause read");
        int flow = ((OneRegisterInstruction) instructions.get(index)).getRegisterA();
        target.getImplementation().addInstruction(index + 1,
            new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
                NuvioRemainingEpisodesPatch.parameterStart(target), 1,
                new ImmutableMethodReference(EXTENSION, "nextEpisodePluginPause", List.of("Ljava/lang/Object;"),
                    "Lkotlinx/coroutines/flow/Flow;")));
        target.getImplementation().addInstruction(index + 2,
            new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, flow));
    }

    /** The manual next-episode route otherwise discards the preloaded session with forceRefresh=true. */
    static void hookNextEpisodePicker(MutableClass owner) {
        MutableMethod target = NuvioRemainingEpisodesPatch.unique(owner, "invokeSuspend", 1);
        List<Instruction> instructions = NuvioRemainingEpisodesPatch.instructions(target);
        String picker = NuvioLayout.current("Lna/jb;") + "->C(" + NuvioLayout.current("Lna/h8;")
            + "Lcom/nuvio/tv/domain/model/Video;Z)V";
        List<Integer> calls = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (NuvioRemainingEpisodesPatch.calls(instruction, EXTENSION, "nextEpisodePickerRefresh"))
                throw NuvioRemainingEpisodesPatch.unsupported("Next-episode picker hook already installed");
            if (instruction instanceof ReferenceInstruction &&
                ((ReferenceInstruction) instruction).getReference().toString().equals(picker)) {
                if (instruction.getOpcode() != Opcode.INVOKE_STATIC || !(instruction instanceof FiveRegisterInstruction)
                    || ((FiveRegisterInstruction) instruction).getRegisterCount() != 3)
                    throw NuvioRemainingEpisodesPatch.unsupported("Next-episode picker invocation changed");
                calls.add(i);
            }
        }
        if (calls.size() != 3) throw NuvioRemainingEpisodesPatch.unsupported("Next-episode picker routes changed");
        for (int i = calls.size() - 1; i >= 0; i--) {
            int index = calls.get(i);
            int force = ((FiveRegisterInstruction) instructions.get(index)).getRegisterE();
            target.getImplementation().addInstruction(index,
                new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, force, 1,
                    new ImmutableMethodReference(EXTENSION, "nextEpisodePickerRefresh", List.of("Z"), "Z")));
            target.getImplementation().addInstruction(index + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT, force));
        }
    }

    private static void hookReturn(MutableMethod target, String callback) {
        List<Instruction> instructions = NuvioRemainingEpisodesPatch.instructions(target);
        int index = -1;
        for (int i = 0; i < instructions.size(); i++) if (instructions.get(i).getOpcode() == Opcode.RETURN_VOID) {
            if (index >= 0) throw NuvioRemainingEpisodesPatch.unsupported("Ambiguous constructor return");
            index = i;
        }
        if (index < 0) throw NuvioRemainingEpisodesPatch.unsupported("Missing constructor return");
        insert(target, index, callback);
    }

    private static void insert(MutableMethod target, int index, String callback) {
        for (Instruction instruction : NuvioRemainingEpisodesPatch.instructions(target))
            if (NuvioRemainingEpisodesPatch.calls(instruction, EXTENSION, callback))
                throw NuvioRemainingEpisodesPatch.unsupported("Stream hook already installed: " + callback);
        target.getImplementation().addInstruction(index, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
            NuvioRemainingEpisodesPatch.parameterStart(target), 1,
            new ImmutableMethodReference(EXTENSION, callback, List.of("Ljava/lang/Object;"), "V")));
    }

    private static InputStream extension(String module) {
        InputStream input = NuvioStreamPreloadPatch.class.getClassLoader().getResourceAsStream("extensions/" + module + ".mpe");
        if (input == null) throw new IllegalStateException("Missing bundled stream-preloading extension: " + module);
        return input;
    }
}
