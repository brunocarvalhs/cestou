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
  - **Scope correction (same session, before the login blocker above)**: the initial fix for bug 2
    replaced hardcoded English asserts with hardcoded pt-BR asserts — still a fixed-language
    hardcode, just a different one, so it would still break on a CI emulator (typically en-US
    default) even though it now passes on this pt-BR physical device. Caught before merging.
    Reworked to a `testTag`-based selector strategy instead: added `Modifier.testTag(...)` to every
    Compose element the 8 flows actually interact with (bottom nav, dialogs/sheets, form fields,
    buttons, settings items — ~25 files across `core/ui`, `feature/shopping`, `feature/cart`,
    `feature/products`, `feature/settings`, `feature/profile`, `feature/chat`), scoped only to
    what's touched, not a blanket sweep. Flow YAML now selects by `id:` (the testTag) instead of
    display text for anything structural; plain text is kept only where the text itself is the
    thing under test (one instance: `join_list_flow.yaml`'s invalid-token error message), and that
    one assertion is parameterized as `${JOIN_ERROR_TEXT}` and resolved by the new
    `.maestro/scripts/run.sh` from the device's actual `persist.sys.locale` at run time, not a
    fixed language either direction. `app:assembleDebug` succeeds with all the `testTag` additions;
    installed on the device (`adb install -r`, data-preserving) and `onboarding_flow.yaml` reran
    clean against the new build. Full rationale in `.maestro/README.md` "Language / locale".
  - Two more real app bugs surfaced while wiring testTags (found by reading the actual Compose
    source, not the flow — neither fixed here, both worth a tech-lead ticket): (a)
    `CartBottomBar.kt`'s purchase-trigger button is hardcoded `Text("Checkout")` — not a
    `stringResource` at all, so it never localizes and would never have matched the old English
    *or* pt-BR flow assert either way; (b) `SettingsHeader.kt`'s back-icon `contentDescription` is
    hardcoded literal `"Voltar"` (harmless on a pt-BR device, but not translated for any other
    locale).
- **2026-09-10 (same session, continued)**: While waiting on device/login, added two more flows
  identified in the earlier navigation-map audit: `cart_interactions_flow.yaml` (covers
  `ShareOptionsRoute` and `ConfirmItemRoute` in `feature/cart` — adds a second, unpriced product
  and marks it purchased to trigger the confirm-price sheet) and `settings_about_flow.yaml`
  (covers Terms/Privacy/Open Source Licenses/Release Notes in `feature/settings`; deliberately does
  not tap Support section items, which hand off to an external app with no in-app state to assert
  against). `ProductHistoryRoute` is *not* covered and moved from "not yet covered" to a documented
  known gap: it only renders once a shopping list has 2+ members (`CartScreen.kt`'s
  `showAttribution`), which a single test account structurally cannot produce — same category as
  joining a real list. Added the `Modifier.testTag`s these two new flows need (`ConfirmItemContent`,
  `ProductHistoryContent`, `ShareOptionsBottomSheet`, `QuickAddForm`'s submit button,
  `ProductHeader`'s close button, three settings screens' titles via `SettingsHeader`'s existing
  `titleTestTag` param). `test_suite.yaml` updated with correct ordering (`cart_interactions_flow`
  must run before `finish_purchase_flow`, which locks the list). `app:assembleDebug` still succeeds
  (11 flows total now). Cross-checked every `Modifier.testTag(...)` literal in source against every
  `id:` reference in every flow YAML (`comm`/`grep` diff) — one gap found and fixed
  (`edit_item_quantity_field` had a tag but nothing asserted it). Ran `maestro check-syntax` on
  `test_suite.yaml` and all 11 flow files (works without a device) — all OK. This validates YAML
  structure only, not that the `id:`s actually resolve on a running device — that still needs a
  real run.
  - Opened **PR #75** (`test/maestro-e2e-coverage` → `develop`) as a **draft**, explicitly marked
    "WORK IN PROGRESS / blocked on device" with a checklist of what's left, so the testTag approach
    and code are reviewable now instead of waiting indefinitely. Not ready to take out of draft:
    none of the 10 authenticated flows have run against a real session yet (only
    `onboarding_flow.yaml`, twice, pre-login).
  - Device connectivity has been intermittent all session (wireless adb): connected → app on
    Welcome screen (not logged in) → disconnected entirely (`adb devices -l` / `adb mdns services`
    both empty) as of this entry. Two bounded background polls (`adb devices -l` every 4–5s, ~6 and
    ~9.5 min windows) both timed out with no device found. Coordinator confirmed bruno is aware and
    intends to have QA log in and validate soon.
- **Next step**: once the device reconnects AND the app shows an authenticated session (not just
  device connectivity — confirm both before running anything), run the corrected 10-flow suite +
  `onboarding_flow.yaml` via `.maestro/scripts/run.sh`, fix whatever real drift the actual runs turn
  up (not guesses — the `settings_flow.yaml`/`account_data_flow.yaml` back-navigation step counts in
  particular are unverified assumptions, flagged inline in those files), then take PR #75 out of
  draft once cited with real pass/fail output.
- **Blockers**: device disconnected (wireless adb) as of this entry, and even once reconnected, a
  human still needs to log into the Cestou app manually (Google Sign-In) before any authenticated
  flow can run — this session's own test run cleared the session earlier and it hasn't been
  restored yet. Do not attempt to automate the Google account picker.
- **Uncommitted files**: none — everything through this entry is committed and pushed to
  `test/maestro-e2e-coverage` (commits `fd5dcf95`, `cdd0ed9c`, `065d865f`, `59b300a1`)
- **Branch**: test/maestro-e2e-coverage (PR target: develop, from `develop` @ the commit this
  branch forked from). PR: https://github.com/brunocarvalhs/How-Much/pull/75 (draft)
