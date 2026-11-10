# Car Scanner 2.2.42

Latest original: `../OriginalApps/Car_Scanner-2.2.42.apk`, pulled from the installed
`com.ovz.carscanner` on `emulator-5554`. Manifest confirms **2.2.42 / 52002042**,
minSdk 23, targetSdk 36. This APK contains **x86_64 only**. It does not establish
compatibility with an arm64 build of 2.2.42.

APK SHA-256: `a068f95d73d5a4902c0a13fc93fa313f71fa4ffaa30f5a9ac0f87dd70a842c73`.
Evidence: `E:/Morphe/Skills/inspection-evidence/.inspect-carscanner-2.2.42`.

## Patch choices

Select exactly one:

- **Car Scanner - Premium status** adds exact 2.2.42 support. Existing 2.1.50
  and 2.2.21 implementations remain unchanged.
- **Car Scanner - Premium status (Experimental)** has no version-name restriction.
  It retains the legacy store method, the 2.2.21 managed/arm64 method, and the new
  2.2.42 managed/x86_64 method. Archive layout selects the implementation, while
  exact native/store hashes reject unknown or already modified resources. This
  is not an automatic port to future app binaries: changed images need review.

All required transformations are calculated before writing any resources.
Neither patch writes purchase records or modifies a server account.

## Reviewed contracts

`CarScanner242` validates the following original images before mutation:

| Image | SHA-256 |
| --- | --- |
| `libassembly-store.so` | `c335a31f92a2e65d33cee39841c57214d3bded10f1d54d0921b04c22dbcff1ad` |
| decoded `CarScannerMaui.dll` | `13ed668f0e5c13228fbdbe8c93a4defd45b79bcecc62500f020003121cdd5801` |
| `libaot-CarScannerMaui.dll.so` | `1647f3bc6e1f781dbaaae7037d88cbf97742b4729917cde5a388902567183b83` |
| `libusbtransport.so` | `b7d613318de8e9c235cdbcc49aa2af8b3757919aabfbf0341caa03576850c47c` |
| `libvndctx.so` | `1200790584238e24822b4075cd1f768f50932ae4ba99aa852a758f3d7fdd4c7d` |

The store is XABA `0x80030003` at ELF offset 16384, with 216 descriptors and
5616 index bytes. Descriptor **192**, XALZ index **123**, identifies the shared
assembly at file offset 9831424, compressed length 25113624, decoded length
40799232. The new store format differs from 2.2.21's `0x80010003`.

Independent System.Reflection.Metadata inspection confirms:

- `SharedSettings::get_AdsProductPurchased`, token `0x060015dc`, instance bool(),
  signature `20 00 02`, RVA `0x9f518`, file header `0x9d718`, 124 IL bytes.
  It reads the native nullable purchase status and license finish date.
- `OBDDataReader/<>c::<Initialize>b__181_0`, token `0x06002901`, instance
  bool(string), signature `20 01 02 0e`, RVA `0x1ab2b0`, header `0x1a94b0`,
  115 IL bytes. It compares the supplied certificate with the native signature
  allowlist. Both IL bodies become `ldc.i4.1; ret` with unreachable NOP padding.

Mono AOT format 187 has method-address table VA `0x1819098`, with five-byte CALL
entries. Getter slot file `0x1817ddf` targets VA `0x206480` / file `0x202480`;
predicate slot file `0x181dd98` targets VA `0x65d5c0` / file `0x6595c0`.
Both compiled bodies become `mov eax,1; ret` before stack changes (12 bytes total).

The failure scheduler at VA/file `0x2c170` creates a pthread at `0x2c800`, which
sleeps then calls `_exit(0)` at `0x2c860`. Its first byte becomes `ret`; JNI,
validation, transport and the worker body remain unchanged.

Decoder variant 1 converges at `0x225ba`. Replacing its six-byte multiply with
`mov ecx,0x162f2a9e; nop` reproduces the normal multiply result. The following
native XOR produces zero, and the original mixing stores normal seed zero on
all validation paths. This covers settings reads and writes. Variant 2 selects
`0xec225c72` instead of failure constant `0x9dac8f86`; `cmovne ecx,eax` at
`0x2573d` becomes `mov eax,ecx; nop`. Its original mixing also yields seed zero.
Only nine decoder bytes are in the mutation regions; transforms and bounds stay
native. Both variants must remain covered to avoid the previous settings reset.

## Verification

Use Java 21 and the workspace Android SDK:

```powershell
.\build-local.ps1 :patches:buildAndroid :patches:generatePatchesList :patches:verifyCarScanner242 :patches:verifyCarScanner242Bundle :patches:verifyAndroidBundle
```

The host verifier checks managed mutation scope, all 215 other assemblies, ELF
relocation/bounds, exact native mutation scope, settings byte round trips and
rejection of changed/already-patched resources. The bundle verifier discovers
and applies each alternative independently to the original and checks its output.
Historical verifier fixtures and implementations are unchanged.

Device UI, restart/settings persistence and real ECU connectivity on 2.2.42
require separate validation. No APK was signed or installed.

### Completed checks

`buildAndroid`, `generatePatchesList`, `verifyCarScanner242` and
`verifyCarScanner242Bundle` passed. Both alternatives apply independently.
The unchanged `verifyCarScanner221Bundle` passed. The optional unchanged
`verifyCarScannerManagedBundle` could not run because its original
`OriginalApps/Car-Scanner-ELM-OBD2_2.1.50.apk` is absent; no replacement older
fixture was created. Separate `verifyAndroidBundle` and `verifyMorpheDesktopImport`
runs passed with `-PmorpheDesktopJar=E:/Morphe/morphe-desktop-2.1.1-all.jar`.
Desktop imported all 38 patches across 9 apps. `git diff --check` passed.

Bundle: `patches/build/libs/patches-0.17.0.mpp`, SHA-256
`a92d24c686d257eb5df0a99f2357138ed0e60db892323e0574b65cb5125a238e`.
No release version was changed.

Desktop 2.1.1 rebuilt each alternative with `--exclusive --unsigned`:

- `../Apps/Car-Scanner-ELM-OBD2/Car-Scanner-2.2.42-premium-unsigned.apk`
- `../Apps/Car-Scanner-ELM-OBD2/Car-Scanner-2.2.42-experimental-unsigned.apk`

Both APKs have SHA-256
`b5e90cf30bf2cb6ed3a5038bfa186b3617da7ace2ff2338a9eb2fc213c55ffb3`.
The delivery audit compared every non-signature entry with the original: only
the four reviewed native libraries change. Manifest, both DEX files, assets and
all unrelated libraries are unchanged; no archive entries were added. Each
modified library matches its corresponding bundle-verifier output byte for byte.
An independent metadata reader reloaded both patched IL bodies, and LLVM objdump
reloaded the native decoder. Evidence logs and `apk-audit.json` remain in the
canonical 2.2.42 evidence directory. These are host/rebuild results, not device
or ECU test results.
