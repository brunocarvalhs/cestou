# Item Add Flow & Authorship Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its
Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is
the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review,
Verifier, discrimination sensor). If the skill isn't installed in the current environment, follow
this document's per-task/per-phase structure manually and apply the same rigor: don't start a task
without its dependencies done, don't skip its Gate.

---

**Design**: `.specs/features/item-add-authorship/design.md`
**Status**: Approved

---

## PR plan

Five PRs, each a complete, independently mergeable purpose — matching this project's existing
process (`.specs/MVP-ROADMAP.md`: "each item ships as its own branch + PR, never committed or
merged directly"). Branch from whatever branch currently carries this architecture
(`feat/new-layout` per the roadmap, or `develop` once merged).

| PR | Branch | Purpose | Requirement | Ships alone? |
| -- | ------ | ------- | ------------ | ------------- |
| PR1 | `feat/iaa-history-model` | `Product.history` data model, persisted, empty by default | IAA-01 (foundation) | Yes — no behavior change, purely additive schema |
| PR2 | `feat/iaa-write-points` | The three write sites append history entries | IAA-01 (data) | Yes — history fills in silently, still invisible in UI |
| PR3 | `feat/iaa-attribution-ui` | Avatars on item rows + tap-to-see-history sheet | IAA-01 (visible) | Yes — this is what makes P1 user-observable |
| PR4 | `feat/iaa-duplicate-warning` | Warn when adding a name that matches an active item | IAA-02 | Yes — independent of PR5 |
| PR5 | `feat/iaa-quick-add` | New default "Quick Add" surface replaces AI-first picker | IAA-03 | Yes, but land last — biggest UI/nav change, easiest to review in isolation once PR1-4 are in |

PR3 depends on PR1+PR2 (needs real history to display). PR4 and PR5 both depend on PR1+PR2 (PR4
reads history for the warning message; PR5 routes adds through the same save path). PR4 and PR5 do
not depend on each other and can be built in parallel once PR2 lands.

---

## Test Coverage Matrix

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------- | ---------------------- | ------------------ | ------------- |
| Domain model (`Product`, `ProductActivity`, `withActivity`) | unit | Every computed property + the append function | `core/domain/src/test/**/model/*Test.kt` | `./gradlew test` |
| Domain / Use Case (`ProductSaveUseCase`, `ProductDuplicateCheckUseCase`) | unit | 1:1 to spec ACs | `feature/products/src/test/**/usecase/*Test.kt` | `./gradlew test` |
| Presentation / ViewModel (`CartViewModel`, `EditItemViewModel`, `ConfirmItemViewModel`, `QuickAddViewModel`) | unit | History append + `memberProfiles` resolution logic | `feature/cart/src/test/**/viewmodel/*Test.kt`, `feature/products/src/test/**/viewmodel/*Test.kt` | `./gradlew test` |
| Data / Mapper (`ProductMapper`) | unit | `history` round-trip incl. missing-field default | `feature/products/src/test/**/mapper/*Test.kt` | `./gradlew test` |
| E2E (Maestro) | flow update | `product_management_flow.yaml` reflects the new default surface | `.maestro/flows/product_management_flow.yaml` | `maestro test .maestro/test_suite.yaml` (device required — per `MVP-ROADMAP.md` F0.3, this suite has never actually run in this environment) |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After each task | `./gradlew :core:domain:test` / `:feature:products:test` / `:feature:cart:test` (module matching the task) |
| Build | After each PR | `./gradlew test lint` |
| Device | Only for PR5 (nav/default-screen change), if a device is available | `maestro test .maestro/flows/product_management_flow.yaml` |

---

## Execution Plan

```
PR1 → PR2 → PR3
            PR2 → PR4
            PR2 → PR5
```

PR3, PR4, PR5 all require PR2 done; PR3/PR4/PR5 are otherwise independent of each other.

---

## Task Breakdown

### PR1 — History data model

#### T1: Add `ProductActivity` domain model

**What**: New `@Serializable data class ProductActivity(userId, action: Action, timestamp)` with
`Action { ADDED, EDITED, PURCHASED }`.
**Where**: `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/model/ProductActivity.kt`
**Depends on**: None
**Requirement**: IAA-01

**Done when**:
- [ ] File created, matches `design.md`'s Domain Layer Design exactly.

**Tests**: none (plain data class)
**Gate**: Quick

---

#### T2: Extend `Product` with `history` + computed properties + `withActivity`

**What**: Add `history: List<ProductActivity> = emptyList()` field; add `addedBy`, `lastEditedBy`,
`purchasedBy`, `lastActivity` as member computed properties (same style as the existing `total`);
add `Product.withActivity(action, userId): Product` as the single construction point.
**Where**: `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/model/Product.kt`
**Depends on**: T1
**Requirement**: IAA-01

**Done when**:
- [ ] `history` defaults to `emptyList()` (backward compatible with existing callers/tests).
- [ ] All four computed properties present and correctly derived (`firstOrNull`/`lastOrNull`/`maxByOrNull` per `design.md`).
- [ ] `withActivity` is the only place in the codebase that constructs a `ProductActivity` (verified in PR2 tasks).

**Tests**:
- [ ] Unit test: `addedBy` returns first `ADDED` entry's `userId`; `null` when `history` is empty.
- [ ] Unit test: `lastEditedBy`/`purchasedBy` return the *last* matching entry (not first), to cover repeated edits.
- [ ] Unit test: `withActivity` appends without mutating the original instance (data class copy semantics).

**Gate**: Quick

---

#### T3: Persist `history` in the data layer

**What**: Add `history` to `ProductModel` (serialized as a list of maps), extend `ProductMapper`
both directions. Missing/null on read → `emptyList()`.
**Where**:
- `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/data/model/ProductModel.kt`
- `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/data/mapper/ProductMapper.kt`

**Depends on**: T2
**Requirement**: IAA-01

**Done when**:
- [ ] `ProductModel.toMap()`/`fromMap()` (or equivalent) round-trip `history` losslessly.
- [ ] A `ProductModel` built from a map with no `"history"` key maps to `Product(history = emptyList())`, not a crash.

**Tests**:
- [ ] Mapper unit test: round-trip a `Product` with 3 mixed-action history entries.
- [ ] Mapper unit test: map missing `"history"` key → `emptyList()`.

**Gate**: Build (`./gradlew :feature:products:test :core:domain:test`)

---

### PR2 — Wire the three write points

#### T4: `ProductSaveUseCase` appends `ADDED`

**What**: Both `invoke(...)` overloads and `execute(...)` (the AI agent path) call
`AuthService.getOrCreateUserId()` and save `product.withActivity(ADDED, userId)` instead of the raw
product. For the AI path specifically, use `AiSession.userId` when available (falls back to
`AuthService` if `session.userId` is null) — per spec Edge Cases, the AI acts *for* the human, never
as its own identity.
**Where**: `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/domain/usecase/ProductSaveUseCase.kt`
**Depends on**: T3
**Requirement**: IAA-01

**Done when**:
- [ ] Every code path that ends in `repository.saveProduct(...)` goes through `withActivity(ADDED, ...)` first — no bypass.

**Tests**:
- [ ] Unit test: `invoke(name, quantity, shoppingId)` → saved product has one `ADDED` entry with the current user id.
- [ ] Unit test: `execute(...)` (AI path) → `ADDED` entry uses `session.userId`.

**Gate**: Quick

---

#### T5: `EditItemViewModel.onSaveEdit` appends `EDITED`

**What**: `onSaveEdit` builds `product.withActivity(EDITED, currentUserId)` before calling
`useCase.update(...)`. Needs `AuthService` injected (not currently a dependency of this ViewModel).
**Where**: `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/viewmodel/EditItemViewModel.kt`
**Depends on**: T3
**Requirement**: IAA-01

**Done when**:
- [ ] `AuthService` added to the constructor.
- [ ] Saved product's `history` has a new `EDITED` entry on top of whatever it had before (existing entries preserved, not overwritten).

**Tests**:
- [ ] Unit test: editing a product that already has an `ADDED` entry results in `[ADDED, EDITED]`, in that order.

**Gate**: Quick

---

#### T6: `ConfirmItemViewModel.onConfirmPurchased` appends `PURCHASED`

**What**: Same pattern as T5, action `PURCHASED`. Needs `AuthService` injected.
**Where**: `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/viewmodel/ConfirmItemViewModel.kt`
**Depends on**: T3
**Requirement**: IAA-01

**Done when**:
- [ ] `AuthService` added to the constructor.
- [ ] Confirming a purchase appends `PURCHASED` without disturbing prior entries.
- [ ] Per spec's confirmed decision: later edits via `EditItemContent` do **not** re-trigger or move `purchasedBy` — only this call site sets a `PURCHASED` entry.

**Tests**:
- [ ] Unit test: confirming purchase on a product with `[ADDED]` results in `[ADDED, PURCHASED]`.

**Gate**: Build (`./gradlew :feature:cart:test :feature:products:test`) — end of PR2, run the full module test set since T4-T6 all touch the shared `Product.withActivity` contract.

---

### PR3 — Attribution UI

#### T7: `CartUiState.memberProfiles` + resolution in `CartViewModel`

**What**: Add `memberProfiles: Map<String, UserProfile> = emptyMap()` to `CartUiState`. In
`CartViewModel`, when `shopping`/`products` load, collect distinct `userId`s from `shopping.users`
and every `Product.history` entry in the current list, resolve each via
`UserRepository.getUserProfile(id)` **once**, store the result in `CartUiState.memberProfiles`.
**Where**:
- `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/state/CartUiState.kt`
- `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/viewmodel/CartViewModel.kt`

**Depends on**: T3 (needs `history` to exist to collect ids from)
**Requirement**: IAA-01

**Done when**:
- [ ] No `ProductListItem`/`CartProductItem` composable calls `UserRepository` directly — profile resolution happens exactly once, in the ViewModel (Tech Lead performance requirement, `design.md` Tech Decisions).
- [ ] Map updates when the member set changes (new join) without re-resolving members already cached.

**Tests**:
- [ ] Unit test: `CartViewModel` resolves all distinct ids from `shopping.users` + product histories, and only those.
- [ ] Unit test: a user with no name in `UserProfile` still resolves (no crash), profile stored as-is for the UI to fall back on.

**Gate**: Quick

---

#### T8: Extend `UserAvatars` to render real profile photo/initials

**What**: Today `UserAvatars` (`ShoppingItem.kt:206-227`) draws a generic `Icons.Default.Person`
per user — it doesn't resolve or accept a real profile. Extend it (or add a sibling composable) to
accept a `UserProfile?` and render initials from `name`, falling back to the generic icon when
`name` is null. Do not break its existing callers in `ShoppingItem.kt`.
**Where**: `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/components/shopping/ShoppingItem.kt` (or extracted to a shared `core/ui` location if reused across `feature/shopping` and `feature/cart` — prefer extraction, since both need it now)
**Depends on**: None (independent of T7, but needed before T9)
**Requirement**: IAA-01

**Done when**:
- [ ] Existing `ShoppingItem` call sites of `UserAvatars` compile unchanged (no behavior regression there).
- [ ] New overload/parameter accepts a resolved `UserProfile?` and renders initials or the generic-icon fallback.
- [ ] If extracted to `core/ui`: `feature/shopping`'s existing usage updated to the new location.

**Tests**:
- [ ] Compose preview added (`@Preview`) for both the "has name" and "no name" states — matches this project's existing preview convention (every component read so far has one).

**Gate**: Quick

---

#### T9: Show attribution avatar on `ProductListItem`/`CartProductItem`

**What**: Render the extended `UserAvatars`/avatar for `product.lastActivity`'s `userId`, looked up
in `uiState.memberProfiles`. Hidden when `shopping.users.size <= 1` (spec IAA-01 AC6). Tapping opens
the history sheet (T10).
**Where**:
- `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/components/ProductListItem.kt`
- `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/components/CartProductItem.kt`

**Depends on**: T7, T8
**Requirement**: IAA-01

**Done when**:
- [ ] Avatar hidden on single-member lists — verified in Compose preview with a 1-user vs. 2-user `Product`/`Shopping` fixture.
- [ ] Tap target is a real clickable element (not overloading the existing `combinedClickable` used for purchased-toggle/edit) — needs its own click region so it doesn't collide with T5/T6's existing gestures.

**Tests**:
- [ ] Compose preview: avatar visible (2+ members) vs. hidden (1 member).

**Gate**: Quick

---

#### T10: Product history bottom sheet

**What**: New `ModalBottomSheet` dialog route (same pattern as `ConfirmItemRoute`/`EditItemRoute` in
`CartGraph.kt`), listing every `Product.history` entry chronologically:
`"<ação> por <nome ou ícone genérico> · <hora>"`, resolved via the same `memberProfiles` map (no new
fetch).
**Where**:
- `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/navigation/CartRoutes.kt` (new `ProductHistoryRoute`)
- `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/navigation/CartGraph.kt` (new dialog destination)
- New composable, e.g. `feature/cart/.../presentation/components/ProductHistoryContent.kt`

**Depends on**: T9
**Requirement**: IAA-01

**Done when**:
- [ ] Opening the sheet does not trigger a new `UserRepository` call (reuses `memberProfiles` passed through).
- [ ] Entries sorted oldest-first (spec AC5 example order: added → edited → purchased).
- [ ] Independent Test from spec.md passes manually: two accounts, add → edit → purchase, all three entries visible in order.

**Tests**:
- [ ] Compose preview with a 3-entry `history` fixture.

**Gate**: Build (`./gradlew :feature:cart:test lint`) — end of PR3.

---

### PR4 — Duplicate-item warning

#### T11: `ProductDuplicateCheckUseCase`

**What**: New use case per `design.md` — case-insensitive, trimmed name match against
non-purchased products in the target list.
**Where**: `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/domain/usecase/ProductDuplicateCheckUseCase.kt`
**Depends on**: PR2 complete (needs `addedBy` populated to be useful, though the check itself only needs `ProductRepository`)
**Requirement**: IAA-02

**Done when**:
- [ ] Matches ignore case and leading/trailing whitespace (spec AC1).
- [ ] Returns `null` when the only match is already `isPurchased == true` (spec AC3).

**Tests**:
- [ ] Unit test per AC1/AC3, plus a case-difference case ("leite" matches "Leite ").

**Gate**: Quick

---

#### T12: Wire the warning into the add paths

**What**: Before saving, call `ProductDuplicateCheckUseCase`; if it returns a match, show an inline
warning naming `match.addedBy`'s resolved name (fall back to generic label if unresolved) with a
"add anyway" affordance — no hard block (spec AC2). Wire into both the AI path
(`ProductSaveUseCase.execute`) and the Quick Add path once PR5 lands (T16 wires the UI side; this
task wires the check itself into `ProductSaveUseCase`/AI flow so it's live even before Quick Add
ships).
**Where**: `feature/products/.../domain/usecase/ProductSaveUseCase.kt` (or a thin wrapper invoked before it)
**Depends on**: T11
**Requirement**: IAA-02

**Done when**:
- [ ] Proceeding after a warning still saves the item normally (AC2).
- [ ] Race case (two adds within the same second) is explicitly not handled — matches spec Edge Cases, don't add locking.

**Tests**:
- [ ] Unit test: warning surfaced before save when a match exists; save still succeeds when the caller proceeds anyway.

**Gate**: Build (`./gradlew :feature:products:test lint`) — end of PR4.

---

### PR5 — Quick Add default entry point

#### T13: `Options.QUICK_ADD` + `QuickAddViewModel`

**What**: New `Options` entry. New lightweight `QuickAddViewModel` observing
`ProductsUseCase(shopping.id)` (same collection pattern as `ShoppingDuplicateUseCase`) to compute a
running total/budget comparison for the mini header — does **not** reach into `CartViewModel`
(`AD-005` module-internal boundary, per `design.md` Tech Decisions).
**Where**:
- `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/presentation/components/common/Options.kt`
- New: `feature/products/.../presentation/viewmodel/QuickAddViewModel.kt`
- New: `feature/products/.../presentation/components/product/QuickAddForm.kt`

**Depends on**: PR2 complete
**Requirement**: IAA-03

**Done when**:
- [ ] Mini header shows the same total/over-budget treatment as `SummaryCard` (text + color, not color-only — preserve the existing accessible pattern).

**Tests**:
- [ ] Unit test: `QuickAddViewModel` total matches the sum of `Product.total` for the observed list.

**Gate**: Quick

---

#### T14: Relocate common-product chips out of `SuggestionsAndCommonForm`

**What**: `Options.QUICK_ADD` uses the existing `CommonProductViewModel` +
`CommonProductIntent.onAddToShopping` directly (no new use case). Remove the "Common" toggle from
`SuggestionsAndCommonForm.kt:57-68` — its data source moves, not duplicates.
**Where**:
- `feature/products/.../presentation/components/product/SuggestionsAndCommonForm.kt`
- `feature/products/.../presentation/components/product/QuickAddForm.kt` (from T13)

**Depends on**: T13
**Requirement**: IAA-03

**Done when**:
- [ ] `SuggestionsAndCommonForm` no longer offers a redundant path to common products.
- [ ] Any existing test/snapshot asserting the old Common toggle is updated or removed, not left failing (flagged as a risk in `design.md`).

**Tests**:
- [ ] Update/remove the affected existing test(s) for `SuggestionsAndCommonForm` (search first — `design.md` Risks flags this explicitly, don't skip the search).

**Gate**: Quick

---

#### T15: Make `Options.QUICK_ADD` the start destination

**What**: `ProductScreen`'s internal `NavHost` `startDestination` changes from `Options.AI.name` to
`Options.QUICK_ADD.name`. `ProductHeader`'s option selector includes the new tab; AI/Search/Photo/
Suggestions remain reachable unchanged (spec AC3).
**Where**: `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/presentation/screen/ProductScreen.kt`
**Depends on**: T13, T14
**Requirement**: IAA-03

**Done when**:
- [ ] Opening the `+` FAB from `CartScreen` lands on Quick Add, not AI chat.
- [ ] `.maestro/flows/product_management_flow.yaml` updated to match the new default screen and tab order — this flow currently asserts `"Suggestions"`, `"Search"`, `"Photo"`, `"Barcode"` and item names as immediately visible after opening the add sheet; that assertion set needs to reflect Quick Add's actual first-frame content instead. (Note: this flow separately asserts a `"More options"` → `"Edit"`/`"Delete"` interaction on the product row that does **not** match current `ProductListItem.kt` — that's pre-existing drift, unrelated to this feature; per `MVP-ROADMAP.md` F0.3 the suite has never run on a device, so don't fix that mismatch here, just don't let this task's edits paper over it silently — leave a comment or note it in the PR description.)

**Tests**:
- [ ] `.maestro/flows/product_management_flow.yaml` updated (can't be executed in this environment — no device; note as unverified in the PR, matching how PR #14-#22 in `MVP-ROADMAP.md` already handle this limitation).

**Gate**: Build (`./gradlew test lint`)

---

#### T16: Wire duplicate-check + save into Quick Add's text field

**What**: Quick Add's free-text submit path calls `ProductDuplicateCheckUseCase` (T11) before
`ProductSaveUseCase` (T4), same as the AI path already does after T12.
**Where**: `QuickAddForm.kt` / `QuickAddViewModel.kt` (from T13)
**Depends on**: T12, T15
**Requirement**: IAA-02, IAA-03

**Done when**:
- [ ] Typing an existing active item's name and submitting shows the same warning behavior as the AI path (AC1-AC3 from IAA-02), reusing the same use case — not a reimplementation.

**Tests**:
- [ ] Unit test: Quick Add submit path surfaces the duplicate warning under the same conditions as T12's test.

**Gate**: Build (`./gradlew test lint`) — end of PR5, and of this feature.

---

## Phase Execution Map

```
PR1:  T1 → T2 → T3
PR2:              T3 → T4
                   T3 → T5
                   T3 → T6
PR3:  (T4,T5,T6) → T7 ─┐
                   T8 ─┴→ T9 → T10
PR4:  (T4,T5,T6) → T11 → T12
PR5:  (T4,T5,T6) → T13 → T14 → T15 → T16
                          T12 ┘
```

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1 | 1 file | ✅ Granular |
| T2 | 1 file | ✅ Granular |
| T3 | 2 files | ✅ Granular |
| T4 | 1 file | ✅ Granular |
| T5 | 1 file | ✅ Granular |
| T6 | 1 file | ✅ Granular |
| T7 | 2 files | ✅ Granular |
| T8 | 1 file (+ possible extraction) | ✅ Granular |
| T9 | 2 files | ✅ Granular |
| T10 | 3 files | ✅ Granular |
| T11 | 1 file | ✅ Granular |
| T12 | 1 file | ✅ Granular |
| T13 | 3 files | ✅ Granular |
| T14 | 2 files | ✅ Granular |
| T15 | 2 files (incl. Maestro flow) | ✅ Granular |
| T16 | 2 files | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ------------------------ | --------------- | ------ |
| T1 | None | None | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T3 | T3 → T5 | ✅ Match |
| T6 | T3 | T3 → T6 | ✅ Match |
| T7 | T3 | (T4,T5,T6) → T7 | ✅ Match |
| T8 | None | independent branch into T9 | ✅ Match |
| T9 | T7, T8 | T7,T8 → T9 | ✅ Match |
| T10 | T9 | T9 → T10 | ✅ Match |
| T11 | PR2 complete | (T4,T5,T6) → T11 | ✅ Match |
| T12 | T11 | T11 → T12 | ✅ Match |
| T13 | PR2 complete | (T4,T5,T6) → T13 | ✅ Match |
| T14 | T13 | T13 → T14 | ✅ Match |
| T15 | T13, T14 | T14 → T15 | ✅ Match |
| T16 | T12, T15 | T12,T15 → T16 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | ----------------------------- | ------------------ | ----------- | ------ |
| T1 | Domain model | none (plain data class) | none | ✅ OK |
| T2 | Domain model | unit | unit (3 cases) | ✅ OK |
| T3 | Data / Mapper | unit | unit (2 cases) | ✅ OK |
| T4 | Domain / Use Case | unit | unit (2 cases) | ✅ OK |
| T5 | Presentation / ViewModel | unit | unit | ✅ OK |
| T6 | Presentation / ViewModel | unit | unit | ✅ OK |
| T7 | Presentation / ViewModel | unit | unit (2 cases) | ✅ OK |
| T8 | Presentation / Component | preview (project convention, no unit-test layer for pure Compose UI) | preview | ✅ OK |
| T9 | Presentation / Component | preview | preview | ✅ OK |
| T10 | Presentation / Component + navigation | preview | preview | ✅ OK |
| T11 | Domain / Use Case | unit | unit | ✅ OK |
| T12 | Domain / Use Case | unit | unit | ✅ OK |
| T13 | Presentation / ViewModel | unit | unit | ✅ OK |
| T14 | Presentation / Component | existing test update | existing test update | ✅ OK |
| T15 | Presentation / Screen + E2E | Maestro flow update | Maestro flow update (noted as unverifiable here) | ✅ OK |
| T16 | Presentation / ViewModel | unit | unit | ✅ OK |
