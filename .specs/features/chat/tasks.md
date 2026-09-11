# Unified Chat Tasks — CHAT-01

**Design**: `.specs/features/chat/design.md`
**Status**: Approved. **Amended 2026-09-11** after bruno resolved OQ-1 (process-scoped conversation —
no task change) and OQ-2 (**remove the bottom-nav AI tab** — new task **T5b**, and T7 rewritten).
**Scope**: CHAT-01 only. CHAT-02 is blocked on the delivery-reliability spike — no tasks here.

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `spec-driven` skill: activate it by name and follow its Execute flow
and Critical Rules. If the skill isn't installed in the current environment, follow this document's
per-task structure manually with the same rigor: don't start a task without its dependencies done,
don't skip its Gate.

Process (`.specs/MVP-ROADMAP.md` § Process): each PR below is its own branch off `develop` with its
own PR into `develop`. Never commit or merge directly to `develop`.

---

## PR plan

| PR | Branch | Purpose | Requirement | Ships alone? |
| -- | ------ | ------- | ----------- | ------------ |
| PR1 | `refactor/chat-remove-dead-dock` | Delete the unreachable `feature/cart` AI dock package and its module dependency | CHAT-01 AC2 | Yes — pure dead-code removal, zero user-visible change |
| PR2 | `feat/chat-conversation-store` | Conversation state moves to a `@Singleton` store; `AiChatViewModel` becomes a projection | CHAT-01 AC1, AC5 | Yes — no UI change, fixes nothing observable on its own |
| PR3 | `feat/chat-single-surface` | `AiChat` route takes a `shoppingId`; **bottom-nav AI tab removed**; `Options.AI` navigates instead of rendering inline; `feature/products` → `feature/chat` dependency removed | CHAT-01 AC1, AC3, AC4, AC5, **AC6** | Yes — this is what makes CHAT-01 user-observable |

PR1 is independent of PR2/PR3 and should land first (it is the cheapest review and it shrinks the
surface the other two touch). PR3 depends on PR2.

**Sequencing against other work**: PR3 edits `ProductScreen.kt`, which PROD-04 rewrites. **CHAT-01
must land before PROD-04** — see `.specs/features/products/tasks.md`.

---

## Test Coverage Matrix

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain entity (`ChatMessage`) | unit | `senderId` default + `Sender` has three cases | `feature/chat/src/test/**/domain/entity/*Test.kt` | `./gradlew :feature:chat:test` |
| Domain / store (`AiConversationStore`) | unit | append / context switch / clear semantics | `feature/chat/src/test/**/data/repository/*Test.kt` | `./gradlew :feature:chat:test` |
| Presentation / ViewModel (`AiChatViewModel`) | unit | two ViewModels over one store see the same list | `feature/chat/src/test/**/presentation/viewmodel/*Test.kt` | `./gradlew :feature:chat:test` |
| Presentation / Screen | existing test update | `AiChatScreenTest` still compiles and asserts the same UI | `feature/chat/src/test/**/presentation/screen/*Test.kt` | `./gradlew :feature:chat:test` |
| Cart state | existing test update | `CartUiState` no longer carries `aiDockState` | `feature/cart/src/test/**` | `./gradlew :feature:cart:test` |
| E2E (Maestro) | flow update | `chat_flow.yaml` reaches the chat from both entry points | `.maestro/flows/chat_flow.yaml` | `maestro test .maestro/test_suite.yaml` — **device required; per F0.3 this suite has never run here** |

## Gate Check Commands

| Gate | When | Command |
| --- | --- | --- |
| Quick | after each task | `./gradlew :feature:chat:test` / `:feature:cart:test` / `:feature:products:test` (module matching the task) |
| Build | end of each PR | `./gradlew test lint detekt` |
| Device | PR3 only, if a device exists | `maestro test .maestro/flows/chat_flow.yaml` |

---

## Execution Plan

```
PR1:  T1 → T2
PR2:  T3 → T4
PR3:        T4 → T5 → T5b → T6 → T7
```

T5b is new (bruno's OQ-2 decision). It runs after T5 because removing `AiChat` from `rootRoutes` and
changing the route's shape touch the same two files (`MainActivity.kt`, `MobileRoutes.kt`), and
doing them in one order avoids editing `MainActivity` twice.

---

## Task Breakdown

### PR1 — Remove the dead dock

#### T1: Delete `feature/cart/.../components/ai/`

**What**: Delete `CartAssistantDock.kt`, `AiInputBar.kt`, `AiMessageBubble.kt`,
`AiTypingIndicator.kt`, `ChatContent.kt`. All five are unreachable: `CartAssistantDock` has no call
site outside its own `@Preview`s, and the other four are only reachable through it.
**Where**: `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/components/ai/`
**Depends on**: none
**Requirement**: CHAT-01 AC2

**Done when**:
- [ ] Directory removed; `./gradlew :feature:cart:assembleDebug` is green.
- [ ] `implementation(project(":feature:chat"))` removed from `feature/cart/build.gradle.kts` — verified by grepping `feature/cart/src` for `feature.chat` first and finding nothing.
- [ ] The `feature.products.presentation.components.product.Suggestions` import disappears with the dock. The `:feature:products` **module dependency stays** — `CartViewModel` still uses its use cases. Do not widen this PR into G10.
- [ ] **`ChatMessage.Sender.PARTICIPANT` is untouched.** It becomes "even more unused" after this deletion; it is reserved for CHAT-02 and must not be removed by an unused-symbol cleanup.

**Tests**: none (deletion). Confirm no test references the deleted composables before deleting.
**Gate**: Quick

---

#### T2: Remove `AiDockState` and fix the focus effect it gated

**What**: Delete `AiDockState.kt` and `CartUiState.aiDockState`. `CartScreen.kt:124-128` reads
`uiState.aiDockState != AiDockState.CHAT`; with the state permanently `COLLAPSED` that condition is
always `true`, so it collapses to `if (!isKeyboardVisible) { focusManager.clearFocus() }`.
**Where**:
- `feature/cart/.../presentation/state/AiDockState.kt` (delete)
- `feature/cart/.../presentation/state/CartUiState.kt`
- `feature/cart/.../presentation/screen/CartScreen.kt`

**Depends on**: T1
**Requirement**: CHAT-01 AC2

**Done when**:
- [ ] The `LaunchedEffect(isKeyboardVisible)` is **simplified, not deleted** — deleting it changes real focus behavior on keyboard dismiss.
- [ ] `feature/cart/src/test` searched for `aiDockState`/`AiDockState` **before** the field is removed; any construction updated in the same commit.

**Tests**:
- [ ] Existing `CartViewModelTest`/`CartScreenTest` still green.

**Gate**: Build (`./gradlew :feature:cart:test lint detekt`) — end of PR1.

---

### PR2 — One conversation, one owner

#### T3: `AiConversationStore` + `ChatMessage.senderId`

**What**: New `internal interface AiConversationStore` (`feature/chat/domain/repository/`) exposing
`messages`/`shoppingId`/`isLoading` as `StateFlow` plus `append`, `setShoppingContext`, `setLoading`,
`clear` — shape per `design.md` § "The conversation holder". `@Singleton` in-memory impl in
`feature/chat/data/repository/AiConversationStoreImpl.kt`, `@Binds` in `ChatModule`. Add
`senderId: String? = null` to `ChatMessage`.
**Where**:
- new `feature/chat/.../domain/repository/AiConversationStore.kt`
- new `feature/chat/.../data/repository/AiConversationStoreImpl.kt`
- `feature/chat/.../di/ChatModule.kt`
- `feature/chat/.../domain/entity/ChatMessage.kt`

**Depends on**: none
**Requirement**: CHAT-01 AC1, AC5

**Done when**:
- [ ] Impl is `@Singleton` and `internal`; interface is `internal`. Nothing in `feature/chat` is `public` except `chatGraph` and `ChatInitializer` (AD-005).
- [ ] `senderId` is additive with a default — every existing `ChatMessage(...)` call site compiles unchanged.
- [ ] `setShoppingContext(newId)` where `newId != current && current != null` appends a system/divider `ChatMessage` marking the context switch, rather than silently re-pointing the context (`design.md` Risks).

**Tests**:
- [ ] Unit: `append` emits on `messages`; `clear` empties it.
- [ ] Unit: switching to a different non-null `shoppingId` appends the divider; setting the *same* id again does not.
- [ ] Unit: `ChatMessage.Sender.entries.size == 3` and `PARTICIPANT` is present — documents that the case is reserved, guards T1's risk.

**Gate**: Quick

---

#### T4: `AiChatViewModel` projects the store

**What**: `AiChatViewModel` stops owning `messages`/`isLoading`/`shoppingId`. It keeps `input`
locally and derives `AiChatUiState` by combining its own input with the store's flows.
`setShoppingContext` delegates to the store. `sendMessage` appends to the store. `AiChatViewModel`
and `AiChatScreen` become `internal`.
**Where**:
- `feature/chat/.../presentation/viewmodel/AiChatViewModel.kt`
- `feature/chat/.../presentation/screen/AiChatScreen.kt` (visibility only)

**Depends on**: T3
**Requirement**: CHAT-01 AC1, AC5

**Done when**:
- [ ] `AiChatUiState` still implements `AiAgentContext` — `CartAssistantUseCase`'s signature is unchanged (out of scope per spec).
- [ ] The existing error path (all AI providers failing → `ai_chat_error_message` appended as an `ASSISTANT` message) is preserved verbatim; it is the only user-visible failure signal.
- [ ] Making the two symbols `internal` compiles **only because** T5/T6 removed the external importers — if it doesn't compile, do T5/T6 first rather than reverting the visibility.
- [ ] No `!!` introduced.

**Tests**:
- [ ] Unit: two `AiChatViewModel` instances constructed against the same fake store both observe a message appended through either one (this is AC5, at the unit level).
- [ ] Existing `AiChatViewModelTest`/`AiChatScreenTest` updated to inject the fake store.

**Gate**: Build (`./gradlew :feature:chat:test lint detekt`) — end of PR2.

---

### PR3 — One surface

#### T5: `AiChat` route carries an optional `shoppingId`

**What**: `data object AiChat` → `data class AiChat(val shoppingId: String? = null)` in
`core/navigation/mobile/MobileRoutes.kt`. `chatGraph` reads the route and calls `setShoppingContext`
**only when `shoppingId != null`**. The nullable default is kept on purpose even though (after T5b)
every call site passes a real id — it keeps `chatGraph` total, with no `!!`/`requireNotNull`.
**Where**:
- `core/navigation/.../mobile/MobileRoutes.kt`
- `feature/chat/.../navigation/ChatGraph.kt`
- any remaining `AiChat` reference (grep before editing — `MainActivity` and `CestouBottomNavigation` are handled by T5b)

**Depends on**: T4
**Requirement**: CHAT-01 AC3

**Done when**:
- [ ] `setShoppingContext` is never called with `null` — a context-free entry leaves whatever context was already set alone rather than clearing it.
- [ ] No `!!` or `requireNotNull` introduced in `chatGraph`.

**Tests**:
- [ ] Unit: route serialization round-trips both `AiChat()` and `AiChat("abc")` (`NavTypeSerializerTest` is the existing precedent).

**Gate**: Quick

---

#### T5b: Remove the AI tab from the bottom navigation

**What**: The AI stops being a root destination — bruno's OQ-2 decision. Delete
`BottomNavItem.AiChatItem` (`CestouBottomNavigation.kt:45-50`), remove it from the `items` list
(line 70) and drop the now-unused `AiChat` import; change `MainActivity.kt:118` to
`listOf(ShoppingList, Profile)`; delete the `nav_ai_chat` string from `core/ui` in all three locales
(en/es/pt-BR). `composable<AiChat>` in `chatGraph` **stays** — the destination still exists, it is
just no longer a root.
**Where**:
- `core/ui/.../components/CestouBottomNavigation.kt`
- `app/.../MainActivity.kt`
- `core/ui/src/main/res/values{,-es,-pt-rBR}/strings.xml`

**Depends on**: T5
**Requirement**: CHAT-01 AC6

**Done when**:
- [ ] `grep -rn "AiChatItem\|nav_ai_chat"` across `app`, `core`, `feature` returns nothing.
- [ ] The chat is still reachable — from a list, via `Options.AI` (T6). **Do not land T5b without T6 in the same PR**, or the AI becomes unreachable entirely between the two commits.
- [ ] **Expected, not a regression**: the bottom bar now *hides* while the chat is open, because `showBottomBar = currentRoute != null` and `AiChat` is no longer in `rootRoutes`. Note this in the PR description so a reviewer doesn't flag it as a bug — it makes the chat behave like every other detail destination (`CartFlow`).
- [ ] Verified no start-destination/onboarding dependency: `MainActivity`'s `NavHost` starts at `ShoppingList` or `Welcome`, never `AiChat`.
- [ ] `AiSettings` is still reachable from `AiChatScreen`'s top-bar icon (F3.4's accessibility fix must not be orphaned).
- [ ] The two-item navigation bar is left as-is. If it reads thin on device, that's a visual follow-up — **do not** re-add the tab or invent a replacement destination in this PR.

**Tests**:
- [ ] Existing `core/ui` tests/previews updated for a two-item bar.

**Gate**: Quick

---

#### T6: `Options.AI` navigates instead of rendering inline

**What**: Remove the `composable(Options.AI.name)` branch from `ProductScreen`'s `NavHost` and the
`feature.chat.presentation.*` imports. Selecting the AI chip calls a new `onOpenAiChat: () -> Unit`
parameter, threaded from `ProductsGraph` (which already holds the `Navigator`) as
`{ navigator.goBack(); navigator.navigate(AiChat(shoppingId = route.shopping.id)) }` — dismiss the
sheet, then open the chat. Drop `implementation(project(":feature:chat"))` from
`feature/products/build.gradle.kts`. Do the same for `feature/shopping` if its `feature:chat`
dependency is only this pattern.
**Where**:
- `feature/products/.../presentation/screen/ProductScreen.kt`
- `feature/products/.../navigation/ProductsGraph.kt`
- `feature/products/build.gradle.kts`, `feature/shopping/build.gradle.kts`

**Depends on**: T5b (they must land in the same PR — T5b removes the only other way in)
**Requirement**: CHAT-01 AC1, AC4, AC5

**Done when**:
- [ ] `grep -r "feature.chat" feature/products/src feature/shopping/src feature/cart/src` returns nothing.
- [ ] `Options.AI` stays in the enum and stays visible in `ProductHeader` — only its *behavior* changes. (PROD-04 decides its final home; this task must not pre-empt that.)
- [ ] The `LocalViewModelStoreOwner.current!!` in `ProductScreen` is **not** removed here — it is PROD-04's T-tasks that delete the nested `NavHost` that makes it necessary. Leave it; don't half-fix it in two PRs.

**Tests**:
- [ ] Existing `feature/products` tests green.

**Gate**: Quick

---

#### T7: Maestro flow + docs

**What**: Rewrite `.maestro/flows/chat_flow.yaml`. It currently reaches the chat through the
bottom-nav tab, which no longer exists (T5b) — so this flow **is broken by this PR** and is not
optional cleanup. New shape, matching the amended Independent Test: open a list → open the AI from
inside it → send a message → back out to the list → open the AI again → assert the previous message
is still there (that's AC5, the process-scoped continuity the store provides). Also assert the
bottom navigation no longer offers an AI item. Use `testTag` selectors, never plain text (the app
runs in EN and pt-BR; see `.specs/LESSONS.md` and the Maestro lesson in memory).
**Where**: `.maestro/flows/chat_flow.yaml`
**Depends on**: T6
**Requirement**: CHAT-01 AC5, AC6, Independent Test

**Done when**:
- [ ] No step in the flow depends on a bottom-nav AI item.
- [ ] Every new selector is a `testTag`; any tag that doesn't exist yet is added to the composable in the same PR.
- [ ] F3.4's settings-icon assertion (`ai_chat_settings_content_description`) survives the rewrite — it is existing accessibility coverage, not scaffolding.
- [ ] The PR description states plainly that the flow was **not executed** (no adb/emulator in this environment, per F0.3) — same honesty convention as PRs #14–#22.

**Tests**: the flow itself (unrunnable here).
**Gate**: Build (`./gradlew test lint detekt`) — end of PR3 and of CHAT-01.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1 | 5 file deletions + 1 gradle line | Granular |
| T2 | 3 files | Granular |
| T3 | 4 files | Granular |
| T4 | 2 files | Granular |
| T5 | 2 files | Granular |
| T5b | 5 files (3 are locale strings) | Granular |
| T6 | 4 files | Granular |
| T7 | 1 file | Granular |

## Not in scope (deliberately)

- **G20** (`CartAssistantUseCase` rebuilds the agent per message, contradicting its KDoc) — a real
  behavioral bug found while designing this, logged in `.specs/MVP-ROADMAP.md`. Bundling a behavior
  fix into a consolidation PR is exactly what the G12–G16 split exists to prevent.
- Conversation persistence across app restarts — **decided (OQ-1): process-scoped, not persisted.**
  Persisting would require choosing a store and answering a data-retention question adjacent to G2.
- ~~Whether the AI keeps a bottom-nav slot~~ — **decided (OQ-2): the tab is removed.** Now in scope,
  as T5b.
- Replacing the two-item bottom navigation with a different pattern. Removing the AI tab leaves
  Lists + Profile, below Material's 3–5 destination guidance. Visual follow-up if it reads thin on
  device; not part of CHAT-01.
- G10 at large. CHAT-01 closes two of its edges as a side effect; it does not attempt the rest.
