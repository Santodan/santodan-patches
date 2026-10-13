---
name: morphe-inspect-app
description: Inspect original Android app inputs and prove native hook contracts before creating or changing Santodan Morphe patches.
---

# Inspect the target app

Read `E:\Morphe\santodan-patches\AI_Guide.md`'s Start here section and the relevant
app notes. Check applicable AGENTS.md and repository status. Paths in this skill
are relative to `E:\Morphe`.

1. List originals in `OriginalApps/` and the workspace root. Select the latest
   available version of the requested app, confirming manifest package/version.
   Preserve older support without extending it. `Apps/` contains outputs, not originals.
2. Locate matching `.inspect-*` artifacts. Confirm their provenance against the
   chosen input; extract/decompile to a separate inspection directory if necessary,
   using tools actually available. APKM/XAPK may require multiple splits.
3. Search strings, call sites, and native behavior with `rg`; read focused class/method
   ranges. Trace inputs, outputs, state ownership, and lifecycle, including the disabled
   path. Decompiled Java alone does not prove the original DEX instruction layout.
4. For each proposed hook, record class descriptor, method descriptor, static/instance
   status, parameter/return types, field/constructor contracts, matching instruction
   sequence, register sources, and relevant branch/try boundaries. Show why the match
   is unique across all relevant DEX files. Check against the closest repo matcher.
5. Confirm runtime-facing symbols against the original DEX, not older mappings or
   upstream source that may differ after R8. Prefer native repositories and routes.

Produce a compact evidence note: original path, package/version, proven contracts,
intended mutation, candidate source/verifier files, and unresolved questions.
If evidence is missing, request the specific artifact or tool needed rather than
inventing a hook. Continue independent investigation where possible.

Next load `morphe-create-patch/SKILL.md` from the same skills root. For the same
task, carry the evidence note forward rather than repeating full inspection output.
