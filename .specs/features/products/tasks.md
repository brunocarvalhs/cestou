# Add-Item Flow Redesign Tasks — PROD-04 / PROD-05

**Design**: `.specs/features/products/design.md`
**Status**: Approved. **Amended 2026-09-11** after bruno resolved OQ-3 (`SEARCH`/`SUGGESTIONS` stay
in the unified sheet → T9 extended, new task **T11b**) and OQ-4 (recipes go in `CartScreen`'s
existing overflow menu, not a dedicated button → T15/T17 updated).
**Blocked on**: CHAT-01 PR3 landing first (`.specs/features/chat/tasks.md` T6 removes the
`Options.AI` inline branch from `ProductScreen.kt`, which PR4 below rewrites).

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `spec-driven` skill: activate it by name and follow its Execute flow
and Critical Rules. If the skill isn't installed, follow this document's per-task structure manually
with the same rigor: no task before its dependencies, no skipped Gate.

Process (`.specs/MVP-ROADMAP.md` § Process): each PR is its own branch off `develop`, its own PR into
`develop`. Never commit or merge directly to `develop`.

---

## PR plan

| PR | Branch | Purpose | Requirement | Ships alone? |
| -- | ------ | ------- | ----------- | ------------ |
| PR4 | `feat/prod-add-item-sheet` | Sheet-native content: nested `NavHost` → saveable mode, compact header (Quick Add now a peer chip), persistent total, per-mode scroll/IME invariants for all four modes | PROD-04 AC1, AC4, AC5, AC6 | Yes — camera still behaves as today, just hosted as a mode |
| PR5 | `feat/prod-camera-in-sheet` | Camera becomes a bounded viewport inside the sheet + the R1–R9 hardening | PROD-04 AC2, AC3 | Yes, but only after PR4 |
| PR6 | `feat/prod-recipes-entry-point` | Recipes get their own route + screen, reached from `CartScreen`'s existing overflow menu; `SearchMode.RECIPE` deleted | PROD-05 AC1–AC4 | Yes |

`recipe-list-origin` (PERSONA-ACTION-PLAN #3) is a **separate spec and a separate PR on top of PR6** —
see `design.md` § "PROD-05 + recipe-list-origin". Do not fold it in.

---

## Test Coverage Matrix

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Presentation / Screen (`AddItemSheet`) | Compose preview | one preview per mode, matching the repo's existing preview convention | `feature/products/.../presentation/screen/` | `./gradlew :feature:products:test` |
| Presentation / ViewModel (`RecipeSearchViewModel`) | unit | search + add-ingredients paths, 1:1 to PROD-05 ACs | `feature/products/src/test/**/viewmodel/*Test.kt` | `./gradlew :feature:products:test` |
| Presentation / ViewModel (`ProductSearchViewModel`) | existing test update | `SearchMode` assertions removed, debounce behavior (G16) preserved | `feature/products/src/test/**/viewmodel/ProductSearchViewModelTest.kt` | `./gradlew :feature:products:test` |
| Camera components | **device only** | R1–R9 — not unit-testable | — | F0.3 device pass |
| E2E (Maestro) | flow update | `product_management_flow.yaml` reflects the sheet modes; new recipe flow | `.maestro/flows/` | `maestro test` — **device required, never run here (F0.3)** |

## Gate Check Commands

| Gate | When | Command |
| --- | --- | --- |
| Quick | after each task | `./gradlew :feature:products:test` |
| Build | end of each PR | `./gradlew test lint detekt` |
| Device | PR5 mandatory, PR4/PR6 desirable | manual walkthrough + `maestro test .maestro/flows/product_management_flow.yaml` |

---

## Execution Plan

```
CHAT-01 PR3 ──► PR4: T8 → T9 → T10 → T11 → T11b
                PR5:                       T11b → T12 → T13 → T14
                PR6:                                     T14 → T15 → T16 → T17
```

T11b is new (bruno's OQ-3 decision). It runs last in PR4 because the per-mode invariants it enforces
can only be checked once all four modes are hosted in the sheet (T8) with the final header (T9) and
the persistent total (T10) in place.

---

## Task Breakdown

### PR4 — Sheet-native add-item content

#### T8: Replace the nested `NavHost` with a saveable mode

**What**: `ProductScreen` → `AddItemSheet`. Delete the internal `NavHost`/`rememberNavController`
and the `navigate(option.name)` plumbing; hold the selected `Options` in `rememberSaveable`. Because
the nested destinations disappear, `LocalViewModelStoreOwner.current` becomes the `ProductPickerRoute`
dialog entry itself, so every `hiltViewModel(viewModelStoreOwner = ...)` becomes plain
`hiltViewModel()` and the `!!` at `ProductScreen.kt:46` is deleted.
**Where**: `feature/products/.../presentation/screen/ProductScreen.kt` (renamed `AddItemSheet.kt`)
**Depends on**: CHAT-01 PR3
**Requirement**: PROD-04 AC1, AC6

**Done when**:
- [ ] No `!!` anywhere in the file (tech-lead guardrail — `!!` without written justification is a blocking review comment).
- [ ] Every ViewModel still resolves `savedStateHandle.toRoute<ProductPickerRoute>()` successfully — this is the whole reason the owner was being passed explicitly; if any VM crashes on construction, the mode refactor is wrong, not the VM.
- [ ] System back and the sheet's scrim dismiss the sheet (there is no second back stack to pop anymore).
- [ ] `Options` enum retained as the mode type; `Options.AI` still present and still an action (CHAT-01 T6), not a mode.

**Tests**:
- [ ] Compose preview per mode.

**Gate**: Quick

---

#### T9: Compact, sheet-native header

**What**: `ProductHeader` stops being window chrome: drop `statusBarsPadding()` and the `TopAppBar`
in favor of a sheet-appropriate title row + the existing `FilterChip` mode row (keep the chips, the
icons and the string resources — they are reused verbatim). **Extended by OQ-3:** Quick Add must
become a visible peer chip.
**Where**: `feature/products/.../presentation/components/product/ProductHeader.kt`
**Depends on**: T8
**Requirement**: PROD-04 AC1, AC6

**Done when**:
- [ ] No window-inset padding inside a sheet (double insets are the visible symptom).
- [ ] The existing `product_option_*` string resources and chip icons are reused, not re-created — including `product_option_quick_add`, which already exists but is currently never rendered.
- [ ] **`ProductHeader.kt:80`'s `filterNot { it == Options.QUICK_ADD }` is removed.** Today the way back to Quick Add is the nested `NavHost` back stack; T8 deletes it, so without a Quick Add chip a user who taps "Search" is stranded with no way back to the fast-add surface except closing and reopening the sheet. **This is a regression the refactor introduces, not optional polish** — see `design.md` § OQ-3.
- [ ] The chip row still scrolls horizontally (`LazyRow`) now that it carries five entries (four modes + the AI action) on a narrow screen.
- [ ] Keep the `FilterChip` row; do **not** swap it for `SegmentedButton` in this task — that would discard working three-locale UI for a purely visual change.

**Tests**: preview updated, including a preview with Quick Add selected.
**Gate**: Quick

---

#### T10: Persistent total/budget header across every mode

**What**: Hoist `QuickAddTotalHeader` out of `QuickAddForm` into `AddItemSheet` so it renders above
the mode content in **every** mode (AC4) — including camera, which is the gap IAA-03 left.
**Where**:
- `feature/products/.../presentation/components/product/QuickAddForm.kt`
- `feature/products/.../presentation/screen/AddItemSheet.kt`

**Depends on**: T8
**Requirement**: PROD-04 AC4

**Done when**:
- [ ] The over-budget treatment stays text **+** color, never color-only (accessibility pattern IAA-03 established — do not regress it while moving the composable).
- [ ] `QuickAddViewModel` is still the source of the total; no second total computation is introduced.

**Tests**:
- [ ] Preview: header visible in camera mode (the AC4 case that doesn't exist today).

**Gate**: Quick

---

#### T11: One scrollable + sheet-state guard

**What**: Ensure the sheet content has exactly one vertically-scrolling container (`design.md`
§ Structure) and wire `confirmValueChange` on `rememberModalBottomSheetState` to refuse `Hidden`
while `mode == Options.PHOTO`.
**Where**: `feature/products/.../navigation/ProductsGraph.kt`, `AddItemSheet.kt`
**Depends on**: T9, T10
**Requirement**: PROD-04 AC1, AC2

**Done when**:
- [ ] No `LazyColumn` nested inside a `verticalScroll` anywhere in the sheet (R7).
- [ ] With camera mode active, drag-to-dismiss is refused; the close affordance still works.
- [ ] `skipPartiallyExpanded = true` is retained — the sheet stays full height so mode switches don't animate it (R12). Do not add partial detents.
- [ ] PR description states which of R1–R12 this PR does **not** address (the camera ones land in PR5) — don't let the reviewer assume the camera is hardened yet.

**Tests**: previews.
**Gate**: Quick

---

#### T11b: Per-mode invariants across all four sheet modes

**What**: Added by bruno's OQ-3 decision (`SEARCH`/`SUGGESTIONS` stay in the sheet). With four
content modes sharing one composition, three things that were previously per-screen become
per-mode invariants and have to be handled deliberately rather than inherited:

1. **One vertical scroller per mode (R7).** Audit each: `ProductSearchForm.kt:76` is a
   `LazyColumn(fillMaxSize)`; `ProductSearchForm.kt:254-263` is a `Column(verticalScroll)` wrapping
   a `fillMaxSize` child — that one is the selected-recipe detail and is **deleted by PROD-05
   (T16)**, so don't restructure it here, just don't let it regress;
   `ProductAnalysisConfirmation.kt:78` is a `LazyColumn` in a `fillMaxSize` `Box`;
   `SuggestionsAndCommonForm.kt:61` is a non-scrolling `Column` and `Suggestions.kt:50` is a
   `LazyRow` (horizontal — leave alone).
2. **IME handling on mode change (R10).** Quick Add and Search raise the keyboard; camera must not
   have it. Clear focus when leaving a text mode, request it when entering one — never inherit the
   previous mode's IME state.
3. **Transient per-mode state (R11).** Decide explicitly what survives a switch. Default: the search
   query/results survive (they are already `ProductSearchViewModel`-held — just don't clear them),
   list scroll positions reset.

**Where**: `feature/products/.../presentation/screen/AddItemSheet.kt` and the four mode composables
**Depends on**: T11
**Requirement**: PROD-04 AC1, AC6

**Done when**:
- [ ] Each mode has exactly one vertical scrolling container; documented in a short comment per mode so the next person doesn't reintroduce a nested one.
- [ ] Typing a query in Search, switching to Camera, and switching back does not clear the query.
- [ ] Switching Search → Camera leaves the keyboard closed; switching Camera → Search reopens it.
- [ ] No mode switch resizes the sheet (R12 — follows from T11's `skipPartiallyExpanded`, verify it holds with real Search results loaded).

**Tests**:
- [ ] Previews per mode.
- [ ] Honest note in the PR: the gesture/IME behaviours here are **device-verifiable only**; unit tests cannot close them.

**Gate**: Build (`./gradlew test lint detekt`) — end of PR4.

---

### PR5 — Camera as a bounded viewport

#### T12: Bound the camera viewport

**What**: `ProductPhotoForm` stops being `fillMaxSize().background(Color.Black)`. It becomes a
fixed-aspect viewport (`fillMaxWidth().aspectRatio(3f/4f)`, tune on device) with the black background
scoped to the viewport. Re-tune `CameraCaptureView`'s instruction text, which uses
`padding(top = 96.dp)` assuming a status bar that doesn't exist in a sheet (R9).
**Where**:
- `feature/products/.../presentation/components/product/ProductPhotoForm.kt`
- `feature/products/.../presentation/components/scanner/CameraCaptureView.kt`

**Depends on**: T11b
**Requirement**: PROD-04 AC2, AC3

**Done when**:
- [ ] `ProductPhotoViewModel`, `ProductAnalyzeImageUseCase` and `ProductAnalysisConfirmation` logic are **byte-for-byte unchanged** (AC3) — this task is container-only.
- [ ] The viewport cannot collapse to near-zero height when the IME is up (R6).

**Tests**: previews (`LocalInspectionMode` already stubs the camera — keep that branch).
**Gate**: Quick

---

#### T13: CameraX lifecycle hardening (R1, R2, R3)

**What**: In `CameraCaptureView`: set `PreviewView.implementationMode = COMPATIBLE` (R1); stop
calling `cameraProvider.unbindAll()` and unbind only the use cases this composable bound (R2); add a
`DisposableEffect` that unbinds on dispose (R3) — the same shape as the `DisposableEffect` G11 added
to `CameraPreview`.
**Where**: `feature/products/.../presentation/components/scanner/CameraCaptureView.kt`
**Depends on**: T12
**Requirement**: PROD-04 AC2

**Done when**:
- [ ] `unbindAll()` no longer appears in the file.
- [ ] Switching mode camera → text → camera repeatedly does not accumulate bound use cases.
- [ ] PR states explicitly that R1 (SurfaceView-in-dialog rendering black) is **unverified without a device** and is a named line item on the F0.3 checklist.

**Tests**:
- [ ] Consider an `androidTest` mirroring `CameraPreviewExecutorLifecycleTest` (the repo's only library-module `androidTest`, and it actually ran on a device) for the unbind-on-dispose path.

**Gate**: Quick

---

#### T14: Permission request on intent, not on entry (R4)

**What**: Remove the `LaunchedEffect(Unit)` auto-request in `ProductPhotoForm`. Request CAMERA when
the user taps the camera affordance, guarded by a `rememberSaveable` "already asked" flag. Keep
`PermissionDeniedState` (it already offers `openAppSettings()`) reachable for the permanently-denied
case. Also clear focus / hide the IME on entering camera mode (R6).
**Where**: `feature/products/.../presentation/components/product/ProductPhotoForm.kt`, `AddItemSheet.kt`
**Depends on**: T13
**Requirement**: PROD-04 AC2

**Done when**:
- [ ] Entering camera mode a second time in the same sheet session never re-prompts.
- [ ] Denying once and re-entering lands on `PermissionDeniedState` with a working settings button, not a black rectangle.
- [ ] The permission dialog pausing the activity does not dismiss the sheet (device-verify).

**Tests**: previews for granted / denied.
**Gate**: Build (`./gradlew test lint detekt`) — end of PR5.

---

### PR6 — Recipes as their own entry point

#### T15: `RecipesRoute` + `RecipeSearchViewModel` + `RecipeSearchForm`

**What**: New `@Serializable data class RecipesRoute(val shoppingId: String? = null) : NavKey` in
`feature/products/navigation/ProductsRoutes.kt` (public — the module's AD-005 entry surface). New
`RecipeSearchViewModel` carved out of `ProductSearchViewModel`'s recipe half, calling
`RecipeSearchUseCase` and `RecipeAddToListUseCase` **as-is** (spec AC2). New `RecipeSearchForm`
carved out of `ProductSearchForm`'s recipe half.
**Where**: `feature/products/.../navigation/`, `.../presentation/viewmodel/`, `.../presentation/components/product/`
**Depends on**: PR5
**Requirement**: PROD-05 AC1, AC2, AC3

**Done when**:
- [ ] Ingredients are added via `RecipeAddToListUseCase`, **not** by re-implementing `ProductSearchViewModel.onAddRecipeIngredients` (which is a verbatim duplicate of that use case — the duplicate is what goes away).
- [ ] The G16 debounce/`flatMapLatest` pattern is carried over to recipe search, not dropped in the carve-out.
- [ ] `shoppingId == null` is handled (the `recipe-list-origin` hinge) — at minimum it must not crash. **No entry point passes `null` in PROD-05** (OQ-4: the only entry is `CartScreen`'s overflow, which always has a list), so this is defensive only; the create-a-list-from-recipe behaviour belongs to `recipe-list-origin`.
- [ ] Everything `internal` except the route (AD-005 / L-001).

**Tests**:
- [ ] Unit: search success/failure; add-ingredients routes through `RecipeAddToListUseCase` once per ingredient.
- [ ] Unit: debounce cancels a stale in-flight search (mirror `ProductSearchViewModelTest`).

**Gate**: Quick

---

#### T16: Delete `SearchMode` from product search

**What**: Remove `ProductSearchUiState.SearchMode`, `onSearchModeChange`, `recipes`,
`selectedRecipe`, `onRecipeSelected`, `onClearRecipeSelection`, `onAddRecipeIngredients` and the
`RecipeSearchUseCase` dependency from `ProductSearchViewModel`/`ProductSearchIntent`/
`ProductSearchForm`. Product search becomes product-only (spec AC4).
**Where**: `feature/products/.../presentation/{state,intent,viewmodel,components}/`
**Depends on**: T15
**Requirement**: PROD-05 AC4

**Done when**:
- [ ] `ProductSearchViewModelTest`'s `SearchMode` assertions are **updated, not deleted wholesale** — the debounce/cancellation coverage in that file is G16's regression test and must survive.
- [ ] `AnalyticsParams.SEARCH_MODE` still gets a meaningful value from both screens (`"product"` / `"recipe"`), so `ANALYTICS-PLAN.md`'s funnel doesn't silently lose a dimension.

**Tests**: updated existing tests.
**Gate**: Quick

---

#### T17: Wire the entry point + Maestro flow

**What**: Per bruno's OQ-4 decision — a secondary/menu action, never a permanently visible button.
Add a "adicionar de uma receita" item to **`CartScreen`'s existing `MoreVert` `DropdownMenu`**
(which today holds "edit title" and "clear purchased"), navigating to
`RecipesRoute(shoppingId = shopping.id)`. Register the destination in `productsGraph`. Add a Maestro
flow covering the spec's Independent Test (reach recipes without passing through the add-item sheet).
**Do not** add an affordance to `ShoppingScreen` — it has no overflow menu today, and the
`shoppingId = null` entry it would host belongs to `recipe-list-origin`, when a destination that can
actually create a list exists.
**Where**: `feature/cart/.../presentation/screen/CartScreen.kt`,
`feature/cart/.../presentation/intent/CartIntent.kt` (+ `CartViewModel` for the navigation call),
`feature/products/.../navigation/ProductsGraph.kt`, `.maestro/flows/`
**Depends on**: T16
**Requirement**: PROD-05 AC1, Independent Test

**Done when**:
- [ ] No new permanently visible affordance anywhere — the entry lives inside the existing overflow menu (OQ-4's constraint).
- [ ] Navigation goes through `CartViewModel`'s `Navigator` like every other `CartIntent` destination (`ProductPickerRoute`, `ShareOptionsRoute`…), not from the composable directly — matches the module's existing pattern.
- [ ] No new module edge — `feature/cart` already depends on `:feature:products`; verify no new `implementation(project(...))` line was needed.
- [ ] The menu item is hidden or disabled when the list is locked (`Shopping.Status.FINISH`), consistent with how `CartScreen` already gates "clear purchased" — adding ingredients to a finished list would fail at `ProductRepositoryImpl.checkIsLocked` anyway.
- [ ] Maestro selectors are `testTag`s, never plain text (app runs EN and pt-BR; see `.specs/LESSONS.md`).
- [ ] PR states the flow was not executed (no device here, F0.3).

**Tests**: the flow (unrunnable here).
**Gate**: Build (`./gradlew test lint detekt`) — end of PR6 and of PROD-04/PROD-05.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T8 | 1 file (rewrite) | Granular |
| T9 | 1 file | Granular |
| T10 | 2 files | Granular |
| T11 | 2 files | Granular |
| T11b | 1 file + audit of 4 mode composables | Granular |
| T12 | 2 files | Granular |
| T13 | 1 file | Granular |
| T14 | 2 files | Granular |
| T15 | 4 files | Granular |
| T16 | 4 files | Granular |
| T17 | 4 files | Granular |

## Not in scope (deliberately)

- Moving camera components to `core/*` — would close a real G10 edge (`feature/shopping` imports
  `CameraPreview` from `feature/products`), but belongs to G10's own design pass, not to an add-item
  UX PR. Recorded in `design.md` AD-011 so it isn't rediscovered.
- `recipe-list-origin`'s `Shopping` ↔ recipe linkage — its own spec, its own PR, on top of PR6.
  It also owns the **second** recipes entry point (`RecipesRoute(shoppingId = null)` on
  `ShoppingScreen`, which needs an overflow menu that doesn't exist there yet), because that entry
  only makes sense once a destination that can create a list exists.
- Replacing the `FilterChip` mode row with `SegmentedButton` — visual choice, discards working
  three-locale UI, no structural gain.
- Partial sheet detents. Explicitly rejected (R12): with four modes of very different content
  heights, `skipPartiallyExpanded = true` is what keeps the frame stable.
- `Options.SUGGESTIONS` behavior (IAA-03 already consolidated it) and `Options.AI` behavior
  (CHAT-01 owns it).
