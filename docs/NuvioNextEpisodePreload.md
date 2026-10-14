# Next-episode source reuse on beta.5

The next-episode patch fetches addon source lists after 30 seconds of actual
playback. With its setting enabled, the manual next-episode picker reuses Nuvio's
native search session, and the separate native threshold preloader is suppressed.
Explicit source refresh still performs a fresh search. Missing, expired, or empty
native sessions can still require an addon fetch when opening the next episode.

## Proven target and hooks

- Original: `E:/Morphe/OriginalApps/NuvioTV-1.1.0-beta5.apk`.
- Manifest: `com.nuvio.tv`, `1.1.0-beta.5`, version code `1068`.
- All `.inspect-nuvio-beta5/classes*.dex` hashes match their original APK entries.
- `Lna/mb;->C(Lna/k8;Lcom/nuvio/tv/domain/model/Video;Z)V` is static, has
  167 registers, and delegates to the episode loader `na.mb.p`. The incoming
  force-refresh flag is register 166. All three original call sites belong to
  `Lna/fb;->invokeSuspend`, the next-episode transition coroutine.
- The previously patched APK contains a branch at code address 3864 targeting
  the original picker invoke at 3871, bypassing the guard inserted before it.
  The replacement guard runs at the callee's entry and therefore covers that path.
- `Lna/mb;->w(Lna/k8;)V` is the static threshold preloader. Its 13-register
  body starts by writing scratch register 0 and reads the native `h2` triggered
  flag. An entry predicate uses register 0 to return while the patch is enabled;
  otherwise it branches to the original first instruction.
- Explicit refresh calls the episode loader directly, so it does not pass through
  the guarded next-episode picker. The disabled path keeps native behavior.
- No older-version hooks, mappings, compatibility entries, or tests were extended.

## Verification

From `E:/Morphe/santodan-patches`, with Java 21 selected:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-local.ps1 :patches:buildAndroid :patches:verifyNuvioNextEpisodePreload :patches:verifyNuvioStreamPreloadRuntime :patches:verifyNuvioNextEpisodeBundle --offline --no-daemon --console=plain
```

Passed timing, cancellation, cache reuse, enabled/disabled behavior, explicit
refresh, native hook rejection, standalone application, and application with all
nine Nuvio runtime patches. The runtime fixture now invalidates its cached session
on force-refresh, matching the native cache rather than merely counting the flag.
The assembled DEX checks verify parameter register 166 and the threshold guard's
branch destination, as well as bundled extension and native coroutine signatures.

Bundle: `patches/build/libs/patches-0.10.1.mpp`. The original APK was used to assemble
and inspect patched DEX and combined resources. No complete APK was packaged,
signed, installed, or tested on a TV for this fix.

On the TV, repatch with this local bundle and enable **Layout > Santodan-Patches >
Streams > Preload streams for next episode**. Test Manual mode by waiting for the
30-second preload, then pressing Next Episode and allowing the threshold prompt
to appear. Also verify explicit refresh and disabling the setting.

`SantodanStreams` logs observer registration, preload starts/results, and
`Next episode picker forceRefresh=false`. Cache expiry or failed preloads can
still cause a later search; elapsed time and a loading indicator alone do not prove
whether the native cache was reused.

## Playback readiness diagnostics

Beta.5 device testing showed observer registration without a next-episode search.
The current diagnostic build logs `Next episode state` when readiness changes and
at five-second playing-time milestones through 30 seconds. It reports the persisted
setting, native playing/buffering/ended/exit/error flags, profile and current-target
readiness, and next-episode presence/airing/readiness. It never dumps player state,
stream URLs, or profile identifiers. These diagnostics do not change eligibility or
timing. Beta.5 work is explicitly requested for this issue.

Xiaomi box logs confirmed that the enabled observer reached 30 seconds with an
aired next episode, but rejected its content type (`series=false`, `nextReady=false`).
Case normalization alone did not resolve the device failure. The beta.5 player now
reads the native request type from ViewModel `na.me.W` (controller `na.k8`), field
`Q:String`, which the original `na.mb.p` episode loader uses. The presentation state's
`g` is not authoritative for the request. It accepts case-insensitive `series` and
`tv`, matching the original controller's startup checks, and searches with the
canonical `series` cache key. Older-version dispatch retains its previous behavior.
The host runtime fixture leaves UI type `g` empty and uses controller `Q=SERIES` to
exercise real delayed fetching and cache reuse. The bundle verifier proves both
field descriptors and the original episode loader's `Q` read.

The user installed the controller-fix build on a Xiaomi box and supplied
`logs_patch.log`: episode 7 reached 30 seconds at 12:49:16 on October 14, 2026,
then preloading completed with 13 sources at 12:49:16.725, about 0.6 seconds later.
This confirms delayed source fetching on a physical device. Native cache reuse
and refresh suppression passed host tests; these lines alone do not prove whether
opening the device's picker performed another network request.
