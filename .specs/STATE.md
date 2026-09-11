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

### AD-009
- **Decision**: Firestore access control lives in a versioned `firestore.rules` at the repo root
  (with a minimal `firebase.json` pointing at it), derived from the data layer, and is the single
  authoritative authorization boundary. Client-side ownership checks (e.g.
  `ShoppingListViewModel` gating delete on `roles[uid] == OWNER`) are UX affordances that the rules
  now mirror, never the security boundary itself. **Proposed on `docs/firestore-security-rules`
  (PR #84); not deployed** — the console still runs the default placeholder
  (`allow read, write: if request.time < <date>`), which grants anyone with the project config
  full read/write/delete over every collection. Deploying is bruno's call:
  `firebase deploy --only firestore:rules --project cestou-86785`.
- **Reason**: G5. There is no server tier (Spark plan, no Cloud Functions — see AD-001 context in
  `MVP-ROADMAP.md`), so *every* write is a client write and rules are the only place authorization
  can exist at all. Keeping them in the repo makes them reviewable, diffable, and testable instead
  of being console state nobody can audit.
- **Data model the rules are derived from** (every Firestore path the app actually touches; recipes
  come from TheMealDB and product search from OpenFoodFacts over plain HTTP via `CloudNetwork`, and
  app settings live in DataStore, so none of those are collections):

  | Path | Written by | Access model |
  |---|---|---|
  | `/shopping/{id}` | `ShoppingRepositoryImpl` | shared list — `users: List<String>` + `roles: Map<uid, "OWNER"\|"EDITOR">` |
  | `/shopping/{id}/products/{id}` | `ProductRepositoryImpl` | inherits the parent list's membership |
  | `/users/{uid}` | `UserRepositoryImpl` | own document; readable by other signed-in users |
  | `/users/{uid}/common-products/{id}` | `CommonProductRepositoryImpl` | strictly private |
  | `/notifications/{id}` | `NotificationRepositoryImpl` | recipient reads; any signed-in user may create one addressed to someone else |

- **Rule-by-rule reasoning**:
  - **`/shopping` read** — membership only (`uid in users`). `observeAll/getAll` query
    `whereArrayContains("users", uid)`, the one query shape Firestore can prove safe against this
    rule. Anything looser means "any signed-in user can dump every shopping list", which is the
    hole being closed.
  - **`/shopping` create** — creator must be inside `users`, `roles` may only name actual members,
    and the `id` field must equal the document id (`FirebaseFirestoreManager.post()` writes at
    `data["id"]`, so a mismatch means tampering). It deliberately does *not* require the creator to
    be `OWNER`, because `ShoppingDuplicateUseCase` copies `users`/`roles` verbatim (see open
    questions).
  - **`/shopping` update** — three shapes: (a) owner may change anything but must stay a member,
    since a self-lockout is unrecoverable without a server tier; (b) a non-owner member may edit
    content but not `users`, `roles` or `shortCode` — normal edits rewrite the whole document with
    those fields unchanged, so only escalation attempts trip it; (c) a non-member may append
    *itself* to `users` and nothing else, which is exactly `ShoppingRepositoryImpl.join()`
    (`{"users": [uid]}`, resolved to an `arrayUnion`). The joiner therefore cannot add third
    parties, drop members, grant itself a role or edit content in the same write.
  - **`/shopping` delete** — owner only, making the existing client check authoritative.
  - **`/shopping/{id}/products`** — everyone on a list is a peer editor of its items; that is the
    product. Access mirrors the parent document's membership via `get()` (one extra document read
    per evaluation, acceptable at this volume, and the only way to express it). The `FINISH` lock
    (`ProductRepositoryImpl.checkIsLocked`) is intentionally *not* enforced: it is a product rule,
    not a trust boundary, and moving it server-side would be an unrelated behaviour change.
  - **`/users/{uid}` read** — any signed-in user may `get` a profile, `list` is denied. Wider than
    "own profile" on purpose: `CartViewModel.resolveMemberProfiles`, `ProductSaveUseCase` and
    `QuickAddViewModel` resolve the names/photos of *other* members, and a rule cannot ask "do
    these two users share a list?" without a list id in the request. Denying `list` keeps the
    collection non-enumerable, so exposure is bounded by already knowing a uid.
  - **`/users/{uid}` write** — own document only, field-allowlisted, `create` included because
    `UserRepositoryImpl` only ever PUTs (→ `update()`, which fails on a missing document).
  - **`/users/{uid}/common-products`** — private despite the name; the path is always built from
    the caller's own id and the "cesta básica" defaults are seeded per user.
  - **`/notifications`** — the one legitimate cross-user write, because with no Cloud Functions the
    triggering client is the writer (`ShoppingJoinUseCase`, `FinishPurchaseViewModel`). Reads are
    recipient-only (matching `whereEqualTo("userId", uid)`), creation is pinned to the exact
    `NotificationModel` shape with the two real `type` values, `isRead == false` and size caps so
    the collection cannot be used as arbitrary storage, updates may only flip `isRead` to true, and
    deletes are denied because no code path deletes.
  - **catch-all `if false`** — explicit so a collection added by a future feature fails loudly in
    development instead of silently inheriting a broader rule.
- **Trade-off / known breakage on deploy**:
  1. **Join by short code stops working.** `getByShortCode` queries
     `whereEqualTo("shortCode", code)` as a non-member, and rules cannot inspect a query's
     where-clauses — "read only the list whose code I know" is inexpressible without also allowing
     "read every list". Fix (client work, not a rules change): a `shortCodes/{CODE}` index document
     holding only `{ shoppingId }`, readable by any signed-in user, written by the owner at create
     time; the joiner resolves code → id, performs the blind self-join (already allowed by rule
     (c)), and only then reads the list as a member. Not added to `firestore.rules` here because
     the collection does not exist yet.
  2. **Wear OS traffic stops working entirely.** The watch never signs in to Firebase — pairing
     just copies the phone's uid into DataStore (`WearAuthListenerService` → `updateUserId`), so
     `request.auth` is null on every watch request while `AuthService` still reports an id. Any
     uid-based rule denies it. Needs a real credential on the watch (its own sign-in, or passing a
     custom/ID token during pairing).
- **Open questions for bruno (do not resolve by loosening rules)**:
  - `ShoppingDuplicateUseCase` copies `users`, `roles` *and* `shortCode` from the source list, so
    duplicating a shared list silently shares the copy with the same people, leaves the duplicator
    as a non-owner of their own copy, and creates a second list answering to the same share code
    (`getByShortCode` takes `firstOrNull`). Should duplicate reset ownership/membership/code?
  - `NotificationModel` has no `senderId`, so the rules cannot verify that a notification's writer
    shares a list with its recipient — any signed-in user who knows a uid can send that user a
    well-formed notification. Closing it needs `senderId` + `shoppingId` on the model.
  - `users/{uid}` stores `email` alongside name/photo, and co-members need to read the document.
    Split public (name/photo) from private (email) fields?
  - No "leave list" action exists: with owner-only delete, a guest's only exit is
    `DeleteAccountUseCase`, whose `users - userId` write is itself a no-op because
    `FirebaseFirestoreManager` maps `users` through `arrayUnion` (removals are impossible through
    the current data layer).
- **Verification**: `firestore-tests/rules.test.mjs` — 60 assertions against the local Firestore
  emulator (`firebase emulators:exec --only firestore --project demo-cestou`), all green, covering
  every rule above including the join, escalation and notification-forgery attempts. Local only; it
  never contacts the real project and is not wired into Gradle/CI.
- **Scope**: All Firestore access from `:app`, `:wear` and every `feature/*` data layer.
- **Date**: 2026-09-10
- **Status**: proposed — awaiting bruno's review and manual deploy

### AD-010
- **Decision**: App Check provider coverage is a **release gate**, symmetric with AD-009's rules gate:
  App Check enforcement must not be enabled in the console for any product (Auth, Firestore) while
  release builds install **no** provider. Either enforcement stays off, or
  `FirebaseInitializer` grows a real `PlayIntegrityAppCheckProviderFactory` on the non-debug branch
  *first*. A provider installed only behind `if (BuildConfig.DEBUG)` is to be treated as an
  incomplete implementation, not as a working one.
- **Reason**: `core/common/.../initializer/FirebaseInitializer.kt` installs
  `DebugAppCheckProviderFactory` and calls `getAppCheckToken(false)` entirely inside
  `if (BuildConfig.DEBUG)`. There is no `else`, and a repo-wide grep finds no Play Integrity (or any
  other) provider factory anywhere in `:app`, `:wear`, `core/*` or `feature/*`. Nobody has checked
  whether enforcement is currently on for project `cestou-86785`. If it is — or if it is ever
  switched on without this code landing — every enforced request from a **production** build fails
  outright, with no staged rollout and no way to fix it without shipping a release. Unlike AD-009,
  whose failure mode is "too permissive", this one's failure mode is "the app stops working for
  everyone at once".
- **Evidence that the mechanism is live and already bites** (see the Handoff's App Check entry for
  the full logcat chain): on the debug build, a 403 `App attestation failed` from
  `NetworkClient.exchangeAttestationForAppCheckToken` is followed ~14s after cold launch by
  `FirebaseAuth` "Notifying id token/auth state listeners about a sign-out event". A failed
  attestation is therefore not silently degraded — it can cost the user their session.
- **Trade-off**: Play Integrity needs the app registered in the Play Console and a SHA-256 per
  signing key, so it is real console setup, not a one-line code change; and it makes the debug
  token allowlist an ongoing chore (a fresh debug token per install/data-reset). Accepted, because
  the alternative is enforcement that either does nothing or breaks production.
- **Secondary finding, same area (not the decision itself)**: `libs.firebase.appcheck.debug` is
  declared as plain `implementation` in `core/common/build.gradle.kts:51`,
  `core/data/build.gradle.kts:67` and `app/build.gradle.kts:103` — i.e. the debug App Check
  provider library **ships inside release APKs**. Harmless today only because the provider is never
  installed outside `BuildConfig.DEBUG`; it should become `debugImplementation` when this is
  picked up. Small, isolated, and deliberately **not** to be bundled into an unrelated PR.
- **Scope**: `core/common` (`FirebaseInitializer`), the three build files above, and the Firebase
  console's App Check enforcement settings for `cestou-86785`.
- **Date**: 2026-09-10
- **Status**: proposed — **blocked on bruno**, who alone has console access. Nothing to implement
  until the enforcement status is known: if enforcement is off, this is a pre-production task with a
  known deadline; if it is on, it is an incident. Do not implement a provider speculatively and do
  not enable enforcement to find out.

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
  - **Maestro / F0.3** — draft **PR #75** (branch `test/maestro-e2e-coverage`) fixed two real suite
    bugs (`clearState` logging the session out of Google Sign-In; hardcoded English selectors
    replaced by `testTag`, after a first attempt that merely swapped them for hardcoded pt-BR).
    **Session of 2026-09-10 (this handoff)** extended the same branch: added
    `list_management_flow.yaml` (list-card long-press menu + Edit-list screen + the QR invite
    screen it opens — a destination previously reachable but untested from either of its two entry
    points), and extended `settings_flow.yaml` to actually drill into AI/Shopping/Notifications
    (previously only their section headers were asserted visible, never entered), and
    `cart_interactions_flow.yaml` to follow "Invite to Collaborate" through to the same QR screen
    instead of backing out (confirmed safe by reading `ShareOptionsViewModel`/
    `EditShoppingViewModel` — both reuse the list's existing `shortCode`, no new Firestore token
    minted). ~15 new `testTag`s added across `feature/shopping` and `feature/settings` Compose
    files to support this. **Verified against a real USB-connected device (Samsung SM-A146M,
    `RQCWB07B4WT`) this session, not assumed**: `onboarding_flow.yaml` runs and passes end-to-end
    on a freshly rebuilt debug APK — but only after fixing a *second*, independently-discovered
    regression (see below). **None of the other 12 flows have been run this session**: the device's
    session was logged out (by this session's own `onboarding_flow.yaml` run, same as last time),
    and per this suite's own authentication policy nobody should automate the Google account
    picker to get it back — **needs bruno to log in manually on `RQCWB07B4WT` via "Continuar com
    Google"**, then re-run `.maestro/scripts/run.sh` (or `test_suite.yaml` + `onboarding_flow.yaml`
    separately) to actually verify the other 12. Do not merge and do not report the suite as
    passing beyond `onboarding_flow.yaml`.
  - **Maestro / F0.3 — later the same day (2026-09-10), the above ask was satisfied and the suite
    still did not run. Root cause is an app-side bug, not a flow bug.** bruno signed in manually on
    `RQCWB07B4WT` via "Continuar com Google"; the authenticated home screen with real lists was
    confirmed by screenshot (~21:42). `.maestro/scripts/run.sh` (`test_suite.yaml`, 11 non-onboarding
    flows) was then started and the **first** flow, `home_flow.yaml`, failed at
    `assertVisible: {id: "nav_ShoppingList"}` — the app was back on the login screen. Not a
    rendering race: a screenshot over a minute after the run still showed logged-out. So the
    tally for this cycle is: `onboarding_flow.yaml` **passes**, `home_flow.yaml` **attempted and
    failed for an app-side reason**, and the other **10** flows (`create_list_flow`,
    `list_management_flow`, `product_management_flow`, `cart_interactions_flow`,
    `finish_purchase_flow`, `join_list_flow`, `settings_flow`, `settings_about_flow`,
    `account_data_flow`, `chat_flow`) **have not been exercised at all**. `maestro-qa-engineer`
    deliberately did not attempt them: every flow starts with a cold `launchApp` and every flow
    takes longer than the ~14s window described below, so all ten would fail identically and the
    failures would be meaningless. Re-signing in by hand every ~14s is not practical and will not
    be automated. **The suite remains blocked. It is not green and must not be described as such.**
  - **App Check attestation failure forces a sign-out ~14s after every cold launch (debug builds) —
    new, real app bug, verified on device, and the most consequential finding on this branch so
    far.** This is a *different and bigger* problem than the two suite bugs PR #75 already fixed
    (`clearState` clearing the Google session; hardcoded English selectors): those were defects in
    the test suite, this one is in the app and would affect real users under the right console
    configuration. Chain, from logcat on the exact Maestro-launched process (PID 27921) on
    `RQCWB07B4WT` (Samsung SM-A146M):
      1. `21:42:50.661` — `DebugAppCheckProvider` logs its debug secret.
      2. `21:42:52.505` — `FirebaseException: Error returned from API. code: 403 body: App
         attestation failed.` at `NetworkClient.exchangeAttestationForAppCheckToken`.
      3. `21:43:06.434` — `FirebaseAuth`: "Notifying id token/auth state listeners about a sign-out
         event" — ~14s after cold launch, ~4s after the App Check retries stop.
    The 403 means this device's **current** App Check debug token is not in the console's debug-token
    allowlist for `cestou-86785`. Debug tokens are regenerated per app install / data reset, so every
    full reinstall (which this branch's work does repeatedly) invalidates the previous allowlist
    entry. Firebase Auth then appears to treat the failed attestation as grounds for a forced local
    sign-out. **This plausibly explains the entire "device keeps ending up logged out" pattern that
    has recurred across this multi-session saga** — previously attributed solely to
    `onboarding_flow.yaml`'s `clearState`. That `clearState` diagnosis was correct but only partial;
    treat it as one of two causes, not the cause. Source:
    `core/common/src/main/java/br/com/brunocarvalhs/howmuch/core/common/initializer/FirebaseInitializer.kt`.
    The release-build half of this — no App Check provider at all outside `BuildConfig.DEBUG` — is
    recorded as **AD-010** above, because it is a production risk independent of Maestro.
  - **Connectivity is no longer a suspect.** The NETWORK_ERROR / Wi-Fi-outage hypothesis raised
    earlier in this same session is **resolved and superseded**: the device now runs on a phone
    hotspot ("S23+ bruno") with fully validated internet, and the sign-out reproduces anyway. Do not
    re-litigate the network angle; the App Check 403 above is the blocker.
  - **Real regression found in the suite itself, not the app**: `onboarding_flow.yaml`'s old
    single-sentence terms assertion started failing on a fresh build this session — traced to
    `fix/wire-legal-urls` (PR #83, already merged into `develop`, pulled into this branch by a
    routine `develop` merge) splitting `CustomMethodPickerTerms` into six separate `Text` nodes so
    "Termos de Uso" / "Política de Privacidade" can be individually clickable links. Not an app
    bug — intentional UI — but a reminder that plain-text flow assertions can go stale from *any*
    unrelated UI PR, not just locale drift; fixed in the flow (see its header comment).
  - **Real app bug found (not fixed here, flagged for `tech-lead`)**:
    `EditShoppingViewModel.shareToken()` (`feature/shopping`) navigates to the QR invite screen
    correctly but never updates `EditShoppingUiState.sharingToken`, so `EditShoppingContent.kt`'s
    "already generated a token" Card branch can never render — dead state, cosmetic only (the
    QR screen itself still works via the Edit-list entry point).
  - **Maestro / F0.3 — session of 2026-09-11 (same branch, same device, now reconnected wirelessly
    as `adb-RQCWB07B4WT-EicF2t._adb-tls-connect._tcp`).** Re-verified everything staged from
    2026-09-10 against the actual codebase before running anything (every `id:` referenced by every
    flow was grepped against real `testTag(...)` call sites in `feature/*`/`core/ui` — all resolve;
    none are stale). Rebuilt a fresh debug APK (picks up the uncommitted `testTagsAsResourceId` fix
    in `MainActivity.kt` and the ~15 new `testTag`s) and reinstalled with `adb install -r` — the
    device had no prior install, so this run started from a clean, already-logged-out state (no
    session was destroyed to get there). Found and fixed **two additional real suite bugs while
    actually running flows, not by inspection**:
    1. `login_flow.yaml` had drifted from its own documented scope: two lines
       (`waitForAnimationToEnd` + `tapOn: "Sign in"`) had been appended after the
       `welcome_google_button` tap that actually drove the Google OAuth consent screen's "Sign in"
       button — directly contradicting the flow's own header comment and the README's
       "Authentication" section, both of which say this flow "stops immediately after" the tap and
       never touches the picker. Removed; flow now stops at the tap as documented.
    2. `onboarding_flow.yaml`'s fix from 2026-09-10 (splitting the terms sentence into fragment
       asserts) was itself still wrong: `assertVisible: "Ao continuar, você concorda com nossos "`
       (trailing space) failed on a real run. Screen-hierarchy JSON dump confirmed the rendered
       node's text has no trailing space — `welcome_terms_prefix` (`feature/auth` `strings.xml`) is
       declared with a trailing space in the unquoted XML source, but Android's resource compiler
       trims leading/trailing whitespace on unquoted string resources, so the space never survives
       into the compiled string. Fixed the assertion (dropped the trailing space) and switched the
       two link assertions to their existing `testTag`s (`welcome_terms_of_use_link` /
       `welcome_privacy_policy_link`, already present in `CustomMethodPicker.kt`, previously unused
       by any flow) instead of plain text, per the standing testTag-over-text convention.
    **Confirmed working on device, not assumed**: `onboarding_flow.yaml` now passes end-to-end
    (`maestro test .maestro/flows/onboarding_flow.yaml`, full COMPLETED output, 0 failures) — this
    is also the first real confirmation that the `testTagsAsResourceId` fix from 2026-09-10 actually
    works on a device (`welcome_terms_of_use_link`/`welcome_privacy_policy_link` `id:` selectors
    resolved). `login_flow.yaml` also passes end-to-end after the fix above, and reaching its final
    `waitForAnimationToEnd` left the real Google account-picker sheet on screen (bruno's own account,
    "Cancel"/"Sign in" buttons) — confirming the tap surfaces the picker exactly as documented, and
    per this suite's authentication policy the flow correctly does not touch it. **Session paused
    here, blocked on bruno**: the picker is sitting on screen; tapping "Sign in" is a one-tap action
    only bruno should take (see "Authentication" policy — no agent automates this). None of the
    other 11 flows (`home_flow` through `chat_flow`) or `test_suite.yaml` could be run this session
    without an authenticated session past that picker. Whether the 2026-09-10 App Check
    forced-sign-out (below) still reproduces on this fresh install is therefore **unknown and
    unverified this session** — it needs to be watched for as soon as an authenticated run is
    attempted, not assumed fixed or assumed still broken.
  - **`.specs/BETA-KPI.md`** — owned by `marketing`, still "Draft — para revisão do `pm`". Concrete
    floors already written (activation ≥60%, purchase completion ≥50%, join success ≥70%). Needs
    `pm` sign-off, not new work. Do not create or edit it as `tech-lead`.
  - **Residual risks logged, not promoted to gates** (see `BETA-LAUNCH-PLAN.md` § "Residual risks"):
    hardcoded `Text("Checkout")` in `CartBottomBar.kt:64` (English button on the pt-BR purchase flow,
    found via PR #75), literal `"Voltar"` content descriptions in `SettingsHeader.kt:51` /
    `LinkWearDeviceScreen.kt:44`, unreachable `MobileRoutes.Notifications`, restart-scoped key
    rotation for the two `@Singleton` repositories, and `AnalyticsTracker.setUserId` never plugged in.
- **Next step**: **T7** (`android-engineer-features`) — the only *unblocked* open engineering item on
  the beta gate. AD-010 (release-build App Check provider) is now a second open engineering item, but
  it cannot start until bruno answers the enforcement question; do not begin it speculatively.
  Everything else on the gate needs bruno.
- **Blockers (all bruno, none resolvable by any agent here)**: rotate/revoke the Gemini key in the
  Firebase + Google AI Studio consoles (the code change alone mitigates nothing until the old key is
  revoked); merge PR #67 once T7 turns it green; decide G3 hosting, then wire the URL into
  `CustomMethodPickerTerms`, Settings and the Play Console field; capture G4 screenshots + feature
  graphic; confirm G5 Firestore rules in the console; run F0.3 (Maestro, authenticated session) and
  F2.2 (Google Sign-In) on a device; complete the Play Console Internal-testing track; and the
  `develop` → `master` decision, which stays exclusively his.
- **New bruno-only blockers from 2026-09-10 (App Check, console access — see AD-010). These now
  gate F0.3 ahead of everything else on the Maestro branch**:
  1. **Check whether App Check enforcement is enabled for Authentication and Firestore in project
     `cestou-86785`.** This single answer decides whether AD-010 is a scheduled task or an incident,
     and no agent here can look it up.
  2. **Add this device's current App Check debug token to the console allowlist** so the Maestro
     suite can finish. The token changes on every reinstall/data reset — grab a fresh one from
     logcat (`DebugAppCheckProvider` logs it on launch) each time, do not reuse an old entry.
  3. **Decide whether release builds get a real `PlayIntegrityAppCheckProviderFactory`** before any
     App Check enforcement reaches production. Separate call from (1) and (2); see AD-010 for the
     trade-off and for the `debugImplementation` clean-up that should ride along with it, in its own
     PR, not folded into the Maestro branch.
- **New bruno-only blocker from 2026-09-11 (immediate, unblocks the rest of F0.3 today)**: the
  device (`RQCWB07B4WT`, reconnected wirelessly) has a fresh debug install sitting on the real
  Google account-picker sheet (`login_flow.yaml`'s last step surfaced it, as designed) —
  **tap "Sign in" (or Cancel, then sign in manually via "Continuar com Google") on the device to
  establish an authenticated session**, then say so, so the remaining 11 flows + `test_suite.yaml`
  can actually be run this session instead of staying "unexercised" for a third session running.
  While there, watch for whether the 2026-09-10 App Check forced-sign-out (below) reproduces on
  this fresh install/new debug token — if `home_flow.yaml` fails back onto the login screen again,
  that's the same bug recurring and items 1–2 above are the fix, not a new flow bug.
- **Uncommitted files**: on `test/maestro-e2e-coverage`, work across 2026-09-10 and 2026-09-11 is
  **staged/modified but not yet committed** — the `.maestro/` flow additions and fixes (including
  this session's `login_flow.yaml`/`onboarding_flow.yaml` corrections), the ~15 `testTag`s across
  `feature/shopping` and `feature/settings`, the still-uncommitted `testTagsAsResourceId` change in
  `app/.../MainActivity.kt`, and the `.specs/STATE.md` + `.specs/MVP-ROADMAP.md` updates (including
  this entry, the App Check entry, and AD-010). Commit them together as the branch's next commit;
  nothing here belongs on `develop` directly. The earlier three-owner tangle from the previous
  handoff stays resolved — every piece of *that* landed on its own branch and PR.
- **Branch**: `test/maestro-e2e-coverage` (draft **PR #75**) → PR into `develop`. Still draft, still
  not mergeable as a green suite: as of 2026-09-11, 2 of 13 flows have actually been run and pass
  (`onboarding_flow.yaml`, `login_flow.yaml`, both pre-login and therefore runnable without an
  authenticated session), 11 remain unexercised this session pending the sign-in ask above. Do not
  report the suite as passing beyond those 2. `docs/firestore-security-rules` (PR #84, AD-009)
  merged into `develop` and is no longer the working branch.
