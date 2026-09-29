# Cross-Feature Module Coupling (G10) Specification

Formalizes `.specs/G10-DESIGN-PASS.md` (Clean Architecture review, 2026-09-29) into SDD. Logged as
a known gap in `.specs/MVP-ROADMAP.md:49` and tracked in `.specs/BETA-LAUNCH-PLAN.md:432,544-545`
("G10 now has an owner: the `android-engineer-architecture` subagent"). **Not a beta blocker** —
confirmed post-beta backlog in `MVP-ROADMAP.md`'s persona cross-check ("Zero diferença observável
para qualquer persona"). Do not let this delay `BETA-LAUNCH-PLAN.md`'s open items.

## Problem Statement

A Clean Architecture review traced the actual import graph and found feature modules coupled more
tightly than AD-005 allows: `feature/shopping` imports `feature/products`' **Composables**
(`CameraPreview`, `QRCodeOverlay`) and use cases (`ProductsUseCase`, `ShareShoppingUseCase`), not
just its navigation entry point; several features import `feature/settings` directly for a single
4-line pass-through use case; `feature/cart` alone depends on five sibling feature modules. This
already has one concrete cost: `:wear` pulls in `feature/cart`/`chat`/`shopping` transitively,
forcing a `WearSubscriptionModule` stub (`wear/src/main/java/.../di/WearSubscriptionModule.kt:13-21`)
just to satisfy Dagger's aggregated Hilt component for `@HiltViewModel`s the watch never shows.

## Goals

- [ ] `SettingsRepository` lives in `core/domain`/`core/data`, consumed directly by `app`,
      `feature/shopping`, `feature/chat` — no feature-to-feature import for it.
- [ ] `CameraPreview`/`QRCodeOverlay` live in `core/ui`; `feature/shopping` no longer imports
      `feature/products`' presentation package for scanning.
- [ ] `ShareShoppingUseCase`'s module boundary is a decision `tech-lead` has explicitly recorded
      (as a new `AD-0xx`), not something this pass or `android-engineer-architecture` decides
      unilaterally.
- [ ] `feature/cart`'s `build.gradle.kts` module dependencies match its actual imports — no stale
      `implementation(project(":feature:x"))` lines left over once T1–T3 land.
- [ ] `:wear`'s Hilt graph is re-verified after the graph shrinks; the `WearSubscriptionModule`
      stub is removed if it's provably no longer required, kept (with an accurate comment) if it
      still is.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| New Gradle modules (e.g. `core/scanner`, `core/settings-api`) | Explicitly rejected in `G10-DESIGN-PASS.md`'s Decision — over-engineering for a fan-out this small (2 consumers each). `core/domain`, `core/data`, `core/ui` already exist and already do this job for `ShoppingRepository`/`UserRepository`/`SubscriptionRepository`. |
| Relitigating AD-005 ("all implementation details MUST be `internal`") | Moved classes stay `internal` in their new home unless cross-module visibility is the point of the move (the promoted repository interfaces, which must be `public` to be implemented elsewhere). |
| Promoting `ProductRepository` to `core/domain` as a standalone goal | Only in scope as a side effect if `tech-lead` picks G10-03 Option (a); not pursued for its own sake. |
| Any beta-blocking work | This is post-beta backlog per `MVP-ROADMAP.md`'s persona cross-check. Do not pull beta-blocker items into this feature's scope, and do not let this feature delay them. |
| Removing the `WearSubscriptionModule` stub as a given outcome | G10-05 is a verification task; the stub is removed only if provably unnecessary after the graph shrinks, in its own small PR with reasoning in the commit message. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | --------------- | --------- | ---------- |
| Pattern for promoting shared contracts | Interface in `core/domain`, implementation in `core/data` (or the owning feature module's `data/` layer, mirroring `SettingsRepositoryImpl`) | Matches the existing pattern for `ShoppingRepository`, `UserRepository`, `SubscriptionRepository` — reuse, don't invent a new shape. | [y] |
| `GetSettingsUseCase` fate | Delete it; consumers inject `SettingsRepository` directly | It is a 4-line pass-through (`repository.getSettings()`) with no logic of its own — keeping it as a wrapper after the repository moves adds a layer with no purpose. | [y] |
| Execution order | G10-01 and G10-02 run independently (parallel-safe); G10-04 and G10-05 run last, after G10-01–G10-03 land | Matches `G10-DESIGN-PASS.md`'s own dependency notes and its Checkpoints section. | [y] |
| **G10-03 (`ShareShoppingUseCase`'s home)** | **Not chosen here.** Two options are on the table (see below); `android-engineer-architecture` proposed Option (b) as a non-binding recommendation, `tech-lead` decides. | Moving `ShareShoppingUseCase` mechanically like G10-01/G10-02 would drag `ProductRepository` (not yet a `core/domain` interface) into scope — too big a call for a reviewer to make unilaterally. Per this project's role boundaries, a `spec-driven` agent formalizes plans but does not decide architecture; per `SKILL.md`, this is a Design-phase decision (not a Discuss gray area — `discuss.md` explicitly excludes "technical architecture" from that phase). | [n] |

**Open questions:** one — **G10-03's destination is unresolved and blocks only that task**
(tracked below and in `design.md`'s Tech Decisions). G10-01, G10-02, and the parts of G10-04/G10-05
that don't depend on G10-03's outcome are unaffected and ready for `tasks.md`.

### G10-03 decision: needs a `tech-lead` call before implementation starts

`ShareShoppingUseCase` composes `Product` data (via `ProductsUseCase` → `ProductRepository`, not
yet a `core/domain` interface), a `Shopping` model, and an Android `Intent`-based share action.

- **Option (a)**: Promote `ProductRepository` to `core/domain` (mirrors `ShoppingRepository`) and
  move `ShareShoppingUseCase` + `ProductsUseCase` there too. Larger diff (**Scope: L**); forces a
  decision about `ProductRepository`'s home as a side effect of a sharing feature.
- **Option (b)**: Leave the implementation in `feature/products`, define a `ShareShoppingUseCase`
  **port** (interface) in `core/domain`, implemented by `feature/products`, consumed by
  `feature/shopping`/`feature/cart` via DI (Crossing Boundaries pattern). Smaller diff
  (**Scope: M**); does not force `ProductRepository`'s home.
- **`android-engineer-architecture`'s recommendation** (from `G10-DESIGN-PASS.md`): Option (b) —
  same fix shape as G10-01/G10-02 at lower cost.
- **Gate**: `tech-lead` records the choice as a new `AD-0xx` in `.specs/STATE.md` before any
  G10-03 implementation task starts. Until then, G10-03 stays in `tasks.md` as a blocked task.

---

## User Stories

### P1: Promote Shared Contracts Out of Feature Modules ⭐ MVP

**User Story**: As a developer maintaining `feature/shopping` or `feature/chat`, I want
`SettingsRepository` and the camera-scanning Composables available from `core/*` instead of
reaching into `feature/settings`/`feature/products`, so that my module's declared Gradle
dependencies match what it actually needs and AD-005 isn't silently violated.

**Why P1**: Both moves are mechanical (no ambiguous destination), independent of each other and of
the G10-03 decision, and unblock the cleanup/verification stories below.

**Acceptance Criteria**:

1. WHEN any file outside `feature/settings` needs settings data THEN it SHALL depend on
   `core/domain`'s `SettingsRepository` interface, not `feature.settings.domain.usecase.GetSettingsUseCase`.
2. The system SHALL NOT contain `GetSettingsUseCase` after this story ships — it is deleted, not
   deprecated.
3. WHEN `feature/shopping` or `feature/chat` no longer imports anything else from
   `feature/settings` THEN their `build.gradle.kts` SHALL drop `implementation(project(":feature:settings"))`.
4. WHEN any feature needs the scanner UI THEN it SHALL import `CameraPreview`/`QRCodeOverlay` from
   `core/ui`, not `feature.products.presentation.components.scanner.*`.
5. The system SHALL NOT add any new Gradle module to accomplish either move.
6. WHILE the promoted repository interface is implemented outside its original module, its
   implementation class SHALL stay `internal` unless cross-module visibility is the actual point
   of that specific class (only the interface itself needs to be `public`).

**Independent Test**: `grep -rh "import br.com.brunocarvalhs.howmuch.feature.settings" feature/shopping/src/main feature/chat/src/main` and the equivalent for the scanner Composables in `feature/shopping/src/main` both return zero matches; `./gradlew :core:domain:test :core:data:test :core:ui:build :feature:settings:test :feature:shopping:test :feature:chat:test :app:test` is green.

---

### P2: Resolve `ShareShoppingUseCase`'s Module Boundary

**User Story**: As `tech-lead`, I want to make the explicit call on where `ShareShoppingUseCase`
(and, if Option (a) is chosen, `ProductRepository`) lives, so the decision is recorded and the
implementing engineer isn't left guessing or making an unreviewed architecture call.

**Why P2**: This is the one G10 task that is genuinely ambiguous (two valid shapes, different
blast radius) rather than mechanical — it needs a human decision before code moves.

**Acceptance Criteria**:

1. WHEN `tech-lead` chooses Option (a) or (b) THEN the choice SHALL be recorded as a new `AD-0xx`
   entry in `.specs/STATE.md`, following the existing Decision/Reason/Trade-off/Scope/Date/Status
   format.
2. IF no `AD-0xx` decision exists yet THEN `android-engineer-architecture` SHALL NOT begin
   implementing G10-03 — the task stays blocked in `tasks.md`.
3. WHEN the chosen option is implemented THEN `feature/shopping` and `feature/cart` SHALL NOT
   import `feature.products.domain.usecase.ShareShoppingUseCase` concretely (only via the
   interface/port from `core/domain`, or via the promoted location under Option (a)).
4. WHEN the chosen option is implemented THEN `ShareShoppingUseCaseTest` SHALL pass from its final
   location, with test count preserved (no silent deletions).

**Independent Test**: `.specs/STATE.md`'s `## Decisions` section contains an `AD-0xx` entry whose
Decision line names G10-03's chosen option; after implementation, the grep from P1's Independent
Test (extended to `ShareShoppingUseCase`) returns zero matches from `feature/shopping`/`feature/cart`.

---

### P3: Shrink and Re-Verify the Dependency Graph

**User Story**: As a developer relying on `:wear`'s build, I want `feature/cart`'s declared module
dependencies and `:wear`'s Hilt graph to reflect the smaller import graph left by P1/P2, so stale
dependencies and workaround stubs don't outlive the coupling that caused them.

**Why P3**: Cleanup and verification only make sense once the moves in P1/P2 exist to verify
against — this closes the loop `G10-DESIGN-PASS.md`'s Checkpoints section describes.

**Acceptance Criteria**:

1. WHEN P1 and P2 are complete THEN every `implementation(project(":feature:*"))` line in
   `feature/cart/build.gradle.kts` SHALL have at least one corresponding import in
   `feature/cart/src/main`.
2. IF a `feature/cart/build.gradle.kts` dependency has no matching import THEN it SHALL be removed.
3. WHEN the dependency graph has shrunk THEN `./gradlew :wear:hiltJavaCompileDebug` SHALL succeed.
4. IF `WearSubscriptionModule`'s stub is provably no longer required after the graph shrinks THEN
   it SHALL be removed in its own small PR with the reasoning in that PR's commit message.
5. IF `WearSubscriptionModule`'s stub is still required THEN its explanatory comment SHALL still
   accurately describe why it exists.

**Independent Test**: Re-run the dependency grep from the original `G10-DESIGN-PASS.md` review
(`grep -rh "import br.com.brunocarvalhs.howmuch.feature\." feature/cart/src/main --include="*.kt" | sort -u`) and confirm the cross-feature edge count went down, not just moved; `./gradlew build` is clean.

---

## Edge Cases

- IF re-grepping after a move (G10-01) shows a feature module still imports something else from
  `feature/settings` beyond `GetSettingsUseCase` THEN the `implementation(project(":feature:settings"))`
  line in that module's `build.gradle.kts` SHALL NOT be dropped — the acceptance criterion is
  "no matching import", not "the move happened."
- IF `CameraPreviewExecutorLifecycleTest` (the G11 regression test) cannot be run because no
  instrumented device is available THEN the task SHALL be noted as unverified rather than skipped
  or marked done on faith — matches this project's existing convention for instrumented tests
  (see `.specs/STATE.md`'s Maestro/F0.3 handling).
- IF `tech-lead` has not yet recorded an `AD-0xx` for G10-03 THEN `tasks.md`'s G10-03 task SHALL
  remain in a `Blocked` state and MUST NOT be started, even if G10-01/G10-02/G10-04/G10-05 are done.
- IF removing `WearSubscriptionModule` turns out to still be required (partial graph shrink) THEN
  G10-05's acceptance criterion is the comment staying accurate, not the file being deleted —
  deleting a still-needed stub is a regression, not progress.

---

## Requirement Traceability

| Requirement ID | Story                                          | Phase  | Status  |
| --------------- | ----------------------------------------------- | ------ | ------- |
| G10-01          | P1: Promote `SettingsRepository`                 | Design | Pending |
| G10-02          | P1: Move `CameraPreview`/`QRCodeOverlay`         | Design | Pending |
| G10-03          | P2: `ShareShoppingUseCase`'s module boundary     | Design | Pending — blocked on `tech-lead` `AD-0xx` |
| G10-04          | P3: Re-audit `feature/cart`'s dependency list    | Design | Pending |
| G10-05          | P3: Re-verify `:wear`'s Hilt graph               | Design | Pending |

**ID format:** `G10-[NUMBER]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 5 total, 5 mapped to `design.md`/`tasks.md` (G10-03's implementation task is created
but held `Blocked` pending the `AD-0xx` decision), 0 unmapped ✅

---

## Success Criteria

- [ ] `./gradlew :core:domain:test :core:data:test :core:ui:build :feature:settings:test :feature:shopping:test :feature:chat:test :feature:products:test :feature:cart:test :app:test` all green after G10-01/G10-02/G10-04 land.
- [ ] Zero cross-feature imports remain for `GetSettingsUseCase` or the scanner Composables.
- [ ] `tech-lead` has recorded G10-03's decision as `AD-0xx` in `.specs/STATE.md` before any code
      moves for that task.
- [ ] `./gradlew :wear:hiltJavaCompileDebug` succeeds and `WearSubscriptionModule`'s state (kept
      accurately-commented, or removed with reasoning) matches the post-shrink graph.
- [ ] No new Gradle module was added anywhere in this effort.
