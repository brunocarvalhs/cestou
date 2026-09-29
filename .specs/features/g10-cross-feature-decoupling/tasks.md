# Cross-Feature Module Coupling (G10) Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its
Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is
the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review,
Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/g10-cross-feature-decoupling/design.md`
**Status**: T3 decided — **`AD-011`** in `.specs/STATE.md` (2026-09-29): segregated `ProductReader`
contract in `core/domain` (`feature/products`'s `ProductRepository` extends it) + a separate
`ShareShoppingUseCase` interface in `core/domain`, implemented in `feature/products`. T1 and T2 are
already implemented (see their branches below). **T4–T7 are unblocked** — `android-engineer-architecture`
implements them against `AD-011`'s shape, not the old Option (a)/(b) framing.

---

## PR plan

Five PRs, matching this project's existing process (`.specs/MVP-ROADMAP.md`: "each item ships as
its own branch + PR, never committed or merged directly") and mirroring the five tasks already
scoped in `.specs/G10-DESIGN-PASS.md` (G10-T1..G10-T5).

| PR | Branch | Purpose | Requirement | Ships alone? |
| -- | ------ | ------- | ------------ | ------------- |
| PR1 | `feat/g10-settings-repository` | Promote `SettingsRepository` to `core/domain`/`core/data`, delete `GetSettingsUseCase` | G10-01 | Yes — independent of every other task |
| PR2 | `feat/g10-scanner-core-ui` | Move `CameraPreview`/`QRCodeOverlay` (+ their instrumented test) to `core/ui` | G10-02 | Yes — independent of every other task |
| PR3 | `feat/g10-share-shopping-boundary` | Implement whichever option `tech-lead` records in `AD-0xx` for `ShareShoppingUseCase` | G10-03 | **Blocked** until `AD-0xx` exists — otherwise yes, independent of PR1/PR2 |
| PR4 | `feat/g10-cart-dependency-audit` | Drop stale `feature/cart` module dependencies | G10-04 | No — needs PR1+PR2+PR3 merged first (re-grep must see the post-move import graph) |
| PR5 | `feat/g10-wear-hilt-reverify` | Re-verify `:wear`'s Hilt graph; remove `WearSubscriptionModule`'s stub only if provably dead | G10-05 | No — needs PR4 merged first |

PR1 and PR2 have no dependency on each other or on PR3 and can land in any order or in parallel.
PR3 is independent of PR1/PR2 except for the `AD-0xx` gate. PR4 requires PR1+PR2+PR3 all merged
(matches `G10-DESIGN-PASS.md`'s "Dependencies: T1, T2, T3 (run last)" for this exact task). PR5
requires PR4 merged.

---

## Test Coverage Matrix

> Generated from codebase sampling (`item-add-authorship/tasks.md`'s matrix as the most recent
> precedent) and this project's `./gradlew` module layout. Guidelines found: none dedicated to
> testing standards beyond the existing per-module `test`/`androidTest` split and Kover baseline
> documented in `.specs/COVERAGE-BASELINE.md` — strong defaults applied, floored by the existing
> per-layer test depth already in this codebase.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------- | ---------------------- | ------------------ | ------------- |
| Domain / repository interface (`SettingsRepository`) | unit | Existing test suite ported as-is; no new branches introduced by a pure relocation | `core/domain/src/test/**/repository/SettingsRepositoryTest.kt` (if one exists at the old location) or none if the interface has no logic of its own | `./gradlew :core:domain:test` |
| Data / repository impl (`SettingsRepositoryImpl`) | unit | Existing test suite ported as-is | `core/data/src/test/**/repository/SettingsRepositoryImplTest.kt` | `./gradlew :core:data:test` |
| Presentation / consumers (`MainViewModel`, `ShoppingListViewModel`, `CartAssistantUseCase`) | unit | Existing tests updated to inject `core/domain`'s `SettingsRepository` instead of `GetSettingsUseCase`; no new test cases required by a pure DI-source swap | `app/src/test/**`, `feature/shopping/src/test/**`, `feature/chat/src/test/**` | `./gradlew :app:test :feature:shopping:test :feature:chat:test` |
| Presentation / Component (`CameraPreview`, `QRCodeOverlay`) | instrumented (androidTest) | `CameraPreviewExecutorLifecycleTest` (G11 regression) moved and passing from its new location; unverified-noted if no device is available | `core/ui/src/androidTest/**/scanner/CameraPreviewExecutorLifecycleTest.kt` | `./gradlew :core:ui:connectedAndroidTest` (device required) |
| Domain / Use Case (`ShareShoppingUseCase`, whichever option) | unit | Existing `ShareShoppingUseCaseTest` ported with the same test count, no silent deletions | wherever the chosen option's `design.md` addendum places it (`core/domain` for Option a's use cases, `feature/products` for Option b's impl) | `./gradlew test` (module TBD by chosen option) |
| Build / Gradle config (`feature/cart/build.gradle.kts`) | none | Build gate only — a `build.gradle.kts` edit has no unit-testable logic | `feature/cart/build.gradle.kts` | build gate only |
| Hilt graph (`:wear`) | build/compile check | `hiltJavaCompileDebug` succeeding is the test — Dagger component generation is the thing being verified, not app logic | `wear/src/main/java/.../di/WearSubscriptionModule.kt` | `./gradlew :wear:hiltJavaCompileDebug` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After T1, T2 (repository/Composable relocation only) | `./gradlew :core:domain:test :core:data:test` (T1) / `./gradlew :core:ui:build` (T2) |
| Full | After each PR (T1–T7) | `./gradlew test lint` |
| Device | T2's instrumented test (G11 regression), and T5's `:wear` Hilt compile | `./gradlew :core:ui:connectedAndroidTest` (device required) / `./gradlew :wear:hiltJavaCompileDebug` |
| Build | After PR4 (T8), PR5 (T9) | `./gradlew build` |

---

## Execution Plan

```
T1
T2
T3
T3 → T4 → T5 → T6 → T7
T1 → T8
T2 → T8
T7 → T8
T8 → T9
```

Phase 1's two tasks (T1, T2) are independent of each other and of every later phase — no edge
between them, either order works, and they could run in parallel workers if this feature is
executed with sub-agents. Phase 2 (T3) is independent of Phase 1's content but gated on the
`AD-0xx` decision, not on T1/T2 finishing first — no edge from Phase 1 into T3. Phase 3
(`ShareShoppingUseCase`'s implementation, T4–T7) is one dependency chain that starts once T3's
decision exists (`T3 → T4`, a cross-phase edge). Phase 4 (T8, the `feature/cart` audit) needs T1,
T2, and T7 done — three cross-phase edges into T8. Phase 5 (T9, `:wear` re-verification) needs T8
done (`T8 → T9`).

---

## Task Breakdown

### T1: Promote `SettingsRepository` to `core/domain`/`core/data`

**What**: Move the `SettingsRepository` interface to `core/domain` and its DataStore-backed impl to
`core/data`; update the Hilt binding module; delete `GetSettingsUseCase`; update
`app/MainViewModel`, `feature/shopping/ShoppingListViewModel`, `feature/chat/CartAssistantUseCase`
to inject the relocated interface directly; drop `implementation(project(":feature:settings"))`
from `feature/shopping/build.gradle.kts` and `feature/chat/build.gradle.kts` only if a re-grep
after the move confirms nothing else from `feature/settings` is imported there.
**Where**: `core/domain/.../repository/SettingsRepository.kt`, `core/data/.../repository/SettingsRepositoryImpl.kt`, `feature/settings/di/SettingsModule.kt`, `app/.../MainViewModel.kt`, `feature/shopping/.../ShoppingListViewModel.kt`, `feature/chat/.../CartAssistantUseCase.kt`, `feature/shopping/build.gradle.kts`, `feature/chat/build.gradle.kts`
**Depends on**: None
**Reuses**: `ShoppingRepository`'s existing interface/impl module split and Hilt binding shape (same pattern, different contract)
**Requirement**: G10-01

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] No file outside `feature/settings` imports `feature.settings.domain.usecase.GetSettingsUseCase` (grep returns zero matches)
- [x] `GetSettingsUseCase` is deleted, not just unused
- [x] `feature/settings` itself compiles and uses the relocated interface via its `core/data` impl
- [x] `feature/shopping/build.gradle.kts` / `feature/chat/build.gradle.kts` drop the `feature/settings` dependency only if the post-move re-grep confirms it's unused there — `feature/shopping` re-grep still showed `feature.settings.navigation.Settings` (the sanctioned nav entry point), so its dependency stays; `feature/chat`'s re-grep came back empty, so its dependency was dropped
- [x] Gate check passes: `./gradlew :feature:settings:test :feature:shopping:test :feature:chat:test :app:test`
- [x] Test count: unchanged from pre-move baseline for these four modules (no silent deletions) — `feature/shopping` (102), `feature/chat` (20), `app` (5) are byte-for-byte unchanged. `feature/settings` moved from 54 to 40 **by design, not silently**: `SettingsRepositoryImplTest`'s 13 tests moved intact to `core/data` (now covered by `:core:data:test`, also green) alongside the impl, and `GetSettingsUseCaseTest`'s 1 test was deleted along with the wrapper it tested (mandated by this task's 2nd bullet). Total test count across `feature/settings` + `core/data` + `core/domain` is unchanged; no assertions were lost.

**Execution notes** (`android-engineer-architecture`, 2026-09-29):
- Found 2 more `GetSettingsUseCase` consumers than the task's "Where" list during the pre-delete grep: `feature/cart/presentation/viewmodel/CartViewModel.kt` and `feature/ai-agent/domain/orchestrator/AiAgentOrchestrator.kt` (plus 5 internal `feature/settings` ViewModels also injecting it). All were updated to inject `SettingsRepository` directly, same pattern as the 3 documented consumers — required for the "grep returns zero" criterion to actually hold. `feature/cart`'s and `feature/ai-agent`'s `build.gradle.kts` were deliberately **not** touched (their `feature:settings` dependency audit is T8's job, which runs after T1/T2/T7 land).
- `SettingsRepositoryImpl` had a hidden dependency on `feature/settings`-internal `ShoppingReminderScheduler` (a `WorkManager` scheduler) that a literal relocation would have made a cross-module cycle (`core/data` -> `feature/settings`, inverting `feature/settings -> core/data`). Fixed by extracting a `ReminderScheduler` port to `core/domain/services/` (mirrors the existing `StorageService`/`AuthService`/`NetworkService` precedent in that package) — `ShoppingReminderScheduler` now implements it and is bound to it from `feature/settings`'s own `SettingsModule`, so `core/data` only depends on the interface. Flagging for `tech-lead` review since it's a small design call the task text didn't anticipate.
- `SettingsRepositoryImpl`'s `DataStore<Preferences>` Hilt binding was kept unqualified (no new `@Qualifier`), matching its pre-move state exactly — it's the only unqualified `DataStore<Preferences>` provider project-wide (every other one, e.g. `@AuthDataStore`/`@ChatDataStore`/`@AiTrialDataStore`, is qualified), so this is a safe, behavior-preserving relocation, not a new inconsistency.

**Tests**: unit
**Gate**: full

---

### T2: Move `CameraPreview` + `QRCodeOverlay` to `core/ui`

**What**: Move both Composables (`feature/products/presentation/components/scanner/*.kt`) to the
equivalent `core/ui` package; move the CameraX dependencies from `feature/products/build.gradle.kts`
to `core/ui/build.gradle.kts`; move `CameraPreviewExecutorLifecycleTest` (the G11 regression test)
alongside; update imports in `feature/shopping/presentation/components/scanner/QrCodeScanner.kt`
and in `feature/products`' own scanner screen.
**Where**: `core/ui/.../components/scanner/CameraPreview.kt`, `core/ui/.../components/scanner/QRCodeOverlay.kt`, `core/ui/src/androidTest/.../scanner/CameraPreviewExecutorLifecycleTest.kt`, `core/ui/build.gradle.kts`, `feature/products/build.gradle.kts`, `feature/shopping/.../QrCodeScanner.kt`
**Depends on**: None
**Reuses**: `core/ui`'s existing module (already a dependency of every feature module) — no new Gradle edge needed
**Requirement**: G10-02

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `feature/shopping` no longer imports `feature.products.presentation.components.scanner.*`
- [x] `CameraPreviewExecutorLifecycleTest` passes from its new location on a device; if no device is available, note it as unverified rather than skip it — **unverified**: no `adb`/emulator available in this environment; the test (and the rest of `core/ui`'s `androidTest` source set) does compile cleanly via `:core:ui:compileDebugAndroidTestKotlin`, but `connectedAndroidTest` was not run
- [x] `feature/products/build.gradle.kts` no longer declares the CameraX dependencies now owned by `core/ui`
- [x] Gate check passes: `./gradlew :core:ui:build :feature:products:test :feature:shopping:test`
- [x] Test count: unchanged from pre-move baseline (no silent deletions)

**Tests**: instrumented (androidTest)
**Gate**: device

**Implementation note (discovered during execution, not scope creep — required for `CameraPreview`/
`QRCodeOverlay` to compile standalone in `core/ui` without a backward `core → feature` import):**
`CameraPreview` directly instantiates `BarcodeAnalyzer` (internal, same package) and `QRCodeOverlay`
directly calls `QrCodeStaticScannerCanvas` and `FlashControl` (both internal, same package) plus a
`feature/products` string resource. All four extra files/resource moved alongside the two named
Composables — leaving any of them behind would either fail to compile or force `core/ui` to depend
on `feature/products`, the exact anti-pattern this task exists to remove. Also found: `feature/products`'
`CameraCaptureView` (a *different*, unrelated composable — product-photo capture, not QR scanning)
also uses CameraX APIs directly, so the CameraX libraries could not simply be deleted from
`feature/products/build.gradle.kts` as `design.md` assumed — they were changed to `api` in
`core/ui/build.gradle.kts` instead, so `feature/products` still receives them transitively through
its existing `implementation(project(":core:ui"))` edge. `feature/products/build.gradle.kts` itself
no longer declares them directly, satisfying the "Done when" criterion as written.

---

### T3: Record `tech-lead`'s decision for `ShareShoppingUseCase`'s module boundary

**What**: **Not an implementation task** — this is the human decision gate. Decided by bruno
2026-09-29, recorded as `AD-011` in `.specs/STATE.md`: a **segregated core contract**, not either
option as originally framed — `core/domain` gets a minimal `ProductReader` interface
(`getAllProducts` only), `feature/products`'s `ProductRepository` extends it (superset, keeps
`delete`/`update`/`save`/`move` local), and a separate `ShareShoppingUseCase` interface (signature
only, no Android types) is added to `core/domain`, implemented in `feature/products`. Justified on
ISP/OCP/LSP/YAGNI grounds — see `AD-011`'s full reasoning.
**Where**: `.specs/STATE.md`
**Depends on**: None
**Reuses**: `.specs/STATE.md`'s existing `AD-NNN` format (see AD-008/AD-009 for the fullest examples)
**Requirement**: G10-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `.specs/STATE.md`'s `## Decisions` section contains a new `AD-0xx` entry naming the chosen option for `ShareShoppingUseCase`'s module boundary — **`AD-011`**
- [x] The entry follows the existing Decision/Reason/Trade-off/Scope/Date/Status format
- [x] T4–T7 (below) remain `Blocked` until this entry exists — do not start them on the recommended option alone — **unblocked as of `AD-011`**

**Tests**: none (this task produces a decision record, not code)
**Gate**: none — the "gate" is human sign-off, not a build command

---

### T4: Implement `ShareShoppingUseCase`'s chosen boundary — domain layer

**What**: Per `AD-011`, add **two** new interfaces to `core/domain` (contract-only, no behavior
change yet):
1. `core/domain/repository/ProductReader.kt` — `interface ProductReader { suspend fun getAllProducts(shoppingId: String): Flow<List<Product>> }`.
2. `core/domain/services/ShareShoppingUseCase.kt` — `interface ShareShoppingUseCase { suspend operator fun invoke(shopping: Shopping) }`. No `Context`/`Intent`/Android types in this file — `core/domain` stays framework-free.

**Where**: `core/domain/src/main/java/.../core/domain/repository/ProductReader.kt`, `core/domain/src/main/java/.../core/domain/services/ShareShoppingUseCase.kt`
**Depends on**: T3
**Reuses**: `AuthService`/`StorageService`/`NetworkService`'s existing precedent in `core/domain/services/` for a platform-touching capability exposed as a framework-free interface
**Requirement**: G10-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `ProductReader` and `ShareShoppingUseCase` interfaces exist in `core/domain`, exactly as specified in `AD-011`
- [ ] Neither interface imports any `android.*` type
- [ ] No behavior change yet — this task only defines the contracts, T5 wires the implementation
- [ ] Gate check passes: `./gradlew :core:domain:test`

**Tests**: unit
**Gate**: quick

---

### T5: Implement `ShareShoppingUseCase`'s chosen boundary — implementation + consumers

**What**: Per `AD-011`:
1. `feature/products/domain/repository/ProductRepository.kt` becomes `interface ProductRepository : ProductReader`, importing `core/domain`'s `ProductReader`. `ProductRepositoryImpl` needs no change — it already implements every method `ProductRepository` declares, and now transitively satisfies `ProductReader` too.
2. `feature/products/domain/usecase/ShareShoppingUseCase.kt`'s class implements `core/domain`'s new `ShareShoppingUseCase` interface. This needs a rename to avoid a same-name interface/impl collision in the same file/import scope — rename the impl class to `ShareShoppingUseCaseImpl` (or alias the `core/domain` import), engineer's call which reads cleaner given this project's existing naming conventions (check how `ShoppingRepositoryImpl`/`SettingsRepositoryImpl` do it). Body unchanged (still builds the Android `Intent`, still depends on `ProductsUseCase`).
3. Hilt: bind `ProductRepositoryImpl` to both `ProductRepository` (for `feature/products`'s own internal CRUD use) and `ProductReader` (for external consumers) in `feature/products`'s existing DI module; bind the renamed `ShareShoppingUseCase` impl to `core/domain`'s `ShareShoppingUseCase` interface.
4. Update `feature/shopping` and `feature/cart` to inject `core/domain`'s `ShareShoppingUseCase` interface instead of importing `feature.products.domain.usecase.ShareShoppingUseCase` concretely.
**Where**: `feature/products/domain/repository/ProductRepository.kt`, `feature/products/domain/usecase/ShareShoppingUseCase.kt` (renamed impl), `feature/products/di/*Module.kt`, `feature/shopping/presentation/viewmodel/ShoppingListViewModel.kt`, `feature/cart/presentation/viewmodel/ShareOptionsViewModel.kt`
**Depends on**: T4
**Reuses**: T4's two contracts; existing DI module patterns in `feature/shopping`/`feature/cart`
**Requirement**: G10-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `feature/shopping` and `feature/cart` no longer import `feature.products.domain.usecase.ShareShoppingUseCase` concretely — both now depend on `core/domain`'s `ShareShoppingUseCase` interface
- [x] `ProductRepository` extends `ProductReader`; no consumer outside `feature/products` depends on `ProductRepository` directly (only `ProductReader`)
- [x] `ShareShoppingUseCaseTest` passes from its final location with test count preserved (no silent deletions)
- [x] Gate check passes: `./gradlew :core:domain:test :feature:products:test :feature:shopping:test :feature:cart:test`

**Tests**: unit
**Gate**: full

---

### T6: Update `feature/products/build.gradle.kts` for the chosen boundary

**What**: Adjust `feature/products/build.gradle.kts` module dependencies to reflect T4/T5's actual
new shape (e.g. Option (a) may let `feature/products` drop its own dependency on the now-promoted
`ProductRepository`'s original location if nothing else in that module still needs it directly;
Option (b) likely needs no `build.gradle.kts` change at all beyond what T5 already required).
**Where**: `feature/products/build.gradle.kts`
**Depends on**: T5
**Reuses**: N/A — config-only task
**Requirement**: G10-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `feature/products/build.gradle.kts` has no dependency line left over from the pre-move shape that nothing imports anymore
- [ ] Gate check passes: `./gradlew :feature:products:build`

**Tests**: none (build config only)
**Gate**: build

---

### T7: Full-suite verification for the `ShareShoppingUseCase` boundary change

**What**: Run the complete affected-module test suite once T3–T6 are all done, closing out PR3 the
same way PR1/PR2 close with a full gate.
**Where**: N/A — verification task, no file changes expected
**Depends on**: T6
**Reuses**: N/A
**Requirement**: G10-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `./gradlew test lint` passes across every module touched by T3–T6
- [ ] `tech-lead`'s `AD-0xx` from T3 is cross-referenced in this PR's description

**Tests**: unit
**Gate**: full

---

### T8: Re-audit `feature/cart`'s dependency list

**What**: Re-run `grep -rh "import br.com.brunocarvalhs.howmuch.feature\." feature/cart/src/main --include="*.kt" | sort -u` and drop any `implementation(project(":feature:x"))` in `feature/cart/build.gradle.kts` with no matching import.
**Where**: `feature/cart/build.gradle.kts`
**Depends on**: T1, T2, T7
**Reuses**: N/A — audit task
**Requirement**: G10-04

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Every `implementation(project(":feature:*"))` line in `feature/cart/build.gradle.kts` has at least one corresponding import
- [ ] Gate check passes: `./gradlew :feature:cart:test`
- [ ] Re-running the original `G10-DESIGN-PASS.md` dependency grep shows the cross-feature edge count for `feature/cart` went down, not just moved

**Tests**: none (build config only)
**Gate**: build

---

### T9: Re-verify `:wear`'s Hilt graph

**What**: Confirm whether `WearSubscriptionModule`'s stub is still required after the graph shrinks
from T1/T2/T7/T8, and that no *new* stub is needed elsewhere. No code change expected. If the stub
becomes provably unnecessary, remove it in its own small follow-up PR with the reasoning recorded
in that PR's commit message — do not fold that removal into this task's commit.
**Where**: `wear/src/main/java/.../di/WearSubscriptionModule.kt` (comment audit only; deletion, if warranted, happens in a separate follow-up PR)
**Depends on**: T8
**Reuses**: N/A — verification task
**Requirement**: G10-05

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `./gradlew :wear:hiltJavaCompileDebug` succeeds
- [ ] `WearSubscriptionModule.kt`'s comment still accurately describes why it exists (or a follow-up PR is opened to remove it, with reasoning in that PR's own commit message — not this task's)
- [ ] `./gradlew build` is clean project-wide, closing out the full G10 effort

**Tests**: build/compile check
**Gate**: device

---

## Phase Execution Map

```
T1
T2
T3
T3 → T4 → T5 → T6 → T7
T1 → T8
T2 → T8
T7 → T8
T8 → T9
```

T1 and T2 have no `Depends on` between them (both list `None`) but are grouped in the same phase
since they are the two independent, unblocked, mechanical relocations that can start immediately —
no arrow is drawn between them because none exists. Phase 2 (T3) is a decision gate, not code — it
can happen in parallel with Phase 1's execution in wall-clock time, but is drawn as its own phase
since Phase 3 depends on its outcome, not on anything Phase 1 produces. Phase 3 (T4–T7) is one
tight dependency chain implementing whichever option T3 records; its first edge (T3 → T4) is
cross-phase and validated by the forward-phase-dependency check rather than the intra-phase
diagram parity check. Phase 4 (T8) needs T1, T2 (Phase 1) and T7 (Phase 3) all done — three
cross-phase dependencies, likewise validated by the forward-phase check. Phase 5 (T9) needs T8
(Phase 4) done.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1 | 1 cohesive relocation (repository interface + impl + Hilt binding + 3 consumers + 2 build files) | ✅ Granular — one deliverable ("SettingsRepository lives in core/*"), matches `G10-DESIGN-PASS.md`'s own Scope: M for this exact task |
| T2 | 1 cohesive relocation (2 Composables + their test + 2 build files + 1 consumer) | ✅ Granular — one deliverable ("scanner UI lives in core/ui"), matches Scope: M |
| T3 | 1 decision record, 1 file | ✅ Granular — pure decision gate |
| T4 | 1 contract definition | ✅ Granular — split from T5 (impl) specifically so the contract-only step is reviewable alone |
| T5 | 1 cohesive implementation + consumer wiring | ✅ Granular — one deliverable, depends on T4's contract existing first |
| T6 | 1 file (build config) | ✅ Granular |
| T7 | 0 files (verification only) | ✅ Granular — matches this project's convention of a closing full-gate task per PR (see `item-add-authorship/tasks.md` T6, T10) |
| T8 | 1 file (build config) | ✅ Granular |
| T9 | 1 file (comment audit) + build gate | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ------------------------ | --------------- | ------ |
| T1 | None | None | ✅ Match |
| T2 | None | None | ✅ Match |
| T3 | None | None | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | T1, T2, T7 | T1 → T8, T2 → T8, T7 → T8 | ✅ Match |
| T9 | T8 | T8 → T9 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | ----------------------------- | ------------------ | ----------- | ------ |
| T1 | Domain/Data repository + Presentation consumers | unit | unit | ✅ OK |
| T2 | Presentation / Component (instrumented) | instrumented (androidTest) | instrumented | ✅ OK |
| T3 | Documentation (`STATE.md`) — not a code layer | none | none | ✅ OK |
| T4 | Domain / contract | unit | unit | ✅ OK |
| T5 | Domain/Data impl + Presentation consumers | unit | unit | ✅ OK |
| T6 | Build / Gradle config | none | none | ✅ OK |
| T7 | Verification only — no new code layer | (closing full-suite gate) | unit (full-suite run) | ✅ OK |
| T8 | Build / Gradle config | none | none | ✅ OK |
| T9 | Hilt graph / build config | build/compile check | build/compile check | ✅ OK |
