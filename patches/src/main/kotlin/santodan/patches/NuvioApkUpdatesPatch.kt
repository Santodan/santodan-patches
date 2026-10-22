package santodan.patches

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import software.santodan.nuvio.NuvioNative

@Suppress("unused")
val nuvioApkUpdatesPatch = bytecodePatch(
    name = "NuvioTV - GitHub APK updates",
    description = "Checks Santodan GitHub releases for APK updates using NuvioTV's native download and installation flow. Set the installed release version when patching each APK.",
    default = false,
) {
    compatibleWith(NuvioSideBySideCompatibility.create())
    val repository = stringOption(
        key = "repository", default = "Santodan/santodan-patches",
        title = "GitHub repository", description = "Public repository in owner/name format containing APK release assets.", required = true,
    ) { it != null && it.matches(Regex("[A-Za-z0-9-]+/[A-Za-z0-9_.-]+")) }
    val releaseVersion = stringOption(
        key = "releaseVersion", default = "0.12.0",
        title = "Installed APK release version",
        description = "Version of THIS patched APK, matching its GitHub release tag without v (for example 0.12.0). Increase for every APK release, including patch-only updates.",
        required = true,
    ) { it != null && NuvioApkUpdates.validVersion(it) }
    execute {
        check(packageMetadata.packageName == NuvioNative.PACKAGE_NAME && packageMetadata.versionName == NuvioNative.VERSION)
        val repo = requireNotNull(repository.value).split('/')
        NuvioApkUpdates.apply(NuvioApkUpdates.TYPES.associateWith { mutableClassDefBy(it) }, repo[0], repo[1], requireNotNull(releaseVersion.value))
    }
}
