# STATE

## Decisions

### AD-001
- **Decision**: Multi-module Clean Architecture.
- **Reason**: Separation of concerns, scalability, and independent feature development.
- **Trade-off**: Increased boilerplate for module configuration.
- **Scope**: Entire project.
- **Date**: 2026-08-20
- **Status**: active

### AD-002
- **Decision**: Jetpack Compose for all UI components.
- **Reason**: Modern, declarative UI framework with better productivity and Material 3 support.
- **Trade-off**: Requires modern Android tooling and differs from legacy XML patterns.
- **Scope**: :app, :feature modules.
- **Date**: 2026-08-20
- **Status**: active

### AD-003
- **Decision**: Hilt for Dependency Injection.
- **Reason**: Standard Android DI solution, simplifies boilerplate for ViewModel and Repository injection.
- **Trade-off**: Compile-time overhead and less flexibility than raw Dagger.
- **Scope**: Entire project.
- **Date**: 2026-08-20
- **Status**: active

### AD-004
- **Decision**: MVI (Model-View-Intent) presentation pattern with Data Class Intent.
- **Reason**: Decouples UI from business logic using a unidirectional data flow. Data class intents (lambdas) in the ViewModel simplify communication and state management in Compose.
- **Trade-off**: Requires boilerplate for State and Intent classes for simple screens.
- **Scope**: :app, :feature modules.
- **Date**: 2026-08-24
- **Status**: active

### AD-005
- **Decision**: Modular Feature Anatomy with Internal Visibility.
- **Reason**: Enforce strict isolation and standardization. Features follow a flat structure: `data/`, `di/`, `domain/`, `presentation/`, and `navigation/`. All implementation details MUST be `internal`.
- **Trade-off**: Requires careful management of public API surface (usually just the Navigation entry point).
- **Scope**: All feature modules.
- **Date**: 2026-08-24
- **Status**: active

### AD-006
- **Decision**: Navigation 3 with NavKey (Kotlinx Serialization).
- **Reason**: Type-safe navigation, multi-backstack support, and better integration with Compose and ViewModels.
- **Trade-off**: Requires serialization boilerplate for all routes.
- **Scope**: Entire project.
- **Date**: 2026-08-24
- **Status**: active

### AD-007
- **Decision**: AI Integration via AgentActionUseCase.
- **Reason**: Expose app workflows to the internal AI agent in a structured way, enabling voice commands and system shortcuts.
- **Trade-off**: Requires registering use cases in Hilt modules with specific annotations.
- **Scope**: Core and Feature modules providing user-executable actions.
- **Date**: 2026-08-24
- **Status**: active

### AD-008
- **Decision**: Remotely-rotatable secrets read via `RemoteVariableService`, with the compiled
  `BuildConfig` value as fallback default — never as the direct source.
- **Reason**: A key compiled into the APK cannot be revoked without shipping a release. Reading it
  through Remote Config makes rotation a console operation. `FirebaseRemoteConfigService.getString`
  returns the caller's `default` when the key was never fetched/activated, so an unconfigured console
  degrades to today's behaviour instead of breaking the feature.
- **Trade-off**: The compiled fallback still ships inside the APK, so this buys *fast rotation*, not
  *secrecy* — the old key stays valid until manually revoked at the provider. Callers holding the
  client `by lazy` in a `@Singleton` (`ProductRepositoryImpl`, `RecipeRepositoryImpl`) only pick up a
  rotated key after an app restart. Callers that re-read per invocation (`AiAgentFactoryImpl.create()`)
  rotate immediately. Each call site must also guard against a blank remote value, which `getString`
  returns verbatim.
- **Scope**: Any API key or secret currently sourced from `BuildConfig` (today: `GEMINI_API_KEY`).
- **Date**: 2026-09-09
- **Status**: active — landed on `develop` via **PR #69**. All three call sites also apply the
  blank-value guard (`.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY`). The console-side
  half (publish `gemini_api_key`, revoke the old key at the provider) is **still owed by bruno**;
  until then the decision is implemented but delivers no actual mitigation.

## Handoff

- **Feature**: beta-launch (see `.specs/BETA-LAUNCH-PLAN.md` — the ordered task queue T1–T6 — and
  `.specs/MVP-ROADMAP.md` § "Beta Launch Priority" for the `pm`'s persona-based gating)
- **Phase / Task**: Execution / Beta blocker closure
- **Completed (earlier sessions)**: `feat/new-layout` merged into `develop` via PR #30 (closes G1).
  CI rebuilt around Git Flow with staged checks (PR #47). `AiChatScreen` settings-icon
  `contentDescription` fixed (F3.4). `.maestro/flows/account_data_flow.yaml` added. G11 camera
  executor leak fixed.
- **Completed (session of 2026-09-09, `tech-lead`)**: Reconciled `BETA-LAUNCH-PLAN.md` with the `pm`
  priority pass. **Resolved the flagged discrepancy: accepted the split of "G12–G16 closed" — G13 and
  G15 gate the beta; G12/G14/G16 ship inside the beta window but do not gate go/no-go.** Rationale:
  the flat grouping reflected shared discovery date (one bug audit), not shared risk. Reviewed the
  G15 fix and recorded AD-008. Corrected `ANALYTICS-PLAN.md`'s claim that `BarcodeAnalyzer` was dead
  code — it is wired from `feature/shopping`'s `QrCodeScanner`, which is exactly why G13 was real.
- **Completed (session of 2026-09-09/10, whole team — all merged into `develop`, verified against
  `origin/develop`, not against PR descriptions)**:
  - **G15** — Gemini key via Remote Config with blank-value guard — **PR #69**
  - **Analytics** — beta funnel instrumentation + `ANALYTICS-PLAN.md` — **PR #71**
  - **Coverage** — real Kover baseline **82.00% → 84.47%** line (branch 46.9% → 48.9%), zero-coverage
    gaps closed in `CloudNetwork`, four `feature/products` use cases and `core/auth.authState`, plus
    the repo's first library-module `androidTest` (G11 regression, **actually run on a device**) —
    **PR #72**, documented in `.specs/COVERAGE-BASELINE.md`
  - **G13** — QR-join scan debounce (`ScannerViewModel.isJoining` + analyzer throttle) — **PR #73**
  - **G16** — `ProductSearchViewModel` debounce + cancel-previous — **PR #77**
  - **G12** — `CartViewModel` collector leak fixed with `flatMapLatest` — **PR #78**
  - **G14** — `ProfileViewModel` reconciles the Firestore emission — **PR #79**
  - New subagents: `android-engineer-architecture` (owns G10), `android-engineer-release`,
    `android-engineer-wear` (**PR #76**). `docs/wear-qa-agent` (**PR #80**, Wear Maestro QA agent) is
    still **open**, not merged.
- **Verification honesty — read before reporting any of the above as "done"**: G12, G13, G14 and G16
  are backed by **JVM unit tests and code review only**. Nothing merged this round has been exercised
  on a device except the G11 regression test. This is accepted for their risk class (all are
  cancellation/flow-plumbing changes whose failure mode is a stale or missing UI update, never data
  corruption), but it must not be restated elsewhere as "verified". The first hardware exercise of
  these four will be the beta cohort unless F0.3 covers them deliberately.
- **In-progress / needs care**:
  - **G9 / PR #67 is NOT merely awaiting bruno's merge click** — this corrects the previous handoff.
    CI is **red**: `Detekt` fails on a single `MaximumLineLength` (>120 chars) at
    `feature/shopping/src/test/java/.../ShoppingRepositoryImplTest.kt:220` ("Analysis failed with 1
    weighted issues"), which fails the `PR Gate`; and the branch is **18 commits behind `develop`**
    (`mergeStateStatus: BEHIND`). Tracked as **T7** in `BETA-LAUNCH-PLAN.md`: wrap the line, merge
    `develop` in (do not force-push), re-request review. Merging still belongs to bruno.
  - **Analytics are code-complete but DebugView-unverified** — `ANALYTICS-PLAN.md` states this
    plainly. Merging #71 satisfied only half of that checklist line; the checklist now splits it.
    Fold the verification into the F0.3 device pass, DebugView open, one walk per funnel.
  - **Maestro / F0.3** — draft **PR #75** fixed two real suite bugs (`clearState` logging the session
    out of Google Sign-In; hardcoded English selectors replaced by `testTag`, after a first attempt
    that merely swapped them for hardcoded pt-BR). Only `onboarding_flow.yaml` has ever passed
    end-to-end; the other flows need an authenticated session on a device whose wireless adb keeps
    dropping. Do not merge and do not report the suite as passing.
  - **`.specs/BETA-KPI.md`** — owned by `marketing`, still "Draft — para revisão do `pm`". Concrete
    floors already written (activation ≥60%, purchase completion ≥50%, join success ≥70%). Needs
    `pm` sign-off, not new work. Do not create or edit it as `tech-lead`.
  - **Residual risks logged, not promoted to gates** (see `BETA-LAUNCH-PLAN.md` § "Residual risks"):
    hardcoded `Text("Checkout")` in `CartBottomBar.kt:64` (English button on the pt-BR purchase flow,
    found via PR #75), literal `"Voltar"` content descriptions in `SettingsHeader.kt:51` /
    `LinkWearDeviceScreen.kt:44`, unreachable `MobileRoutes.Notifications`, restart-scoped key
    rotation for the two `@Singleton` repositories, and `AnalyticsTracker.setUserId` never plugged in.
- **Next step**: **T7** (`android-engineer-features`) — the only open engineering item on the beta
  gate. Everything else on the gate needs bruno.
- **Blockers (all bruno, none resolvable by any agent here)**: rotate/revoke the Gemini key in the
  Firebase + Google AI Studio consoles (the code change alone mitigates nothing until the old key is
  revoked); merge PR #67 once T7 turns it green; decide G3 hosting, then wire the URL into
  `CustomMethodPickerTerms`, Settings and the Play Console field; capture G4 screenshots + feature
  graphic; confirm G5 Firestore rules in the console; run F0.3 (Maestro, authenticated session) and
  F2.2 (Google Sign-In) on a device; complete the Play Console Internal-testing track; and the
  `develop` → `master` decision, which stays exclusively his.
- **Uncommitted files**: none — the three-owner working-tree tangle described in the previous handoff
  was resolved; every piece landed on its own branch and PR.
- **Branch**: `docs/beta-readiness-recheck` (this readiness re-check) → PR into `develop`.
