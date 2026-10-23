# NuvioTV merged tracking progress

Latest device feedback: the user reports the native-library-isolation build is
working almost perfectly. Preserve that behavior; further remote incremental-sync
work is deferred. Remaining performance and service rate limits are not claimed
to be eliminated.

## Native library isolation correction (beta.8, 2026-10-23)

The latest log shows the merge-added library call at 18:29:26 receiving HTTP 429
at 18:30:05 and increasing quick-sync time to 67.5s. That added call was the wrong
fix and is removed, together with its runtime helper and feature-specific tests.
Quick sync is back to three progress-only steps. Native Nuvio retains library
ownership through Tracking settings; the remaining-count/cache-publication fixes
are retained. This does not guarantee native library requests cannot be rate limited.

Packaged beta.8 verification now compares method fingerprints and field layouts
of nine native library classes against the original APK in every standalone and
combined patch selection: repository ba.e1, interface/registry q9.i/q9.j, provider
adapters ba.lc/ca.h3/u9.g6, and backing repositories ba.q8/ca.f7/u9.b3. This guards
against future changes to library behavior through the merge. Combined selection
allows only the already existing, independent launcher-channels constructor
observation hook; all remaining library method bodies and fields must match.

Build/runtime checks passed via `build-local.ps1`: `:patches:buildAndroid`,
`:patches:verifyNuvioProviderRefreshRuntime`, `:patches:verifyNuvioRemainingSyncRuntime`
and, after adjusting the guard for that existing launcher hook,
`:patches:verifyNuvioBeta8Bundle --offline`. The rebuilt APK passed
`:patches:verifyNuvioPlaybackApk -PnuvioPlaybackApk=E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-native-library-isolation-signed.apk -PnuvioRequireStability=true --offline`.
All 16 selected patches applied against original `com.nuvio.tv` beta.8/code1071;
signature/alignment passed with the existing signer and no new cross-DEX findings.
Delivery: `E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-native-library-isolation-signed.apk`
and `E:/Morphe/Apps/NuvioTV/patches-0.13.0-native-library-isolation.mpp`; source
bundle remains `patches/build/libs/patches-0.13.0.mpp`. No installation/device test ran.

## Startup resume counts (beta.8, 2026-10-23)

The device log shows Undead Unluck composed with no registered ID at 18:01:18,
then its native NextUpInfo registered at 18:02:21. First registration did not
invalidate the already displayed card. Cached resume progress now registers
remaining-count identities before Home consumes the restored cache. Late first
registration also invalidates composition and associates recently visible titles
with the arriving ID. Native NextUpInfo remains authoritative over a synthetic
resume seed. The counter retains dirty notifications arriving during an active
count pass, preventing restored membership from being dropped by coalescing.

Quick sync's membership-ready state now exposes complete cached history to the
remaining-count patch. It explicitly publishes restored history to visible-card
counts and already cached badge metadata. It does not claim full WatchedItem
models are loaded or initiate the full-library badge metadata scan.

The temporary selected-library refresh added to merge startup was removed.
Library selection, refresh scheduling, cache and errors belong to native Nuvio.
The merge must not call library providers or the library source-setting action.

The log's termination is an input-dispatch ANR, with repeated GC stalls and high
CPU; no Java fatal exception stack is available. Native MDBList library refresh
also logs HTTP 429. These fixes address concrete startup omissions and count
publication races; resolution of the ANR and remote rate limiting needs device
retesting. Do not describe the ANR as a proven fixed Java crash.

Original DEX evidence is in `.inspect-nuvio-beta8/startup-library-contracts.txt`,
`library-provider-contracts.txt`, and `library-refresh-implementations.txt` under
the workspace root. All three native library implementations actually suspend
until their backing refresh returns. Remaining runtime verification covers a
resume card displayed before registration, membership arriving later, completion
clearing the count and the existing stale-request/seed protections. Provider
verification covers the exact erased continuation ABI, awaited completion,
failure propagation and cancellation; beta.8 verifies the actual native symbols.

Verification passed through `build-local.ps1` with the existing Gradle cache:
`:patches:buildAndroid :patches:verifyNuvioProviderRefreshRuntime :patches:verifyNuvioRemainingSyncRuntime :patches:verifyNuvioRemainingCounts :patches:verifyNuvioBeta8 :patches:verifyNuvioBeta8Bundle --offline`,
then `:patches:verifyNuvioPlaybackApk -PnuvioPlaybackApk=E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-startup-library-counts-signed.apk -PnuvioRequireStability=true --offline`.
CLI applied all 16 selected patches and rebuilt/signed beta.8/code1071 from the
original `com.nuvio.tv` APK. Signature, ZIP alignment and rebuilt-APK checks passed;
there are no new cross-DEX findings. Delivery files are
`E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-startup-library-counts-signed.apk` and
`E:/Morphe/Apps/NuvioTV/patches-0.13.0-startup-library-counts.mpp`; source bundle
remains `patches/build/libs/patches-0.13.0.mpp`. No installation/device test ran.

## Buffered restoration; startup watermark versus remote refresh (beta.8, 2026-10-23)

The latest cached launches take 30.0/30.3s: 27.25/27.41s decoding an existing
NMC1 membership cache, about 0.5s applying local changes and 2s publishing/saving.
No remote provider refresh runs in this path. `savedAt` already persists the
local reconciliation watermark; `quickProgress` applies newer local progress,
retaining remote entries. Completed episode memberships replay since the saved
full-history timestamp so repeated quick launches cannot lose completion badges.
The menu's Sync now still performs a full provider refresh.

The proven beta.8 provider interface exposes refresh intent plus continuation,
and snapshot flows; it does not provide a uniform `since` argument to the patch.
This is a statement about the native interface, not a claim that the services'
HTTP APIs lack incremental endpoints. A remote incremental integration would need
separate provider contracts and removal/completion handling. Quick sync does not
fetch changes made on other devices.

NMC1 reader/writer now buffer decompressed/uncompressed bytes between Data streams
and GZIP. Previously small primitive reads/writes repeatedly entered the inflater/
deflater. The persisted format is unchanged: no cache migration or full sync is
needed. A desktop synthetic comparison of the previous and current production
reader (401,641-character cache, 3,173 histories, 7,501 alias entries, 10 measured
iterations after warmup) measured 37.1ms versus 19.1ms average, with identical maps
and timestamps. This desktop result does not predict the TV's actual duration.
Runtime verification also reads an independently written old-format fixture and
retains corruption/cancellation checks. Device performance remains to be measured.

Verification passed via `build-local.ps1` using the existing Gradle cache:
`:patches:buildAndroid :patches:verifyNuvioWatchedSnapshotRuntime :patches:verifyNuvioProviderRefreshRuntime :patches:verifyNuvioBeta8Bundle --offline`,
then `:patches:verifyNuvioPlaybackApk -PnuvioPlaybackApk=E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-buffered-membership-signed.apk -PnuvioRequireStability=true --offline`.
All 16 selected patches rebuilt/signed successfully against original
`com.nuvio.tv` beta.8/code1071; signature and ZIP alignment passed. The APK adds
no cross-DEX findings above the original baseline. Nothing was installed or
device-tested. Delivery files: `E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-buffered-membership-signed.apk`
and `E:/Morphe/Apps/NuvioTV/patches-0.13.0-buffered-membership.mpp`; source bundle
remains `patches/build/libs/patches-0.13.0.mpp`.

## Shared-set membership cache (beta.8, 2026-10-23)

The next device log confirms cached sync totals of 88.2s, 69.1s and 76.0s.
Membership restoration alone takes 83.4s, 64.8s and 70.1s; preparation is under
120ms. Splitting the previous JSON file did not adequately solve the problem.

The new `NuvioMembershipCache` stores a string dictionary and distinct episode/
alias sets with ID-to-set references, then compresses the payload. Restoration
shares immutable sets and episode values instead of allocating a full copy per
catalog ID. The timestamp, empty sets, special episodes and ambiguous-alias marker
are preserved. Local playback updates replace map entries without mutating shared
sets. Versioned `membership_v2` preferences avoid opening the previous JSON XML
after migration. Existing JSON migrates once after Home; full history remains a
failure fallback. No provider refresh is added to quick sync.

Logs now separate store opening, decoding and migration saving, and report the
cache format, encoded size and restored map counts. Host verification exercises
six million expanded memberships backed by only 2,000 distinct episodes, asserts
exact map equality and shared-set identity, and checks malformed/truncated caches,
cancellation, empty snapshots and ambiguity. Actual TV timings still need testing.

Verification passed using `build-local.ps1` with `GRADLE_USER_HOME=C:/Users/Danny/.gradle`:
`:patches:buildAndroid :patches:verifyNuvioWatchedSnapshotRuntime :patches:verifyNuvioProviderRefreshRuntime :patches:verifyNuvioBeta8Bundle --offline`,
then `:patches:verifyNuvioPlaybackApk -PnuvioPlaybackApk=E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-grouped-membership-signed.apk -PnuvioRequireStability=true --offline`.
CLI rebuilt/signed all 16 patches against original `com.nuvio.tv` beta.8/code1071;
APK signature/alignment passed with the existing signer. There are no new
cross-DEX findings above the original baseline. Nothing was installed or tested
on a device. Delivery: `E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-grouped-membership-signed.apk`
and `E:/Morphe/Apps/NuvioTV/patches-0.13.0-grouped-membership.mpp` (source bundle
`patches/build/libs/patches-0.13.0.mpp`).

## Cached sync timing and separate membership storage (beta.8, 2026-10-23)

The latest device log shows a 120.7s full sync (97.6s provider reads), followed
by cached syncs taking about 75s and 82s from Home presentation. Those cached
runs explicitly made no provider refresh requests. Exact local stage costs were
not logged, so the cache hydration contribution still needs device measurement.

Quick completion now reports total elapsed time including preparation, in both
the native banner and saved menu summary. Full completion uses the same total;
errors and deferrals also show elapsed time. Logs include preparation, membership
restore, local changes, and publication/save durations.

Full sync saves a separate per-profile membership store containing episode
membership, aliases and the full-history timestamp. Cached launches read this
store without opening the full history SharedPreferences XML or scanning its
WatchedItem section. Existing installs migrate once during cached sync; subsequent
launches use the separate store. Legacy settings migration opens full history
preferences only if there are actual legacy snapshots to migrate. Full history
remains available for fallback. Cancellation/profile checks remain in place.

This reduces avoidable disk/cache work; the actual speed improvement needs a
device run, particularly the first migration versus the subsequent cached launch.

Verification passed with `GRADLE_USER_HOME=C:/Users/Danny/.gradle` via
`powershell -NoProfile -ExecutionPolicy Bypass -File .\build-local.ps1`:

- `:patches:buildAndroid :patches:verifyNuvioProviderRefreshRuntime :patches:verifyNuvioWatchedSnapshotRuntime :patches:verifyNuvioBeta8Bundle --offline`
- `:patches:verifyNuvioSyncBannerRuntime --offline` (earlier build in this change)
- `:patches:verifyNuvioPlaybackApk -PnuvioPlaybackApk=E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-sync-timing-cache-signed.apk -PnuvioRequireStability=true --offline`

Original input remains `com.nuvio.tv` 1.1.0-beta.8/code1071. The CLI applied all
16 selected Nuvio patches and rebuilt/signed the APK as `com.nuvio.santodan`.
APK signature verification and ZIP alignment passed; the signer matches prior
test builds. Bundle combinations and the rebuilt APK add no cross-DEX findings
above the original optional-library baseline. `git diff --check` passed.
The APK was not installed or tested on a device. Source bundle is
`patches/build/libs/patches-0.13.0.mpp`; delivery copies are
`E:/Morphe/Apps/NuvioTV/patches-0.13.0-sync-timing-cache.mpp` and the APK above.

## Early full splash before MainActivity (beta.8, 2026-10-23)

Launcher entry now goes through a lightweight `NuvioStartupActivity` before
constructing the native Hilt-backed MainActivity. It draws the full splash layout:
the native 48dp wordmark, profile gradient or cached local background, 32dp gap,
and a 48dp animated loading indicator. This is a framework View renderer of the
native splash design, avoiding Compose and MainActivity dependencies at this stage.
MainActivity and its normal Compose splash then take over.

The initial Android launch window uses the gradient and wordmark too, replacing
the previous square app icon. Framework/Application creation still precedes any
Activity drawing; the patch does not move Android lifecycle or Hilt initialization
to an arbitrary background thread. Native startup work continues while the early
splash is visible. Main-thread native initialization can briefly pause animation.

Original beta.8 manifest/DEX evidence differs from the upstream source: six
`com.nuvio.tv.launcher.AppIcon*` entries are real Activity subclasses of
`com.nuvio.tv.MainActivity`, each with a constructor only. Their manifest entries
become aliases of the early entry, preserving names, enabled state, icons,
banners, labels and intent filters. The original MainActivity and VIEW filters
remain intact. The early target is non-exported and has no history. It forwards
the incoming intent's data, categories, extras, ClipData and URI grants to the
native class in the installed package. Single-top/clear-top handoff reuses an
existing native Activity and avoids an extra Back destination or second task.

Splash appearance uses native `startup_splash` preferences (`color_<profile>`,
`theme_<profile>`, `bg_url_<profile>`). The active profile is supplied by the existing
repository hook and remembered on the cache IO path. Background decoding runs on
a bounded worker and is limited to app-owned local cached files, sampled to a
maximum 1920px dimension. It makes no network requests; missing, remote-only,
expired or slow backgrounds fall back to the gradient until the native splash
loads them. Cache preparation has a 1.5s deadline. After the prepared splash has
drawn, handoff is posted after a renderer frame and only while resumed. Pause,
destruction and repeated callbacks cannot start a duplicate Activity.

`SantodanStartup` logs early creation, appearance preparation and frame submission
before native MainActivity starts. Measure total startup from process creation to
the native Home-presented marker: Android's Displayed/fully-drawn timings now refer
to separate Activities rather than one continuous launcher Activity.

Verification passed: `build-local.ps1 :patches:buildAndroid :patches:verifyNuvioBeta8
:patches:verifyNuvioProviderRefreshRuntime :patches:verifyNuvioBeta8Bundle`.
Tests cover manifest preservation/rejection, prepared-frame and resume gating,
duplicate/destruction handling, original native contracts, compiled resources,
packaged classes and exact inherited Android SDK method resolution. The bundle
verifier applies merge alone, merge plus side-by-side, and all runtime patches
together. All outputs have no new cross-DEX findings beyond the original's 36
optional-library findings. No installable APK was rebuilt, signed or installed;
the transition, selected background, Back/resume, icon selection and TV-launcher
task behavior still require device tests. Artifact: `patches/build/libs/patches-0.13.0.mpp`.

## Native launch logo and initialization timings (beta.8, 2026-10-23)

The merge patch includes an unnamed resource dependency that replaces the solid
native launch drawable with the same background plus the original Nuvio logo,
centered at 160dp. This is the Android launch window shown before Compose can
render the in-app splash. It requires no runtime disk reads or network requests.
The native Android 12+ splash configuration remains in place.

Original beta.8 resources prove `drawable/splash_screen_background` (0x7f08011f)
is `res/0n.xml`: a rectangle with `color/splash_background` (0x7f06005d).
`drawable/app_logo_mark` (0x7f08004f) points to the existing `res/sR.png`.
The resource transformation rejects changed or already-layered input and keeps the
original background shape. Compiled resource checks inspect the merge-only and
combined resource APKs for the native logo reference.

Logcat has paired `Startup begin` / `Startup end` messages for merge repository,
settings store/component, activity resume, stream-preload component, launcher
component, and IntroDB activity setup. They report the calling thread at entry
and elapsed milliseconds at exit. Use the timestamps alongside process creation,
activity creation, first frame and fully-drawn events to distinguish patch work
from the intervals between hooks. Method timings exclude class initialization
that happens before method entry.

Verification commands: `build-local.ps1 :patches:buildAndroid
:patches:verifyNuvioBeta8 :patches:verifyNuvioProviderRefreshRuntime
:patches:verifyNuvioBeta8Bundle :patches:verifyNuvioIntroDbRuntime
:patches:verifyNuvioStreamPreloadRuntime`.
All passed, including logo inspection in the compiled merge-only/combined resource
APKs, resource rejection tests, runtime regressions and cross-DEX checks with no
new findings beyond the original's optional-library references.
`generatePatchesList` records the new unnamed resource dependency; the Android
bundle was rebuilt after generation and its packaged checks rerun.
The artifact remains `patches/build/libs/patches-0.13.0.mpp`; the visual transition
and logo size still require a device cold-launch check. No installable APK was
rebuilt, signed or installed.

## Separate history from startup settings (beta.8, 2026-10-23)

The next device capture showed two cold launches with first-frame times of
16.896s/16.834s and fully-drawn times of 22.520s/22.312s. Lightweight cache
restoration improved from 5.165s on legacy migration to 1.776s on the next launch,
but both processes spent about 12s before activity creation completed. Thus the
remaining pre-splash delay precedes the Continue Watching restoration stage.

The merge settings and full history still shared one SharedPreferences XML file;
the first synchronous settings read can wait for Android to load all its large
snapshot strings. Full snapshots now persist in a separate history preferences
file. After Home appears, migration copies existing profile snapshots durably
before removing only their large values from the settings file. Settings, source
choices, summaries and cached records are retained. Failed copy writes never
remove the source; retries preserve any newer destination history.

The first launch after this update can still pay the old settings-file load cost.
Allow `History storage migration: complete=true` to appear before restarting; the
next cold launch uses the smaller settings file. `Merge settings first read`
reports its elapsed milliseconds so a new capture can confirm how much of the
pre-splash delay belongs to this path. Exact attribution and the speed improvement
remain device checks, rather than conclusions established by host tests.

The same build, beta.8 DEX, provider-refresh runtime and standalone/combined bundle
commands listed below passed for this change with no new cross-DEX findings.
Migration checks cover multiple profiles,
failed writes/cleanup, retry safety, newer destination data and retained settings.

## Split startup cache and defer history (beta.8, 2026-10-23)

Continue Watching restoration no longer waits for full watched history, sibling
aliases, or watched-item model construction. Progress, next-up seeds and origins
have a separate small preferences file. Existing installations migrate their
previous snapshot by streaming past history fields without constructing their JSON
trees. No cache reset is needed. The current Continue Watching entries remain
visible until a complete refresh applies additions, updates and removals.

Automatic merge waits for the owning activity's native fully-drawn reporter.
Saved full history is now restored only as a cancellable failure fallback;
successful syncs read fresh provider history directly. Manual sync runs on request,
including a queued handoff from a cancelling automatic worker. See
[the latest device-log fixes](NuvioBeta8.md) for phase budgets and verification.
The original beta.8
`Lc/j;->getFullyDrawnReporter()Lc/m;` exposes the reporter; `Lc/m;->a()V` sets
`f:Z` and invokes callbacks in `g:ArrayList` under lock `c:Object`. The listener
uses that same lock, handles an already-reported activity, and ignores an obsolete
activity's callback. Navigation, foreground, profile and stale-publication guards
continue to apply. No additional bytecode hooks were needed.

Logcat now includes `Startup CW cache loading`, `Startup CW cache ready`
(JSON-read/parse and total milliseconds, entry counts, legacy migration flag),
`Startup Home presented`, and `Deferred watched cache loading/ready` (history
counts and elapsed milliseconds). These distinguish the first legacy migration
from subsequent launches and separate cache work from rendering and remote sync.

`build-local.ps1 :patches:buildAndroid :patches:verifyNuvioBeta8
:patches:verifyNuvioProviderRefreshRuntime :patches:verifyNuvioBeta8Bundle`
passed. Host tests exercise cache publication before presentation, deferred-work
release, already-reported activities and separate activity reporters, together
with the existing cache retention/cancellation regressions. Original DEX contract
checks and standalone/combined bundle assembly passed with no new cross-DEX
findings. Bundle: `patches/build/libs/patches-0.13.0.mpp`. No installable APK was
rebuilt, signed, installed or tested on a device. Startup speed, legacy migration
and card/focus retention still require device checks on the first and subsequent
launches.

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

### Cache-first sync after Home, with the native update banner (beta.8, 2026-10-23)

The previous startup projection experiment is superseded. The latest device log
showed `Startup CW cache ready` taking 46,658 ms and 51,173 ms, with fully drawn
Home at approximately 57 seconds. Loading watched membership on the launch path
was the source of that added delay. Do not restore full history or membership
projections before Home presentation.

The launch path now reads only progress, next-up seeds and origins from the new
`santodan_nuvio_merged_progress_startup_light` preferences file. Full watched
history and aliases stay in the separate history cache. The previous oversized
startup file is never opened before Home; its CW-only contents can be migrated
by the post-Home worker. On the first update, cached CW cards may populate after
Home is already visible. Subsequent launches use the migrated lightweight file.

Once the owning Activity's native FullyDrawnReporter has presented Home and the
real route is Home, one sync starts per active profile/session. **Quick startup
sync** is enabled by default in Tracking progress. With a validated saved snapshot
it restores episode membership/aliases after Home, skips all provider refreshes
and full WatchedItem decoding, and applies newer local playback changes. Cached
remote entries are retained when the local list is empty; local completion removes
only its matching resume entry. The lightweight cache is updated without rewriting
the full history. Changes from other devices/services require **Sync now**, which
always performs a full USER_INITIATED refresh. Disabling Quick startup sync also
makes startup perform that full refresh. This is an explicit freshness/speed tradeoff.

An old cache without the validated-provider marker, a configuration change, or no
saved cache requires one full refresh before quick startup can be used. The previous
log accepted Trakt's unready empty progress, so its saved snapshot cannot be treated
as authoritative. Full refresh now triggers provider projections, waits for native
p() readiness, and re-reads progress/history before publishing. A temporary empty
value cannot replace CW; a genuinely ready empty snapshot still applies removals.
Incomplete/failed/cancelled refreshes retain the prior snapshot. One startup request
runs per active profile, not on every Home return or provider emission. Interrupted
requests retry on Home; failed/completed requests do not loop. The full-refresh
budget remains 180 seconds, with preparation budgeted separately.

The sync can continue while browsing details/settings, but entering the player,
backgrounding, or changing profile cancels it and rejects stale publication.
There is no periodic timer. Native provider work already launched in its own
scope may finish independently; its completion cannot publish through a cancelled
merge. Playback entry also invalidates the worker's playback revision, including
rapid player entry/exit.

The custom Android-view banner has been removed. Sync status is fed through the
actual native updater host `fb.uj.i` and native banner `mb.a.b`, inside the app's
existing Compose tree/theme. Native drawing, theme colors, 180 ms
`updateBannerProgress` animation, layout and enter/exit animations are reused.
Synthetic updater state is scoped to sync; it never enters the native updater
repository. Download/install callbacks are inert for sync, the native installed-
feedback branch suppresses download actions, and only scoped status text changes.
Real update/permission/feedback state takes precedence and retains all callbacks.
Status expires after six seconds on completion or fifteen on error; the merge
menu retains the result. Progress counts sync stages, not network bytes.

Original beta.8 evidence is in `native-sync-progress-contracts.txt`,
`native-banner-host-contracts.txt`, `native-banner-mount-contracts.txt`, and
`provider-incremental-contracts.txt` under `E:/Morphe/.inspect-nuvio-beta8/`.
Root `q8.l.invoke` fingerprint is
`b9bcb00e932f362e8bd4d632b937371b65ebae2e4f019c0064c69f9db0ebaf86`:
its sole native host call uses v2..v11 (ten arguments). Native banner
`mb.a.b` fingerprint is
`50512aec1d322f8645869c4d99221fdccfb518d08abfdd302b73198a0d49ef9b`:
74 registers, parameters v64..v73, four grouped text lookups, one formatted
progress lookup and one return. Hooks preserve registers/drawing code and scope
text/action adaptation on every invocation, including native recomposition.
`lb.a` has seven constructor parameters; `kb.k0` has sixteen. Native host/drawing
references and these constructors are proven against the original DEX.

Host checks cover cache preservation, newer local progress, ready versus unready
empty providers, request/lifecycle gates, and native-hook changed/missing/ambiguous/
already-patched rejection. Standalone/combined bundles write/reload the new hooks,
check the exact call counts and resolve extension signatures. Actual banner
appearance/focus, sync duration, and playback still require device tests.

Quick-cache eligibility is published only after its matching full history is
durably committed. Interrupted/failed saves leave the CW cache usable but
unvalidated; the next sync repairs it rather than trusting partial history. Cache
save failures are reported through sync status. The local overlay replays
completions since the full-history timestamp on every launch, so a completion
survives multiple quick-sync restarts without a remote refresh.

Validation uses `:patches:buildAndroid`, `:patches:verifyNuvioSyncBannerRuntime`,
`:patches:verifyNuvioProviderRefreshRuntime`, `:patches:verifyNuvioWatchedSnapshotRuntime`,
`:patches:verifyNuvioBeta8`, and `:patches:verifyNuvioBeta8Bundle`. The production
banner bridge is executed against native-ABI fixtures with Android scheduling
stubbed; real update/permission priority, native preference flags, callback/content
preservation, scoped labels/grouping, errors and hiding are covered. These tests
do not establish on-device rendering or performance.

The checks above passed, including a repeat after the cache durability change.
The final quick-to-full button handoff was rebuilt and the runtime checks passed.
The CLI applied all 16 patches to the original beta.8 input, rebuilt and signed
`E:/Morphe/Apps/NuvioTV/NuvioTV-1.1.0-beta.8-native-banner-quick-sync-signed.apk`.
`:patches:verifyNuvioPlaybackApk -PnuvioRequireStability=true` passed against this
artifact with no additional cross-DEX findings beyond the original 36 optional
library findings. Signature and ZIP alignment passed; package is
`com.nuvio.santodan`, beta.8/code 1071, and all 108 native libraries are unchanged.
Signer SHA-256 matches the existing app:
`c6f46fdf215e35e421ce61153b7fa671a485c3e266ccf03eabfe1d59c900a0d5`.
APK SHA-256 is `407836e46297f21d593b0653eaa138fdb6f9d043aa17b4eaf1455d6840811052`.
Evidence is under `E:/Morphe/.inspect-nuvio-beta8/native-banner-quick-sync-*`;
`native-banner-patched-tail.txt` confirms scope cleanup before the nullable
restart-scope branch. The build has not been installed or device-tested. The
previous unvalidated cache needs one guarded full refresh before quick startup
is eligible. No release version bump, commit, or push was made.

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
