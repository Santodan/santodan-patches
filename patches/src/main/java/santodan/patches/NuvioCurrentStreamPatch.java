package santodan.patches;

import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import java.util.*;
import kotlin.Unit;

/** Adds a visible label using the native session-selected source flag. */
public final class NuvioCurrentStreamPatch {
    static final String LABEL = "[Playing] ";
    static final String EXTENSION = "Lsoftware/santodan/extension/nuviocurrentstream/NuvioCurrentStream;";
    static final String LABEL_METHOD = EXTENSION + "->label(Ljava/lang/String;Z)Ljava/lang/String;";
    static final String CARD = "Lva/z;->q(Lcom/nuvio/tv/domain/model/Stream;Lc2/a0;ZZZZLl9/y;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Lh1/o;II)V";

    public static BytecodePatch getNuvioCurrentStreamPatch() {
        return PatchKt.bytecodePatch("NuvioTV - Indicate current stream",
            "Labels the current stream with [Playing], with an enabled-by-default toggle in Santodan-Patches > Streams.", false, builder -> {
                builder.compatibleWith(new Compatibility("com.nuvio.tv", "NuvioTV", null, ApkFileType.APK,
                    null, null, List.of(new AppTarget("1.1.0-beta.7", false, null)), false));
                builder.dependsOn(NuvioSettingsMenuPatch.getMenuPatch());
                builder.extendWith(() -> NuvioStreamPreloadPatch.extension("nuvio-current-stream"));
                builder.execute(context -> {
                    if (!"com.nuvio.tv".equals(context.getPackageMetadata().getPackageName()) ||
                        !"1.1.0-beta.7".equals(context.getPackageMetadata().getVersionName()))
                        throw new IllegalStateException("Current stream indicator requires NuvioTV 1.1.0-beta.7");
                    MutableClass card = context.mutableClassDefBy("Lva/z;");
                    NuvioPlaybackButtonsPatch.validateUiMethod(new MutableClass(context.classDefBy("Lab/uh;")),
                        "Lab/uh;->m(Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Lx1/q;Lkotlin/jvm/functions/Function0;ZLk2/e;JZLh1/o;II)V");
                    hook(card);
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
    }

    static MutableMethod validate(MutableClass owner) {
        MutableMethod method = NuvioRemainingEpisodesPatch.unique(owner, "q", 13);
        if (!method.toString().equals(CARD) || (method.getAccessFlags() & 8) == 0 ||
            method.getImplementation() == null || method.getImplementation().getRegisterCount() != 39 ||
            !method.getImplementation().getTryBlocks().isEmpty() ||
            !NuvioPlaybackButtonsPatch.fingerprint(method).equals("805e7f5bb31e9250a168414f273e7e29863c1ea0a9ebd7f9b3fb4eb4bfab8066"))
            throw new IllegalStateException("Current stream card changed, ambiguous or already patched");
        return method;
    }

    static void hook(MutableClass owner) {
        MutableMethod method = validate(owner);
        var code = method.getImplementation();
        // Original instruction 141 casts the remembered title in v7. At 142,
        // v8 is the effective session-selected flag.
        // Decorate after remember(), so changing sources never caches the label.
        code.addInstruction(142, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 7, 8, 0, 0, 0,
            new ImmutableMethodReference(EXTENSION, "label", List.of("Ljava/lang/String;", "Z"), "Ljava/lang/String;")));
        code.addInstruction(143, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 7));
    }
}
