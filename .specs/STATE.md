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
- **Decision**: For the AI chat, **delete the cross-feature coupling instead of formalizing it**.
  `feature/chat` becomes a leaf module that no other feature module imports. Concretely: (a) the
  conversation moves out of `AiChatViewModel` into an `internal @Singleton AiConversationStore`
  owned by `feature/chat`; (b) `core/navigation`'s `AiChat` route gains an optional `shoppingId`;
  (c) `Options.AI` inside `feature/products` stops rendering `AiChatScreen` inline and navigates to
  that route; (d) `AiChatScreen`/`AiChatViewModel` become `internal`. **No `core/chat` module and no
  new contract in `core/ai` is created.**
- **Reason**: G17/CHAT-01. The brief asked whether unifying the AI surface should extract a shared
  contract into `core/ai` or a new `core/chat`. Once each feature reaches the chat through the
  `core/navigation` route — which AD-005 already defines as a feature's public surface — there is no
  cross-feature *type* left to share, so a contract module would name a dependency that no longer
  exists and widen the `core/*` graph for zero call sites. The `@Singleton` store (not shared
  back-stack entries) is what makes CHAT-01 AC1/AC5 hold, because a route carrying a `shoppingId`
  argument is by definition a distinct `NavBackStackEntry` with its own `ViewModelStore` — entry
  identity can never be the mechanism.
- **Trade-off**: The conversation is process-scoped, not persisted — the spec's Edge Case claiming
  history survives a force-close describes behaviour the code has never had (see OQ-1, bruno's
  call). A single shared conversation also means switching between shopping lists carries the
  previous list's messages forward; mitigated by appending a visible context-switch divider rather
  than silently re-pointing the context.
- **Forward-compatibility (CHAT-02 is blocked, not cancelled)**: `ChatMessage.Sender.PARTICIPANT` is
  retained and explicitly protected against "unused symbol" cleanup by a unit test;
  `ChatMessage` gains `senderId: String? = null` now, while the model is cheap and unpersisted;
  the store is conversation-shaped, so a participant thread is an added key, not a rewrite. **If**
  CHAT-02 later needs another module to observe participant messages, *that* is when the store's
  interface gets promoted to `core/chat` — a file move plus a Gradle line, deliberately deferred.
- **Side effect on G10**: closes two edges — `feature/cart → feature/chat` and
  `feature/products → feature/chat`, both including the Gradle dependency. Does not attempt the rest
  of G10.
- **Amendment 2026-09-11 (bruno, OQ-2)**: the **bottom-nav "AI Assistant" tab is removed**. The AI
  is reachable only from inside a shopping list, where it has context — the same call IAA-03 made
  for the add-item flow, one level up. Touches `CestouBottomNavigation.kt` (delete
  `BottomNavItem.AiChatItem`), `MainActivity.kt:118` (`rootRoutes` loses `AiChat`) and the now-unused
  `nav_ai_chat` string in three locales. A breakage check found nothing depending on the tab: the
  `NavHost` start destination is `ShoppingList`/`Welcome`, no onboarding path routes through the
  chat, `AiSettings` is reached from the chat's own top bar, and Wear has no AI destination. One
  intended visual consequence: the bottom bar now *hides* while the chat is open
  (`showBottomBar = currentRoute != null`), matching every other detail destination. The nav bar
  drops to two items, below Material's 3–5 guidance — accepted; replacing the bar is a visual
  follow-up, not a reason to reopen the decision. **The conversation store is still required** —
  re-entering the chat pops and re-pushes the destination, producing a new ViewModel, so one entry
  point does not make shared state unnecessary.
- **Scope**: `feature/chat`, `feature/cart`, `feature/products`, `core/navigation`, `core/ui`, `:app`.
- **Date**: 2026-09-11
- **Status**: active — design approved (`.specs/features/chat/design.md`), amended after bruno
  resolved OQ-1/OQ-2, **not yet implemented**. No open questions remain for CHAT-01.

### AD-011
- **Decision**: The add-item redesign (PROD-04/PROD-05) stays **entirely inside `feature/products`**.
  No `core/*` growth, no new module. The add-item sheet drops its nested `NavHost` in favour of a
  `rememberSaveable` mode, and the camera becomes a bounded viewport inside the same sheet rather
  than a nested destination. The new recipes entry point is a public route in
  `feature/products/navigation/`, consumed by `feature/shopping`, which already depends on that
  module.
- **Reason**: G18/G19. Verified while designing: `ProductScreen` is **already** hosted in a
  `ModalBottomSheet` (`ProductsGraph`'s `dialog<ProductPickerRoute>`), so PROD-04 is a content
  refactor inside an existing container, not a new presentation mode — and the camera already runs
  inside that sheet's dialog window today. The nested `NavHost` is also the direct cause of the
  `LocalViewModelStoreOwner.current!!` at `ProductScreen.kt:46`: the sub-destinations' back-stack
  entries are not the entry carrying `ProductPickerRoute`, which every one of those ViewModels reads
  via `savedStateHandle.toRoute()`. Removing the nested graph removes the `!!`.
- **Trade-off**: Promoting the camera components (`CameraPreview`/`CameraCaptureView`/
  `BarcodeAnalyzer`) to a `core/*` module would close a real G10 edge — `feature/shopping` imports
  `CameraPreview` from `feature/products` — but doing a CameraX plumbing move inside an add-item UX
  rewrite makes one PR unreviewable. **Explicitly deferred to G10's own design pass (F3.5)** and
  recorded there so it isn't rediscovered.
- **Known unverifiable risk**: hosting a CameraX preview in a dialog window carries nine enumerated
  hazards (`.specs/features/products/design.md` § "Camera-in-sheet risk register"), led by
  `PreviewView`'s default SurfaceView implementation mode rendering black inside a dialog on some
  OEM/API combinations. **None is closable by a unit test and none can be exercised in this
  environment** (no adb/emulator). They belong on the F0.3 device checklist as named line items.
- **Amendment 2026-09-11 (bruno, OQ-3 + OQ-4)**:
  - **OQ-3 — `SEARCH` and `SUGGESTIONS` stay in the unified sheet** with Quick Add and the camera.
    This adds no feature work (all five `Options` are already sheet-hosted modes today), but it
    raises the risk surface from 9 items to 12: the one-scrollable rule becomes a per-mode invariant
    (R7), and IME carry-over between modes (R10), transient per-mode state (R11) and sheet-height
    stability (R12) become real. It also makes one regression unavoidable: `ProductHeader.kt:80`
    excludes Quick Add from the chip row because today the way back is the nested `NavHost` back
    stack — once that graph is deleted, **a user who taps "Search" has no way back to Quick Add**.
    Quick Add must become a visible peer chip. Effort revised **M → M–L**, driven entirely by
    hardening, not by new features.
  - **OQ-4 — recipes are a secondary menu action.** The entry goes into `CartScreen`'s **existing**
    `MoreVert` overflow menu (`RecipesRoute(shoppingId = shopping.id)`) — no new permanent
    affordance. This **supersedes** the earlier draft placement on `ShoppingScreen`, which has no
    overflow menu today; the `shoppingId = null` "start a list from a recipe" entry ships with
    `recipe-list-origin`, when a destination that can actually create a list exists.
- **Scope**: `feature/products`, `feature/cart` (overflow-menu entry only).
- **Date**: 2026-09-11
- **Status**: active — design approved (`.specs/features/products/design.md`), amended after bruno
  resolved OQ-3/OQ-4, **not yet implemented**. No open questions remain.

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
- **Completed (session of 2026-09-11, `tech-lead` — Design phase for the two new `pm` specs)**:
  Designed and broke down CHAT-01, PROD-04 and PROD-05. Four new documents:
  `.specs/features/chat/{design,tasks}.md` and `.specs/features/products/{design,tasks}.md`.
  Two new decisions recorded above: **AD-010** (chat) and **AD-011** (add-item/recipes). Four new
  gaps in `MVP-ROADMAP.md`: **G17** (CHAT-01), **G18** (PROD-04), **G19** (PROD-05),
  **G20** (a bug found while designing G17), with Phase 3 entries **F3.6–F3.9**. Ten tasks
  (T8–T17) across six PRs, plus three PRs for CHAT-01 — hard-sequenced
  `CHAT-01 → PROD-04 → PROD-05 → recipe-list-origin`, because CHAT-01 and PROD-04 rewrite the same
  file and PROD-04 rehosts the file PROD-05 edits.
  - **Three spec premises were corrected against the code before designing** — do not re-derive them
    from the spec text: (1) **`CartAssistantDock` is dead code** — the whole
    `feature/cart/.../components/ai/` package (5 files) has no call site; `AiDockState` survives
    only as a permanently-`COLLAPSED` `CartUiState` field read by one always-true condition at
    `CartScreen.kt:125`, so CHAT-01 AC2 is a pure deletion and nothing is lost. (2) **`ProductScreen`
    is already inside a `ModalBottomSheet`** (`ProductsGraph`'s `dialog<ProductPickerRoute>`), so
    PROD-04 AC1 is already satisfied by the container and the camera already runs in a sheet today —
    PROD-04 is a content refactor, materially cheaper than the spec implies. (3) **The AI
    conversation has never been persisted**; the spec's Edge Case about surviving a force-close
    describes behaviour that does not exist.
  - **`recipe-list-origin` decision** (the `pm` asked whether to merge or sequence with PROD-05):
    **Design merged, delivery sequenced.** One entry point, built once, hinged on a nullable
    `shoppingId` route argument (`null` → start a list *from* a recipe, Yasmin's flow); the
    `Shopping` ↔ recipe field lands in its own spec and PR on top, because a nullable-field +
    mapper + AD-009 rules change is a different risk class from a navigation change.
    `.specs/PERSONA-ACTION-PLAN.md` item #3 should be updated by the `pm` to reflect this —
    not edited here, it is the `pm`'s document.
  - ~~Four questions are open for bruno before implementation starts~~ — **all four resolved by
    bruno the same day; the five affected documents were amended and nothing is open.** Net effect:
    - **OQ-1 → process-scoped conversation.** Confirms the recommendation; no implementation change.
      `chat/spec.md`'s Edge Case, which assumed persistence the code never had, is corrected in place
      with the original struck through so the change is auditable.
    - **OQ-2 → the bottom-nav AI tab is removed.** This *changed* the design, which had kept the tab
      as the no-surprises default. New `chat/spec.md` AC6, new task **T5b**, `chat/tasks.md` T7
      rewritten (the existing `chat_flow.yaml` reaches the chat through the tab, so this PR breaks
      it — not optional cleanup). **T5b and T6 must ship in the same PR** or the AI is unreachable
      between commits.
    - **OQ-3 → search/suggestions stay in the sheet.** Worth recording plainly, because the question
      was framed as a scope expansion: it is the *cheaper* branch — those modes are already hosted
      in the sheet today, so no new feature work exists. What grew is hardening (new task **T11b**,
      risks R10–R12) plus the Quick Add chip regression described in AD-011.
    - **OQ-4 → recipes in `CartScreen`'s existing overflow menu**, superseding the earlier
      `ShoppingScreen` placement. T15/T17 updated.
  - **Both features are now unblocked and ready to implement in order:**
    `CHAT-01 (PR1→PR3) → PROD-04 (PR4→PR5) → PROD-05 (PR6) → recipe-list-origin`.
- **Next step**: **T7** (`android-engineer-features`) — still the only open engineering item on the
  beta gate; G17–G20 are Phase 3 and must not pull ahead of it. Everything else on the gate needs
  bruno.
- **Blockers (all bruno, none resolvable by any agent here)**: rotate/revoke the Gemini key in the
  Firebase + Google AI Studio consoles (the code change alone mitigates nothing until the old key is
  revoked); merge PR #67 once T7 turns it green; decide G3 hosting, then wire the URL into
  `CustomMethodPickerTerms`, Settings and the Play Console field; capture G4 screenshots + feature
  graphic; confirm G5 Firestore rules in the console; run F0.3 (Maestro, authenticated session) and
  F2.2 (Google Sign-In) on a device; complete the Play Console Internal-testing track; and the
  `develop` → `master` decision, which stays exclusively his.
- **Uncommitted files (2026-09-11)**: the design work sits, uncommitted, on the dedicated branch
  `docs/chat-products-design` (branched from `develop` by bruno) — seven files:
  `.specs/features/chat/{spec,design,tasks}.md`,
  `.specs/features/products/{spec,design,tasks}.md`, plus `.specs/STATE.md` and
  `.specs/MVP-ROADMAP.md`. Note both `spec.md` files are included: the `pm`'s originals were
  untracked/modified in the same tree, and the tech-lead amended them (CHAT-01's ACs and Edge Cases,
  PROD-05's placement) to record bruno's OQ-1..OQ-4 decisions at the source rather than only in the
  design docs. Committing is bruno's; no agent committed anything.
- **The earlier working-tree tangle is resolved.** The in-flight AI-error-handling fix (all
  providers failing left the user with a silently vanishing spinner) was extracted to its own branch,
  `fix/ai-fallback-error-handling`, so it no longer overlaps CHAT-01's T4. **That branch should land
  before CHAT-01 starts** — T4 rewrites `AiChatViewModel` and its design explicitly requires
  preserving that error path verbatim, so merging it first avoids re-deriving it from a diff.
  `.specs/PERSONA-ACTION-PLAN.md` remains the `pm`'s to update (item #3 `recipe-list-origin`, now
  merged with PROD-05's design); the tech-lead did not edit it.
- **Branch**: `docs/firestore-security-rules` (G5 rules proposal) → PR into `develop`. The
  CHAT-01/PROD-04/PROD-05 design work is on `docs/chat-products-design` (off `develop`), uncommitted.
