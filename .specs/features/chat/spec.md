# Feature Specification: Unified Chat

## Problem Statement

The app has two separate, redundant AI-chat surfaces today:

1. A bottom-nav tab ("AI Assistant", `AutoAwesome` icon — `AiChat` root route in `MainActivity.kt`)
   that opens `AiChatScreen`/`AiChatViewModel` (`feature/chat`) full-screen, backed by
   `CartAssistantUseCase`.
2. `CartAssistantDock` (`feature/cart/.../components/ai/CartAssistantDock.kt` +
   `AiDockState.COLLAPSED/EXPANDED/CHAT`), a dock embedded in `CartScreen` that expands from a
   small bar into its own full-screen chat.

Both talk to the same AI, about the same shopping context, through two different UI patterns and
(as far as this review found) two different pieces of state/plumbing. This is confusing in exactly
the way `item-add-authorship` (IAA-03) already flagged and fixed for the *add-item* flow: a
first-time or low-patience user (Dona Célia, Camila-e-Pedro) has no reason to know "the AI" is two
different buttons that do overlapping things. IAA-03 explicitly scoped `CartAssistantDock` **out**
of its own fix ("Changing the AI chat dock inside an already-open list — out of scope, this spec
only touches the item picker"), so the dock/tab duplication is still open. bruno has already
authorized deleting `CartAssistantDock`; this spec defines what replaces it and what a *second*,
distinct capability — chatting with other list members, not just the AI — would need.

Separately, `ChatMessage.Sender.PARTICIPANT` exists in the domain enum today but is **unused
everywhere in the codebase** — there is no participant-to-participant messaging model, no Firestore
collection for it, and no UI for it. Collaboration on a shared list already exists (list sharing,
member roles, `history`/authorship on items per IAA-01, join/collaboration notifications per G5) —
but two members have never been able to *talk* to each other inside the app, only see what each
other did to the list.

## Current State (verified by reading the code, not assumed)

- `feature/chat`: `AiChatScreen`, `AiChatViewModel`, `CartAssistantUseCase` — AI-only, full-screen,
  reachable from the bottom nav (`AiChat` root route) and from `Options.AI` inside `ProductScreen`
  (`feature/products`).
- `feature/cart/.../CartAssistantDock.kt` + `AiDockState` — AI-only, embedded in `CartScreen`,
  three-state expand/collapse (`COLLAPSED` → `EXPANDED` → `CHAT`).
- `ChatMessage.Sender` enum has `USER`, `ASSISTANT`, `PARTICIPANT` — only `USER`/`ASSISTANT` are
  ever constructed today (confirmed: no usage of `Sender.PARTICIPANT` outside the enum
  declaration).
- No domain model, repository, or Firestore collection exists for person-to-person messages.
- The project runs on Firebase's **Spark (free) plan — no Cloud Functions**. Anything that would
  normally be "server pushes a notification to the other user's device the instant a message is
  sent" is not available; the existing collaboration-notification feature (G5) works around this by
  writing a Firestore `notifications` document client-side, which only reaches another member while
  *their* device has the app open with an active listener — not a true push. Any chat feature
  inherits this exact limitation.
- `.specs/MVP-ROADMAP.md` G10 already flags `cart`/`shopping`/`chat`/`ai-agent`/`products` as
  cross-importing each other directly instead of going through a `navigation`-only contract
  (violates AD-005). This feature sits right in the middle of that graph — `cart` already imports
  `feature.chat`, and a unified chat will need `shopping`'s member/collaboration data. **This spec
  does not resolve G10**; it is flagged here because Phase 2 (below) is exactly the kind of change
  that will make G10 worse if the tech-lead doesn't decide the shared-contract boundary first.

## Persona Check (`customer-personas` skill)

**Phase 1 (unify the two existing AI surfaces into one)** — serves every persona that touches the
AI/chat surfaces today (Marina, Dona Marlene, Dona Célia, Camila-e-Pedro, Rodrigo, Juliana*) or
neutral for the rest: fewer redundant UI patterns to learn, one visual identity for "talking to the
AI" instead of two. No persona's "o que ela rejeita" is triggered by *removing* a duplicate surface.
Same logic IAA-03 already used to justify demoting AI chat as the default add-item screen applies
here at the "how many chat UIs exist" level.

**Phase 2 (participant-to-participant messaging)** — evaluated persona by persona:

- **Lucas** (roommates splitting a shared cart) — the one persona with a plausible real use case:
  "avoid duplicate purchases, coordinate who's buying what." **But** he explicitly "rejeita qualquer
  feature que só funcione bem com um único usuário" — and a chat with no push notifications only
  "funciona bem" if the other person happens to have the app open at the same time. Sent without
  reliable delivery, this reads *worse* than the WhatsApp thread he already uses today (which does
  push), not better. Verdict: **serves, conditionally** — only if delivery reliability is addressed
  or the feature is framed honestly as best-effort, not real-time.
- **Bianca e Diego** — they explicitly reject "qualquer solução que dependa de um canal separado
  (chat, ligação)" to stay aligned, but their actual scenario is standing together in the same
  store with the *list itself* as the shared source of truth — which already exists and works.
  In-app chat with another person doesn't add much when the real-time list already does the
  coordinating; if anything, adding a chat surface is a "canal separado" of its own unless it's
  tightly anchored to specific items. Verdict: **neutral, not their bottleneck.**
- **Dona Célia, Marina, Dona Marlene, Rafael, Anderson, Eduardo, Rodrigo, Yasmin, Camila-e-Pedro,
  Juliana** — none of their stated goals or pains involve messaging another person through the app;
  most of them shop alone or the "coordination" pain (Bianca-e-Diego) is already solved by the live
  list. Verdict: **neutral** across the board — this is not a broadly-demanded feature, it's a
  narrow, single-persona case (Lucas) with a real technical caveat attached.

**Conclusion:** Phase 1 is uncontroversial and should proceed. Phase 2 has one real beneficiary
persona (Lucas) whose own stated rejection criterion ("doesn't work well with just one user online
at a time") is exactly the risk the Spark-plan push limitation creates. Recommend treating Phase 2
like `pantry-capture` in `.specs/PERSONA-ACTION-PLAN.md`: **gated by a technical spike**, not
spec'd straight to Tasks — the open question isn't "is this a good idea" in the abstract, it's
"can delivery be made reliable enough that it doesn't undercut the exact persona it's for."

## Out of Scope

| Item | Reason |
| ---- | ------ |
| Real-time push delivery of participant messages while the recipient's app is closed | No Cloud Functions on the Spark plan; would require either a paid plan upgrade or a client-exposed server credential (same anti-pattern flagged for the Gemini key in G15). Decision belongs to bruno, not this spec. |
| Group/global chat across all of a user's lists | Each conversation is scoped to one shared shopping list's membership, same scope as collaboration/notifications (G5) today. |
| Read receipts, typing indicators, message editing/deletion | Standard messaging polish, not needed to validate the core value; adds surface area to an already infra-heavy Phase 2. |
| Changing what the AI agent can *do* (its tool calls, `CartAssistantUseCase` logic) | This spec is about *where* the AI conversation lives (one surface instead of two), not what the AI is capable of. |
| Resolving G10 (cross-feature module coupling) | Named as a risk for the tech-lead; fixing the module graph is its own initiative per the roadmap, not bundled here. |

## User Stories

### P1: Unify the Two AI Chat Surfaces ⭐ Phase 1

**User Story**: As any user, I want one consistent place to talk to the AI assistant about my
shopping list, instead of a bottom-nav "AI Assistant" tab and a separate expanding dock inside the
cart that do overlapping things.

**Acceptance Criteria**:

> **Amended 2026-09-11 (tech-lead, after bruno resolved OQ-1 and OQ-2).** AC2 and AC5 below are
> rewritten and AC6 is new. The original AC5 assumed the bottom-nav "AI Assistant" tab would
> survive; bruno decided to **remove it** (OQ-2), so the AI is now reachable only from inside a
> shopping list — the same direction IAA-03 took for the add-item flow. See
> `.specs/features/chat/design.md`.

1. WHEN the user wants to talk to the AI from inside an open shopping list, THEN they SHALL reach
   the same chat surface (same state/history) as any other AI entry point — not a second,
   separately-stateful chat.
2. WHERE `CartAssistantDock` exists today (`feature/cart/.../components/ai/CartAssistantDock.kt`,
   `AiDockState`), THEN it SHALL be removed. **Verified during Design: this package is unreachable
   dead code** — `CartAssistantDock` has no call site anywhere, so no user-facing capability is lost
   and no replacement affordance is owed for it. In-cart AI access today comes from `Options.AI`
   inside the add-item sheet, which AC4 covers.
3. WHEN the unified chat is opened from within a specific shopping list, THEN it SHALL retain the
   existing context-scoping behavior (`setShoppingContext(shopping.id)`) so AI answers stay specific
   to that list, matching today's behavior in `Options.AI`.
4. WHERE `Options.AI` inside `ProductScreen` (`feature/products`) opens AI chat today, THEN it
   SHALL continue to open the same unified surface, not a third variant.
5. WHERE the user has an ongoing AI conversation and leaves the chat, THEN returning to it from any
   entry point **within the same app session** SHALL show the same conversation history, not a blank
   chat — including after the add-item sheet that launched it has been dismissed and reopened.
6. WHERE the bottom-nav "AI Assistant" tab exists today (`AiChat` root route, `AutoAwesome` icon),
   THEN it SHALL be removed, and the AI SHALL be reachable only from inside a shopping list, where
   it has context to reason about. (bruno's decision, OQ-2. Rationale: an AI tab opened from the
   root has no list context, which is the same "dropped into a chatbot with nothing to act on"
   problem IAA-03 removed from the add-item path.)

**Independent Test**: Open a shopping list, ask the AI "o que ainda falta comprar?", leave the chat
back to the list, then open the AI again — the same conversation and answer are there, not a blank
chat. Separately: there is no longer an "AI Assistant" item in the bottom navigation.

---

### P2: Message Other List Members (Phase 2 — spike-gated, not ready for Tasks)

**User Story**: As Lucas, I want to send a message to my roommates inside the same app I already
use for our shared list, instead of switching to WhatsApp, so coordination about what's already
been bought stays attached to the list itself.

**Why gated, not spec'd to Tasks yet**: This is new infrastructure end to end — a domain model for
person-to-person messages, a Firestore collection, security rules restricting a message to the
sender + list members only, a conversation-list UI, and (per the persona check above) an honest
answer to "what happens when the recipient's app is closed," which today's Spark plan cannot
guarantee via push. Building this without that answer risks shipping something that reads as
"broken chat" for the one persona it's meant to serve — worse than not having it. Recommend
treating this the same way `.specs/PERSONA-ACTION-PLAN.md` treats `pantry-capture`: a technical
spike first (can delivery be made reliable enough — e.g., relying on Android's existing FCM data
messages triggered by a client write plus a foreground/background listener, evaluated for
robustness — before this moves past "Pending" here.

**Draft Acceptance Criteria** (subject to change once the spike resolves the delivery question):

1. WHEN a user opens the unified chat for a shared shopping list, THEN they SHALL be able to switch
   between "AI" and each other list member (or a single "list conversation" all members share —
   open UX question for the tech-lead/design) using the existing
   `ChatMessage.Sender.PARTICIPANT` case that is already declared but unused.
2. WHEN a user sends a message to another member, THEN it SHALL be persisted (new Firestore
   collection, scoped to the shopping list's membership, gated by security rules analogous to
   AD-009's `notifications` rules) so it is visible to that member the next time their client is
   listening — explicitly **not** guaranteed as instant/real-time delivery unless the spike finds a
   viable push path.
3. WHERE the recipient's app was closed when the message was sent, THEN the app SHALL surface it
   (e.g., via the existing `notifications` mechanism from G5) the next time they open the app —
   reusing G5's client-side pattern rather than inventing a second one.
4. IF the sender is not a member of that shopping list, THEN the write SHALL be rejected by
   Firestore security rules (same trust boundary already established for `notifications` writes in
   G5/AD-009).

**Independent Test**: Not defined yet — blocked on the spike; an Independent Test that promises
"user A sends, user B receives" without qualifying delivery timing would overstate what's
technically possible today.

---

## Edge Cases (Phase 1)

> **Corrected 2026-09-11 (tech-lead).** Both original edge cases rested on premises that reading the
> code disproved. They are restated below with what is actually true; the originals are kept
> struck-through so the change is auditable.

- ~~IF a user has an in-progress AI conversation and force-closes the app, THEN reopening either the
  bottom-nav tab or the in-cart entry point SHALL resume the same history — this is existing
  `AiChatViewModel` behavior and must not regress when the dock is removed.~~
  **There is no such existing behavior.** `AiChatViewModel` holds messages in an in-memory
  `MutableStateFlow`; nothing is persisted anywhere, so a force-close has always produced a blank
  chat. **bruno's decision (OQ-1): conversation continuity is scoped to the process.** The
  conversation survives navigating away and back, and is shared by every entry point, for as long as
  the app lives; a cold start begins a new conversation. Persisting it is explicitly **out of
  scope** — it would require choosing a store and answering how long an AI conversation containing a
  user's list contents is retained, which is a privacy question adjacent to G2 (account/data
  deletion), not an engineering one.
- ~~IF `CartAssistantDock` is removed and no replacement entry point ships in the same change, THEN
  users lose in-context AI access from inside an open cart entirely.~~
  **Nothing is lost.** The dock is unreachable dead code (no call site); in-cart AI access already
  comes from `Options.AI` inside the add-item sheet, which AC4 preserves. No replacement affordance
  is owed.
- IF the user switches to a *different* shopping list while a conversation is in progress, THEN the
  conversation continues with the new list's context rather than starting over — a visible
  context-switch marker is appended so the user can see the AI is now reasoning about a different
  list, instead of the context changing silently underneath an existing thread.

## Requirement Traceability

| Requirement ID | Story                                          | Phase   | Status  |
| --------------- | ----------------------------------------------- | ------- | ------- |
| CHAT-01         | P1: Unify the Two AI Chat Surfaces               | In Tasks | In Tasks — Design approved (`design.md`, AD-010), tasks broken down (`tasks.md`), OQ-1/OQ-2 resolved by bruno 2026-09-11. Tracked as **G17 / F3.6** in `.specs/MVP-ROADMAP.md` |
| CHAT-02         | P2: Message Other List Members                   | Pending | Blocked — needs delivery-reliability spike before Design. Forward-compatibility deliberately preserved by CHAT-01's design (`Sender.PARTICIPANT` retained + test-guarded, `ChatMessage.senderId` added, conversation store is keyed) |

**ID format:** `CHAT-[NUMBER]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

## Roadmap Placement

Neither CHAT-01 nor CHAT-02 is a beta blocker under `.specs/MVP-ROADMAP.md`'s definition (a real
beta tester hitting a broken/unsafe/non-compliant happy path) — the app's core loop (sign in, build
a list, shop/share, finish) works without either. Recommended placement:

- **CHAT-01** (unify AI surfaces) — add to **Phase 3 (post-launch polish)** in
  `.specs/MVP-ROADMAP.md`, and note it as a prerequisite cleanup before CHAT-02 is even considered
  (no point building participant messaging into a chat surface that's about to be consolidated).
  Low risk, no new infra, directly continues the UX-simplification direction IAA-03 already set.
- **CHAT-02** (participant messaging) — do **not** add to the roadmap's gap list yet. It needs the
  delivery-reliability spike outcome first, the same way `pantry-capture` sits in
  `.specs/PERSONA-ACTION-PLAN.md`'s "Depois" bucket pending its own spike. If the spike concludes
  best-effort delivery is honestly communicable to the user (e.g., a visible "sent, not yet seen"
  state rather than implying WhatsApp-grade delivery), this can move to a real spec with Tasks.
