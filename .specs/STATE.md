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

## Handoff

- **Feature**: launch-action-plan (see `.specs/MVP-ROADMAP.md`)
- **Phase / Task**: Execution / In progress
- **Completed**: `feat/new-layout` merged into `develop` via PR #30 (closes G1 — this doc's previous
  handoff and MVP-ROADMAP.md's "biggest risk item" were stale). CI rebuilt around Git Flow with
  staged checks (`ci/restructure-pipeline-stages`, PR #47). Fixed `AiChatScreen` settings icon
  missing `contentDescription` (accessibility/testability gap noted in `.maestro/README.md`).
  Added `.maestro/flows/account_data_flow.yaml` covering the previously-untested Delete
  all data / Delete Account confirmation sheets (cancel-only, non-destructive).
- **In-progress**: G9 (`ShoppingRepositoryImpl.updatePositions` no-op) has an open, unmerged fix —
  PR #67 (`fix/shopping-update-positions`) — review/merge is a call for the repo owner, not
  something to duplicate.
- **2026-09-09 (this session, `test/maestro-e2e-coverage`)**: First-ever real execution of the
  Maestro suite (Samsung SM-A146M, Android 15, `persist.sys.locale=pt-BR`, wireless adb). Confirmed
  two bugs in the suite itself (not the app):
  1. `home_flow.yaml`'s `launchApp: clearState: true` logs the device out of Google Sign-In (local
     session only) instead of landing on an authenticated empty home. Fixed by splitting off
     `.maestro/flows/onboarding_flow.yaml` (the only flow allowed to use `clearState`, deliberately
     excluded from `test_suite.yaml`) and rewriting `home_flow.yaml` to assume an existing session
     and not assert an empty-list state (a real persisted account may already have lists synced
     from Firestore).
  2. All 8 flows asserted hardcoded English strings (`values/strings.xml`) against the device's
     real pt-BR strings (`values-pt-rBR/strings.xml` per module). Audited and fixed every flow.
     Also found and fixed, while auditing: `create_list_flow.yaml` tapped a stale screen-percentage
     point (`77%,4%`) for the create-list FAB instead of its actual content description
     (`"Criar lista"`); `product_management_flow.yaml` asserted a `"Unit"` field in the Edit Product
     sheet that does not exist in `EditItemContent.kt` (only Product Name / Category / Unit Price /
     Quantity do) — removed.
  - `onboarding_flow.yaml` ran against the device and **passed in full** (all 5 assertions).
  - **Effect of this session's own test run**: running `onboarding_flow.yaml`'s `clearState` (and
    an earlier, prior-session run that first surfaced the two bugs) logged the device out. All
    other flows need an authenticated session and cannot run until a human logs back in via
    "Continuar com Google" on the device — this cannot be automated (real Google account picker/
    OAuth consent, out of scope for Maestro). **Paused here, waiting on bruno to log in manually**
    before running the rest of the corrected suite and any new coverage flows.
  - Real app finding surfaced during navigation-map audit, not fixed here (belongs to
    android-engineer-features via tech-lead triage): `core/navigation/mobile/MobileRoutes.kt`'s
    `Notifications` destination is registered in `ShoppingGraph.kt` but has no reachable UI entry
    point anywhere in the app (`core/ui`'s `content_description_join_list` string is also dead —
    unused, superseded by `shopping_management_button_join`). Neither blocks Maestro coverage, both
    worth a ticket.
- **Next step**: once bruno confirms a manual login on the device, resume running the corrected
  8-flow suite + `onboarding_flow.yaml`, fix whatever real drift the actual runs turn up (not
  guesses), then audit `core/navigation` + each `feature/*/navigation` entry point against the 8
  existing flows to add coverage for currently-untested screens (candidates identified so far:
  `ProductHistoryRoute`/`ConfirmItemRoute`/`ShareOptionsRoute` in `feature/cart`, the About/Support
  legal screens in `feature/settings`, the AI/Shopping settings sub-screens).
- **Blockers**: device session logged out by this session's own test run — needs bruno to log back
  into the Cestou app manually (Google Sign-In) before any further authenticated flow can run.
- **Uncommitted files**: none (this session's changes are on `test/maestro-e2e-coverage`, PR not
  yet opened — waiting to finish the run before requesting tech-lead review)
- **Branch**: test/maestro-e2e-coverage (PR target: develop, from `develop` @ the commit this
  branch forked from)
