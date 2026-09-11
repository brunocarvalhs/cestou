# Design: Unified Chat — CHAT-01

**Spec**: `.specs/features/chat/spec.md`
**Scope**: CHAT-01 only. CHAT-02 (participant messaging) is **not designed here** — it is blocked on
the delivery-reliability spike. This document does, however, state explicitly which of its decisions
are load-bearing for CHAT-02 so that none of them has to be undone later.
**Status**: Approved (tech-lead), 2026-09-11. **Amended the same day** after bruno resolved OQ-1
(conversation is process-scoped) and OQ-2 (**the bottom-nav AI tab is removed**). No open questions
remain for CHAT-01.

---

## Corrections to the spec's "Current State"

The spec was written from a read of the code; three of its statements do not survive a second read.
The design is built on the corrected facts, not the spec's.

| Spec says | Actually true | Consequence |
| --- | --- | --- |
| `CartAssistantDock` is "a dock embedded in `CartScreen`" — one of two live AI surfaces | **`CartAssistantDock` is dead code.** It has no call site anywhere (`feature/cart/.../components/ai/` — `CartAssistantDock`, `AiInputBar`, `AiMessageBubble`, `AiTypingIndicator`, `ChatContent` — is unreachable from the app). `CartScreen` never renders it; it only reads `uiState.aiDockState` in one `LaunchedEffect` condition (`CartScreen.kt:125`) that, with the state permanently `COLLAPSED`, is always `true`. | AC2 is a **pure deletion**. No replacement entry point is being lost, and the spec's Edge Case "users lose in-context AI access from inside an open cart entirely" is already the status quo — in-cart AI today is reached through `Options.AI` inside the add-item sheet, not through the dock. |
| The two surfaces are the bottom-nav tab and the dock | The two *live* surfaces are the bottom-nav `AiChat` tab and `Options.AI` inside `ProductScreen` (which is itself already inside a `ModalBottomSheet` — see `products/design.md`). | The redundancy to fix is **tab vs. `Options.AI`**, and it is a *state* redundancy, not just a visual one. |
| Resuming after a force-close "SHALL resume the same history — this is existing `AiChatViewModel` behavior and must not regress" | **There is no persistence.** `AiChatViewModel` holds `messages` in an in-memory `MutableStateFlow`; nothing writes them anywhere. `CartAssistantUseCase` additionally calls `agentFactory.create(settings)` on **every** message, so even the agent-side `AiSession.history` is rebuilt per turn, contradicting that class's own KDoc. | That Edge Case cannot be met without new infrastructure. CHAT-01 explicitly scopes it **out** and downgrades it to process-scoped continuity — **confirmed by bruno (OQ-1)**; `spec.md`'s Edge Case has been corrected to match. The `CartAssistantUseCase` per-message agent rebuild is logged separately as **G20**, not bundled here. |

The real defect behind AC1/AC5 is scoping: `chatGraph` builds `AiChatViewModel` with `hiltViewModel()`
against the `AiChat` `NavBackStackEntry`, while `ProductScreen` builds it with
`hiltViewModel(viewModelStoreOwner = LocalViewModelStoreOwner.current!!)` against the
`ProductPickerRoute` dialog entry. Two `ViewModelStore`s, two ViewModels, two independent message
lists — and the picker's instance is destroyed when the sheet is dismissed. **AC5 fails today.**

---

## Module boundary decision (AD-010)

The question put to the tech-lead was: does unifying the AI surface deepen the G10 coupling, or is it
the moment to extract a shared contract into `core/ai` or a new `core/chat`?

**Decision: neither. Delete the coupling instead of formalizing it.** `feature/chat` becomes a *leaf*
— no other feature module imports anything from it.

| Edge today | After CHAT-01 | How |
| --- | --- | --- |
| `feature/cart` → `feature/chat` (`ChatMessage` in the dead dock) | **removed**, incl. the Gradle dependency | the dock package is deleted |
| `feature/cart` → `feature/products` (`Suggestions` composable, used only by the dock) | one import removed (the module dependency stays — `cart` still uses `products` use cases; that remains G10's problem, not ours) | dock deletion |
| `feature/products` → `feature/chat` (`AiChatScreen` + `AiChatViewModel`, i.e. another feature's `presentation/` layer — a direct AD-005 violation) | **removed**, incl. the Gradle dependency | `Options.AI` stops rendering chat inline and instead navigates to the single `AiChat` destination |
| `feature/shopping` → `feature/chat` | verify and remove if it is only the same inline-render pattern | task T6 |

Why not a shared contract module:

- A `core/chat` (or a contract in `core/ai`) would give a *name* to a dependency that only exists
  because two features render the same screen. Once each feature reaches the chat through
  `core/navigation`'s `AiChat` route — which is what AD-005 says a feature's public surface is — there
  is no cross-feature type left to share. Creating a module to hold a contract nobody needs is the
  more expensive answer, and it would make the `core/*` graph wider for zero call sites.
- `ChatMessage` therefore **stays** in `feature/chat/domain/entity`, and `AiChatScreen` /
  `AiChatViewModel` become `internal` (they are `public` today purely so `products` could import
  them — that is the leak).

**Forward-compatibility for CHAT-02 (explicit, per the brief):**

1. `ChatMessage.Sender.PARTICIPANT` is **kept** and must not be removed as "unused" during the
   cleanup — a Detekt/IDE "unused" cleanup pass on the dead-code task is the realistic way this gets
   lost. Called out as a `Done when` checkbox on T1.
2. `ChatMessage` gains `senderId: String? = null` now (additive, no behavior change, no persistence).
   CHAT-02 needs to know *which* participant sent a message; adding the field while the model is
   cheap and unpersisted avoids reshaping it later. It also pre-answers AD-009's open question about
   `NotificationModel` having no `senderId`.
3. The conversation state moves out of the ViewModel into an injectable, **conversation-shaped**
   holder (below) rather than being flattened into "the AI's messages". It is keyed, so a second
   conversation kind (a list's participant thread) is an added key, not a rewrite.
4. **If** CHAT-02 later requires another feature module to observe or render participant messages,
   *that* is the moment to promote the holder's interface to `core/chat`. It is already an interface,
   so promotion is a file move plus a Gradle line — deliberately deferred, not forgotten.

---

## Architecture

### The conversation holder

`AiChatViewModel` is currently the owner of the conversation. Ownership moves one level down:

```
feature/chat/
  domain/
    entity/ChatMessage.kt            (+ senderId, PARTICIPANT kept)
    repository/AiConversationStore.kt   internal interface
  data/
    repository/AiConversationStoreImpl.kt  @Singleton, in-memory MutableStateFlow
  di/ChatModule.kt                   @Binds the impl
  presentation/
    viewmodel/AiChatViewModel.kt     internal; thin projection over the store
    screen/AiChatScreen.kt           internal
```

`AiConversationStore` (internal to `feature/chat`):

```
messages: StateFlow<List<ChatMessage>>
shoppingId: StateFlow<String?>
isLoading: StateFlow<Boolean>
append(message: ChatMessage)
setShoppingContext(shoppingId: String?)
setLoading(loading: Boolean)
clear()
```

`@Singleton` scope is the whole point: it makes AC1/AC5 hold **regardless of which back-stack entry
hosts the screen**. Any number of `AiChatViewModel` instances project the same list, so entry
identity never has to be the mechanism — which matters because a route carrying a `shoppingId`
argument is a distinct `NavBackStackEntry` with its own `ViewModelStore` by definition.

**The store is still required after OQ-2 removed the bottom-nav tab**, and this is worth stating
because the obvious reading is that one entry point makes shared state unnecessary. It doesn't:
opening the chat from a list, backing out to the list, and opening it again **pops and re-pushes the
destination**, producing a new entry, a new ViewModel and — without the store — a blank chat. That is
precisely the failure AC5 now describes ("returning to it from any entry point within the same app
session SHALL show the same conversation history"). The store is what makes re-entry continuous.

**Lifetime is process-scoped by decision, not by omission** (bruno, OQ-1): the conversation survives
navigation for as long as the app lives and is gone after a cold start. Nothing is persisted.

`AiChatViewModel` keeps only the transient `input` field in its own state and reads everything else
from the store. `AiChatUiState` stays the `AiAgentContext` implementation (no change to
`CartAssistantUseCase`'s contract).

**Lifetime**: process-scoped. Killing the app clears the conversation. Decided by bruno (OQ-1), not
an open question.

### The entry points — bottom-nav tab removed (bruno's decision, OQ-2)

The AI stops being a root destination. It is reachable **only from inside a shopping list**, where it
has something to reason about. This is the same call IAA-03 made for the add-item flow, applied one
level up: an AI opened from the app root has no list context, which is exactly the "dropped into a
chatbot with nothing to act on" problem IAA-03 removed.

`core/navigation`'s `AiChat` route still gains the argument — but now it is effectively always
populated:

```kotlin
@Serializable
data class AiChat(val shoppingId: String? = null) : NavKey
```

The nullable default is kept deliberately, even though every remaining call site passes a real id:
it keeps `chatGraph` total (no `!!`, no `requireNotNull`) and it leaves the door open for a
context-free entry (e.g. a future Wear or deep-link path) without another route change.
`chatGraph` calls `setShoppingContext(route.shoppingId)` **only when non-null**.

**What has to change to remove the tab** (verified by grepping every `AiChat` reference):

| File | Change |
| --- | --- |
| `core/ui/.../components/CestouBottomNavigation.kt` | Delete `BottomNavItem.AiChatItem` (lines 45-50), remove it from the `items` list (line 70), drop the `AiChat` import. |
| `app/.../MainActivity.kt:118` | `rootRoutes` becomes `listOf(ShoppingList, Profile)`. |
| `core/ui` string resources | `nav_ai_chat` becomes unused in all three locales (en/es/pt-BR). Remove it in the same PR so lint doesn't start reporting it. |
| `feature/chat/.../navigation/ChatGraph.kt` | `composable<AiChat>` stays — the destination still exists, it is just no longer a *root* destination. |

**Breakage check (explicitly requested)** — nothing depends on the tab existing:

- **Start destination / first launch / onboarding**: `MainActivity`'s `NavHost` starts at
  `ShoppingList` when authenticated and `Welcome` otherwise. `AiChat` has never been a start
  destination and no onboarding path routes through it. No impact.
- **Bottom bar visibility**: `showBottomBar = currentRoute != null`, where `currentRoute` is matched
  against `rootRoutes`. Once `AiChat` leaves that list, the bar **hides while the chat is open** —
  which is the correct new behaviour (the chat becomes a detail destination reached from a list,
  exactly like `CartFlow`). Flagging it because it is a real, intended visual change, not an
  accident.
- **`AiSettings`**: reached from `AiChatScreen`'s top-bar settings icon (`intent.onSettings`), not
  from the tab. Still reachable. F3.4's accessibility fix is unaffected.
- **Wear**: `WearRoutes` has no AI destination. No impact.
- **Analytics**: `AiChatViewModel` still fires `trackScreenView("ai_chat", ...)`, so the screen-view
  event survives; what changes is that every occurrence now has list context. Worth a one-line note
  to `data-engineer` — `.specs/ANALYTICS-PLAN.md` should not keep implying an AI entry point that
  no longer exists.
- **Maestro**: `chat_flow.yaml` currently reaches the chat via the tab. It must be rewritten to
  enter through a list (T7).

**One consequence worth surfacing to design, not decided here**: the bottom navigation drops from
three items to two (Lists, Profile). Material's guidance for a navigation bar is 3–5 destinations;
two is below that, and a two-item bar often reads better as something else entirely. This design
keeps the two-item bar because it is the smallest change that implements bruno's decision — but if
it looks thin on device, replacing the bar is a *visual* follow-up, not a re-litigation of OQ-2.

### What replaces `Options.AI` inside the add-item sheet

`Options.AI` today renders `AiChatScreen` inline in the sheet. It becomes an **action**: tapping the
AI chip dismisses the sheet and navigates to `AiChat(shoppingId)`. This satisfies AC4 ("continues to
open the same unified surface, not a third variant") literally — there is now exactly one
implementation of the chat UI in the app — and it is the same direction PROD-04 takes anyway
(`products/spec.md` P4 AC6 explicitly permits AI staying a full-screen destination).

`ProductScreen` therefore needs a navigation callback it does not have today. `ProductsGraph` already
holds the `Navigator`; the callback is threaded down as `onOpenAiChat: () -> Unit`, the same shape as
the existing `onBack`. No feature-module import of `feature.chat` is required.

---

## Error handling / regressions to not introduce

- Deleting `AiDockState` removes `CartUiState.aiDockState`, which is read at `CartScreen.kt:125`.
  That condition is currently always `true`, so the correct replacement is
  `if (!isKeyboardVisible) { focusManager.clearFocus() }` — **not** deleting the effect. Deleting it
  changes real focus behavior; simplifying it does not.
- `CartScreenTest` and `CartViewModelTest` may construct `CartUiState(aiDockState = ...)`. T2
  requires searching for those before the state field is removed, not after the build breaks.
- `AiChatScreenTest` / `AiChatViewModelTest` exist and will need updating once the ViewModel reads
  from a store instead of owning state — the store gets a fake in tests, not a mock of `StateFlow`.

## Decisions taken by bruno (both previously open, both closed 2026-09-11)

- **OQ-1 — conversation lifetime → process-scoped.** The spec's Edge Case promised history would
  survive a force-close; the code has never done that. CHAT-01 ships **process-scoped** continuity:
  the same history across every entry point while the app lives, empty after a cold start.
  Persisting it would mean picking a store and answering how long an AI conversation containing the
  user's list contents is retained — a privacy question adjacent to G2 (account/data deletion), not
  an engineering one. `spec.md`'s Edge Case has been corrected to match. **No implementation change
  from the original design; this only removes an unfounded expectation.**
- **OQ-2 — the bottom-nav "AI Assistant" tab is removed.** This *does* change the design: the
  original draft kept the tab as the no-surprises default. The AI is now reachable only from inside
  a shopping list. Design impact is in § "The entry points" above (four files, plus a breakage check
  that found nothing depending on the tab); task impact is a new **T5b** in `tasks.md`. The
  conversation store is **still required** — see § "The conversation holder" for why one entry point
  does not make shared state unnecessary.

## Risks

| Risk | Mitigation |
| --- | --- |
| `@Singleton` conversation shared across shopping lists: the user asks about list A, then opens list B and sees list A's messages with list B's context | Accepted for CHAT-01 (it is what AC5 asks for). `setShoppingContext` switching to a *different* non-null id appends a visible context-switch marker rather than silently re-pointing the context. Covered by T3's `Done when` and now written into `spec.md`'s Edge Cases. |
| A "clean up unused code" pass removes `Sender.PARTICIPANT` | Explicit `Done when` checkbox on T1 + a unit test asserting the enum has three cases (cheap, and it documents intent to the next reader). |
| ~~`AiChat` gaining an argument breaks bottom-nav tab selection~~ | **Moot after OQ-2** — there is no tab to select. `MainActivity`'s `rootRoutes` no longer contains `AiChat` at all. |
| Removing the tab silently changes bottom-bar visibility on the chat screen | Intended, not accidental: `showBottomBar = currentRoute != null`, so the bar now hides while the chat is open, matching every other detail destination (`CartFlow`). Called out in § "The entry points" and as a `Done when` on T5b so a reviewer doesn't read it as a regression. |
| Two-item bottom navigation (Lists, Profile) falls below Material's 3–5 destination guidance | Accepted as the smallest change that implements OQ-2. If it reads thin on device, replacing the bar is a *visual* follow-up — not a reason to reopen OQ-2. Flagged for design, not blocking. |
