# NuvioTV merged tracking progress

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
  and `d` (6 parameters). Choosers and progress use Android platform dialogs.

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
