package santodan.patches;

import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.util.*;
import kotlin.Unit;

public final class NuvioIntroDbPatch {
    static final String EXT = "Lsoftware/santodan/extension/nuviointro/NuvioIntroDb;";
    static final String RENDER = EXT + "->renderControl(Ljava/lang/Object;Ljava/lang/Object;)V";
    static final String POSITION = "Lva/z9;->e(Lva/k8;)Ljava/lang/Long;";
    public static BytecodePatch getNuvioIntroDbPatch() {
        return PatchKt.bytecodePatch("NuvioTV - Submit segments to IntroDB",
            "Adds an optional playback button that captures segment start/end times and opens a native NuvioTV submission dialog. Enable it and enter your IntroDB API key in Settings.", false, builder -> {
            builder.compatibleWith(new Compatibility("com.nuvio.tv", "NuvioTV", null, ApkFileType.APK,
                null, null, List.of(new AppTarget("1.1.0-beta.7", false, null)), false));
            builder.dependsOn(NuvioSettingsMenuPatch.getMenuPatch());
            builder.extendWith(() -> NuvioStreamPreloadPatch.extension("nuvio-introdb"));
            builder.execute(context -> {
                if (!"com.nuvio.tv".equals(context.getPackageMetadata().getPackageName()) ||
                    !"1.1.0-beta.7".equals(context.getPackageMetadata().getVersionName()))
                    throw new IllegalStateException("IntroDB requires NuvioTV 1.1.0-beta.7");
                validateState(new MutableClass(context.classDefBy("Lva/re;")));
                for (String signature : UI) require(new MutableClass(context.classDefBy(signature.substring(0, signature.indexOf("->")))), signature);
                fields(new MutableClass(context.classDefBy("Lva/bd;")), "c:Lva/ue;", "e:Lva/re;", "o:Lc2/a0;", "p:Lkotlin/jvm/functions/Function0;", "q:Lkotlin/jvm/functions/Function0;");
                fields(new MutableClass(context.classDefBy("Lva/ue;")), "Y:Lva/k8;");
                fields(new MutableClass(context.classDefBy("Lva/k8;")), "H0:Lkotlinx/coroutines/flow/StateFlow;", "T0:Lkotlinx/coroutines/Job;");
                fields(new MutableClass(context.classDefBy("Lva/g3;")), "d:Z");
                fields(new MutableClass(context.classDefBy("Lza/d;")), "a:Ljava/lang/String;", "b:Ljava/lang/String;");
                fields(new MutableClass(context.classDefBy("Ldb/e;")), "b:J", "c:J", "l:J", "x:J");
                fields(new MutableClass(context.classDefBy("Ldb/m0;")), "b:F", "c:F");
                fields(new MutableClass(context.classDefBy("Lio/sentry/d2;")), "b:Ljava/util/ArrayList;");
                staticFields(new MutableClass(context.classDefBy("Ldb/l0;")), "a:Ldb/m0;");
                staticFields(new MutableClass(context.classDefBy("Ly2/m1;")), "q:Lh1/e3;");
                staticFields(new MutableClass(context.classDefBy("Lab/q1;")), "a:Lr1/s;", "b:Lr1/s;");
                staticFields(new MutableClass(context.classDefBy("Le2/z;")), "b:J");
                MutableMethod position = require(new MutableClass(context.classDefBy("Lva/z9;")), POSITION);
                for (String reference : List.of("Lva/z9;->i(Lva/k8;)Z", "Lva/b2;->l()J", "Landroidx/media3/common/Player;->getCurrentPosition()J"))
                    if (NuvioAiringSeriesPatch.instructions(position).stream().noneMatch(i -> NuvioPlaybackButtonsPatch.reference(i).equals(reference)))
                        throw new IllegalStateException("Native playback position semantics changed");
                MutableMethod timer = require(new MutableClass(context.classDefBy("Lva/sa;")), "Lva/sa;->F(Lva/k8;)V");
                if (NuvioAiringSeriesPatch.instructions(timer).stream().filter(i -> NuvioPlaybackButtonsPatch.reference(i).equals("Lva/k8;->T0:Lkotlinx/coroutines/Job;")).count() != 2)
                    throw new IllegalStateException("Native controls timer job changed");
                hook(context.mutableClassDefBy("Lva/bd;"), context.mutableClassDefBy("Lcom/nuvio/tv/MainActivity;"));
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
    }
    static final List<String> UI = List.of(
        NuvioPlaybackButtonsPatch.BUTTON, POSITION,
        "Lc2/a0;-><init>()V",
        "Lh1/h;->s(Ljava/lang/Object;)Lh1/t1;", "Lh1/l0;->d0(I)V", "Lh1/l0;->p(Z)V",
        "Lab/n0;-><init>(Lc2/a0;Ly2/n2;Lh1/k1;Lh1/k1;I)V",
        "Lab/d;-><init>(ILh1/k1;)V", "Lab/m0;-><init>(Lc2/a0;I)V",
        "Lab/p0;-><init>(Lh1/d3;I)V",
        "Landroidx/compose/foundation/layout/b;->f(Lx1/q;F)Lx1/q;",
        "Lc2/d;->i(Lx1/q;Lkotlin/jvm/functions/Function1;)Lx1/q;",
        "La/a;->y(Lh1/o;)Ldb/e;", "La/a;->A(Lh1/o;)Ldb/m;",
        "Ly/n;->a(JF)Ly/v;", "Ldb/m;->a(FF)Ly/v;",
        "Ll0/g;->b(F)Ll0/f;", "Ly5/c;-><init>(Ly/v;Le2/w0;I)V",
        "Ly5/m;->b(JJLh1/o;II)Ly5/l;", "Ly5/m;->a(Ly5/c;Ly5/c;Lh1/o;I)Ly5/k;",
        "Ly5/m;->e(Le2/w0;I)Ly5/q;", "Ly5/m;->d(FI)Ly5/p;",
        "Ly5/g1;->c(Lkotlin/jvm/functions/Function0;Lx1/q;Lkotlin/jvm/functions/Function0;Ly5/q;Ly5/l;Ly5/p;Ly5/k;Ly5/n;Lc0/k;Lkotlin/jvm/functions/Function3;Lh1/o;II)V",
        "Ly5/g1;->a(Lkotlin/jvm/functions/Function0;Lx1/q;ZLy5/h;Ly5/i;Ly5/e;Ly5/d;Le0/o1;Lkotlin/jvm/functions/Function3;Lh1/o;III)V",
        "Ly5/f;->b(JJJJJJLh1/o;I)Ly5/e;",
        "Lk2/d;-><init>(Ljava/lang/String;FFFFJIZI)V", "Lk2/d;->a(Lk2/d;Ljava/util/ArrayList;ILe2/y0;)V", "Lk2/d;->b()Lk2/e;",
        "Le2/y0;-><init>(J)V", "Lio/sentry/d2;-><init>(I)V", "Lio/sentry/d2;->o(FF)V", "Lio/sentry/d2;->m(FF)V", "Lio/sentry/d2;->e()V",
        "Lr1/s;-><init>(ILjava/lang/Object;Z)V",
        "Lha/m2;->z(Lkotlin/jvm/functions/Function0;Ljava/lang/String;Ljava/lang/String;FIZZLe2/v;Le2/z;FFFFLkotlin/jvm/functions/Function3;Lr1/s;Lh1/o;III)V",
        "Lab/uh;->d(Ljava/lang/String;Lkotlin/jvm/functions/Function0;ZZLh1/o;II)V",
        "Lab/uh;->e(Lx1/c;Lr1/s;Lh1/o;II)V",
        "Lza/a;->b(Lx1/q;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/util/List;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;ZLh1/o;I)V",
        "Lab/uh;->m(Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Lx1/q;Lkotlin/jvm/functions/Function0;ZLk2/e;JZLh1/o;II)V",
        "Lab/uh;->a(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;Lx1/q;Lkotlin/jvm/functions/Function0;ZLk2/e;Lk2/e;JLk2/e;Ljava/lang/Integer;FJLh1/o;III)V",
        "Lab/kj;->b(Ljava/lang/String;Lx1/q;Lab/vh;Lh1/o;II)V",
        "Lva/sa;->F(Lva/k8;)V",
        "Lkotlinx/coroutines/Job$DefaultImpls;->cancel$default(Lkotlinx/coroutines/Job;Ljava/util/concurrent/CancellationException;ILjava/lang/Object;)V",
        "Lza/d;-><init>(Ljava/lang/String;Ljava/lang/String;)V");
    static MutableMethod require(MutableClass owner, String signature) {
        MutableMethod method = owner.getMethods().stream().filter(m -> m.toString().equals(signature)).reduce((a,b) -> {
            throw new IllegalStateException("Ambiguous IntroDB native API: " + signature);
        }).orElseThrow(() -> new IllegalStateException("Missing IntroDB native API: " + signature));
        boolean instance = method.getName().equals("<init>") || List.of("Lh1/l0;", "Ldb/m;", "Lio/sentry/d2;").contains(owner.getType()) ||
            signature.equals("Lk2/d;->b()Lk2/e;");
        if (((method.getAccessFlags() & 8) == 0) != instance) throw new IllegalStateException("Native API static status changed: " + signature);
        return method;
    }
    static void fields(MutableClass owner, String... expected) {
        for (String signature : expected) if (owner.getFields().stream().filter(f -> f.toString().equals(owner.getType() + "->" + signature) && (f.getAccessFlags() & 8) == 0).count() != 1)
            throw new IllegalStateException("IntroDB field changed: " + signature);
    }
    static void staticFields(MutableClass owner, String... expected) {
        for (String signature : expected) if (owner.getFields().stream().filter(f -> f.toString().equals(owner.getType() + "->" + signature) && (f.getAccessFlags() & 8) != 0).count() != 1)
            throw new IllegalStateException("IntroDB native UI constant changed: " + signature);
    }
    static void validateState(MutableClass owner) {
        fields(owner, "x0:Ljava/lang/Integer;", "y0:Ljava/lang/Integer;", "z0:Ljava/lang/String;", "i:Ljava/lang/String;");
        var body = NuvioAiringSeriesPatch.instructions(NuvioRemainingEpisodesPatch.unique(owner, "toString", 0));
        String[] labels = {", currentSeason=", ", currentEpisode=", ", currentVideoId="};
        String[] names = {"x0:Ljava/lang/Integer;", "y0:Ljava/lang/Integer;", "z0:Ljava/lang/String;"};
        for (int n = 0; n < labels.length; n++) {
            int hits = 0;
            for (int i = 0; i + 2 < body.size(); i++) if (NuvioPlaybackButtonsPatch.reference(body.get(i)).equals(labels[n]) &&
                NuvioPlaybackButtonsPatch.reference(body.get(i + 2)).equals("Lva/re;->" + names[n])) hits++;
            if (hits != 1) throw new IllegalStateException("IntroDB episode field semantics changed");
        }
    }
    static MutableMethod lifecycle(MutableClass activity, String name) {
        MutableMethod m = NuvioRemainingEpisodesPatch.unique(activity, name, 0);
        if (!m.getReturnType().equals("V") || (m.getAccessFlags() & 8) != 0 || m.getImplementation() == null)
            throw new IllegalStateException("Activity lifecycle changed");
        return m;
    }
    // Restore only known same-register bridges on a copy before checking the original layout.
    static MutableMethod originalMain(MutableMethod source) {
        MutableMethod copy = new MutableMethod(source);
        var code = NuvioAiringSeriesPatch.instructions(copy);
        for (int i = code.size() - 1; i >= 0; i--) {
            String ref = NuvioPlaybackButtonsPatch.reference(code.get(i));
            if (ref.equals(RENDER) || ref.equals(NuvioPlaybackButtonsPatch.EXTENSION + "->registerMore(Ljava/lang/Object;)V")) {
                if (code.get(i).getOpcode() != Opcode.INVOKE_STATIC_RANGE || !(code.get(i) instanceof RegisterRangeInstruction))
                    throw new IllegalStateException("Malformed optional player hook");
                RegisterRangeInstruction call = (RegisterRangeInstruction)code.get(i);
                if (ref.equals(RENDER) ? call.getStartRegister() != 46 || call.getRegisterCount() != 2 :
                    call.getRegisterCount() != 1 || i == 0 || !NuvioPlaybackButtonsPatch.reference(code.get(i - 1)).equals(NuvioPlaybackButtonsPatch.MORE_CONSTRUCTOR) ||
                    call.getStartRegister() != ((RegisterRangeInstruction)code.get(i - 1)).getStartRegister())
                    throw new IllegalStateException("Optional player hook registers changed");
                copy.getImplementation().removeInstruction(i); continue;
            }
            if (!ref.startsWith(NuvioPlaybackButtonsPatch.EXTENSION + "->")) continue;
            MethodReference old = (MethodReference)((ReferenceInstruction)code.get(i)).getReference();
            MethodReference nativeRef;
            if (old.getName().equals("more")) nativeRef = new ImmutableMethodReference("Lv/b0;", "d", List.of("Z","Lx1/q;","Lv/x0;","Lv/y0;","Ljava/lang/String;","Lr1/s;","Lh1/o;","I"), "V");
            else if (List.of("play","next","subtitles","audio","sources","engine","episodes","party","finish").contains(old.getName()))
                nativeRef = new ImmutableMethodReference("Lva/yd;", "b", List.of("Lk2/e;","Lj2/a;","Ljava/lang/String;","Lkotlin/jvm/functions/Function0;","Lc2/a0;","Lc2/a0;","Z","Lkotlin/jvm/functions/Function0;","Lkotlin/jvm/functions/Function0;","J","Lh1/o;","I","I"), "V");
            else throw new IllegalStateException("Unknown playback bridge");
            List<String> erased = new ArrayList<>();
            for (CharSequence type : nativeRef.getParameterTypes()) erased.add(type.toString().startsWith("L") ? "Ljava/lang/Object;" : type.toString());
            if (!old.getParameterTypes().equals(erased) || !old.getReturnType().equals("V") || code.get(i).getOpcode() != Opcode.INVOKE_STATIC_RANGE)
                throw new IllegalStateException("Playback bridge signature changed");
            RegisterRangeInstruction range = (RegisterRangeInstruction)code.get(i);
            copy.getImplementation().replaceInstruction(i, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, range.getStartRegister(), range.getRegisterCount(), nativeRef));
        }
        return copy;
    }
    static void validate(MutableClass main, MutableClass activity) {
        MutableMethod m = NuvioPlaybackButtonsPatch.invoke(main, 2);
        if (!main.getType().equals("Lva/bd;") || NuvioAiringSeriesPatch.instructions(m).stream().anyMatch(i -> NuvioPlaybackButtonsPatch.reference(i).startsWith(EXT)) ||
            !NuvioPlaybackButtonsPatch.fingerprint(originalMain(m)).equals("2c382f8f1152d222b534cab5f4e8fc70c38cbf80515be32533ed434002e75f00"))
            throw new IllegalStateException("IntroDB player control layout changed or already patched");
        for (String name : List.of("onResume", "onPause")) {
            var code = NuvioAiringSeriesPatch.instructions(lifecycle(activity, name));
            if (code.stream().anyMatch(i -> NuvioPlaybackButtonsPatch.reference(i).startsWith(EXT)) || code.stream().filter(i -> i.getOpcode() == Opcode.RETURN_VOID).count() != 1)
                throw new IllegalStateException("IntroDB lifecycle changed or already patched");
        }
    }
    static void hook(MutableClass main, MutableClass activity) {
        validate(main, activity);
        MutableMethod method = NuvioPlaybackButtonsPatch.invoke(main, 2);
        var code = NuvioAiringSeriesPatch.instructions(method);
        for (int i = code.size() - 1; i >= 0; i--) {
            String ref = NuvioPlaybackButtonsPatch.reference(code.get(i));
            if (ref.equals(NuvioPlaybackButtonsPatch.BUTTON) || ref.startsWith(NuvioPlaybackButtonsPatch.EXTENSION + "->finish(")) {
                // Customization must collect the optional control before flushing the native row.
                int insertion = ref.startsWith(NuvioPlaybackButtonsPatch.EXTENSION) ? i : i + 1;
                method.getImplementation().addInstruction(insertion, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, 46, 2,
                    new ImmutableMethodReference(EXT, "renderControl", List.of("Ljava/lang/Object;", "Ljava/lang/Object;"), "V")));
                break;
            }
        }
        for (String name : List.of("onResume", "onPause")) {
            MutableMethod m = lifecycle(activity, name);
            int index = name.equals("onPause") ? 0 : m.getImplementation().getInstructions().size() - 1;
            m.getImplementation().addInstruction(index, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, m.getImplementation().getRegisterCount() - 1, 1,
                new ImmutableMethodReference(EXT, name.equals("onResume") ? "resume" : "pause", List.of("Landroid/app/Activity;"), "V")));
        }
    }
}
