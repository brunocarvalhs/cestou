# Feature Specification: Item Add Flow & Authorship

## Problem Statement

Today, "add item" funnels every user into a single default surface reached by tapping the `+` FAB
in the cart (`CartAdd.kt` → `CartViewModel.onToggleProductPicker` → `ProductPickerRoute` →
`ProductScreen`). `ProductScreen` opens on `Options.AI` (an AI chat) by default, with Search,
Suggestions, and Photo as separate tabs the user has to discover on their own — and a UI/UX review
(2026-09-04, see Design Review Findings below) found the real path to a common item is worse than
that: `Options.SUGGESTIONS` itself hides a second toggle between "Suggestions" and "Common"
(`SuggestionsAndCommonForm.kt:57-68`), so reaching a plain everyday item takes two nested tab
switches plus a scroll.

A persona review (customer-personas skill, 2026-09-04) surfaced two related gaps in this flow:

1. It's disorienting for less tech-savvy or first-time users (Dona Célia; Camila e Pedro) — the
   first thing they see when adding an item is a chatbot, not a plain "type and add" field.
2. On shared lists (Lucas; Bianca e Diego), `Product` (`core/domain/model/Product.kt`) carries no
   record of which member added, edited, or purchased an item. Collaborators can't tell who is
   already handling something, which is exactly what causes duplicate purchases and "who bought
   this" disputes.

## Goals

- [ ] Give "add item" one clear, low-friction default surface instead of landing on AI chat.
- [ ] Keep AI / Photo / Suggestions reachable, but as secondary options, not the first impression.
- [ ] Keep a lightweight history per item of who added it, who edited it, and who marked it
      purchased — not just a single snapshot field — so anyone on the list can check it on demand.
- [ ] Show the most recent activity (avatar/initials) directly on each item row.
- [ ] Warn the user inline when they're about to add an item that's already on the list, naming
      who added it.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Global/cross-list activity feed or a dedicated audit-log screen | Bigger feature. This spec keeps history scoped to one item at a time (a bottom sheet on that row), not a standalone timeline screen aggregating the whole list or account. |
| Assigning an item to a specific person before it's bought ("this one's yours") | Task assignment is a different feature from after-the-fact attribution. |
| Push notification when someone adds/edits/buys an item | Covered by the existing `NotificationsScreen`; wiring a new trigger is a follow-up, not core to closing the authorship gap. |
| Backfilling history on pre-existing products | Historical items simply render with no attribution (`history` starts empty). |
| Changing the AI chat dock inside an already-open list (`CartAssistantDock`) | Out of scope — this spec only touches the item picker (`ProductPickerRoute`), not the in-cart AI dock. |

### Related findings (not fixed by this spec, tracked separately)

The design review below surfaced two pre-existing issues unrelated to authorship or the add flow.
Noted here so they aren't lost, but they are **not** part of this spec's Success Criteria:

- **Contrast**: the selected `FilterChip` in `ProductHeader.kt:100-104` pairs `onPrimary` (white)
  text on `colorScheme.primary` (`CestouBrightGreen`) — roughly 3.3:1 contrast, below the 4.5:1
  WCAG AA minimum for normal text. Any new Quick Add chip that reuses this exact style inherits the
  failure; worth a theme-level fix, not a one-off patch here.
- **Untranslated/raw strings**: `ShoppingItem.kt:130` hardcodes `"$itemCount products"` in English,
  and `ShoppingItem.kt:147` renders the raw enum name (`IN_PROGRESS`, `FINISH`) instead of a
  localized label — breaks the "no jargon" bar this whole effort is trying to hit for Dona Célia,
  but it's pre-existing and outside `feature/products`/`feature/cart`.

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| ---------------------- | --------------- | --------- | ---------- |
| Current user id source | `AuthService.getOrCreateUserId()` | Already the pattern used for `CommonProductRepositoryImpl` writes. | [y] |
| Where to store authorship | `Product.history: List<ProductActivity>`, each entry `{ userId, action: ADDED \| EDITED \| PURCHASED, timestamp }`, embedded on the product document — **not** separate flat `addedBy`/`purchasedBy` fields | Superseded 2026-09-04: the user asked for edit history too ("quem editou e quem adicionou"), not just a point-in-time snapshot. A small embedded list answers "who added / who last edited / who bought" from one source of truth, with no new collection or join. | [y] |
| Convenience accessors | `addedBy` = `history.firstOrNull { ADDED }?.userId`; `lastEditedBy` = `history.lastOrNull { EDITED }?.userId`; `purchasedBy` = `history.lastOrNull { PURCHASED }?.userId` | Computed in the domain layer from `history`, not stored — avoids two sources of truth drifting apart. | [y] |
| History growth | No cap or pruning in v1; every ADDED/EDITED/PURCHASED action appends one entry | A shopping-list item realistically gets edited a handful of times at most (price/quantity fixes), so unbounded growth is a non-issue at this scale. Revisit only if usage data says otherwise — not worth the complexity of a cap now. | [y] |
| How to resolve id → name/photo | `UserRepository.getUserProfile(id)` | Reuses the profile infra already used to display shopping list members. | [y] |
| How history is surfaced in UI | Item row shows only the most recent actor's avatar (via the existing `UserAvatars` component already used in `ShoppingItem.kt:206-227`); tapping it opens a `ModalBottomSheet` listing every `history` entry chronologically — reusing the same bottom-sheet pattern as `ConfirmItemRoute`/`EditItemRoute`, not a new screen | Confirmed by design review: keeps the row uncluttered (one avatar, not a stack) while still answering "who did what" on demand, at near-zero new UI vocabulary. | [y] |
| New default add surface | Replace `Options.AI` as the start destination with a new `Options.QUICK_ADD` (text field + common-product chips + scan affordance, all on one screen); AI/Search/Photo/Suggestions move to a secondary option row | Matches what every persona valued: fewest taps to add a plain item (Marina, Dona Célia, Rodrigo). AI chat stays as the surface for higher-level help — creating a whole list from scratch and managing the purchase in progress — not for adding one item at a time; that scope can grow later. | [y] |
| Quick Add must absorb `CommonProductForm`, not sit beside it | The chips in `Options.QUICK_ADD` are sourced directly from `CommonProductGetAllUseCase`; the existing "Common" toggle inside `SuggestionsAndCommonForm.kt:57-68` is removed from that screen (its data source moves, the screen doesn't duplicate it) | Design review flagged that adding Quick Add as a *5th* surface next to an untouched Suggestions/Common toggle would just be one more confusing path, not a fix. | [y] |
| Cart total visible during Quick Add | `Options.QUICK_ADD` shows a persistent mini header/bar with the current list's running total (and budget if set), sourced the same way as `CartBottomBar` | Design review found the total disappears today because `ProductScreen` is a separate full screen from `CartScreen` — this closes that gap for Marina/Dona Marlene at near-zero extra cost since the screen is being rebuilt anyway. | [y] |

**Open questions:** none of the above block Design. One scope question is still open — see
"Companion accessibility fix?" right below.

### Companion accessibility fix? (needs your call)

The design review also found that editing or deleting an item has **no visible affordance** at
all today — only a hidden `onLongClick` (`ProductListItem.kt:72-76`) and a swipe-to-dismiss
gesture (`ProductListItem.kt:56-66`), with the identical pattern on shopping-list rows
(`ShoppingItem.kt:83-86`: edit/duplicate/share/finish/delete all behind long-press). That directly
hurts Dona Célia, the persona this spec is partly written for, but it spans two feature modules
(`feature/cart` and `feature/shopping`) and isn't required to close the authorship gap or the
add-flow confusion — it's a separate, real gap the review happened to notice.

**Decided (2026-09-04)**: tracked as a separate follow-up spec (`item-row-affordances` or similar,
TBD), not folded into IAA-01..03. This spec (IAA-01..03) is unaffected and ready to move to
Design.

### Note on the AI chat's role going forward

Confirming decision 1 also scopes what AI chat is *for*, so P3 doesn't read as "AI demoted for no
reason": `Options.AI` stops being the default single-item add path, but keeps — and is expected to
grow — its job as the assistant for higher-level actions: drafting a whole list from a prompt or a
recipe, and helping manage an in-progress purchase (e.g., "o que ainda falta comprar?", budget
check-ins). That evolution is explicitly out of scope for this spec (see Out of Scope) and belongs
to a future iteration of `feature/chat` / `feature/ai-agent`, not to IAA-03.

---

## User Stories

### P1: Item History & Authorship ⭐ MVP

**User Story**: As a member of a shared list, I want to see who added, edited, or bought each item
so that I don't duplicate a purchase or lose track of who's responsible for what.

**Why P1**: This is the actual gap identified against the Lucas / Bianca-e-Diego personas. It's
independent of the add-flow redesign (P3) and can ship first.

**Acceptance Criteria**:

1. WHEN a product is created through any add path (Search, Photo, Suggestions, AI, or the new
   Quick Add), THEN a `ProductActivity(action = ADDED, userId = current user, timestamp = now)`
   entry SHALL be appended to `Product.history`.
2. WHEN a user edits a product's name, category, price, or quantity (`EditItemContent` →
   `onSaveEdit`), THEN a `ProductActivity(action = EDITED)` entry SHALL be appended to `history`.
3. WHEN a user confirms an item as purchased (`ConfirmItemContent` → `onConfirmPurchased`), THEN a
   `ProductActivity(action = PURCHASED)` entry SHALL be appended to `history`.
4. WHERE a product row is rendered (`ProductListItem`, `CartProductItem`), THEN the avatar for the
   user of the most recent `history` entry SHALL be shown, resolved via
   `UserRepository.getUserProfile(id)`.
5. WHERE the user taps that avatar, THEN a bottom sheet SHALL list every `history` entry in
   chronological order (action, who, when — e.g. "Adicionado por Ana · 14:02",
   "Editado por Ana · 14:10", "Comprado por Bruno · 14:32").
6. IF the shopping list has a single member (`shopping.users.size <= 1`), THEN the row-level
   attribution avatar SHALL be hidden — no value for personas who shop alone (Dona Célia, Rafael,
   Eduardo), just visual noise. (The history bottom sheet can stay reachable regardless, since it
   costs nothing extra once built.)
7. IF a `history` entry references a user with no name in their cached `UserProfile`, THEN the UI
   SHALL fall back to a generic person icon/label instead of blank space or a crash.

**Independent Test**: Two accounts join the same list. User A adds an item; user B opens the list
and sees A's avatar on that row. User B edits the quantity, then confirms it purchased; the row's
avatar now reflects B, and opening the history sheet shows all three entries in order (A added,
B edited, B purchased).

---

### P2: Duplicate-Item Warning

**User Story**: As Lucas, I want to be warned if I'm about to add something already on the list so
my roommate and I don't both buy it.

**Why P2**: Directly closes the "duplicidade de item" pain point, using the `history` data
established in P1 (specifically, the first `ADDED` entry's `userId`).

**Acceptance Criteria**:

1. WHEN the user types or confirms a product name that matches an existing non-purchased item on
   the list (case-insensitive, trimmed), THEN the system SHALL show an inline warning naming who
   already added it, before the item is saved.
2. WHERE the user proceeds anyway, THEN the item SHALL be added normally — no hard block, since
   buying two packs on purpose is legitimate.
3. IF the matching existing item is already `isPurchased == true`, THEN no warning SHALL be shown
   (nothing left to duplicate).

**Independent Test**: List has "Leite" added by user A, not purchased. User B types "leite" in
Quick Add and sees "Leite já foi adicionado por [A]" before confirming.

---

### P3: Simplified Add Entry Point

**User Story**: As Dona Célia / Camila e Pedro, I want one obvious way to add an item without
having to understand AI chat, tabs, or jargon first.

**Why P3**: Addresses "muitos vão achar confuso" directly. It also gives P1 and P2 a single place
to run consistently, instead of duplicating that logic across four separate tabs.

**Acceptance Criteria**:

1. WHEN the user taps the `+` FAB in `CartScreen` (`CartAdd` → `onToggleProductPicker`), THEN
   `ProductScreen` SHALL open on a new `Options.QUICK_ADD` surface by default, instead of
   `Options.AI`.
2. WHERE `Options.QUICK_ADD` is shown, THEN it SHALL present, on one screen with no further
   navigation: a text field to type a name, quick-tap chips for the user's common products
   (`CommonProductGetAllUseCase`), and a scan/photo affordance.
3. WHERE the user wants AI assistance, Search, Photo, or Suggestions specifically, THEN those
   SHALL remain reachable via the existing `ProductHeader` option selector, unchanged in behavior.
4. WHEN an item is added from `Options.QUICK_ADD`, THEN it SHALL go through the same
   `ProductSaveUseCase` path as the other entry points, so P1 authorship/history and P2's duplicate
   check apply uniformly regardless of which surface was used.
5. WHERE `Options.QUICK_ADD` is shown, THEN its common-product chips SHALL be sourced directly from
   `CommonProductGetAllUseCase`, replacing the separate "Common" toggle inside
   `SuggestionsAndCommonForm.kt` — common items get one path, not two.
6. WHERE `Options.QUICK_ADD` is shown, THEN the list's current total (and budget, if set) SHALL
   stay visible in a persistent header, computed the same way as `CartBottomBar`'s total — adding
   an item SHALL never hide the running total.

**Independent Test**: On a fresh list, tapping `+` lets the user add "Arroz" from one screen
without opening AI chat or switching tabs, and the running total stays visible the whole time.

---

## Edge Cases

- IF the current user has no cached `UserProfile` name yet (e.g., just joined anonymously), THEN
  the avatar SHALL fall back to a generic person icon.
- IF a product is added by the AI agent on the user's behalf (`ProductSaveUseCase.execute` via
  `AgentActionUseCase`), THEN the resulting `ADDED` history entry SHALL carry the human user id
  (`AiSession.userId`), not a synthetic "AI" identity — the assistant acts *for* the user, not as
  its own collaborator.
- IF two users add the same-named item within the same second (race), THEN both items SHALL be
  saved (no server-side lock); P2's warning simply won't have caught the second one in time — this
  is an accepted trade-off per P2 AC2 (no hard block).
- IF a list transitions from single-member to shared (a new member joins) after items already
  exist, THEN previously added items SHALL start showing their (already-recorded) `history`-based
  avatar once the member count exceeds one — no migration needed, this is a pure render-time rule.
- IF the same user edits an item multiple times in a row (e.g., fixes quantity, then fixes price
  right after), THEN each save SHALL append its own `EDITED` entry — no de-duplication/merging of
  consecutive entries from the same user in v1 (see "History growth" assumption).

---

## Requirement Traceability

| Requirement ID | Story                          | Phase | Status   |
| --------------- | ------------------------------ | ----- | -------- |
| IAA-01          | P1: Item History & Authorship   | Tasks | In Tasks |
| IAA-02          | P2: Duplicate-Item Warning       | Tasks | In Tasks |
| IAA-03          | P3: Simplified Add Entry Point   | Tasks | In Tasks |

**ID format:** `IAA-[NUMBER]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 3 total, 16 tasks mapped (T1-T16 across 5 PRs), 0 unmapped ✅ — see
`.specs/features/item-add-authorship/tasks.md`.

---

## Success Criteria

- [ ] Every `Product` written after this ships has at least one `ADDED` entry in `history`.
- [ ] Editing or purchasing an item reliably appends a corresponding `EDITED`/`PURCHASED` entry —
      verified by opening the history bottom sheet after each action.
- [ ] A user can add a plain item to a shared list in ≤2 taps from the list screen, without seeing
      AI chat first, and without losing sight of the running total.
- [ ] Adding a duplicate-named active item surfaces a warning naming the original adder, in at
      least the Quick Add and AI paths.
- [ ] `./gradlew test` and `./gradlew lint` pass with `Product.history` wired through
      `ProductModel`/mappers.
