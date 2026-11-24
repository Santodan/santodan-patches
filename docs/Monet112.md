# Monet 1.1.2 (premium)

## Target and scope

Original: `../OriginalApps/monet-1.1.2.apk`; manifest package `com.klevico.monet`,
version 1.1.2, code 136, minimum SDK 26, one `classes.dex`.
SHA-256: `bce58185ad51864a92c356650c35453fd12b506b18b45427eba2f5ea32686ffc`
(`classes.dex` `52a570a7fdd009edffbd95852a607004cc4b29caf6fa5e654847b848968eb2a5`).

- **Monet - Unlock premium** now accepts 1.1.2, retaining 1.1.1 / 1.1.0 / 1.0.97.
- **Monet - Unlock premium (Experimental, any version)** keeps every layout it
  accepted before and is now verified on 1.1.2. Its matcher was also widened to
  tolerate more of the drift kinds R8 can introduce (see below).
- **Unlimited displayed cards** and **Individual Nuvio catalog rows** are unchanged
  and still target 1.1.1 at the latest. Their exact native UI contracts were not
  ported to 1.1.2. Select only one premium alternative.

## Native contract (unchanged semantics)

Evidence: `../Skills/inspection-evidence/.inspect-monet-1.1.2/{manifest,billing}.txt`.

- Evaluator: unique public instance `La/dz5;->a()Z`, four registers, no try blocks.
  It has the same 13 instructions and offsets as 1.1.1 (`La/ov5;`). Fields are loaded as `b,a,c,d`, and
  the formula is `b || (a && (c || !d))`. Index 11 (code address 19) is
  `const/4 v3, 0`. Only that literal becomes `1`.
- Billing owner: `La/n70;-><init>(Landroid/content/Context;La/zu0;)V` holds
  `billing_prefs` and `license_blob_v1`. It is the only site that invokes
  `La/dz5;-><init>(ZZ)V`. That constructor stores `a`/`b` and seeds `La/jl7;`
  through the evaluator. The license refresh is `La/dz5;->c(Z)V`: it stores its
  parameter in `b`, then re-evaluates. It is called from `La/n70;->h(String)Z`
  and `La/w60;`.
- R8 merged unrelated code into `La/dz5;` (a `(String,String,Z)` constructor,
  `b()Z`, and outside reads/writes of `c`/`d`). Do not fingerprint its full field
  or method list.

## Future-compatibility hardening

Across 1.0.97 → 1.1.2, the evaluator semantics, billing keys and holder lifecycle
stayed stable. What changed were names, the `final` flag and the descriptor. The
shared matcher (`MonetPremiumPatch.findUnique` / `semantics`) now proves identity
by behavior and ownership only. It accepts everything it accepted before, plus:

- any visibility (public, private, protected or package) with optional `final`.
  Static, synchronized, native and abstract methods are still rejected. Callers may use `invoke-virtual` or
  `invoke-direct` (including range forms).
- the four owned boolean fields loaded in any order, or lazily between branches.
  The truth table is checked against `L || (A && (X || !Y))` with roles bound to fields
  in any order, rather than by load position.
- several or shared return blocks. Every returned value must come from a `const/4 0|1`;
  every such false literal is patched (same width and register). Branching on a
  constant, returning a field, loops, dead code and other opcodes are rejected.
- the storage keys anywhere in the billing owner class, not only in its constructor,
  and holder constructors with one to four boolean parameters.

It also adds one semantic tie-in. The holder's `(Z)V` license refresh must store its
parameter in the field that alone grants premium (`L`). If a refresh stores another
evaluator field, the candidate is rejected.

The regular patch keeps exact compatibility. `MonetPremiumPatch.REVIEWED` maps each
version to its reviewed descriptor, and the targets, the version gate and the bundle verifier
all derive from it. A future release therefore needs one map entry and `LATEST`, after
`verifyMonet` passes on the new original. The experimental alternative needs no code change
while the native contract keeps this shape. It still fails closed if Monet changes its
entitlement logic, inlines the evaluator, or moves the billing keys or refresh.

## Verification

Run with `build-local.ps1 ... --offline`:

- `:patches:verifyMonet` (1.1.2; args: hook `La/dz5;->a()Z`, owner `La/n70;`,
  license field `b`). It also checks the new drift variants (private/final, reordered loads,
  lazy loads, shared return), as well as field-return and conflicting-refresh rejection.
- `:patches:verifyMonet111`: the same verifier on the retained 1.1.1 contract.
- `:patches:verifyMonetBundle` (1.1.2, both alternatives from the built MPP).
- The 1.1.1 combined checks `verifyMonetDisplayedCards111(Bundle)` and
  `verifyMonetCatalog111(Bundle)` exercise the widened matcher with cards/catalog.
- `:patches:buildAndroid`, `:patches:generatePatchesList`,
  `:patches:verifyMorpheDesktopImport`, `:patches:verifyAndroidBundle`.
