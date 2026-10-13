---
name: morphe-create-patch
description: Implement or modify bytecode and resource patches in santodan-patches using proven target-app contracts and current Morphe APIs.
---

# Implement a Morphe patch

Read the guide's Start here section and relevant app notes. Require a verified
original input/version and native hook evidence; load `morphe-inspect-app/SKILL.md`
if those are missing. Work in `E:\Morphe\santodan-patches`.

- Target the latest requested-app version available locally. Preserve older branches
  without adding mappings, hooks, compatibility entries, or tests to them.
- Read the closest implementation and its verifier. Java patches live in
  `patches/src/main/java/santodan/patches`; Kotlin resource patches live in the
  corresponding Kotlin directory. Match existing public getter/property discovery.
- Confirm API availability from `settings.gradle.kts`, `gradle/libs.versions.toml`,
  dependency sources, and working repo examples. Use `app.morphe.patcher`; do not
  copy unsupported ReVanced or newer reference APIs by assumption.
- Declare exact compatibility and validate required native contracts. Find a unique
  structural match and reject missing, ambiguous, incompatible, or already-patched
  layouts before mutation. Do not blindly select the first matching method.
- Keep edits narrow. For bytecode, account for register types and wide values,
  invoke register limits, move-result adjacency, branch labels and exception regions.
  Use actual mutable-method APIs. Avoid changing register counts without updating
  parameter references. Verify inserted/replaced code through DEX write/reload.
- For manifest/resource changes, use structured XML edits. Side-by-side identity
  requires inspecting app-owned permissions, provider authorities, task affinity,
  launcher activity/alias labels, and resources/providers added by other patches.
  Do not rename third-party identifiers indiscriminately.
- Preserve native authentication, error handling, navigation, and unrelated flows.
  If additional runtime behavior is required, load `morphe-runtime-extension/SKILL.md`.
- Update docs for the changed behavior. Do not manually bump release versions or
  fabricate generated patch metadata. Never commit or push unless requested.

Load `morphe-verify-patch/SKILL.md` before declaring completion. Report unsupported
input clearly; a patch must not log success if its intended hook was not applied.
