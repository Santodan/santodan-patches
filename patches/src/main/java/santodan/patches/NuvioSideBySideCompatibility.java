package santodan.patches;

import software.santodan.nuvio.NuvioNative;

import app.morphe.patcher.patch.ApkFileType;
import app.morphe.patcher.patch.Compatibility;

/** Java bridge for Morphe compatibility metadata whose constructor is Kotlin-internal. */
public final class NuvioSideBySideCompatibility {
    private NuvioSideBySideCompatibility() {}

    public static Compatibility create() {
        return new Compatibility(
            NuvioNative.PACKAGE_NAME,
            "NuvioTV",
            null,
            ApkFileType.APK,
            null,
            null,
            NuvioLayout.targets(),
            app.morphe.patcher.patch.FormFactor.TV, false
        );
    }
}
