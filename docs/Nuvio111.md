# NuvioTV 1.1.1

Target: `com.nuvio.tv`, version `1.1.1`, version code `1081`.
Original: `../OriginalApps/NuvioTV-1.1.1.apk`.
SHA-256: `1ac8aa80ad9fff7b4c21cfe4bfdfed61b4de44a120ff4b1b5743149ccb00928d`.
SDK aapt confirms the package and version. All seventeen public NuvioTV patches
use this exact target, including metadata add-on selection. Bundle versions remain
owned by the release workflow.

## Migration

The native contract now describes 1.1.1. Dedicated `VerifyNuvio111*` fixtures and
tasks drive `verifyNuvioLatest`; historical fixture sources remain unchanged.

- Current-stream rendering moved from `bb.wn.h` to static `bb.vn.h`. The complete
  thirteen-argument signature, register count and instruction body are unchanged.
- The controller's playback request type moved from `T` to `U`, binge group from
  `u3` to `v3`, preload-trigger flag from `t2` to `u2`, controls job from `U0` to
  `V0`, and playback session flow from `H0` to `I0`. The player-state accessor
  still returns a StateFlow, now through controller `G0`. Shared contract names
  bind the preload and IntroDB readers to the same fields their patches validate.
- Native Info/Speed factories moved to `z0.e.b` / `z0.c.b`. External/report
  factories moved to `k8.d.B` / `k8.d.x`. Native caption, aspect and chapter caches
  are `k8.d.h`, `k8.j.e` and `k8.i.b`. Real-DEX glyph checks retain Info, Play,
  Groups and BugReport semantics and also verify Speed and OpenInNew.
- Library cards now call static `z0.d.e(String,Composer):int` before requesting
  their image. The metadata artwork hook uses this full reviewed reference.
  Native repositories, enrichment constructors and per-profile persistence remain
  unchanged. Metadata hooks still compose with movie/finale decorations.
- Startup, player controls/timers, native update banner, installer and updater
  fingerprints were refreshed after reviewing their native reference moves and
  embedded version constants. Signature, mutation-scope, try-layout and
  missing/changed/ambiguous/already-patched checks remain in place.

## Evidence

Use `../Skills/inspection-evidence/.inspect-nuvio-111/` directly.
`provenance.json` records both original APK hashes and all original DEX hashes.
The retained 1.1.0 fixtures were compared byte-for-byte with their original APK;
the five 1.1.1 fixtures were extracted independently.

`baseline.tsv`, `candidate.tsv` and their `*-extra.tsv` counterparts contain
fresh original-Dex definitions, method fingerprints and focused instruction dumps.
`reviewed-method-comparison.json` proves eleven guarded bodies retain their
opcodes, registers, literals and branch offsets after the explicitly reviewed
reference moves. Focused `*-diff.txt` files cover player controls, timers, startup,
metadata library artwork, update banner and installer changes. The structural
discovery report is a candidate-finding aid, not compatibility proof.

## Verification and delivery

Run from the repository with Java 21 and SDK platform 36:

```powershell
.\build-local.ps1 :patches:buildAndroid :patches:generatePatchesList :patches:verifyNuvioLatest :patches:verifyAndroidBundle :patches:verifyMorpheDesktopImport '-PmorpheDesktopJar=E:/Morphe/morphe-desktop-2.2.0-all.jar' --continue --offline
```

`final-validation.log` records the complete suite. Its sole remaining failure was
an IntroDB verifier assertion containing the preceding release's control-row hash;
the dedicated 1.1.1 fixture and default verifier entry points are checked in
`final-fixtures.log`, which passed. All required checks passed across these runs
and the APK audits on 2026-11-21. The packaged verifier applies
eleven standalone/combined selections, including metadata alone and all patches
together, and checks DEX roundtrips, runtime/native references and cross-DEX
findings. Settings, playback, IntroDB, preloading, tracking, lifecycle and
negative matcher checks run separately from package import.

The CLI rebuild applies all seventeen patches to the original without `--force`.
`rebuild-all.command.json`, `rebuild-all.json`, `rebuild-all.log` and
`rebuild-all.audit.json` retain the command, patch results and delivery audit.
The output retains all 108 original native libraries and passes 16-KB alignment.
Clone authorities, restart process and private settings activities are audited.

`verify-apk.ps1` records the actual-APK audit invocation. `apk-validation-2.log`
records successful playback, updater, installer, date-label editor,
settings transfer/restart, IntroDB, metadata and new `verifyNuvio111Apk` checks.
The latter checks migrated native definitions and reflected bindings in the
delivered preload, IntroDB and playback-icon runtime classes. Cross-DEX findings
remain the original's 36 optional-library findings.

Bundle: `patches/build/libs/patches-0.20.0.mpp`.
Combined APK: `../Apps/NuvioTV/NuvioTV-1.1.1-patches-0.20.0-all-unsigned.apk`.
Initial migration bundle SHA-256: `5916f857ee31ffe5249edf9a88d2460c6af02e71965922f286d6253654d5deb8`.
APK SHA-256: `c00e4b8fcb7438ee5b98dc8de9808e6341d16d9254399eca669682fdf63c42e9`.

The APK is unsigned. No signing, installation or device testing was performed.
Device checks remain for remote focus, native settings editors, playback/resume,
profiles, tracking, metadata artwork across restarts, TV channels and installation.
