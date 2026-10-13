---
name: morphe-runtime-extension
description: Add or modify injected Java/Kotlin runtime modules, native UI hooks, and shared settings for Santodan Morphe patches.
---

# Extend native runtime behavior

Use only when the patch needs runtime code beyond direct bytecode/resource edits.
Read the guide's Start here section and the relevant existing feature contracts.
Require latest-target DEX evidence from `morphe-inspect-app`.

1. Inspect a nearby extension's `build.gradle.kts`, source, and patch integration.
   Use `extensions/<feature>/src/main/java/`, its namespace and `.mpe` naming pattern.
   Confirm how the current Gradle plugin discovers modules. Follow the actual patch's
   extension merge/dependency calls; creating a source folder does not bundle it.
2. Validate every native method, field, constructor, and continuation contract used
   by the extension. Avoid speculative reflection fallbacks or new older-version
   dispatch branches. When modifying a shared extension, preserve existing dispatch.
3. Reuse native repositories, sessions, navigation, profile state, and controls where
   appropriate. Keep optional features opt-in with persisted settings and a working
   disabled path. Nuvio settings integrate with the shared unnamed settings dependency;
   independently selectable patches must work alone and together.
4. Keep network/file/database work off UI/composition callbacks. Bound queues, caches,
   timeouts, and retries. Coalesce repeated events and isolate active-profile data.
   Observe with native lifecycle and cancel/dispose observers and consumers correctly.
   Await native suspend completion and propagate errors; invocation is not completion.
5. For Nuvio Compose hooks, follow this guide's current composer/group, native sizing,
   focus, modifier, and style contracts. Do not infer packed Color/TextUnit values or
   R8-erased default arguments. Close groups on early returns. Use distinct group keys
   and verify independent patch combinations when shared settings are affected.
6. Add focused host-runtime checks for observable behavior such as disabled work,
   profile switches, duplicate requests, cancellation, and failure completion. Validate
   packaged extension classes and native references against the real target DEX.

Use `morphe-verify-patch` to build and apply the resulting bundle. Record remaining
device checks for UI focus, lifecycle, persistence, and native integration.
