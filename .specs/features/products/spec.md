# Feature Specification: Products Management

## Problem Statement
Users need to manage items within a shopping list, including adding, editing, and tracking purchases with the help of AI and scanning tools.

## User Stories

### P1: Core Product Listing & Management
**Acceptance Criteria**:
1. WHERE the user views a shopping list, THEN the products SHALL be grouped by category.
2. WHERE the user marks an item as purchased, THEN the UI SHALL reflect the purchased status imutably.
3. WHERE the user deletes an item, THEN it SHALL be removed from the list and database.
4. WHERE the user edits an item, THEN the changes SHALL be persisted in real-time.

### P2: Scanning & Photo Capture
**Acceptance Criteria**:
1. WHERE the user scans a barcode, THEN the system SHALL attempt to fetch product details automatically.
2. WHERE the user takes a photo of a product, THEN the system SHALL use AI to identify the product title and details.

### P3: Cart Assistant (AI)
**Acceptance Criteria**:
1. WHILE the user is in the products list, THEN a chat-based assistant SHALL be available for queries.
2. IF the AI identifies products in a message, THEN it SHALL suggest adding them to the list.

> **Note (2026-09-11):** the in-cart half of P3 (`CartAssistantDock`) is being folded into the
> unified chat effort tracked in `.specs/features/chat/spec.md` (CHAT-01) — bruno has authorized
> removing `CartAssistantDock` once a single replacement entry point exists. This spec's P3 remains
> accurate for the underlying AI capability (`Options.AI` inside `ProductScreen`); only the in-cart
> dock UI is affected, and that change ships under the `chat` spec, not here.

---

## Add-Item Flow Redesign (2026-09-11)

### Context

IAA-03 (`.specs/features/item-add-authorship/spec.md`) already fixed the worst version of this
problem — it moved the default add surface from `Options.AI` to `Options.QUICK_ADD` so a user isn't
dropped into an AI chatbot just to add "arroz." That shipped. What bruno is now flagging as "a parte
de adicionar produtos está ruim" is the layer above that: confirmed by reading the current code,

- `ProductScreen` (`feature/products/.../screen/ProductScreen.kt`) is a **full navigation
  destination** — a `Scaffold` with a `NavHost` switching between five `Options` tabs
  (`QUICK_ADD`, `AI`, `SUGGESTIONS`, `SEARCH`, `PHOTO`) reached by navigating away from the cart,
  not an overlay on top of it. Adding an item still means leaving the screen you were on.
- The camera/photo flow (`Options.PHOTO` → `ProductPhotoForm` → `CameraCaptureView`) is one of
  those five tabs — a fully separate destination the user has to navigate to and back from, not an
  option surfaced inside the quick-add form itself. `QuickAddForm` does have a camera icon button
  (`onNavigateToPhoto`), but pressing it **navigates away** to a different `Options` tab rather than
  presenting the camera inline.
- Recipes are **not** a separate entry point today — they're a hidden mode switch inside
  `Options.SEARCH`. `ProductSearchViewModel` and `ProductSearchForm` share one screen between
  product search and recipe search via `ProductSearchUiState.SearchMode.PRODUCT` /
  `.RECIPE`, and `RecipeSearchUseCase`/`RecipeAddToListUseCase` are only ever called from inside
  that same ViewModel. A user has to already be in the "add a product" flow, then discover a mode
  toggle, to find recipes at all — confirming bruno's read that recipes are coupled into add-item
  rather than being their own thing.

This connects directly to an already-planned, not-yet-spec'd backlog item:
`.specs/PERSONA-ACTION-PLAN.md`'s "Agora" item **#3, `recipe-list-origin`** (linking a `Shopping`
list back to the recipe that generated it, for Yasmin). That item and this redesign's recipe
decoupling (P5 below) will very likely touch the same screen/entry point — **recommend the
tech-lead sequence or merge these two specs' Design phase** rather than building a standalone
recipe entry point twice.

### Persona Check (`customer-personas` skill)

- **Bottom sheet unification (P4)** — serves Dona Célia and Camila-e-Pedro directly (fewer full-screen
  navigations, add-item feels like "one small action," not "leave and come back"); neutral-to-positive
  for everyone else since it doesn't remove any capability, just changes its container. No persona
  rejects this.
- **Camera as an option inside the form, not a separate tab (P4)** — serves Marina and Dona Marlene
  ("total sempre visível," fewer steps before seeing the total); today, navigating to `Options.PHOTO`
  is its own screen where the running total (added in IAA-03's Quick Add header) is not shown. Folding
  camera into the same bottom sheet keeps that total visible the whole time, closing a gap IAA-03 left
  for this one path.
- **Recipes as a separate entry point (P5)** — serves Yasmin directly (same persona `recipe-list-origin`
  already targets) and Camila-e-Pedro indirectly (a recipe-driven list is exactly the "starting point"
  they said they lack). No persona depends on recipes staying inside the add-item flow — Rafael,
  Eduardo, and Dona Célia, who all want the fastest possible plain-item add, are neutral-to-positive
  about recipes moving out of their way.

### Out of Scope

| Item | Reason |
| ---- | ------ |
| Changing what `RecipeSearchUseCase`/`RecipeAddToListUseCase` do internally | This is about *where* recipe search lives in the nav, not the search/ingredient-add logic itself. |
| Building `recipe-list-origin`'s `Shopping` ↔ recipe linkage | Separate, already-planned spec in `.specs/PERSONA-ACTION-PLAN.md`; flagged here only because both will likely touch the same new recipe entry point. |
| Changing `Options.AI` or `Options.SUGGESTIONS` behavior | Out of scope for this redesign; AI unification is covered by `.specs/features/chat/spec.md`, and Suggestions/Common already got its consolidation in IAA-03. |
| Redesigning barcode scanning (`MlKitImageAnalyzerService`, `ProductImageTextRecognizer`) internals | Only the *container* the camera capture appears in is changing (inline option vs. separate screen), not the recognition pipeline. |

### P4: Unified Add-Item Bottom Sheet ⭐

**User Story**: As any user tapping "+" to add an item, I want one bottom sheet with a text field,
common-product chips, and a camera option all in the same place, instead of being navigated to a
separate full screen with tabs to reach the camera.

**Acceptance Criteria**:

1. WHEN the user taps the `+` FAB in `CartScreen`, THEN a `ModalBottomSheet` SHALL open containing
   the current `Options.QUICK_ADD` content (text field, running total/budget header, common-product
   chips) — replacing today's full-screen `ProductScreen` navigation destination for this entry
   point.
2. WHERE the bottom sheet is open, THEN a camera/photo option SHALL be reachable **inside the same
   sheet** (e.g., expanding the sheet to show the camera preview, or a dedicated section within it)
   rather than navigating to a separate `Options.PHOTO` destination.
3. WHEN the user captures or picks a photo from within the bottom sheet, THEN the existing AI
   product-identification flow (`ProductPhotoViewModel`/`ProductAnalysisConfirmation`) SHALL run
   unchanged — only its container changes, not its logic.
4. WHERE the bottom sheet is open, THEN the list's running total/budget header (already added by
   IAA-03) SHALL remain visible regardless of whether the user is on the text-field view or the
   camera view within the sheet.
5. WHEN an item is added from any surface inside this bottom sheet (typed, chip-tap, or
   photo-confirmed), THEN it SHALL go through the same `ProductSaveUseCase` path as today, so
   IAA-01 (authorship/`history`) and IAA-02 (duplicate warning) keep applying uniformly.
6. WHERE `Options.AI` and `Options.SUGGESTIONS` are reached from this flow (e.g., "more options"
   inside the sheet or a separate affordance), THEN they MAY remain full-screen destinations —
   this story only requires Quick Add + Photo to share one bottom sheet; consolidating every
   `Options` tab into the sheet is a design decision, not a hard requirement here.

**Independent Test**: Tap "+" from `CartScreen`; without leaving the current screen context (sheet
over cart, not a full-screen navigation), add "Arroz" by typing, then reopen "+" and add a second
item by taking a photo — both go through the same sheet, and the running total is visible the whole
time.

---

### P5: Recipes as a Separate Entry Point

**User Story**: As Yasmin (or anyone building a list from a recipe), I want to search for and add a
recipe's ingredients from its own place in the app, not by discovering a hidden toggle inside the
add-a-single-item flow.

**Acceptance Criteria**:

1. WHEN a user wants to add ingredients from a recipe, THEN they SHALL reach that capability from a
   distinct entry point (exact placement — e.g., a menu item on the shopping list screen, a
   separate action next to "+" — is a design decision for the tech-lead), not from a mode toggle
   inside product search.
2. WHERE the recipe entry point is used, THEN `RecipeSearchUseCase` and `RecipeAddToListUseCase`
   SHALL be reused as-is — this story changes navigation/placement, not the underlying search or
   ingredient-add logic.
3. WHEN recipe ingredients are added to a list, THEN each SHALL go through `ProductSaveUseCase`
   exactly as `ProductSearchViewModel.onAddRecipeIngredients` does today, preserving IAA-01
   authorship history for each ingredient.
4. WHERE `Options.SEARCH` (product search) is used after this ships, THEN it SHALL no longer expose
   `ProductSearchUiState.SearchMode.RECIPE` or the recipe mode toggle — product search becomes
   product-only, matching P4's goal of removing hidden mode switches from the fast-add path.
5. IF the tech-lead's Design phase decides to combine this entry point with the planned
   `recipe-list-origin` spec (linking a resulting `Shopping` back to its source recipe), THEN that
   combination SHOULD happen at Design time, not be treated as two unrelated screens built twice.

**Independent Test**: From outside the add-item flow entirely, a user finds and opens "recipes,"
searches for one, and adds its ingredients to a list — without ever passing through `Options.SEARCH`
or the QUICK_ADD bottom sheet.

---

## Requirement Traceability

| Requirement ID | Story                       | Phase     | Status    |
| -------------- | ---------------------------- | --------- | --------- |
| PROD-01        | Core Listing & Management    | Execution | Verified  |
| PROD-02        | Scanning & Photo              | Execution | Verified  |
| PROD-03        | AI Cart Assistant             | Execution | Verified  |
| PROD-04        | Unified Add-Item Bottom Sheet | Pending   | Pending   |
| PROD-05        | Recipes as a Separate Entry Point | Pending | Pending |
