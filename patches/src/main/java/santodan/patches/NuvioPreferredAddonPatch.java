package santodan.patches;

import app.morphe.patcher.patch.*;
import app.morphe.patcher.util.proxy.mutableTypes.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import java.util.*;
import kotlin.Unit;

public final class NuvioPreferredAddonPatch {
    static final String EXTENSION = "Lsoftware/santodan/extension/nuviopreferred/NuvioPreferredAddon;";
    static final String STREAM = "Lcom/nuvio/tv/domain/model/Stream;";
    static final Map<String, String> HASHES = Map.of(
        "snapshot", "7f0eb705b24abdaf09c7a6255a6d5e08e7162ff1dfed1cdd9b8e981ea68b9a66",
        "order", "83b43b9f154447b90d266406a3ae736d448502b588f977896a1fa0c9a8ce2984",
        "select", "330e31fcb5ca72b812565b38f9c6b75de022cb081a2588a5f632fc20a4f52268",
        "gate", "f79963aa3ced943b7abeea5245e0ea02d39076f2d11034a136e58301e4ea0e1e",
        "regex", "7e7b351ccacf8ab5e9a52b16c07285d0301df7c421520604ed479f958109118a",
        "apply", "e2652661b9d251299dcdf2c40d42c04055d8611e977839a50af90533055a6601",
        "load", "ac141e9228292e7f4aed3757b64f4a8f9aec34229c38984bf5371cda547cb308");
    static final Map<String, String> SIGNATURES = Map.of(
        "snapshot", "Lba/m4;-><init>(ILjava/util/List;ZLjava/util/List;ZLjava/util/List;Lcom/nuvio/tv/domain/model/DebridSettings;)V",
        "order", "Ld9/w1;->d(Ljava/util/List;Ljava/util/List;Ljava/util/Set;)Ljava/util/List;",
        "select", "Ld9/w1;->e(Ljava/util/List;Lcom/nuvio/tv/data/local/r7;Ljava/lang/String;Lcom/nuvio/tv/data/local/s7;Ljava/util/Set;Ljava/util/Set;Ljava/util/Set;Ljava/lang/String;ZZ)" + STREAM,
        "gate", "Ld9/w1;->a(Lcom/nuvio/tv/data/local/x4;)Z",
        "regex", "Ld9/w1;->c(Ljava/lang/String;)Z",
        "apply", "Lbb/u1;->a(Ljava/util/List;Ljava/util/Set;Lbb/d2;Lkotlin/jvm/internal/Ref$BooleanRef;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/nuvio/tv/data/local/x4;Ljava/util/List;Z)V",
        "load", "Lbb/u1;->invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;");

    public static BytecodePatch getNuvioPreferredAddonPatch() {
        return PatchKt.bytecodePatch("NuvioTV - Preferred source add-on",
            "Choose a per-profile add-on to refresh and display first, with optional automatic playback of its first playable stream.", false, builder -> {
                builder.compatibleWith(new Compatibility("com.nuvio.tv", "NuvioTV", null, ApkFileType.APK,
                    null, null, List.of(new AppTarget("1.1.0-beta.7", false, null)), false));
                builder.dependsOn(NuvioSettingsMenuPatch.getMenuPatch());
                builder.extendWith(() -> NuvioStreamPreloadPatch.extension("nuvio-preferred-addon"));
                builder.execute(context -> {
                    if (!"com.nuvio.tv".equals(context.getPackageMetadata().getPackageName()) ||
                        !"1.1.0-beta.7".equals(context.getPackageMetadata().getVersionName()))
                        throw new IllegalStateException("Preferred add-on requires NuvioTV 1.1.0-beta.7");
                    validateContracts(type -> new MutableClass(context.classDefBy(type)));
                    MutableClass snapshot = context.mutableClassDefBy("Lba/m4;");
                    MutableClass selector = context.mutableClassDefBy("Ld9/w1;");
                    validate(snapshot, selector);
                    MutableClass loader = context.mutableClassDefBy("Lbb/u1;");
                    validateLoader(loader, selector);
                    hookComponent(context.mutableClassDefBy("Lq8/e;"));
                    hook(snapshot, selector);
                    hookLoader(loader, selector);
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
    }
    interface Classes { MutableClass get(String type); }
    static void field(Classes classes, String owner, String name, String type) {
        if (classes.get(owner).getFields().stream().filter(f -> f.getName().equals(name) && f.getType().equals(type)).count() != 1)
            throw new IllegalStateException("Preferred add-on field changed: " + owner + "->" + name);
    }
    static void contract(Classes classes, String owner, String name, List<String> parameters, String result, boolean isStatic) {
        var method = NuvioRemainingEpisodesPatch.unique(classes.get(owner), name, parameters.size());
        if (!method.getParameterTypes().equals(parameters) || !method.getReturnType().equals(result) || ((method.getAccessFlags() & 8) != 0) != isStatic)
            throw new IllegalStateException("Preferred add-on contract changed: " + owner + "->" + name);
    }
    static void validateContracts(Classes classes) {
        field(classes, "Lq8/e;", "B", "Lvb/c;");
        field(classes, "Lq8/e;", "Y2", "Lvb/c;");
        field(classes, "Lg9/e;", "f", "Lkotlinx/coroutines/flow/StateFlow;");
        field(classes, "Lba/d5;", "c", "Lfa/a;");
        field(classes, "Lba/p;", "r", "Lkotlinx/coroutines/flow/StateFlow;");
        field(classes, "Lbb/d2;", "t", "Z");
        field(classes, "Lkotlin/jvm/internal/Ref$BooleanRef;", "element", "Z");
        for (String getter : List.of("getBaseUrl", "getDisplayName"))
            contract(classes, "Lcom/nuvio/tv/domain/model/Addon;", getter, List.of(), "Ljava/lang/String;", false);
        contract(classes, "Lcom/nuvio/tv/domain/model/AddonKt;", "enabledAddons", List.of("Ljava/util/List;"), "Ljava/util/List;", true);
        contract(classes, "Ld9/w1;", "b", List.of(STREAM), "Z", true);
        for (String owner : List.of(STREAM, "Lcom/nuvio/tv/domain/model/AddonStreams;"))
            contract(classes, owner, "getAddonName", List.of(), "Ljava/lang/String;", false);
        contract(classes, "Lab/fi;", "<init>", List.of("Ljava/lang/Object;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Lm3/n;", "I"), "V", false);
        NuvioPlaybackButtonsPatch.validateUiMethod(classes.get("Lab/uh;"),
            "Lab/uh;->m(Ljava/lang/String;Ljava/lang/String;ZLkotlin/jvm/functions/Function0;Lx1/q;Lkotlin/jvm/functions/Function0;ZLk2/e;JZLh1/o;II)V");
        NuvioPlaybackButtonsPatch.validateUiMethod(classes.get("Lab/uh;"),
            "Lab/uh;->j(Ljava/lang/String;Ljava/util/List;Ljava/lang/Object;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Ljava/lang/String;FFLh1/o;II)V");
        NuvioPlaybackButtonsPatch.validateUiMethod(classes.get("Lab/uh;"),
            "Lab/uh;->a(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function0;Lx1/q;Lkotlin/jvm/functions/Function0;ZLk2/e;Lk2/e;JLk2/e;Ljava/lang/Integer;FJLh1/o;III)V");
    }
    static Map<String, MutableMethod> methods(MutableClass snapshot, MutableClass selector) {
        return Map.of("snapshot", NuvioRemainingEpisodesPatch.unique(snapshot, "<init>", 7),
            "order", NuvioRemainingEpisodesPatch.unique(selector, "d", 3),
            "select", NuvioRemainingEpisodesPatch.unique(selector, "e", 10),
            "gate", NuvioRemainingEpisodesPatch.unique(selector, "a", 1));
    }
    static void validate(MutableClass snapshot, MutableClass selector) {
        if (!snapshot.getType().equals("Lba/m4;") || !selector.getType().equals("Ld9/w1;")) throw new IllegalStateException("Wrong add-on classes");
        methods(snapshot, selector).forEach(NuvioPreferredAddonPatch::validateMethod);
    }
    static String tryLayout(MutableMethod method) {
        StringBuilder result = new StringBuilder();
        for (var block : method.getImplementation().getTryBlocks()) {
            result.append(block.getStartCodeAddress()).append(':').append(block.getCodeUnitCount()).append('[');
            for (var handler : block.getExceptionHandlers()) result.append(handler.getExceptionType()).append(':').append(handler.getHandlerCodeAddress()).append(',');
            result.append(']');
        }
        return result.toString();
    }
    static void validateMethod(String key, MutableMethod method) {
        String tries = key.equals("select") ? "358:13[null:372,]" : key.equals("regex") ? "31:13[null:45,]" : "";
        boolean isStatic = !key.equals("snapshot") && !key.equals("load");
        if (!method.toString().equals(SIGNATURES.get(key)) || ((method.getAccessFlags() & 8) != 0) != isStatic ||
            method.getImplementation() == null || !tryLayout(method).equals(tries) ||
            !NuvioPlaybackButtonsPatch.fingerprint(method).equals(HASHES.get(key)))
            throw new IllegalStateException("Preferred add-on " + key + " changed or already patched");
    }
    static ImmutableMethodReference ref(String name, List<String> args, String result) {
        return new ImmutableMethodReference(EXTENSION, name, args, result);
    }
    static void hookComponent(MutableClass owner) {
        MutableMethod method = NuvioRemainingEpisodesPatch.unique(owner, "<init>", 1);
        var body = NuvioRemainingEpisodesPatch.instructions(method);
        List<Integer> returns = new ArrayList<>();
        for (int i = 0; i < body.size(); i++) {
            if (NuvioRemainingEpisodesPatch.calls(body.get(i), EXTENSION, "registerComponent")) throw new IllegalStateException("Preferred add-on already registered");
            if (body.get(i).getOpcode() == Opcode.RETURN_VOID) returns.add(i);
        }
        if (returns.size() != 1 || !method.toString().equals("Lq8/e;-><init>(Lm3/a;)V") || (method.getAccessFlags() & 8) != 0)
            throw new IllegalStateException("Preferred add-on Hilt constructor changed");
        method.getImplementation().addInstruction(returns.get(0), new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
            NuvioRemainingEpisodesPatch.parameterStart(method), 1, ref("registerComponent", List.of("Ljava/lang/Object;"), "V")));
    }
    static void hook(MutableClass snapshot, MutableClass selector) {
        validate(snapshot, selector);
        var methods = methods(snapshot, selector);
        var init = methods.get("snapshot").getImplementation();
        // Constructor v2 = profile ID, v3 = enabled add-ons. No new registers.
        init.addInstruction(0, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, 2, 2,
            ref("prioritize", List.of("I", "Ljava/util/List;"), "Ljava/util/List;")));
        init.addInstruction(1, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 3));
        var order = methods.get("order").getImplementation();
        var originalOrder = NuvioRemainingEpisodesPatch.instructions(methods.get("order"));
        for (int i = originalOrder.size() - 1; i >= 0; i--) if (originalOrder.get(i).getOpcode() == Opcode.RETURN_OBJECT) {
            int register = ((OneRegisterInstruction) originalOrder.get(i)).getRegisterA();
            order.replaceInstruction(i, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, register, 1,
                ref("orderGroups", List.of("Ljava/util/List;"), "Ljava/util/List;")));
            order.addInstruction(i + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, register));
            order.addInstruction(i + 2, new BuilderInstruction11x(Opcode.RETURN_OBJECT, register));
        }
        var select = methods.get("select").getImplementation();
        var original = select.newLabelForIndex(0);
        select.addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, ref("autoPlayEnabled", List.of(), "Z")));
        select.addInstruction(1, new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
        select.addInstruction(2, new BuilderInstruction21t(Opcode.IF_EQZ, 0, original));
        select.addInstruction(3, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, 14, 1,
            ref("select", List.of("Ljava/util/List;"), "Ljava/lang/Object;")));
        select.addInstruction(4, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        select.addInstruction(5, new BuilderInstruction21c(Opcode.CHECK_CAST, 0,
            new com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference(STREAM)));
        select.addInstruction(6, new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
        wrapBooleanReturns(methods.get("gate"), "autoPlayGate");
    }
    static void wrapBooleanReturns(MutableMethod gateMethod, String callback) {
        var gate = gateMethod.getImplementation();
        var gateBody = NuvioRemainingEpisodesPatch.instructions(gateMethod);
        for (int i = gateBody.size() - 1; i >= 0; i--) if (gateBody.get(i).getOpcode() == Opcode.RETURN) {
            int register = ((OneRegisterInstruction) gateBody.get(i)).getRegisterA();
            gate.replaceInstruction(i, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, register, 1, ref(callback, List.of("Z"), "Z")));
            gate.addInstruction(i + 1, new BuilderInstruction11x(Opcode.MOVE_RESULT, register));
            gate.addInstruction(i + 2, new BuilderInstruction11x(Opcode.RETURN, register));
        }
    }
    static Map<String, MutableMethod> loaderMethods(MutableClass loader, MutableClass selector) {
        return Map.of("apply", NuvioRemainingEpisodesPatch.unique(loader, "a", 10),
            "load", NuvioRemainingEpisodesPatch.unique(loader, "invokeSuspend", 1),
            "regex", NuvioRemainingEpisodesPatch.unique(selector, "c", 1));
    }
    static void validateLoader(MutableClass loader, MutableClass selector) {
        if (!loader.getType().equals("Lbb/u1;") || !selector.getType().equals("Ld9/w1;")) throw new IllegalStateException("Wrong preferred loader classes");
        loaderMethods(loader, selector).forEach(NuvioPreferredAddonPatch::validateMethod);
    }
    static void hookLoader(MutableClass loader, MutableClass selector) {
        validateLoader(loader, selector);
        var methods = loaderMethods(loader, selector);
        // All native result branches converge here. Preserve manual selection and
        // the native resolved-session flag, while overriding cached binge choices.
        var apply = methods.get("apply").getImplementation();
        var branch = (BuilderInstruction21t) apply.getInstructions().get(339);
        var target = branch.getTarget();
        apply.replaceInstruction(339, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 4, 9, 3, 1, 2, 0,
            ref("choose", List.of("Ljava/lang/Object;", "Ljava/util/List;", "Ljava/lang/Object;", "Ljava/lang/Object;"), "Ljava/lang/Object;")));
        apply.addInstruction(340, new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 9));
        apply.addInstruction(341, new BuilderInstruction21c(Opcode.CHECK_CAST, 9,
            new com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference(STREAM)));
        apply.addInstruction(342, new BuilderInstruction21t(Opcode.IF_EQZ, 9, target));
        var load = methods.get("load").getImplementation();
        // beta.7 instruction 735 reads streamReuseLastLinkEnabled into v1.
        load.addInstruction(736, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE, 1, 1,
            ref("allowNativeReuse", List.of("Z"), "Z")));
        load.addInstruction(737, new BuilderInstruction11x(Opcode.MOVE_RESULT, 1));
        wrapBooleanReturns(methods.get("regex"), "autoPlayGate");
    }
}
