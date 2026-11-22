# NuvioTV label customization

Optional patch: **NuvioTV - Customize labels**, for original NuvioTV **1.1.1 / 1081**
only. The runtime toggle defaults to off. Open **Settings > Layout >
Santodan-Patches > Label customization** and enable **Enable label customizations**
to use saved styles.

The label dropdown and **Text / Label** buttons share one fixed header on the left,
with the buttons to the right of the dropdown. The native RGB picker and lower
customization controls scroll below that header. The right section contains the
preview, which grows to fit the space while preserving poster/card proportions.
Apply and Cancel remain in a separate footer. The native dialog uses the active
landscape composition's dimensions and disables Android's default narrow width.

Text and background have independent color, opacity and size settings. Background
size scales padding; text size scales the native font. **Label text** opens a
single-line caption field, so New Season can be shortened to S+. Empty text uses
the original translated caption. `{value}` includes the current native value,
useful for progress, dates, remaining time and file size.

**Copy from another label** selects a source, then offers:

- **Text:** caption, text color/opacity and text size.
- **Label:** background color/opacity, background size and position.
- **All:** both groups.

Copying an unchanged dynamic caption retains `{value}` instead of freezing a
sampled number. **Reset label** restores only the selected label's native settings.
Switching labels or targets retains all drafts. **Apply** saves every edited label;
dismissing the editor discards the session, including copy and reset changes.

The initial list includes New Season, New Episode, Next Up, Upcoming, Resume,
watched progress, remaining time, air dates and file size. Metadata/rating and
technical text badges are added when Nuvio renders them; their names persist so
they remain selectable. Rating numbers stay live when their provider caption is
changed. Badges baked into images, ordinary app buttons and screen headings are
outside this text-badge patch. Labels added by separate Santodan date patches
retain their own Date label appearance editor.

Overrides are sparse: unedited properties retain each native renderer's colors,
font and translated text. Reset removes overrides. Placement uses native container
alignment where supported. Saved overrides and discovered names are included in
patch settings export/import, independently of installation package name.

Implementation uses the shared `nuvio-settings-menu` runtime. Only selecting this
public patch activates `NuvioLabelStyles.available()` and installs ten reviewed
badge scopes plus the guarded caption-field decoration. Typed wrappers delegate
to native Compose helpers, preserve register counts, clear text scopes on native
exceptions, and retain native visibility/progress selection. The shared date
preview supports bounded enlargement for this editor; the existing date editor
retains its original dimensions.

The control viewport uses original `d0.b0.a(b0, Modifier)`, inspected as a weight
of 1 with fill enabled. The editor reads Configuration from the original
`AndroidCompositionLocals_androidKt.a` local. Host tests deliberately supply
portrait Application resources with a landscape active composition, and verify
the explicit width flag, padding, header/scroll ordering, enlarged poster aspect
ratio, two screen sizes, draft persistence, copy/reset and Apply/Cancel behavior.
The caption field's native ABI is checked against the original DEX separately
from the editor fixture; actual keyboard/focus still needs TV confirmation.

The user's sync crash log showed `NoSuchMethodException: na.f2.z/19`, followed by
metadata Compose state casts and an endGroup failure. Tracking dialogs now use
the validated 1.1.1 `na.f2.B` binding. Conditional dialogs always occupy a stable
group, and installed patch settings have separate Compose groups. The latest
tracking fixture exposes B rather than the obsolete z method.

Verification tasks: `verifyNuvioLabelCustomization`, `verifyNuvioLabelsRuntime`,
`verifyNuvio111DateStyleRuntime`, `verifyNuvioSettingsBackup`,
`verifyNuvio111SettingsMenuRuntime`, `verifyNuvio111TrackingDialogs`,
`verifyNuvio111Bundle` with selections `labels,merged,combined`, and delivered
`verifyNuvioLabelsApk`, `verifyNuvio111Apk` and `verifyNuvioDateColorApk`.
The Android bundle and Desktop 2.2.0 import also pass. Combined cross-DEX checks
add no findings beyond the original's 36 optional-library findings. Earlier APKs
serve as negative checks for the obsolete sync binding and clipped dialog widths.

Current local output (all eighteen Nuvio patches, including side-by-side):
`E:\Morphe\Apps\NuvioTV\NuvioTV-1.1.1-patches-0.20.0-all-labels-large-poster-unsigned.apk`.
SHA-256: `b981d08492669da95b8fe308cc29a25001a40b730c8db8ff2f906fa4081abbbe`.
Native libraries are unchanged and 16 KiB alignment passes. The APK is unsigned;
no installation or final TV visual/keyboard/focus test was performed.

Bundle: `patches/build/libs/patches-0.20.0.mpp`. Version remains controlled by the
release process. Original SHA-256:
`1ac8aa80ad9fff7b4c21cfe4bfdfed61b4de44a120ff4b1b5743149ccb00928d`.
Inspection and verification evidence is under
`E:\Morphe\Skills\inspection-evidence\.inspect-nuvio-111`, including
`labels-native-evidence.txt`, `labels-mockup-layout-evidence.txt`,
`labels-sync-layout-device-crash.txt`, `labels-sync-layout-final.log`,
`labels-landscape-window-validation.log` and `labels-large-poster-*`.
