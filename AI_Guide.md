# SantoDan Patches

An independent patch bundle for **Morphe Desktop**, targeting Peafowl Theme Maker
`GMS_27.5.1`, package `h7.hamzio.emuithemeotg`.

Patch: **Peafowl - Unlock Theme Ownership (Experimental)**.

Ready-to-use bundle: [santodan-patches-0.1.1.mpp](dist/santodan-patches-0.1.1.mpp).
Download the binary file from GitHub, then import it into Morphe Desktop.

## Apply and test

1. Disable/remove the old `0.1.0` source, then import `dist/santodan-patches-0.1.1.mpp`
   as a local patch bundle in Morphe Desktop. Use the file under `dist`, not `build`.
2. Select the original Peafowl `GMS_27.5.1` APK and enable the patch above.
3. If you also want the general Pro features, enable Nai64's **Unlock Premium**.
4. Build and sign the APK with Morphe, then install it using your usual process.
5. Open a theme that previously showed `RE_ISOWNED`. Check the normal theme action,
   theme download/export/apply, and repeat after restarting the app.

Pairip Bypass and Free In-app Purchases are not dependencies. Start from the
original APK for each build; this patch deliberately rejects an already modified
theme initialization. Preserve your current Morphe signing key if you want to update an
installation signed with that key.

Morphe should report `Applied: Peafowl - Unlock Theme Ownership (Experimental)`.
The new build logs `SantoDan 0.1.1: routed theme initialization through the existing free-theme path`.
An unsupported layout raises an error instead of silently succeeding.

## Build

The build is offline and uses Java's Morphe API interop. It compiles against your
Morphe Desktop all-in-one JAR, which already contains the patcher, Kotlin runtime,
and dexlib dependencies. No Gradle, GitHub credentials, or Android SDK is required.

From this folder in PowerShell, using the existing sibling JDK and Morphe JAR:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -TestDex ..\Peafowl\classes.dex
```

For another installation, supply paths explicitly:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -MorpheJar 'C:\Tools\morphe-desktop-all.jar' -JdkHome 'C:\Tools\jdk' -TestDex 'C:\APKs\Peafowl\classes.dex'
```

The execution-policy option applies only to that build process; it does not change
your system's PowerShell policy. Use JDK 17 or newer, capable of reading your Morphe JAR's class version. This
project was built with JDK 26.0.2 and Morphe Desktop 1.15.1-dev.4 / Patcher 1.9.0.
The output is an MPP with JVM bytecode. **It is not an Android Morphe bundle**:
on-device patch loading would additionally require a DEX build of the patch code.

## Scope and implementation

Version 0.1.0 modified the successful customer-info callback. Device testing exposed
`RE_ISOWNED`: the preceding theme-offerings request can fail before that callback
ever executes. Version 0.1.1 replaces that strategy.

The matcher now finds the theme initializer's `sku.equals("free")` decision, its
paid-theme boolean, the RevenueCat offerings branch, and the alternative
`Handler.post(Runnable)` free-theme setup. It requires exactly one matching layout
in `ThemePreviewActivity`; the obfuscated method name is not hard-coded.

One `move-result` becomes a same-width `const/4 <original-register>, 1`. The existing
code clears the paid-theme flag and takes the free-theme branch. This preserves
the original SKU, theme data, and asynchronous UI setup while skipping the theme
offerings/customer-info preflight entirely. It does not merely hide its error.
Register counts, branch offsets, exception-handler layout, and other classes are
preserved. There are no resource edits or app extensions.

This changes local theme access. It does not create a Google Play purchase,
change RevenueCat account records, or supply server-protected theme downloads.
Other network operations remain. Device testing is required to establish whether
the complete download/apply flow works.

## Verification

`build.ps1 -TestDex ...` checks the real input DEX for a unique match, rejects
unrelated/ambiguous/changed/already-patched input, verifies one equal-width
replacement, and writes/reloads a patched DEX. The regression check interprets the
actual classification instructions for both paid and free SKUs: both must skip
the offerings request and retain the native asynchronous theme setup. It also
loads the resulting bundle with Morphe's `list-patches` command.

Version 0.1.1 was additionally applied alone to the original APK using Morphe's
default `STRIP_FAST` mode. Patching and rebuilding passed. That unsigned build is
an inspection artifact under `build/verification-0.1.1`, not an installable release.

Only `src/main/java` is packaged. Test code, the original APK, the Morphe JAR, and
build dependencies are not redistributed in the bundle.
