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
- **Branch**: `docs/firestore-security-rules` (G5 rules proposal, this PR) → PR into `develop`.
