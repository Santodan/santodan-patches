# Pillo 0.6.27

All five Pillo patches support `xyz.rtrvr.pillo` **0.6.27**, version code **472**:
hybrid notifications, local backup/restore, JSON weight import, weight summaries,
and weight in Progress. Progress includes summaries.
Historical app-version support remains separate.

Original input: `E:/Morphe/OriginalApps/pillo-0.6.27.apks`, exported by the user
from Google Play. Its base manifest confirms the package/version above, and its
nine root DEX files are byte-identical to the initially inspected APKPure build.
Inspection is in `E:/Morphe/.inspect-pillo-627-play/`. Preserve
`split_config.arm64_v8a.apk`, `split_config.en.apk`, and `split_config.xxhdpi.apk`
when repackaging. The original APKPure XAPK supplies only ARMv7 libraries and
cannot install on the ARM64-only emulator configuration. No older patched APK
supplies resources or executable code for this port.

## Native hook contracts

`Pillo627*` Java contracts and `*627` runtime classes isolate the new R8/Compose
ABI. Exact reflection contracts are in `Pillo627Contracts.java` and
`Pillo627Import.java`; dispatch is restricted to the exact package/version.

- **Notifications:** unique `ui/alarm/process/StartAlarmProcess.invoke(Context,
  String,models/alarm/Alarm,String,Continuation):Object`. The foreground decision
  is at instruction 565; its light-route branch targets 602 and invokes at 607;
  fullscreen invokes at 586. Both use context v3. Replace the predicate with
  `shared/android/PowerManagerUtil.INSTANCE.isTrulyInteractive(Context):Z` and
  invert the branch without changing registers, widths or exception regions.
  Select Pillo's Banner/Light mode.
- **Backup lifecycle:** run before `PilloApp.attachBaseContext` locale reads and
  at the final normal-process return of `onCreate`. Preserve its new crash-report
  early return; restore runs only in the main process. The audit scheduler now
  lives under `domain/audit`. Database/preferences remain `pillo.db` and
  `pillo_preferences`. Current Room identity:
  `0b0400f334f4d39ec5f093b3c677e070`, schema version 53.
- **Backup choices:** Settings uses static
  `SettingsOtherEntriesKt.SettingsOtherEntries$lambda$0$2$2$0(Function0,Context,
  ComponentActivity,Function0):Unit` with parameters v17-v20. Range invocation
  preserves this layout and early return uses `Unit.a`. Onboarding's
  `rememberChooseRestoreDataOption(Function0,Function0,g3.q,int)` returns
  `ui.compose.BottomSheetController`. Capture the original callbacks before p0
  is reused for the result; retain native Medisafe/account restore routes.
- **Weight import:** redirect `LoggingBottomBar` in
  `TrackerRecordLogLayoutWeightKt.LogWeightLayout_Uww_Ezs$lambda$1`. Compose uses
  `g3.q`/`s3.s`; use native `ButtonM-vRFhKjU` and `SpacedColumn-DzVHIIc` defaults.
  The repository moved to `data.repository.tracker.TrackerEventRepository`.
  `shared.coroutine.FlowExtensionsKt.firstOnIO(mw.l,Continuation)` provides
  initial records; singleton fields are `a`, resumed failures use `ResultKt.b`.
  Preserve async completion, units, profiles, duplicates and partial retries.
- **Summaries:** `jv.e.s1(int,List)` reverses the old takeLast argument order.
  Keep full history in the list wrapper. The filter button moved into
  `WeightChartContent$lambda$7$0`. Wrap the static bridge to
  `WeightLineChart_TN_CM5M$lambda$12$0`, capturing its explicit unit/record state
  and restoring prior rendering context in `finally`. Redirect its native
  Latest/Avg renderers. Last since uses the native filter button/date picker.
  Date comparisons use native timestamps' ISO representation and device calendar.
- **Progress:** replace the single 12-register `z0.l.c` call in
  `ProgressScreenKt.ProgressScreen$lambda$5`; append chart and dated entries to
  the same lazy list. Native retained viewModels factory/store/extras suppliers
  and `androidx.lifecycle.j1` resolve the Hilt reading VM in the host store.
  `g3.q0.c` owns disposal and `jw.q1.b(CancellationException)` cancels once.
  Observe selection with `ComparisonsKt.t`; never pass the empty-ID sentinel to
  the chart. Use `ViewTrackerReadingListContent$lambda$4$0$1$0` for native items.

## Verification

Run `verifyPillo627Bundle` and `verifyPillo627Runtime` against the original Play
base, then `generatePatchesList`, `verifyAndroidBundle` and Desktop import with
`-PmorpheDesktopJar=../../morphe-desktop-1.19.1-all.jar`. The bundle checks individual
selections and all five together, original contracts, rejection cases, cross-DEX
reload and bundled signatures. Runtime checks exercise observer disposal, profile
selection, renderer cleanup, summaries and asynchronous weight imports.

Bundle: `patches/build/libs/patches-0.15.0.mpp`. No release version was bumped.
Historical support remains unchanged. No new signing or installation was performed.
