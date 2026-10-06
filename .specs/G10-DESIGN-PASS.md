# G10 — Cross-Feature Module Coupling: Design Pass & Action Plan

Concrete task breakdown for the G10 gap already logged in `.specs/MVP-ROADMAP.md` (line 49) and
`.specs/BETA-LAUNCH-PLAN.md` ("G10 now has an owner: the `android-engineer-architecture`
subagent"). Not a beta blocker — confirmed post-beta backlog in `MVP-ROADMAP.md`'s persona
cross-check ("Zero diferença observável para qualquer persona"). Do not pull this into beta scope.

## Context

A Clean Architecture review (2026-09-29) traced the actual import graph and found the coupling is
worse than "features share a use case" — one feature imports another's **Composables**:

```
feature/shopping → feature/products : ProductsUseCase, ShareShoppingUseCase,
                                        CameraPreview, QRCodeOverlay  ← presentation layer, not navigation
feature/shopping → feature/settings : GetSettingsUseCase
feature/products → feature/settings, feature/chat
feature/cart     → feature/ai-agent, feature/settings, feature/chat,
                     feature/products, feature/shopping
feature/chat     → feature/ai-agent, feature/settings
feature/auth     → feature/settings
feature/profile  → feature/settings
```

This already has one concrete cost: `:wear` pulls in `feature/cart`/`chat`/`shopping`, which
transitively drag in subscription-related `@HiltViewModel`s the watch never shows, forcing the
`WearSubscriptionModule` stub (`wear/src/main/java/.../di/WearSubscriptionModule.kt:13-21`) just to
satisfy Dagger's aggregated component. That stub is correct and should stay — it's the effect, not
the thing to fix.

## Decision

Follow the pattern already established in this codebase: shared repository interfaces live in
`core/domain`, implementations in `core/data`/feature modules (`ShoppingRepository`,
`UserRepository`, `SubscriptionRepository` all do this today). **No new Gradle module.** Reuse
`core/domain`, `core/data`, `core/ui` — they already exist and already do this job for everything
else. Creating `core/scanner` or `core/settings-api` for two consumers each would be
over-engineering for the actual fan-out.

## Task Queue

### G10-T1 — Promote `SettingsRepository` to `core/domain` / `core/data`
- **Owner:** `android-engineer-architecture`
- **Why this shape:** `SettingsRepository` (`feature/settings/domain/repository/SettingsRepository.kt`)
  and its DataStore-backed impl are consumed by `app`, `feature/shopping`, `feature/chat` — none of
  which need any other part of `feature/settings`.
- **Changes:**
  - Move `SettingsRepository` interface → `core/domain/repository/SettingsRepository.kt`.
  - Move `SettingsRepositoryImpl` → `core/data/repository/SettingsRepositoryImpl.kt`; update its
    Hilt binding module accordingly.
  - Delete `GetSettingsUseCase` — it is a 4-line pass-through (`repository.getSettings()`) with no
    logic of its own; consumers inject `SettingsRepository` directly instead.
  - Update `app/MainViewModel`, `feature/shopping/ShoppingListViewModel`,
    `feature/chat/CartAssistantUseCase` to depend on `core/domain`'s interface.
  - Drop `implementation(project(":feature:settings"))` from `feature/shopping/build.gradle.kts`
    and `feature/chat/build.gradle.kts` **only if** nothing else from `feature/settings` is
    imported there (re-grep after the move, don't assume).
- **Acceptance criteria:**
  - [ ] No file outside `feature/settings` imports `feature.settings.domain.usecase.GetSettingsUseCase`
  - [ ] `feature/settings` itself still compiles and uses the relocated interface
  - [ ] `./gradlew :feature:settings:test :feature:shopping:test :feature:chat:test :app:test` green
- **Files likely touched:** `SettingsRepository.kt`, `SettingsRepositoryImpl.kt`,
  `feature/settings/di/SettingsModule.kt`, `MainViewModel.kt`, `ShoppingListViewModel.kt`,
  `CartAssistantUseCase.kt`, 2 `build.gradle.kts`. **Scope: M.**
- **Dependencies:** None.

### G10-T2 — Move `CameraPreview` + `QRCodeOverlay` to `core/ui`
- **Owner:** `android-engineer-architecture`
- **Why:** these are generic scanner Composables with no `products`-specific logic; `core/ui` is
  the module every feature already depends on for shared UI.
- **Changes:**
  - Move both files (`feature/products/presentation/components/scanner/*.kt`) → `core/ui`'s
    equivalent package.
  - Move the CameraX dependencies (`androidx.camera.core/camera2/lifecycle/view`) from
    `feature/products/build.gradle.kts` to `core/ui/build.gradle.kts`.
  - Move `CameraPreviewExecutorLifecycleTest` (the G11 regression test) alongside.
  - Update imports in `feature/shopping/presentation/components/scanner/QrCodeScanner.kt` and in
    `feature/products`' own scanner screen.
- **Acceptance criteria:**
  - [ ] `feature/shopping` no longer imports `feature.products.presentation.components.scanner.*`
  - [ ] `CameraPreviewExecutorLifecycleTest` still passes from its new location (instrumented —
    needs a device; if none available, note it as unverified rather than skip it)
  - [ ] `./gradlew :core:ui:build :feature:products:test :feature:shopping:test` green
- **Files likely touched:** `CameraPreview.kt`, `QRCodeOverlay.kt`, the androidTest file, 2
  `build.gradle.kts`, `QrCodeScanner.kt`. **Scope: M.**
- **Dependencies:** None (independent of T1).

### G10-T3 — Decide `ShareShoppingUseCase`'s home (needs a human call, not auto-fixable)
- **Owner:** `android-engineer-architecture`, decision reviewed by `tech-lead`
- **Why this one is different:** it composes `Product` data (via `ProductsUseCase` →
  `ProductRepository`, which is *not* yet a `core/domain` interface), a `Shopping` model, and an
  Android `Intent`-based share action. Moving it mechanically like T1/T2 would drag
  `ProductRepository` down too — a bigger call than this review should make unilaterally.
- **Options to put in front of `tech-lead`:**
  - (a) Promote `ProductRepository` to `core/domain` (mirrors `ShoppingRepository`) and move
    `ShareShoppingUseCase` + `ProductsUseCase` there too.
  - (b) Leave the implementation in `feature/products`, but define a `ShareShoppingUseCase`
    **port** (interface) in `core/domain`, implemented by `feature/products`, consumed by
    `feature/shopping`/`feature/cart` via DI — the Crossing Boundaries pattern from the Clean
    Architecture review, smaller diff than (a).
- **Recommendation:** (b) — same fix shape as T1/T2 at lower cost, and doesn't force a decision on
  `ProductRepository`'s home as a side effect of a sharing feature.
- **Acceptance criteria:**
  - [ ] `tech-lead` has recorded the choice as a new `AD-0xx` in `STATE.md`
  - [ ] Whichever option: `feature/shopping` and `feature/cart` no longer import
    `feature.products.domain.usecase.ShareShoppingUseCase` concretely
  - [ ] `ShareShoppingUseCaseTest` passes from its final location
- **Files likely touched:** depends on option chosen. **Scope: M (b) / L (a) — if (a), break it
  into its own sub-tasks before starting.**
- **Dependencies:** None to start the decision; implementation can follow T1/T2's pattern once decided.

### G10-T4 — Re-audit `feature/cart`'s dependency list
- **Owner:** `android-engineer-architecture`
- **Why:** `cart` depends on 5 sibling feature modules today; T1–T3 should shrink this, and any
  edge that's now unused should be removed rather than left stale.
- **Changes:** re-run
  `grep -rh "import br.com.brunocarvalhs.howmuch.feature\." feature/cart/src/main --include="*.kt" | sort -u`
  and drop any `implementation(project(":feature:x"))` in `feature/cart/build.gradle.kts` with no
  matching import.
- **Acceptance criteria:**
  - [ ] Every `implementation(project(":feature:*"))` line in `feature/cart/build.gradle.kts` has
    at least one corresponding import
  - [ ] `./gradlew :feature:cart:test` green
- **Files likely touched:** `feature/cart/build.gradle.kts`. **Scope: S.**
- **Dependencies:** T1, T2, T3 (run last).

### G10-T5 — Re-verify `:wear`'s Hilt graph
- **Owner:** `android-engineer-architecture`
- **Why:** confirm whether `WearSubscriptionModule`'s stub is still required after the graph
  shrinks, and that no *new* stub is needed elsewhere.
- **Changes:** none expected — this is a verification task. If the stub becomes provably
  unnecessary, remove it in its own small PR with the reasoning recorded in its own commit message;
  don't fold that into T1–T4.
- **Acceptance criteria:**
  - [ ] `./gradlew :wear:hiltJavaCompileDebug` succeeds
  - [ ] Comment in `WearSubscriptionModule.kt` still accurately describes why it exists (or the
    file is removed with a matching commit message if it no longer applies)
- **Dependencies:** T1–T4.

## Checkpoints

- **After T1 + T2** (independent, can run in parallel): full module test suite green, no new
  Gradle module added, `settings.gradle.kts` unchanged.
- **After T3 decision recorded, before implementing:** `tech-lead` sign-off in `STATE.md`.
- **After T4 + T5:** `./gradlew build` clean; re-run the dependency grep from the original review
  and confirm the cross-feature edge count actually went down, not just moved.

## Non-goals / scope guard

- Not a beta blocker — do not let this delay `BETA-LAUNCH-PLAN.md`'s open items.
- No new Gradle modules.
- Don't relitigate AD-005 ("all implementation details MUST be `internal`") while moving files —
  keep moved classes `internal` in their new home unless cross-module visibility is the whole point
  of the move (e.g. the promoted repository interfaces, which must be `public` to be implemented
  elsewhere).
