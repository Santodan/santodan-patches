---
name: morphe-verify-patch
description: Build and verify changed Santodan Morphe patches against original app DEX and packaged bundles, then review the diff and report evidence.
---

# Verify a patch

Work from `E:\Morphe\santodan-patches`. Read the guide's build and verification
instructions. Verify only what actually ran; distinguish host tests from device tests.

1. Inspect `patches/build.gradle.kts` and relevant verifier source for actual tasks,
   inputs, default versions, and outputs. Java verifiers are `JavaExec` programs;
   `:patches:test` alone does not run them. Do not invent task names.
2. Check Java 21 and configured Android SDK/platform 36. Use `build-local.ps1` to
   load ignored credentials without displaying them. If credentials or fixtures are
   unavailable, report the exact missing prerequisite and complete unaffected checks.
3. Run `.\build-local.ps1 :patches:buildAndroid`. Run
   the relevant feature's `verify*` tasks. Existing multi-version regression tasks
   may be run unchanged; do not extend older-version tests or hooks unless requested.
4. For matcher changes, verify unique real-input matching, changed/ambiguous/missing
   and already-patched rejection, intended mutation scope, and DEX write/reload.
   For runtime changes, exercise observable failures/lifecycle as well as success.
   Add meaningful checks where needed rather than tests that repeat source text.
5. Use existing bundle verification to load/apply the built `.mpp` to the original
   input, check merged extension classes and native references, and rebuild where
   supported. Preserve required APKM/XAPK splits. Inspect local Morphe CLI help
   before issuing commands. Do not substitute an older patched APK as the input.
6. If metadata changed, run
   `.\build-local.ps1 :patches:generatePatchesList` and review generated changes.
   Inspect the actual `.mpp` under `patches/build/libs`; do not manually bump versions.
7. Run `git diff --check`, `git diff --stat`, and inspect the final diff plus new files.
   Confirm user changes remain, no credentials/original apps are added, and no new
   older-version support slipped in. Do not commit or push unless asked.

Report the target package/version, changed behavior/files, exact commands and their
results, bundle path, and remaining checks. Explicitly state whether the app was
rebuilt, signed, installed, or tested on a device. Do not install without a request.
Signing verification, manifest collision checks, and cross-DEX validation are
required when relevant to the changed patch; a compiled bundle alone is insufficient.
