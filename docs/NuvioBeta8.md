# NuvioTV 1.1.0-beta.8

All fifteen public NuvioTV patches target `com.nuvio.tv` version `1.1.0-beta.8`,
version code `1071`. No older-version mappings or dispatch branches were added.
The historical beta.7 fixtures remain on disk.

The optional [GitHub APK updates patch](NuvioApkUpdates.md) uses the native
updater with `Santodan/santodan-patches` release assets. Set `releaseVersion`
for each APK build. Its focused original-APK verifier and standalone/combined
bundle checks passed; a final all-fifteen-patch unsigned APK was rebuilt and
verified. Earlier validation records below describe their original selections.

Native symbols now come from a shared generated contract, with structural
insertion anchors and direct runtime accessors. See [maintenance workflow](NuvioMaintenance.md)
for the read-only update report and the `verifyNuvioLatest` verification command.

Original: `../OriginalApps/NuvioTV-1.1.0-beta8.apk`.
SHA-256: `7aeae6f64e9128be2c7545c50061376839e20a88d47722ac220ba40be7c182f8`.
The package and version were read with Android SDK aapt. All six original DEX
files were extracted to `../.inspect-nuvio-beta8/`; normalized comparisons,
method candidates, instruction/register dumps and verification logs are there.

## Proven native contracts

| Contract | Beta.8 original DEX |
| --- | --- |
| Progress repository | `Lba/re;`, 14-parameter constructor, registry `k:Lq9/a0;`, profile `j:Lg9/f;` |
| Local progress writes | `Lba/re;->E` and `s`, `(int,WatchProgress,ContinuationImpl,boolean):Object` |
| Provider selection | `Lba/ad;->invokeSuspend`; hook only its final provider return, preserving the first watch-party return |
| Hilt providers | `Lq8/e;->B:Lac/c;` supplies profile, `e3` supplies `Lba/a5;`, `R3` supplies `Lq9/a1;`; proven through `Lq8/f;->get()` |
| Stream search | `Lba/a5;->l(String,String,Integer,Integer,boolean):Flow`; pause field `l:MutableStateFlow` |
| Detail resolver | static `Lwa/p0;->D(Meta,NextToWatch,List):Video` |
| Player/controller | `Lab/qm;` (53 constructor parameters), controller `Y:Lab/mc;`, request `Lab/mc;->T:String` |
| Player state | `Lab/nm;`: current season `A0:Integer`, episode `B0:Integer`, video `C0:String`, next `v1:Lab/s4;`, error `j1:String` |
| Next-episode routes | `Lab/fh;->E(Lab/mc;Video;Z)V`, three callers exclusively `Lab/xg;->invokeSuspend`; threshold `x(Lab/mc;)V` reads `r2:Z`, has 13 registers |
| Home/library | `Lxa/s3;`, helper `Lxa/h5;`, library `Lba/e1;`; item lambdas `Lab/nt;`, `Lma/x;`, `Lma/l0;` |
| Poster image | `Lg0/t;->b`, 15 parameters; Composer at invoke-range start + 11 |
| Date cards | library `Lma/u1;` (36 registers), collection `Lma/k0;` (63 registers) |
| Layout menu | `Lab/tr;->invoke` constructs `Lfb/z6;`; native row anchor `Lg0/j;->q` |
| Shared settings | rows `Lfb/nd;`, section `Lfb/ve;`, picker `Lfb/wd;`, style controls `Lfb/mc;` |
| Compose | state factory `Lh1/h;->t(Object)Lh1/r1;`, Composer `Lh1/k0;`, empty remembered-slot sentinel `Lh1/m;->a`; focus initializer `Lg/e;->g(Lh1/k0;)Lc2/z;` |
| Playback customization | main `Lab/cj;->invoke(Object,Object)` has 59 registers; More `Lab/ij;->invoke(Object,Object,Object)` has 58; More constructor adds remembered state, 13 parameters plus receiver |
| Native controls | `Lab/pl;->c` renders standard controls, `M` renders report; nine main controls and five More controls |
| Current stream | `Lab/bp;->h(Stream,FocusRequester,ZZZZ,...):void`, 39 registers; structural label hook after the remembered-title cast; title and session-flag registers derived from DEX |

Named PlayerUiState `toString()` labels prove the episode/video/error field shifts.
Runtime provider identities are checked through the original provider's returned
cast, rather than the presence of a field with a generic provider type.

Beta.8 introduces Chapters in the main row and moves player-engine selection to
More. The customization patch collects Chapters independently, retains native
availability/callbacks, and defaults engine selection to More. Existing saved
button preferences retain their selected IDs and order.

## Verification

Run from the repository with Java 21 and Android SDK platform 36:

```powershell
.\build-local.ps1 :patches:buildAndroid :patches:generatePatchesList
.\build-local.ps1 :patches:verifyNuvioBeta8 :patches:verifyNuvioBeta8Launcher :patches:verifyNuvioBeta8Player
.\build-local.ps1 :patches:verifyNuvioCurrentStream :patches:verifyNuvioPreferredAddon :patches:verifyNuvioPlaybackButtons :patches:verifyNuvioIntroDb
.\build-local.ps1 :patches:verifyNuvioBeta8DateStyleRuntime :patches:verifyNuvioBeta8SettingsMenuRuntime :patches:verifyNuvioBeta8TrackingDialogs
.\build-local.ps1 :patches:verifyNuvioCurrentStreamRuntime :patches:verifyNuvioPreferredAddonRuntime :patches:verifyNuvioPlaybackButtonsRuntime :patches:verifyNuvioIntroDbRuntime
.\build-local.ps1 :patches:verifyNuvioStreamPreloadRuntime :patches:verifyNuvioProviderRefreshRuntime :patches:verifyNuvioSettingsStoreRuntime
.\build-local.ps1 :patches:verifyNuvioBeta8Bundle
```

Run Gradle invocations sequentially: a concurrent resource rebuild can remove
extension resources while a packaged-bundle verifier is using them on Windows.
Verifiers check missing/changed/ambiguous/duplicate rejection, original input
mutation scope, both IntroDB/customization patch orders, and DEX write/reload.
Host runtime fixtures cover disabled behavior, settings callbacks/persistence,
remembered focus slots, errors, cancellation, profile isolation and playback delay.
Device checks remain for remote focus/navigation, playback, profile changes,
opt-in features, and Android TV channel publication. No device installation is
part of this update.

## Completed validation and artifacts

`buildAndroid` and `generatePatchesList` succeeded. The beta.8 DEX, launcher,
player, settings/date-style, tracking dialogs, current-stream, preferred-add-on,
playback customization, IntroDB, provider refresh and preload verifiers passed.
The lazy settings-store fixture was updated to the proven `R3` provider and
passed separately after the main verification run. The preload check includes
the production 30-second playback delay. Playback tests also exercise Chapters
in the main row and engine selection in More, with native availability retained.

`verifyNuvioBeta8Bundle` passed all six standalone selections and all thirteen
behavior patches together. Cross-DEX verification found the same 36 optional
library issues in the original and every patched selection, with no new finding.

The Morphe CLI rebuilt the original APK with all fourteen patches, including
side-by-side installation, with no failed patches. The resulting APK is
`com.nuvio.santodan`, `1.1.0-beta.8` / `1071`. `verifyNuvioPlaybackApk` passed
with all 17 native playback hooks, resolved extension signatures and unchanged
cross-DEX findings. Alignment passed. All 108 native libraries are byte-identical
to the original. Owned permissions, provider authorities and launcher labels
were checked, including `com.nuvio.santodan.santodan.launcher.artwork`.
`apksigner verify` confirms the expected unsigned output (missing signature
manifest); the CLI was explicitly run with `--unsigned`.

- Bundle: `patches/build/libs/patches-0.12.0.mpp`.
- Unsigned APK: `../Apps/NuvioTV/NuvioTV-1.1.0-beta.8-menu-fix-unsigned.apk`.
- CLI report: `../.inspect-nuvio-beta8/rebuild-result.json`.
- Verification logs: `../.inspect-nuvio-beta8/bundle-final.log`,
  `final-verification.log`, and `settings-store-final.log`.

The APK was rebuilt, but was not signed, installed, or tested on a device.

```powershell
.\build-local.ps1 :patches:verifyNuvioPlaybackApk '-PnuvioPlaybackApk=E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-menu-fix-unsigned.apk'
```

## Device log: missing menu

`logs_patch.log` exposed an obsolete `h1.h.s(Object)` reflection call in the
shared menu. Beta.8 defines `t(Object):h1.r1`; `s(long):h1.p1` is incompatible.
All affected menu and badge state creators now use the shared
`STATE_FACTORY_METHOD` contract, including current-stream, preferred-add-on,
date appearance, remaining episodes, airing, finale and movie release controls. Their host fixtures use the actual beta.8
names, including the incompatible `s(long)` overload.

The corrected `patches-0.12.0.mpp` passed `:patches:buildAndroid` and
`:patches:verifyNuvioLatest`. The CLI applied all 14 patches without failures
and rebuilt `../Apps/NuvioTV/NuvioTV-1.1.0-beta.8-menu-fix-unsigned.apk`.
The output passes APK DEX verification and alignment. Logs are in
`../.inspect-nuvio-beta8/menu-fix-latest.log`, `menu-fix-apk-verification.log`
and `menu-fix-result.json`. Device confirmation remains pending.
