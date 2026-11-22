# NuvioTV date-label editor

Target: `com.nuvio.tv` **1.1.1 / 1081** only.

Press **Settings > Layout > Santodan-Patches > Date label appearance** to open
the custom editor directly. This entry is a normal clickable row.

A horizontal **Text | Label** button row sits at the top of the editor, above
the color picker and preview. A caption identifies the selected part.

Inside the same menu:

- Select **Label** to change its background color, opacity and size.
- Select **Text** to change its color, opacity and font size.
- Use **Label position** to choose the position in a native picker.
- Use **Reset appearance** to reset the draft to the default style.
- Use **Switch to Card / Switch to Poster** for the independent artwork preview.

Switching between Label and Text keeps both drafts and the same sample movie.
The preview combines both targets' draft colors and sizes with the draft
position. Size retains 50-200% in 10% steps; opacity supports 0-100%.
Apply saves both targets and position together and closes the menu. Cancel/Back
discards the whole draft. Reset also waits for Apply. Reopening starts with saved
values. Native remembered RGB state is recreated on target changes, reset and
reopening so a previous target's color cannot replace the current selection.
The selected part controls the color picker and its saved color, opacity and size.
Opacity and size sit first in the native color-picker column, below the picker.
The Text/Label selector stays in the top row. Position and reset follow the
sliders. All editing controls stay outside the preview. The preview contains only
the sample artwork/date label and its independent Poster/Card view control.
The editing-controls area uses native vertical scrolling.

Existing preference keys, settings export/import and shared styling across
airing-series, finale and movie-release date labels remain in use. Preview
artwork comes from already loaded movies, without new catalog requests.
Ordinary theme editing retains its original preview and Save Theme.

## Native evidence

Original: `../OriginalApps/NuvioTV-1.1.1.apk`, SHA-256
`1ac8aa80ad9fff7b4c21cfe4bfdfed61b4de44a120ff4b1b5743149ccb00928d`.
All five retained DEX files match the original byte-for-byte.
Evidence is in `../Skills/inspection-evidence/.inspect-nuvio-111/`:

- `date-controls-native-evidence.txt`: the native color-picker call and its sibling preview, including register lifetimes.
- `date-editor-native-evidence.txt`: native percentage row and custom editor.
- `date-menu-native-evidence.txt`: settings row, position picker and custom editor.
- `date-menu-scroll-evidence.txt`: `x.m.r(Composer):x.k2` remembers native scroll state; `x.m.s(Modifier,x.k2,boolean):Modifier` provides native scrolling. Its orientation is passed as vertical. `date-menu-scroll-orientation.txt` proves that true selects the enum value named Vertical.

The exact full references are shared by runtime reflection and patch validation.
`gb.hd.t` is a focus modifier, not a scrolling helper; the editing-controls area uses `x.m.r/s`.

The 1.1.1 `gb.x0.b` editor is guarded by its exact original method fingerprint.
The target-row hook is the first child of the editor's outer column, before
the native body: original code offset 561, immediately before its first
`w1.b.j` alignment read. It reuses dead materialized-modifier register 7 for
the live model in 33, with composer 14; the original following sget restores 7.
`d0.v1.a` and static `w1.b.j` provide the horizontal Row policy/alignment.

The controls hook follows `gb.jg.b` in the color-picker column, before the native
button and sibling preview. Its reviewed call uses registers 10-16: composer 15
and a null modifier in 14. The hook reuses dead register 14 for the live color
model in 33 and calls `renderEditorControls(model, composer)`. The original next
two instructions restore the composer into 14 before any later use. Register
counts, branches and unrelated theme rendering remain intact.

## Verification

`verifyNuvio111DateStyleRuntime` rejects the collapsible entry point and exercises
one-click opening, a top horizontal Text/Label row, target switching, native remembered RGB state,
combined live preview, position, reset, Apply/Cancel, reopening and state recreation.
It covers 0%, 30%, 50% and 100% opacity, stable movie artwork, Poster/Card switching
and balanced Compose groups. The fixture rejects any slider or editing action
rendered during preview composition; ordinary theme editing gets no extra controls. `verifyNuvio111Apk` requires the delivered selector,
opacity controls, scrolling references, the target hook before the native picker,
and a controls hook immediately after the
native color picker, before the sibling preview. The existing date-color audit checks
native preview hooks, RGB-only construction and scoped Apply behavior.

Device checks remain for D-pad focus, layout/scrolling and Back on the TV.

Validation passed with Java 21, SDK 36 and the offline Gradle cache:

```powershell
.\build-local.ps1 :patches:buildAndroid :patches:verifyNuvio111 :patches:verifyNuvio111DateStyleRuntime --offline --continue
.\build-local.ps1 :patches:verifyNuvio111Bundle :patches:verifyNuvioMaintenance :patches:verifyAndroidBundle :patches:verifyMorpheDesktopImport '-PmorpheDesktopJar=E:/Morphe/morphe-desktop-2.2.0-all.jar' --offline --continue
```

The final slider-order refinement passed `buildAndroid`,
`verifyNuvio111DateStyleRuntime`, Android bundle validation and Desktop import.
The delivered APK passed `verifyNuvio111Apk`, `verifyNuvioDateColorApk`,
`verifyNuvioPlaybackApk` and `verifyNuvio111HomeTransition`, with their APK
properties targeting the file below and `-PnuvioRequireStability=true`.
All 108 native libraries match the original, 16-KB alignment passes, and
cross-DEX checks add no findings beyond the original 36 optional-library findings.

Evidence: `date-target-row-build.log`, `date-target-row-validation.log`,
`date-target-row-final-build.log`, `date-target-row-apk-validation.log` and
`date-target-row-all.audit.json` in the same 1.1.1 inspection directory.

- Bundle: `patches/build/libs/patches-0.20.0.mpp`.
- APK: `../Apps/NuvioTV/NuvioTV-1.1.1-patches-0.20.0-all-date-target-row-unsigned.apk` (all seventeen patches).
- Bundle SHA-256: `308fa0358e454f629a198e9af4fbe97bbdc54367e423567af8c9b55e92e4e38a`.
- APK SHA-256: `654cca5ea60eaf4b5547636ee46fb24eedf26f6da79215235dfce1ce304b5093`.

The APK is unsigned and was not installed or device-tested. No push, release-version change or publishing workflow was performed for this change.
