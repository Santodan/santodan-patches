# NuvioTV playback buttons

**NuvioTV - Customize playback buttons** targets `com.nuvio.tv` `1.1.0-beta.7`
(1070). It is independently selectable and uses the shared, unnamed settings-menu
dependency. No other optional Nuvio patch is required.

Open **Settings → Layout → Santodan-Patches → Playback**. Enable customization,
then choose **Edit playback buttons**. Select a button to show/hide it, move it
between the main row and More actions, or move it earlier/later within its group.
Changes are saved automatically for this installation. **Reset playback buttons**
restores the native configuration and disables customization.

Supported controls: Play/pause, next episode, subtitles, audio tracks, sources,
player engine, episodes, watch party, playback speed, aspect ratio, external player,
stream information, and playback-issue reporting. Availability and actions remain
native: customization does not create audio tracks, enable unavailable next episodes,
or expose disabled watch-party/report features. The More toggle appears when there
are available controls assigned to More, and remains available to close an expanded
group. Moving everything to the main row removes the need for More.

At least one standard main-row control remains available for native focus navigation.
If a saved layout would leave no such available control for the current video, Play
is temporarily shown in the main row. Its saved placement is retained. Report alone
cannot supply the native progress-bar focus anchor. Native down-focus and More-focus
requesters follow the actual visible controls; callbacks and up/down actions are
otherwise unchanged. Putting many controls in the main row can exceed a TV's width;
move less-used controls into More.

## Original-input evidence and mutation

Input: `../OriginalApps/NuvioTV-1.1.0-beta7.apk`, SHA-256
`a35eb5033615018f50f5205dc51f52e526b3b606f7e563943201dfbd7366d1f2`.
Manifest metadata confirms package/version/code. Original DEX and focused baksmali
output are in `../.inspect-nuvio-beta7/` and `playback-smali/`.

- `Lva/bd;->invoke(Object,Object)Object` is an instance Function2, 49 registers.
  It contains nine unique `Lva/yd;->b` range calls: eight main controls and the More
  toggle. Each uses 14 register words for 13 parameters (including a wide tint).
- `Lpa/a;->invoke(Object,Object,Object)Object` is an instance Function3, 78 registers.
  Its playback branch has four `yd.b` calls and one `yd.G` report-control call.
  Its two unrelated switch branches are retained. Native conditional branches,
  string resources, icon/painter creation, callbacks, default masks and availability
  are retained rather than reconstructed from source.
- The main lambda constructs exactly one playback `pa.a`, using its 12-parameter
  constructor and a 13-word register range. The instance is registered immediately
  after construction. Exactly one `Lv/b0;->d(Z,Modifier,Enter,Exit,String,Content,Composer,I)V`
  invocation provides expanded/collapsed state.
- `Lv/c0;` is the original empty AnimatedVisibilityScope interface. With customization
  enabled, the original More content is evaluated synchronously under a distinct
  composer group, even when collapsed, collecting native control arguments for
  movement into the main row. Its native empty Row emits no controls. The original
  animation call is used unchanged when customization is disabled.
- Native controls render in configured order at the original final More-toggle call,
  inside the existing main Row. Buttons use separate composer keys. Collection is
  thread-local, reset at the start of each main-row composition, cleared before
  rendering, and cleared when evaluating More fails. No background observer is added.
- Exact instruction fingerprints validate both original lambda bodies, including
  register operands, literals, references and branch/switch offsets. Missing,
  ambiguous, modified and already-patched layouts are rejected before either owner
  is mutated. Native control, picker, action, state and composer contracts are also
  validated. The source contains the complete authoritative descriptors.

## Verification and device checks

Run through `build-local.ps1` with Java 21 and Android SDK platform 36:

```powershell
.\build-local.ps1 :patches:buildAndroid :patches:generatePatchesList
.\build-local.ps1 :patches:verifyNuvioPlaybackButtons :patches:verifyNuvioPlaybackButtonsRuntime
.\build-local.ps1 :patches:verifyNuvioBeta7SettingsMenuRuntime :patches:verifyNuvioBeta7Bundle
.\build-local.ps1 :patches:verifyNuvioPlaybackApk
```

The native verifier uses the original APK, rejects invalid inputs, and writes/reloads
modified DEX. Runtime checks compile the production extension against native-signature
fixtures and exercise real settings callbacks, saved preferences/reload/reset,
disabled behavior, movement between groups, visibility, conditional availability,
focus destinations, balanced composer groups and failure cleanup. Shared-menu checks
cover standalone and combined selections. Bundle verification applies playback buttons
alone and with all other runtime patches, checks merged signatures, and compares
cross-DEX hierarchy findings with the original baseline.

Host verification does not establish device behavior. Check on Android TV: settings
focus and Back, each native playback action, subtitles/audio/next-episode availability,
progress-bar down-focus after hiding Play, More focus when Stream info is hidden,
moving every control to either group, reopen/restart persistence, live playback
updates, layout width, and combined-patch navigation. Installation/device testing
requires a separate user request.

## Local verification results — 2026-10-21

All build, metadata, original-DEX, host-runtime, 20 shared-menu selections and
standalone/combined packaged-bundle checks passed. Cross-DEX checks retained the
original 36 optional-library findings with no additions. The new patch was also
applied alone through Morphe Desktop CLI and the APK rebuilt successfully.

- Bundle: `patches/build/libs/patches-0.11.2.mpp`.
- Unsigned APK: `../Apps/NuvioTV/NuvioTV-1.1.0-beta.7-playback-buttons-unsigned.apk`.
- APK manifest: `com.nuvio.tv`, version `1.1.0-beta.7`, code `1070`.
- APK alignment passed `zipalign -c -P 16 4`; all 112 native libraries match the
  original byte-for-byte, and stale JAR signature entries are absent.
- The APK is unsigned. Signing, installation and Android TV testing were not performed.

On this host the build script ran with the process-local prefix
`powershell -NoProfile -ExecutionPolicy Bypass -File .\build-local.ps1`, followed
by the task arguments shown above. No system execution policy was changed.

The rebuild command was:

```powershell
java -Xmx3g -jar ../morphe-desktop-1.18.1-all.jar patch ../OriginalApps/NuvioTV-1.1.0-beta7.apk -p=patches/build/libs/patches-0.11.2.mpp "--enable=NuvioTV - Customize playback buttons" --exclusive --unsigned --out=../Apps/NuvioTV/NuvioTV-1.1.0-beta.7-playback-buttons-unsigned.apk --temporary-files-path=../.inspect-nuvio-beta7/rebuild-playback-buttons --result-file=../.inspect-nuvio-beta7/rebuild-playback-buttons-result.json
```
