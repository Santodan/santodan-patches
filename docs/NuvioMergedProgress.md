# NuvioTV merged tracking progress

## Startup cache retention (beta.8, 2026-10-23)

The current target is original `com.nuvio.tv` `1.1.0-beta.8` (1071),
`../OriginalApps/NuvioTV-1.1.0-beta8.apk`; see [NuvioBeta8.md](NuvioBeta8.md).
The historical implementation notes below describe earlier versions.

Progress and next-up readers now await profile cache restoration on the IO
dispatcher before their first emission. Repository registration no longer marks
an empty startup snapshot as ready, and starting a sync no longer clears a native
snapshot seeded during launch. Cache loading is independent of the Home sync gate.
Subsequent syncs retain the current snapshot until a complete result is ready;
unchanged records remain equal, while additions, updates and removals are published
together. An empty completed result remains valid. Profile changes still invalidate
the previous profile's data. Restore failures retain the saved cache for diagnosis
and retain any usable in-memory startup data.

The provider-refresh host verifier covers delayed restoration without an initial
empty publication, retained entries during refresh, additions/updates/removals,
unchanged-result suppression, cancellation, and valid empty snapshots. A device
relaunch check remains necessary to confirm card visibility and focus throughout
startup and sync.

Verification passed with `build-local.ps1 :patches:buildAndroid
:patches:verifyNuvioBeta8 :patches:verifyNuvioProviderRefreshRuntime
:patches:verifyNuvioBeta8Bundle`. Standalone and combined packaged DEX checks
added no cross-DEX findings beyond the original's 36 optional-library findings.
Bundle: `patches/build/libs/patches-0.13.0.mpp`. No installable APK was rebuilt,
signed, installed, or tested on a device.

Target: original `com.nuvio.tv` `1.1.0-beta.6` (1069),
`../OriginalApps/NuvioTV-1.1.0-beta6.apk`. Input provenance is recorded in
[NuvioBeta6.md](NuvioBeta6.md).

Under Layout → Santodan-Patches → Continue Watching → Tracking progress:

- Enable merged progress and choose highest progress or most recent update.
- Choose the main service: Nuvio Sync, Trakt, Simkl, or MDBList. The previous
  pre-merge source supplies the initial main choice when available; otherwise
  Nuvio Sync is the default. This is the playback write target and failure fallback.
- Select services to merge. All supported services are selected by default;
  disconnected services are skipped. The main service is always included.
- Sync now opens a dialog with a stage progress bar, per-service record counts,
  merged/updated/removed progress counts, Continue Watching and watched-item
  counts, elapsed time, or the failed stage. Closing the dialog allows the sync
  to finish. The last completed summary is saved per profile.

Main-service and included-service choices are profile-specific. Sync combines
snapshots inside NuvioTV; it does not copy progress between remote services.

## Scheduling and playback

The first Home entry at startup and subsequent returns to Home request an
automatic sync. Foreground resume on Home also requests one. Recomposition does
not constitute a new Home entry. There is no periodic refresh timer. Provider
updates mark the snapshot dirty for the next Home sync; they do not start one
while browsing or playing video. Manual sync can run from settings.

Leaving Home interrupts automatic work. Entering the player or backgrounding the
activity also interrupts manual work. Revision checks reject stale publication
after navigation, settings changes, or profile changes. A native refresh that has
already launched work in its own scope may finish independently; it cannot publish
a merged snapshot through an expired merge. Source-search pause flags no longer
control this feature, so source switching and autoplay searches cannot reopen the
merge gate inside the player.

With merging enabled, native scrobbling is filtered to the main connected remote
service. Choosing Nuvio Sync suppresses remote-service scrobbles. Choosing a remote
service suppresses Nuvio Sync remote progress writes through both native
single-progress save routes. Local progress and completed-item persistence remain
available for resuming playback. Native authentication, scrobble failures, seeks,
and completion handling are preserved. Disabling merging restores native routing.

The previous merged snapshot remains visible during a sync and on failure. If no
usable snapshot exists, the patch attempts a bounded main-service snapshot fallback.
Failure does not publish a partially collected combination of services. Native
provider APIs can serve cached data; record counts describe the snapshots read,
not a guarantee that a remote server accepted a new write.

## Proven beta.6 hooks

- `Lp8/m;->invoke(Object,Object,Object)Object`: the unique
  `Lio/sentry/o;->V([Ll5/b1;Lg1/p;)Ll5/n0;` result is the remembered root
  NavController. The bridge observes inherited `Ll5/y;->F:SharedFlow`; entries
  have `Ll5/m;->e:Ll5/g0;` and destination route `Ll5/g0;->o:String`.
- `MainActivity.onResume/onPause/onDestroy`: zero-argument instance lifecycle
  methods with verified native super calls. They own foreground gating, dialog
  activity ownership, and navigation observation cleanup.
- `Lo9/h0;->a()ArrayList`: unique connected-scrobbler result, containing
  `Lo9/e0;->a()Lo9/s0;`. Each scrobbler exposes `Lo9/s0;->a()Lo9/g0;` identity.
- `Lz9/zc;->C` and `s`, both `(I,WatchProgress,ContinuationImpl,Z)Object`:
  save locally through `local/cc.C` and queue remote mutations through `k9/d6.o`.
  The last Boolean parameter is filtered before native parameter moves.
- Both returns of the selected-provider mapper `Lz9/sb;->invokeSuspend` install
  the merged proxy, including Nuvio Sync and disconnected-main null-provider paths.
- Native settings section/button contracts are `Lxa/bd;->a` (11 parameters)
  and `d` (6 parameters). Tracking popups use Nuvio's Compose dialogs as described
  below, inheriting the active app theme.

## Verification and device checks

Run `build-local.ps1 :patches:buildAndroid :patches:verifyNuvioBeta6
:patches:verifyNuvioBeta6Bundle :patches:verifyNuvioProviderRefreshRuntime`.
The native verifier exercises the production navigation/write policy and checks
original DEX contracts, invalid/ambiguous/missing/already-patched rejection, and
DEX write/reload. The bundle verifier applies merged progress alone and all nine
runtime patches together, checking merged hooks and native coroutine resolution.

Device checks remain: TV remote focus in the submenu/dialogs, connected and
disconnected main choices, manual progress/error display, profile changes,
Home → player → source/episode switching → detail/stream → Home, external-player
backgrounding, and remote-service receipts proving only the main service receives
playback writes. Host verification does not establish these device behaviors.

The build and all three verification tasks above passed for this change.
Standalone merge and combined bundles
have the same 36 optional-library hierarchy findings as the original, with no
new findings. The bundle is `../patches/build/libs/patches-0.10.1.mpp`.
No installable APK was assembled, signed, installed, or device-tested for this change.

## Playback progress-read regression (2026-10-16)

MDBList as main service with Nuvio Sync excluded is a supported configuration.
Excluding Nuvio Sync does not require enabling stream preloading.

The aggregate's old progress/next-up reader treated an empty completed snapshot
as missing data. It raced a collection of the main provider's native flow against
the snapshot-ready signal. Original beta.6 MDBList `Ls9/c6;` wraps its progress
and seed flows in `A(Flow)Flow`, which installs an `onStart` loader. Merely
collecting such a flow can start provider loading/refreshing. Racing against an
already-ready signal does not prevent those side effects, and `first()` must
wait for cancellation of its competing collection. This is unsafe on the saved
progress read that precedes starting a new episode.

`NuvioProviderRefresh.retainedSnapshot` now emits the cached merged flow directly
when its snapshot is ready (including an empty list) or native startup fallback
is disallowed by the Home gate. The unready Home startup fallback remains intact.
The production reader is shared with the host regression verifier. The new test
failed on the prior behavior because playback collected a refresh-on-start flow;
it covers empty ready/unready playback reads, completed empty Home reads,
subsequent snapshot publication, and unready Home fallback.

The supplied `logcat_all_allPreloadDisabled.txt` capture has expired application
events and no retained merge exception proving the exact frozen-frame cause.
The merge-disabled capture also contains a recoverable ExoPlayer error, so that
error alone cannot establish the merge regression's cause. The fix addresses a
reproduced invalid read path; the reported episode transition still needs a
device retest with the same MDBList/excluded-Nuvio-Sync settings.

Verification passed with `build-local.ps1 :patches:buildAndroid
:patches:verifyNuvioProviderRefreshRuntime :patches:verifyNuvioBeta6
:patches:verifyNuvioBeta6Bundle`. The regression fails with the prior reader and
passes with the fix. Standalone and combined bundle checks add no cross-DEX
hierarchy findings to the original's 36 optional-library references. The current
bundle is `../patches/build/libs/patches-0.11.0.mpp`; no version was bumped for this
fix. No installable APK was assembled, signed, installed, or device-tested.

## Native tracking dialogs (2026-10-16)

Main-service selection uses the same native single-choice picker as the date-label
settings. Service inclusion uses Nuvio's scrolling multi-choice picker with its
Clear/Save actions. The mandatory main service is named in the subtitle and
excluded from the optional choices; saving or clearing always preserves it.
Back/dismiss discards unsaved service choices. Dialogs close when navigating away
or switching profiles. Closing the sync dialog allows the sync to finish.

Sync status and the disabled-feature message use NuvioDialog's themed surface,
border, corners, typography and native Close button. The progress bar uses the
current app theme's secondary/surface-variant colors, rounded shape and native
layout primitives; it does not use Android's platform ProgressBar. Rendering reads
the active theme through the Composer and updates the dialog's sync status through
the existing Compose revision state.

Additional original beta.6 contracts:

- `Lxa/ub;->j` (11 parameters), `g` (12 parameters), and `Lxa/cc;`'s six-parameter
  constructor supply the native single/multi-choice pickers and option models.
  Width and list height are supplied explicitly because R8 removes some defaults.
- `Lfa/j3;->z` (19 parameters) is the original NuvioDialog. Its defaults are taken
  from the original native picker call: `16336`, explicit width, and second
  changed flags `24576`. Colors, border, radius and spacing remain native defaults.
- `Lxa/ub;->e` (5 parameters) and `d` (7 parameters) render the native dialog action
  row and button. `Lxa/bd;->b` (6 parameters) supplies wrapping SettingsNote text.
- Theme local `Lab/z0;->a` supplies `Lab/e;`: `h:J` is Secondary (from palette
  `Lab/u0;->a`), and `e:J` is SurfaceVariant. Progress uses the original horizontal
  fill `layout/b.e`, height `layout/b.g`, shape `Ll0/g;->b`, background `Ly/l;->g`,
  and Box primitives `Le0/b;->a` / `Le0/q;->a`.

Run `:patches:verifyNuvioTrackingDialogs` alongside the build, beta.6 native/bundle
checks and provider-refresh regression task. The renderer verifier exercises the
production Java helper against host fixtures: picker selection and dismissal,
multi-choice Save/Clear protection of the main service, dialog/button callbacks,
visible dimensions, progress fractions, and live theme changes. Original DEX
verification checks the actual native contracts. TV focus, clipping on smaller
screens, popup appearance and live sync progress still require device testing.

The final `build-local.ps1 :patches:buildAndroid :patches:verifyNuvioTrackingDialogs
:patches:verifyNuvioBeta6 :patches:verifyNuvioProviderRefreshRuntime
:patches:verifyNuvioBeta6Bundle` run passed. Standalone and combined outputs add no
cross-DEX findings. The updated `patches-0.11.0.mpp` contains the playback-read fix, native-dialog
changes, and sync-bar recomposition fix described below. No APK was assembled, signed, installed or
device-tested.

## Sync bar recomposition regression (2026-10-16)

Original beta.6 `Le0/b;->a` remembers its BoxWithConstraints content using only
the track modifier; R8 treats the content argument as static. The full-width
track stays equal across stages, so its remembered fill can retain the initial
fraction while the stage label advances. Scope this native layout in a replaceable
Composer group keyed by the fraction and accent color, and always close the group.
The track remains full width. The host fixture reproduces native content retention
and checks every stage from 1/5 through 5/5 with unchanged colors, repeated completion,
zero progress, and theme changes. Device confirmation of the rendered bar remains required.
