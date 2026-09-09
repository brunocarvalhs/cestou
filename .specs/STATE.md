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
- **Status**: active (introduced by the G15 fix on `fix/gemini-key-remote-config`)

## Handoff

- **Feature**: beta-launch (see `.specs/BETA-LAUNCH-PLAN.md` — the ordered task queue T1–T6 — and
  `.specs/MVP-ROADMAP.md` § "Beta Launch Priority" for the `pm`'s persona-based gating)
- **Phase / Task**: Execution / Beta blocker closure
- **Completed (earlier sessions)**: `feat/new-layout` merged into `develop` via PR #30 (closes G1).
  CI rebuilt around Git Flow with staged checks (PR #47). `AiChatScreen` settings-icon
  `contentDescription` fixed (F3.4). `.maestro/flows/account_data_flow.yaml` added. G11 camera
  executor leak fixed.
- **Completed (this session, `tech-lead`)**: Reconciled `BETA-LAUNCH-PLAN.md` with the `pm` priority
  pass. **Resolved the flagged discrepancy: accepted the split of "G12–G16 closed" — G13 and G15 gate
  the beta; G12/G14/G16 ship inside the beta window but do not gate go/no-go.** Rationale: the flat
  grouping reflected shared discovery date (one bug audit), not shared risk. Reviewed the G15 fix and
  recorded AD-008.
- **In-progress / needs care before touching code**:
  - **G15 fix is uncommitted and on the wrong branch.** The working tree carries a near-complete fix
    (`AiAgentFactoryImpl.kt`, `ProductRepositoryImpl.kt`, `RecipeRepositoryImpl.kt`, their tests,
    `feature/products/build.gradle.kts`) while HEAD is `feat/beta-analytics-instrumentation`. The
    correct branch `fix/gemini-key-remote-config` exists and is currently equal to `develop`. Moving
    is verified safe (no overlap between the modified paths and the analytics commit). Full recovery
    procedure is task **T1 step 0** in `BETA-LAUNCH-PLAN.md` — never `checkout -f`/`reset --hard`/
    `clean`. Review verdict: all three G15 call sites are covered (`GeminiAiAgent` needs no edit — the
    key is injected via its existing `apiKey` constructor default from its only construction site),
    but the fix is **not ready**: no regression test asserts remote-value-vs-fallback, and a blank
    remote value is returned verbatim and would break all AI at once.
  - **G9** — fix open and unmerged as PR #67; a repo-owner merge decision, not work to duplicate.
  - **Analytics** — `data-engineer` committed instrumentation on `feat/beta-analytics-instrumentation`
    (tests green), documented in `.specs/ANALYTICS-PLAN.md`. `tech-lead` found one factual error in
    that doc: it calls `BarcodeAnalyzer.onBarcodeScanned` dead code, but it is wired from
    `feature/shopping`'s `QrCodeScanner` → so it *is* live and *is* the G13 blocker. `data-engineer`
    to correct its own document. This cross-module import is also a fresh instance of G10/AD-005.
  - **`.specs/BETA-KPI.md`** — owned by `marketing`, in progress. Do not create or edit it.
- **Next step**: Release the Android engineers on the T1–T6 queue in `BETA-LAUNCH-PLAN.md`.
  `android-engineer-features` starts at T1 (G15); `android-engineer-quality` starts T3 (Jacoco
  baseline) in parallel. T4 (G16) additionally waits on the analytics branch merging, since both edit
  `ProductSearchViewModel.kt`.
- **Blockers**: No adb/emulator here — F0.3 (Maestro) and F2.2 (Google Sign-In) can only be authored
  or reviewed statically, never reported as passing. G3 hosting, G5 Firestore rules, G4 screenshots,
  the post-T1 Gemini key rotation/revocation, and any `develop` → `master` decision are all bruno's.
- **Uncommitted files**: **yes — do not `git add .`.** The working tree mixes three owners' work:
  the G15 code fix (8 paths, `android-engineer-features` → `fix/gemini-key-remote-config`),
  `.specs/MVP-ROADMAP.md` (`pm`'s Beta Launch Priority section), and
  `.specs/BETA-LAUNCH-PLAN.md` + `.specs/STATE.md` (`tech-lead`, this session). Each belongs to a
  different branch/PR. `tech-lead` deliberately did not commit, to avoid entangling them further —
  bruno should confirm how to split these before anyone commits.
- **Branch**: HEAD is `feat/beta-analytics-instrumentation` (wrong for the uncommitted work — see
  above). Target branch for the G15 fix: `fix/gemini-key-remote-config` → PR into `develop`.
