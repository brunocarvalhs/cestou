# 05 — Decisões de arquitetura (AD-001 a AD-013)

> Registro das decisões de arquitetura citadas no código (`AD-0xx` em comentários de `.kt` e em `firestore.rules`). Este arquivo preserva o texto original da seção "Decisions" de `.specs/STATE.md`, removido no branch `chore/sdd` (commit `ddf8fb57`). O texto está em inglês porque é transcrito sem edição, para não alterar o raciocínio registrado. O histórico completo continua em `git show develop:.specs/STATE.md`.
>
> **Referências históricas:** caminhos citados abaixo dentro de `.specs/` (por exemplo `MVP-ROADMAP.md` e `.specs/features/g10-cross-feature-decoupling/`) referem-se ao diretório `.specs/` pré-`chore/sdd`, removido no commit `ddf8fb57`, e não existem mais na árvore atual — consulte-os no histórico do git (`git show develop:<caminho>`). O `.specs/` atual na árvore é a renomeação do antigo `specs/` do fluxo SDD (ver `AGENTS.md` §8) e tem conteúdo não relacionado a este.
>
> **G5:** item do antigo roadmap (`.specs/MVP-ROADMAP.md`) — regras do Firestore, em especial para escrita em `notifications`. É a motivação da AD-009.
>
> **Antes de publicar `firestore.rules`, leia a AD-009 inteira.** As regras do arquivo ainda não foram publicadas e, quando forem, interrompem o fallback legado de entrada por código e todo o tráfego do Wear OS.

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
  rotated key after an app restart. Callers that re-read per invocation (`AgentFactoryImpl.create()`)
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
  `firebase deploy --only firestore:rules --project how-much-2a72e`.
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
  - **`/shopping` update** — four shapes: (a) owner may change anything but must stay a member,
    since a self-lockout is unrecoverable without a server tier; (b) a non-owner member may edit
    content but not `users`, `roles` or `shortCode` — normal edits rewrite the whole document with
    those fields unchanged, so only escalation attempts trip it; (c) a non-member may append
    *itself* to `users` and nothing else, which is exactly `ShoppingRepositoryImpl.join()`
    (`{"users": [uid]}`, resolved to an `arrayUnion`). The joiner therefore cannot add third
    parties, drop members, grant itself a role or edit content in the same write; (d) any member,
    owner included, may remove *itself*: result is exactly `users - uid`, `roles` only loses the
    `uid` key, nothing else changes — the `{users: arrayRemove(uid), roles.<uid>: deleteField()}`
    write of "leave list" / account deletion (app and web page). An owner leaving leaves the list
    ownerless; the rest keep editing it.
  - **`/shopping` delete** — owner, or the sole remaining member even if not `OWNER` (everyone
    else left). Account deletion deletes `products/*` first, while still a member.
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
    deletes are denied because no code path deletes. sdd-028 added two OPTIONAL fields,
    `shoppingId` and `senderId`: `senderId` must equal the writer's uid, and when `shoppingId` is
    sent both the writer and the recipient must be members of that list. They stay optional so
    older clients keep working, which leaves one gap: a client that omits `shoppingId` can still
    notify any uid it knows. Making both required is the follow-up. Publish `firestore.rules`
    BEFORE the app that sends the new fields (extra keys are rejected by the previous rules).
  - **catch-all `if false`** — explicit so a collection added by a future feature fails loudly in
    development instead of silently inheriting a broader rule.
- **Trade-off / known breakage on deploy**:
  1. ~~**Join by short code stops working.**~~ Fixed client-side: `shortCodes/{CODE}` holds only
     `{ id, shoppingId }` (any signed-in `get`, no `list`, create-only by a member for the list's own
     code, delete by whoever may delete the list or anyone once it is gone). `ShoppingJoinUseCase`
     resolves code → id, self-joins blind (rule (c)), then reads the list as a member; join by full
     id also joins before reading. The index is written on create and backfilled when a member
     opens "share"; `resolveShortCode` keeps the old `shortCode` query as a fallback, which these
     rules deny, so lists never shared after the app update lose their old code once rules ship.
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
- **Verification**: `firestore-tests/rules.test.mjs` — 73 assertions against the local Firestore
  emulator (`firebase emulators:exec --only firestore --project demo-cestou`), all green, covering
  every rule above including the join, self-leave, sole-member delete, escalation and
  notification-forgery attempts. Local only; it
  never contacts the real project and is not wired into Gradle/CI.
- **Scope**: All Firestore access from `:app`, `:wear` and every `feature/*` data layer.
- **Date**: 2026-09-10
- **Status**: proposed — awaiting bruno's review and manual deploy

### AD-010
- **Decision**: Pro entitlement is exposed via a read-only contract,
  `SubscriptionRepository.observeStatus(): Flow<SubscriptionStatus>` (`SubscriptionStatus` = `FREE`
  or `PRO`, no payload — no purchase token, no expiry, no Play Billing type), living in
  `core/domain`. It is the *only* thing a paid feature or the AI-agent dispatch chokepoint may
  depend on to ask "is this user Pro" — never a Play Billing type, never an `Activity`. The actual
  `BillingClient` wrapper (`PlayBillingSubscriptionRepository`) that keeps it in sync with real
  purchases lives in `core:billing`, bound via Hilt in `BillingModule`. Status starts `FREE` and
  only changes when the wrapper re-reads Play's local purchase cache after a real purchase — there
  is no free trial, promo grant, or sign-in-based auto-upgrade to `PRO` anywhere in this contract.
- **Reason**: Isolate the third-party Play Billing SDK and its `Activity`-scoped purchase flow to
  one module (`core:billing`), so every other paid-feature gate (`ShoppingSubscriptionGateViewModel`,
  `CartSubscriptionGateViewModel`, `GeminiAgent`/`OpenRouterAgent`'s dispatch gating) depends on
  a trivial two-state enum instead of Play Billing internals.
- **Trade-off**: `SubscriptionRepository` alone can't express *why* a user is `FREE`/`PRO` (no
  expiry, no product id) — any feature needing that detail has to go through `core:billing`
  directly, which most callers correctly don't.
- **Scope**: `core/domain` (contract), `core/billing` (impl), `:app` and `feature/subscription`
  (the only modules that depend on `core:billing` directly — everyone else only sees
  `SubscriptionRepository`).
- **Date**: 2026-09-27 (commit `ad3c4c1e`, "feat(billing): entitlement contract + Play Billing repo
  + Paywall route")
- **Status**: active — **backfilled here 2026-09-29**. This decision was referenced as "AD-010" in
  15 source files (`SubscriptionRepository.kt`, `SubscriptionStatus.kt`, `BillingModule.kt`,
  `AgentAction.kt`, `MobileRoutes.kt`, gate ViewModels, etc.) from the moment it shipped, but was
  **never actually recorded in this file** until now — a real doc-sync gap, not just a stale
  handoff note. Caught because a later session (same day) independently reached for the `AD-010`
  slot for an unrelated decision (see below) and the collision surfaced the missing entry.

### AD-011
- **Decision**: G10-03's `ShareShoppingUseCase` module boundary is resolved with a **segregated
  core contract + feature-owned superset**, not a wholesale repository promotion (Option a) nor a
  single bespoke port (Option b) — a synthesis of both, chosen by bruno:
  1. `core/domain/repository/ProductReader.kt` — a new, minimal interface exposing only
     `suspend fun getAllProducts(shoppingId: String): Flow<List<Product>>`, the one capability
     `feature/shopping`/`feature/cart` actually need from Products.
  2. `feature/products/domain/repository/ProductRepository.kt` — stays where it is, but now
     `interface ProductRepository : ProductReader`, adding `delete`/`update`/`save`/`move`. Its
     existing impl (`ProductRepositoryImpl`) automatically satisfies both; Hilt binds it to both
     interfaces.
  3. `core/domain/services/ShareShoppingUseCase.kt` — a second new interface, signature only
     (`suspend operator fun invoke(shopping: Shopping)`, no `Context`/`Intent` in the contract, to
     keep `core/domain` framework-free), implemented in `feature/products` where the Android
     `Intent`-building stays.
- **Reason**: Evaluated against SOLID, not just "smaller diff vs. bigger diff":
  - **ISP** — Option (a) would hand `feature/shopping`/`feature/cart` the full CRUD surface of
    `ProductRepository` when they only ever call one read method; `ProductReader` gives them
    exactly what they use, nothing more.
  - **DIP** — satisfied by all three prior options, but the segregated contract keeps the
    abstraction's shape matched to the actual client need instead of the widest available shape.
  - **OCP** — adding a write method to `ProductRepository` later cannot ripple into any consumer
    that only holds `ProductReader`, unlike Option (a) where every `ProductRepository` change is
    visible to every promoted consumer.
  - **LSP** — clean: every `ProductRepository` is a valid `ProductReader` (pure superset, no
    narrowed/overridden read semantics).
  - **YAGNI** — avoids Option (a)'s full-repository promotion for a need ("share the list as
    text") that only ever touches one read method; if a second feature later needs real CRUD
    access to Products, promoting `ProductRepository` itself becomes justified by that actual
    signal, not by this decision.
  - This also fixes a latent inconsistency: `Product` (the entity) already lives in `core/domain`,
    but nothing reading it did before this — `ProductReader` closes that gap without dragging the
    write surface along.
- **Trade-off**: Two new interfaces instead of one (`ProductReader` + `ShareShoppingUseCase`)
  versus Option (b)'s single port — slightly more ceremony now, in exchange for `ProductReader`
  being reusable by any future read-only consumer without inventing a fourth port from scratch.
- **Scope**: `core/domain` (new `ProductReader`, new `ShareShoppingUseCase` interface),
  `feature/products` (`ProductRepository` now extends `ProductReader`; both interfaces
  implemented/bound there), `feature/shopping` and `feature/cart` (consume the two new `core/domain`
  interfaces instead of `feature.products.domain.usecase.{ProductsUseCase,ShareShoppingUseCase}`
  concretely).
- **Date**: 2026-09-29
- **Status**: active — unblocks `tasks.md`'s T3 (recorded here) and releases T4–T7 in
  `.specs/features/g10-cross-feature-decoupling/tasks.md` for `android-engineer-architecture`.

### AD-012
- **Decision** (G18 phase 1, 2026-10-06, after the 2nd Play rejection for missing reviewer login):
  1. **E-mail/password is our own flow on `FirebaseAuth`, not FirebaseUI's email provider.**
     FirebaseUI 10.0.0-beta05 routes every unverified password user to `RequiresEmailVerification`
     without calling `onSignInSuccess` (it would strand the Play reviewer) and reports errors only as
     display strings. `EmailAuthRepositoryImpl` (feature/auth/data) maps Firebase exceptions to a
     typed `EmailAuthError`; FirebaseUI keeps hosting Google only. E-mail verification is sent but
     never blocks.
  2. **Required first + last name, validated by one pure `PersonNameValidator` (core/domain/util)**,
     concatenated as `"<first> <last>"` into Auth `displayName` and `users/{uid}.name` (no separate
     fields). Anti-fake rules per spec EPA-13; errs on the side of accepting.
  3. **Required-name gate in `MainActivity`**: any signed-in account with a blank name goes to
     `CompleteName`, except inside the auth flow (`NavDestination.isAuthFlow()`), where a fresh
     account has no name for a moment. The decision re-reads `AuthService.currentUser` synchronously.
     `AuthService.updateDisplayName` publishes the new name itself (`updateProfile` does not fire the
     AuthStateListener).
  4. **`users/{uid}` is finally written**: `UserRepository.updateProfile` (PUT -> `update()`, failed
     on a missing doc, had no callers) became `saveProfile` (POST -> `set()`, writes only
     `id`/`name`/`photoUrl`, never `email` - partly closes the AD-009 exposure for re-saved docs).
     `MainViewModel` saves it whenever the signed-in user's name/photo change, for every method,
     which also backfills existing Google accounts.
- **Not done here**: phone sign-in (phase 2), `firestore.rules` name check (waits for the AD-009
  deploy), Maestro flows (no device in this session), re-auth before deleting an e-mail account.

### AD-013
- **Decision** (`sdd-011-perfil-configuracoes-unificado`, 2026-10-27): merge `feature:settings`'s
  content into `feature:profile`'s Profile screen as an inline, non-exclusive accordion, instead
  of navigating to a separate full-screen `Settings` route.
  1. **Removed the public `Settings` NavKey** (`feature/settings/navigation/Settings.kt`) and the
     full-screen `SettingsScreen` composable it rendered — both dead once the Profile tab hosts the
     sections inline. `SettingsScreen.kt` now exposes a public `SettingsSectionsList` composable
     (accordion, non-exclusive expand/collapse) consumed by both `feature:settings`'s own
     `SettingsViewModel` and `feature:profile`'s new `ProfileSettingsSection`.
  2. **Widened `feature:settings`'s public surface**: `SettingsViewModel`, `SettingsUiState`,
     `SettingSection` (now carries a stable `id: String`), `SettingItem` and `SettingsIntent` went
     from `internal` to `public` so `feature:profile` can resolve `SettingsViewModel` via
     `hiltViewModel()` inside its own `composable<Profile>` block (same pattern already used for
     `ProfileSubscriptionViewModel`) and render its state/intent. This **deepens** the pre-existing
     `feature:profile` → `feature:settings` Gradle dependency (`feature/profile/build.gradle.kts`)
     from "one NavKey" to "ViewModel + state + composable" — not a new violation of AGENTS.md §2
     ("features não dependem umas das outras diretamente"), but a widening of an exception already
     in place before this task.
  3. **Second consumer found during implementation, not caught by the plan's static search**:
     `feature:shopping`'s `ShoppingGraph.kt` also referenced the `Settings` NavKey
     (`onSettings = { navigator.navigate(route = Settings) }`), wired from `ShoppingScreen`'s
     unused `onSettings` parameter. Neither `ShoppingScreen` nor `ShoppingHeader` ever invoked that
     parameter from their composed UI (no gear icon existed in the rendered tree) — confirmed dead
     before removing it alongside the `Settings` NavKey.
- **Reason**: The accordion is a categorization device, not an exclusivity device (confirmed with
  the user: "o accordion é para categorizar") — hence `expandedSectionIds: Set<String>` rather than
  a single selected section. The expanded state survives returning from a sub-screen because the
  Profile tab's `SettingsViewModel` instance (scoped to the NavGraph entry) isn't recreated, keeping
  the UX the user asked for ("a ideia é manter, uma experiência boa").
- **Trade-off**: `feature:settings`'s public surface is now larger, which makes future indirect
  coupling from a third feature easier to introduce by accident. Mitigated only by code review
  discipline (AGENTS.md §6.7), not by a compiler boundary.
- **Scope**: `feature:settings` (navigation, presentation/{state,intent,viewmodel,screen}),
  `feature:profile` (new `presentation/components/ProfileSettingsSection.kt`, `ProfileScreen`,
  `ProfileIntent`, `ProfileViewModel`, `ProfileGraph`), `feature:shopping` (`ShoppingGraph.kt`,
  `ShoppingScreen.kt` — dead-code cleanup tied to the `Settings` NavKey removal),
  `core:analytics` (new `SETTINGS_SECTION_TOGGLED` event + `SECTION_ID`/`EXPANDED` params).
- **Status**: active.

