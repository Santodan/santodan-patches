# NuvioTV GitHub APK updates

Select **NuvioTV - GitHub APK updates** when patching the original
`com.nuvio.tv` **1.1.0-beta.8** (`1071`). The patch is optional and unselected by
default. It redirects the existing native updater to `Santodan/santodan-patches`.
It retains native update settings, stable/beta channels, release notes, update
prompts, download progress, cached downloads, errors, and the Android installation
handoff. Android may ask for permission to install unknown apps and confirmation
to install the update; this does not enable silent installation.

## Configure each APK build

Patch options:

- `repository`: public GitHub repository, default `Santodan/santodan-patches`.
- `releaseVersion`: the release version of **the APK being built**, default
  `0.12.0`. Set this explicitly for every published APK. It is independent of the
  underlying NuvioTV version and the patch bundle version.

For example, patch the first APK with `releaseVersion=0.12.0`, then publish it
under tag `v0.12.0`. Patch the next APK with `releaseVersion=0.12.1` and publish it
under `v0.12.1`. The first APK will detect the second; after installation the
second will stop offering itself. Changing only a filename or replacing the
asset on the same tag does not create a new version.

Use three-part semantic versions, optionally with a prerelease suffix such as
`0.13.0-beta.1`. The patch option excludes the leading `v`; release tags may have
it. Do not use `+build` metadata to distinguish updates: native semantic version
comparison ignores build metadata. If you forget to update `releaseVersion`
inside the new APK, the app can repeatedly offer the release already installed.

## Publish the APK on GitHub

1. Build an APK with the desired patches, including this updater patch, and set
   `releaseVersion` to the version you intend to publish.
2. Sign it with the **same signing key** as the already installed patched app.
   Keep the **same Android package name**, including any side-by-side package
   option. Its Android version code must not be lower than the installed one.
   Preserve the signing key used by your patching/signing tool across releases.
3. Open [the repository's Releases page](https://github.com/Santodan/santodan-patches/releases)
   and create a release, or edit the matching release if it already exists.
4. Choose the matching tag, for example `v0.12.1`, and enter release notes.
5. Drag the **signed APK** into the release's binary attachments area. Suggested
   name: `NuvioTV-Santodan-0.12.1-universal.apk`.
6. Publish the release. For regular releases leave **pre-release** unchecked and
   set it as the latest release. Draft releases are invisible to the updater.
7. In the installed patched app, use NuvioTV's native update check and download /
   install action. Enable installation from this app if Android requests it.

The APK must be a **Release asset**, not merely committed to a repository folder,
put in source ZIPs, or mentioned as a link in release notes. No JSON file or
workflow is required. The `.mpp` can remain attached to the same release; native
asset selection filters for `.apk`, so it will not download the patch bundle.

Use the same release created by the existing release workflow: edit that release
and attach the APK alongside its generated `.mpp`. The workflow still publishes
only the patch bundle; it does not build, sign or upload an APK automatically.
Keep the generated `.mpp` filename and release tag unchanged when attaching the
APK. Morphe's configured bundle URL continues to point to
`patches-<version>.mpp`; adding an APK asset does not change that URL. Keep the
release's stable/prerelease status consistent with the channel you publish to.

Attach one universal APK for this installation identity to each release. Avoid
mixing stock/clone variants or other applications' APKs in the same release:
native selection matches architecture, not package name. Multiple architecture
builds can use native ABI names (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`) in
their filenames; prefer the single universal APK workflow.

The native stable channel requests GitHub's latest non-prerelease release. If
your latest release contains only an `.mpp`, stable users will not find an APK
update there. Attach the APK to every release you want to distribute to NuvioTV.
The beta channel lists releases, excludes drafts, and sorts eligible semantic
versions, selecting one with an APK. For prerelease APKs, use a tag such as
`v0.13.0-beta.1`, mark the release as a pre-release and select the beta channel
in NuvioTV. The repository must be publicly readable; no token is embedded.

GitHub instructions: [Managing releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository).
Android requirements: [How app updates work](https://developer.android.com/google/play/app-updates).

## Implementation and evidence

Original: `../OriginalApps/NuvioTV-1.1.0-beta8.apk`, SHA-256
`7aeae6f64e9128be2c7545c50061376839e20a88d47722ac220ba40be7c182f8`.
Original manifest confirms `com.nuvio.tv`, `1.1.0-beta.8`, `1071`.
Inspection: `../.inspect-nuvio-beta8/updater-contracts.txt`, derived from original
DEX using dexlib2. No runtime extension is needed.

`Lkb/j0;->a(Lkb/l;Lkotlin/coroutines/jvm/internal/ContinuationImpl;)Ljava/lang/Object;`
is the instance release repository coroutine. Its unique `NuvioMedia` and
`NuvioTV` constants feed both GitHub release API routes. They are changed in
place. Contributor queries and unrelated GitHub URLs remain unchanged.

Six native local-version constants also change in place: `Lkb/q0;->a` and `b`
(static UI/post-install reconciliation and channel label),
`Lkb/m0;->invokeSuspend` (instance update-check coroutine), `Lkb/k;->a`
(static default channel), and `Lkb/q;->a` and `d` (instance cached-download
cleanup/restoration). The updater's `Lkb/r0;` semantic parser/comparator,
`Lkb/a;->a(List):GitHubAssetDto` asset selection, `Lkb/b;` downloader and
installation handoff are retained. The visible general app version remains
the original NuvioTV version; updater comparisons use the patch option.

All seven modified methods are uniquely selected and checked against original
instruction/register/literal/branch fingerprints before any mutation. Eight
`const-string` instructions change without changing their registers or width;
native branches, try regions and coroutine suspension logic are preserved.

Run:

```powershell
.\build-local.ps1 :patches:buildAndroid :patches:generatePatchesList
.\build-local.ps1 :patches:verifyNuvioApkUpdates :patches:verifyNuvioBeta8Bundle
```

The focused verifier checks missing, changed, ambiguous and already-patched
rejection, validation before mutation, option validation, and DEX write/reload
with only the eight intended constants changed. Packaged verification covers
standalone APK updates and coexistence with the other NuvioTV behavior patches.
Device checks remain: publish two correctly versioned/signed APKs, confirm
native manual/automatic check, stable/beta selection, progress/error UI,
unknown-source permission, install confirmation, preserved app data and no
repeat offer after updating. No APK is uploaded or installed by these tests.

## Local validation results

`buildAndroid`, `generatePatchesList`, `verifyNuvioApkUpdates` and
`verifyNuvioBeta8Bundle` passed. The packaged standalone updater and all fourteen
behavior patches together introduced no new cross-DEX findings (36 unchanged
optional-library findings in the original). The final stricter signature checks
also passed the focused verifier and CLI patch application.

The CLI rebuilt all fifteen public patches, including side-by-side installation,
with no failed patches. The output is `com.nuvio.santodan`, `1.1.0-beta.8` /
`1071`, with updater release version `0.12.0`. The focused verifier confirmed
the exact updater methods in the rebuilt APK. ZIP alignment passed; signature
verification reports a missing signature manifest, as expected for `--unsigned`.

- Bundle: `patches/build/libs/patches-0.12.0.mpp`.
- Unsigned validation APK: `../Apps/NuvioTV/NuvioTV-1.1.0-beta.8-apk-updates-unsigned.apk`.
- Logs: `../.inspect-nuvio-beta8/apk-updates-verification.log`,
  `apk-updates-final.log`, `apk-updates-rebuilt-verification.log` and
  `apk-updates-rebuild.log`.

This APK has not been signed, uploaded, installed or tested on a device. Sign
with your existing update key before publishing; the unsigned validation output
is not ready to distribute.
