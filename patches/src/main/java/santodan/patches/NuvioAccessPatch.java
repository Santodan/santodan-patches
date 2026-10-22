package santodan.patches;

import software.santodan.nuvio.NuvioNative;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference;
import java.util.*;
import java.util.function.Function;

/** Installs stable, direct native accessors once for every independent Nuvio feature. */
final class NuvioAccessPatch {
    static final String TYPE = "Lsoftware/santodan/nuvio/NuvioAccess;";
    static final Map<String, String> FIELDS = Map.of(
        "currentVideo", NuvioNative.ACCESS_CURRENT_VIDEO,
        "currentSeason", NuvioNative.ACCESS_CURRENT_SEASON,
        "currentEpisode", NuvioNative.ACCESS_CURRENT_EPISODE,
        "nextEpisode", NuvioNative.ACCESS_NEXT_EPISODE,
        "playbackError", NuvioNative.ACCESS_PLAYBACK_ERROR,
        "profileProvider", NuvioNative.ACCESS_PROFILE_PROVIDER,
        "streamProvider", NuvioNative.ACCESS_STREAM_PROVIDER,
        "trackingProvider", NuvioNative.ACCESS_TRACKING_PROVIDER);

    private NuvioAccessPatch() {}

    static void install(MutableClass bridge, Function<String, MutableClass> classes) {
        Map<MutableMethod, ImmutableFieldReference> targets = new LinkedHashMap<>();
        // Validate the entire bridge before mutating any method.
        FIELDS.forEach((name, signature) -> {
            int arrow = signature.indexOf("->"), colon = signature.indexOf(':', arrow);
            String owner = signature.substring(0, arrow), fieldName = signature.substring(arrow + 2, colon), type = signature.substring(colon + 1);
            if (!type.startsWith("L") && !type.startsWith("[")) throw new IllegalStateException("Native object accessor cannot read a primitive: " + signature);
            MutableClass nativeClass = classes.apply(owner);
            if ((nativeClass.getAccessFlags() & 1) == 0 || nativeClass.getFields().stream().filter(f ->
                f.getName().equals(fieldName) && f.getType().equals(type) && (f.getAccessFlags() & 9) == 1).count() != 1)
                throw new IllegalStateException("Native accessor requires one public instance field: " + signature);
            MutableMethod method = NuvioRemainingEpisodesPatch.unique(bridge, name, 1);
            if (!method.getParameterTypes().equals(List.of("Ljava/lang/Object;")) || !method.getReturnType().equals("Ljava/lang/Object;") ||
                (method.getAccessFlags() & 9) != 9 || !method.getImplementation().getTryBlocks().isEmpty() ||
                method.getImplementation().getRegisterCount() < 2 || method.getImplementation().getRegisterCount() > 16 ||
                NuvioRemainingEpisodesPatch.instructions(method).stream().noneMatch(i -> NuvioRemainingEpisodesPatch.calls(i, TYPE, "read")))
                throw new IllegalStateException("Native accessor stub changed or already installed: " + name);
            targets.put(method, new ImmutableFieldReference(owner, fieldName, type));
        });
        targets.forEach((method, field) -> {
            var code = method.getImplementation();
            int receiver = NuvioAnchors.parameter(method, 0);
            while (!code.getInstructions().isEmpty()) code.removeInstruction(code.getInstructions().size() - 1);
            code.addInstruction(new BuilderInstruction21c(Opcode.CHECK_CAST, receiver, new ImmutableTypeReference(field.getDefiningClass())));
            code.addInstruction(new BuilderInstruction22c(Opcode.IGET_OBJECT, 0, receiver, field));
            code.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
            var present = code.newLabelForIndex(0);
            code.addInstruction(0, new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
            code.addInstruction(0, new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
            code.addInstruction(0, new BuilderInstruction21t(Opcode.IF_NEZ, receiver, present));
        });
    }
}
