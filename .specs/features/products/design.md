# Design: Add-Item Flow Redesign — PROD-04 / PROD-05

**Spec**: `.specs/features/products/spec.md` § "Add-Item Flow Redesign"
**Also covers**: `.specs/PERSONA-ACTION-PLAN.md` "Agora" item #3 `recipe-list-origin` — **Design
merged here**, delivery sequenced (see § "PROD-05 + recipe-list-origin").
**Status**: Approved (tech-lead), 2026-09-11. **Amended the same day** after bruno resolved OQ-3
(`SEARCH` and `SUGGESTIONS` join the unified sheet) and OQ-4 (recipes live in a secondary menu, not
a dedicated button). No open questions remain.

---

## Correction to the spec's "Context" — the single most important finding

> The spec: "`ProductScreen` is a **full navigation destination** — a `Scaffold` with a `NavHost`
> switching between five `Options` tabs … reached by navigating away from the cart, not an overlay
> on top of it."

**It is already an overlay.** `ProductsGraph.kt` registers `ProductPickerRoute` as a
`dialog<ProductPickerRoute>` destination whose body is a `ModalBottomSheet(sheetState =
rememberModalBottomSheetState(skipPartiallyExpanded = true))` wrapping `ProductScreen`. `MainActivity`
even has explicit handling for this (`DialogNavigator.Destination` is excluded from bottom-bar
selection so the bar doesn't reflow behind the sheet).

What makes it *feel* like a full screen is the content, not the container:

- `ProductScreen` is a `Scaffold` (its own top bar + snackbar host) inside the sheet;
- `ProductHeader` applies `statusBarsPadding()` and a `TopAppBar` — i.e. it paints itself as a
  window-level chrome inside a sheet;
- every form is `fillMaxSize()`, and `ProductPhotoForm` additionally paints the whole thing black;
- a nested `NavHost` gives the sheet a second back stack.

**Consequences for PROD-04, all of them de-risking:**

1. AC1 ("a `ModalBottomSheet` SHALL open") is already satisfied by the container. PROD-04 is a
   **content** refactor inside an existing sheet, not the introduction of a new presentation mode.
2. **The camera already runs inside a `ModalBottomSheet` today** (`Options.PHOTO` →
   `ProductPhotoForm` → `CameraCaptureView`, all inside the sheet's dialog window). The "non-trivial
   architecture decision" flagged for the tech-lead is therefore not "can this work at all" — it is
   "which of the already-present camera-in-a-dialog hazards do we now have to handle deliberately,
   because the camera stops being a full-bleed tab and becomes a bounded region next to a text
   field." Those are enumerated in § "Camera-in-sheet risk register" and they are the real content
   of this design.
3. The effort estimate in `PERSONA-ACTION-PLAN.md` item #8 ("Médio") is, if anything, pessimistic on
   the container and optimistic on the camera.

---

## Module boundary decision (AD-011)

**Everything in PROD-04 and PROD-05 stays inside `feature/products`. No `core/*` growth, no new
module.** Reasoning:

| Candidate for extraction | Decision | Why |
| --- | --- | --- |
| The add-item sheet content | `feature/products` | Only `feature/products` renders it; `feature/cart` reaches it through `ProductPickerRoute`, which is already the AD-005-compliant `navigation/` entry point. No other module needs the internals. |
| Camera capture (`CameraCaptureView`, `CameraPreview`, `BarcodeAnalyzer`) | `feature/products`, unchanged | `feature/shopping`'s QR scanner already imports `CameraPreview` from `feature/products` — a real G10 edge, but a **pre-existing** one. Promoting camera components to `core/ui` (or a `core/camera`) is a defensible future move and would close that edge; doing it inside PROD-04 would mean a CameraX-plumbing move and an add-item UX rewrite in the same reviewable unit. **Explicitly deferred to G10's own design pass** — noted here so it isn't rediscovered. |
| Recipe search/add (PROD-05) | `feature/products` | `RecipeSearchUseCase`, `RecipeAddToListUseCase`, `RecipeRepository` and the `Recipe` model all already live there and are reused as-is (spec AC2). The new entry point is a new route in `feature/products/navigation/` — the module's existing public surface — consumed by `feature/shopping`, which already depends on `:feature:products`. **No new module edge is created.** |
| AI chat inside the sheet | moves **out** of `feature/products` | CHAT-01, see `.specs/features/chat/design.md`. |

Net effect on G10: PROD-04/05 add **zero** new cross-feature edges; CHAT-01 removes two.

---

## PROD-04 — Unified add-item bottom sheet

### Structure

Replace `ProductScreen`'s `Scaffold` + nested `NavHost` with sheet-native content driven by a
`rememberSaveable` mode:

```
ProductsGraph
 └ dialog<ProductPickerRoute>
    └ ModalBottomSheet(sheetState)
       └ AddItemSheet(shopping, onBack, onOpenAiChat)     <- replaces ProductScreen
          ├ AddItemSheetHeader        compact: title + close + mode chips, Quick Add INCLUDED as a
          │                           peer chip (no TopAppBar, no statusBarsPadding) — see OQ-3
          ├ QuickAddTotalHeader       ALWAYS visible, every mode (AC4)
          └ when (mode) {
               TEXT    -> QuickAddForm     (text field + common chips)   unchanged logic
               CAMERA  -> ProductPhotoForm (bounded viewport)            unchanged logic, new container
               SEARCH  -> ProductSearchForm                              product-only after PROD-05
               COMMON  -> SuggestionsAndCommonForm
            }
```

### OQ-3 — all four content modes live in the sheet (bruno's decision)

bruno chose to bring `SEARCH` and `SUGGESTIONS` into the unified sheet alongside Quick Add and the
camera, rather than demoting them to full-screen destinations (spec AC6 permitted either).

**Straight answer on scope, because the framing of the question assumed otherwise: this is the
cheaper of the two branches, not an expansion of feature work.** All five `Options` already render
*inside* the `ModalBottomSheet` today — they are nested `NavHost` destinations within it, not
separate screens — and the `when (mode)` block sketched above already listed `SEARCH` and `COMMON`.
Demoting them would have been the change that needed new destinations, new routes and a new way back.
So no new task exists for "move search into the sheet"; it is already there.

What the decision **does** change is the risk surface, and that is real:

1. **The one-scrollable invariant now spans four modes, not two.** Verified in the current code:
   `ProductSearchForm.kt:76` is a `LazyColumn(fillMaxSize)`; `ProductSearchForm.kt:254-263` is a
   `Column(verticalScroll(...))` wrapping a `fillMaxSize` child (the selected-recipe detail — which
   PROD-05 deletes, so that one resolves itself); `ProductAnalysisConfirmation.kt:78` is a
   `LazyColumn` inside a `fillMaxSize` `Box`. `SuggestionsAndCommonForm.kt:61` is a plain
   `Column(fillMaxSize)` with no vertical scroller of its own, and `Suggestions.kt:50` is a
   `LazyRow` — horizontal, so no vertical gesture conflict. R7 is therefore upgraded from "one known
   case" to "an invariant to enforce per mode", and gets its own task (T11b).
2. **Two of the four modes raise the IME** (Quick Add's text field, Search's query field) and one
   must not have it (camera). Mode switching therefore needs deterministic IME handling rather than
   whatever the previous mode left behind — new risk R10.
3. **Transient per-mode state** (list scroll position, search results) either resets on every switch
   or has to be hoisted — new risk R11.
4. **Sheet height must stay fixed.** `skipPartiallyExpanded = true` is retained specifically so the
   sheet does not animate its height as modes with wildly different content lengths swap in. Do not
   introduce partial detents here — new risk R12.

**A concrete defect this decision makes unavoidable, found while re-checking the mode switcher:**
`ProductHeader.kt:80` deliberately excludes Quick Add from the chip row
(`Options.entries.filterNot { it == Options.QUICK_ADD }`), because today the way *back* to Quick Add
is the nested `NavHost`'s back stack — chips navigate with `popUpTo(startDestination) { saveState }`,
so system back returns you to Quick Add. **Once T8 deletes that `NavHost`, system back dismisses the
whole sheet, and there is no affordance left to return to Quick Add from Search.** The user would be
stranded in a secondary mode with no way back to the fast-add surface except closing and reopening
the sheet. T9 must therefore surface Quick Add as a peer chip. This is not optional polish; it is a
regression the refactor introduces if unhandled.

**Mode switcher UI**: keep the existing `FilterChip` row. It already exists, is already localized in
three locales, already has per-option icons, and already handles four-plus items via `LazyRow`.
A `SegmentedButton` row is a legitimate visual alternative but is a design choice, not an
architecture one, and swapping it would discard working localized UI for no structural gain.

### Key decisions

- **Drop the nested `NavHost`.** It gives the sheet a second back stack that competes with
  drag-to-dismiss and the system back gesture, and it is the reason `ProductScreen` needs
  `LocalViewModelStoreOwner.current!!`: the sub-destinations' `NavBackStackEntry`s are not the entry
  that carries `ProductPickerRoute`, which every one of these ViewModels reads via
  `savedStateHandle.toRoute<ProductPickerRoute>()`. With the `NavHost` gone,
  `LocalViewModelStoreOwner` *is* the dialog entry, plain `hiltViewModel()` works, and the
  unjustified `!!` disappears. Mode becomes `rememberSaveable` state.
- **`Options` is repurposed, not deleted.** It survives as the mode enum so `ProductHeader`'s chip
  row, its string resources and its icons are reused. `Options.AI` becomes an action (CHAT-01 T6),
  not a mode — a chip that navigates out.
- **One scrollable.** The sheet content must contain exactly one vertically-scrolling container.
  `LazyColumn` inside a `verticalScroll` inside a draggable sheet is the classic nested-scroll
  deadlock; `ProductAnalysisConfirmation` (post-capture, potentially long) is the concrete case.
- **`ProductSaveUseCase` is untouched** (AC5) — IAA-01 authorship and IAA-02 duplicate warnings keep
  applying because no add path changes; only their container does.

### Sheet state

```kotlin
val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    confirmValueChange = { target -> !(mode == Options.PHOTO && target == SheetValue.Hidden) }
)
```

While the camera is live, drag-to-dismiss is refused and the user leaves via the explicit close/back
affordance. This is the single most important interaction decision in PROD-04: an accidental
half-swipe that tears down a bound CameraX session mid-capture is both a UX and a resource-lifecycle
failure.

### Camera as a bounded viewport

`ProductPhotoForm` today is `Modifier.fillMaxSize().background(Color.Black)`. Inside the sheet it
becomes a fixed-aspect viewport (`fillMaxWidth().aspectRatio(3f/4f)` — verify on device) with the
black background scoped to the viewport only, so the running total header (AC4) and the mode chips
stay visible above it. `CameraCaptureView`'s capture/gallery/analysis logic is untouched (AC3); only
its `Modifier` contract and its instruction-text placement (`padding(top = 96.dp)` assumes a status
bar that isn't there in a sheet) change.

---

## Camera-in-sheet risk register

Requested explicitly by the pm as an open point. **Every item below is a hazard that exists in the
code today** — the camera is already in a sheet. PROD-04 does not create them; it makes them
load-bearing, because the camera stops being a full-bleed tab the user is obviously "inside of".
**None of these can be verified in this environment — there is no adb/emulator. Every one of them
needs the F0.3 device pass.**

| # | Risk | Detail | Mitigation |
| --- | --- | --- | --- |
| R1 | **SurfaceView in a dialog window renders black** | `ModalBottomSheet` renders in a separate dialog window. `PreviewView` defaults to `IMPLEMENTATION_MODE_PERFORMANCE` (SurfaceView), whose surface is z-ordered against its own window; in a dialog this is the classic "preview is black / punches through the scrim" configuration on several OEM + API combinations. `CameraCaptureView.kt:51` constructs `PreviewView(ctx)` with no `implementationMode` set at all. | Set `implementationMode = PreviewView.ImplementationMode.COMPATIBLE` (TextureView) whenever the preview is hosted in a sheet. Costs a little power/latency; buys correctness in a dialog window. **Must be eyeballed on a real device** — this is the top device-QA item for PROD-04. |
| R2 | **`unbindAll()` is global** | `CameraCaptureView.kt:60` calls `cameraProvider.unbindAll()` before binding. That unbinds *every* CameraX use case in the process, including `feature/shopping`'s `QrCodeScanner` if it is alive behind the sheet. | Bind this composable's own use cases and unbind only those. |
| R3 | **No unbind on dispose** | `CameraCaptureView` has no `DisposableEffect` (unlike `CameraPreview`, which got one for its executor in G11). Binding is tied to `LocalLifecycleOwner`, which inside a `dialog<>` destination is the dialog's `NavBackStackEntry` — destroyed on dismiss, so in practice it unwinds, but nothing guarantees it if the composable merely leaves the composition on a **mode switch** (which is exactly what PROD-04 introduces: camera ↔ text within one live entry). | Add a `DisposableEffect` that unbinds on dispose. Same class of defect as G11; do it in the same task that bounds the viewport. |
| R4 | **Permission auto-request re-fires on every mode entry** | `ProductPhotoForm.kt:70` requests CAMERA from `LaunchedEffect(Unit)`. As a separate destination this ran once per navigation. As a *mode*, entering/leaving camera repeatedly re-runs it; Android auto-denies a re-request after the user has dismissed it, silently landing in `PermissionDeniedState`. The permission dialog also pauses the host activity, which can recompose/dismiss the sheet underneath it. | Do **not** auto-request on mode entry. Request on the user's explicit tap of the camera affordance, guarded by a `rememberSaveable` "already asked this session" flag. `PermissionDeniedState` already offers `openAppSettings()`, so the permanently-denied path is covered — keep it reachable. |
| R5 | **Drag gesture conflict** | The sheet's vertical drag and the camera viewport compete. Today's `CaptureControls` has no vertical gesture so it is latent; any future tap-to-focus/pinch-zoom, or simply a user resting a thumb on the preview and sliding, drags the sheet. | `confirmValueChange` (above) refuses `Hidden` while in camera mode; the viewport is a fixed-height region, not a drag handle. |
| R6 | **IME + sheet + camera** | Quick Add's text field raises the keyboard and the sheet resizes. Switching to camera with the IME up leaves the preview a few dozen dp tall, and CameraX will happily bind to it. | Clear focus and hide the IME when entering camera mode; give the viewport a fixed aspect ratio rather than `fillMaxSize()` so it can't collapse. |
| R7 | **Nested scroll deadlock — now an invariant across four modes, not one case** | *Severity raised by bruno's OQ-3 decision.* A scrollable inside a scrollable inside a draggable sheet fights for the vertical gesture. Verified instances: `ProductSearchForm.kt:76` (`LazyColumn(fillMaxSize)`), `ProductSearchForm.kt:254-263` (`Column(verticalScroll)` wrapping a `fillMaxSize` child — deleted by PROD-05), `ProductAnalysisConfirmation.kt:78` (`LazyColumn` in a `fillMaxSize` `Box`). `SuggestionsAndCommonForm.kt:61` is a non-scrolling `Column`; `Suggestions.kt:50` is a `LazyRow` (horizontal, no conflict). | Exactly one vertical scrolling container per mode, enforced per mode rather than once (§ Structure). Own task: **T11b**. |
| R8 | **Rotation / config change while open** | The camera rebinds; `capturedImageUri` survives in `ProductPhotoUiState` (ViewModel-held), but the *mode* would be lost if held in plain `remember`. | `rememberSaveable` for the mode. |
| R9 | **Instruction-text placement assumes a status bar** | `CameraCaptureView.kt:80` uses `padding(top = 96.dp)` — tuned for a full-screen tab, wrong inside a bounded viewport. | Re-tune with the viewport, not the window, as the reference frame. |
| R10 | **IME state carries across mode switches** | *New, from OQ-3.* Two of four modes raise the keyboard (Quick Add's text field, Search's query field) and one must not have it (camera). Switching Search → Camera with the IME up reproduces R6 from a second direction; switching Camera → Search must *restore* focus for the mode to be usable. | Deterministic IME handling on mode change: clear focus when leaving a text mode, request it when entering one, never inherit the previous mode's state. Part of T11b. |
| R11 | **Transient per-mode state on switch** | *New, from OQ-3.* Four modes share one composition. List scroll positions and search results either reset on every switch (jarring: type a query, peek at the camera, come back to an empty search) or must be hoisted deliberately. Note the asymmetry: `ProductSearchViewModel` holds the query in its `uiState` and survives, while a `remember`ed `LazyListState` does not. | Decide per mode, explicitly, in T11b. Default: hoist the search query/results (already ViewModel-held — just don't clear them), let scroll positions reset. |
| R12 | **Sheet height instability across modes** | *New, from OQ-3.* Modes have very different natural content heights (a text field + chip row vs. a long results list). Partial detents would make the sheet jump on every switch. | Keep `skipPartiallyExpanded = true` — the sheet is always full height, so mode content changes inside a stable frame. **Do not** introduce partial detents while adding modes. |

**Verification honesty**: none of R1–R12 can be closed by a unit test. PROD-04's PR must say so
plainly, in the same style `.specs/STATE.md` already uses for G12–G16 ("backed by JVM unit tests and
code review only"). R1 in particular is a "works on my emulator, black on a real device" class of
bug — it belongs on the F0.3 device checklist as a named line item, not as a general "try the app".

---

## PROD-05 + `recipe-list-origin` — merged Design, sequenced delivery

The pm asked whether to merge or sequence. **Merge the Design, sequence the delivery: one entry
point, built once, two PRs.**

Why not one PR: PROD-05 is a pure navigation/placement change reusing existing use cases (spec AC2),
while `recipe-list-origin` adds a nullable field to `Shopping`/`ShoppingModel` and a new Firestore
field — a data-model change with mapper, security-rule (AD-009) and Wear implications. Those are
different risk classes; the roadmap's own G12–G16 note is the precedent for keeping them apart.

Why merge the Design: both need the *same* screen, and the hinge between them is one route parameter.

### The shared design

New route in `feature/products/navigation/ProductsRoutes.kt`:

```kotlin
@Serializable
data class RecipesRoute(val shoppingId: String? = null) : NavKey
```

- `shoppingId != null` → "add this recipe's ingredients to the list I'm already in" (today's
  behavior, reached from inside a list).
- `shoppingId == null` → "start from a recipe" — Yasmin's actual flow, and the reason the parameter
  is nullable from day one even though PROD-05 alone doesn't need it. This is the hinge that lets
  `recipe-list-origin` be additive instead of a second entry point.

**Entry point placement (OQ-4, decided by bruno: a secondary/menu action, never a permanently
visible dedicated button). This supersedes the earlier draft, which put the entry point on
`ShoppingScreen` next to create/join.**

The capability ships where it already works, and only that:

- **PROD-05 → `CartScreen`'s existing overflow menu.** `CartScreen` already has a `MoreVert`
  `DropdownMenu` (items: edit title, clear purchased). Adding "adicionar de uma receita" →
  `RecipesRoute(shoppingId = shopping.id)` is a menu entry in UI that already exists: zero new
  permanent affordance, which is exactly the constraint, and it is a genuinely secondary action next
  to the primary add flow. It still satisfies spec AC1 and the Independent Test, because the overflow
  menu is outside the add-item sheet entirely — the user never passes through `Options.SEARCH` or the
  Quick Add sheet to reach it.
- **`recipe-list-origin` → `ShoppingScreen`, later.** The `shoppingId = null` case ("start a list
  *from* a recipe", Yasmin's actual flow) belongs on the shopping-list screen. But **`ShoppingScreen`
  has no overflow menu today** — verified: it has one top-bar `IconButton` (join) and a FAB (create),
  nothing else. Adding a menu there for a single item whose destination cannot yet create a list
  would ship a dead end. So that entry lands with `recipe-list-origin`, when the behaviour behind it
  exists.

This also removes a module concern: `CartScreen` is in `feature/cart`, which already depends on
`:feature:products` (it uses its use cases and `ProductPickerRoute`). Still no new edge.

New `RecipeSearchViewModel` + `RecipeSearchForm` in `feature/products`, carved out of
`ProductSearchViewModel`/`ProductSearchForm`. `ProductSearchUiState.SearchMode` and its toggle are
deleted, and `ProductSearchViewModel` loses its `RecipeSearchUseCase` dependency (spec AC4).
`RecipeAddToListUseCase` is used directly instead of `ProductSearchViewModel.onAddRecipeIngredients`,
which is a verbatim duplicate of it — one of the two implementations goes away, and the surviving one
is the use case, so IAA-01 authorship still flows through `ProductSaveUseCase` (spec AC3).

### What `recipe-list-origin` adds on top (separate spec, separate PR)

`Shopping.recipeId`/`recipeName` nullable fields (same pattern as `budget`), written when a list is
created through `RecipesRoute(shoppingId = null)`, surfaced on the list. **Not designed here** — it
needs its own spec per the persona plan; this design only guarantees it won't need a second entry
point. Note for whoever writes it: AD-009's `firestore.rules` pins `/shopping` create/update field
shapes, so a new field is a rules change too.

---

## Sequencing (hard constraint)

```
CHAT-01 (PR1→PR3)  →  PROD-04  →  PROD-05  →  recipe-list-origin
```

- **CHAT-01 before PROD-04**: CHAT-01 T6 removes the `Options.AI` inline branch from
  `ProductScreen.kt`; PROD-04 rewrites that file. Doing PROD-04 first means carrying an AI-chat
  rendering branch through the rewrite and then deleting it.
- **PROD-04 before PROD-05**: PROD-05 edits `ProductSearchForm`/`ProductSearchViewModel`, which
  PROD-04 rehosts.
- **All of it soon**: PROD-04 reopens `ProductScreen.kt`, `QuickAddForm.kt`, `ProductPhotoForm.kt`
  and `Options.kt` — the exact four files IAA-03 (`3fd980b8`, `3620be87`) just landed. Confirmed by
  `git log`: those are the most recent commits touching all four. The pm's recommendation to
  sequence this now is correct.

## Decisions taken by bruno (both previously open, both closed 2026-09-11)

- **OQ-3 — `SEARCH` and `SUGGESTIONS` stay in the unified sheet** alongside Quick Add and the
  camera. Design impact is in § "OQ-3 — all four content modes live in the sheet": no new feature
  work (they were already sheet-hosted modes), but R7 escalates to a per-mode invariant, three new
  risks appear (R10 IME carry-over, R11 transient per-mode state, R12 sheet-height stability), and
  one concrete regression becomes unavoidable — Quick Add must become a visible peer chip, or
  deleting the nested `NavHost` strands the user in Search with no way back. New task **T11b**;
  T9 extended.
- **OQ-4 — recipes live in a secondary menu, not a dedicated button.** PROD-05 adds a
  `RecipesRoute(shoppingId = shopping.id)` item to `CartScreen`'s **existing** `MoreVert` overflow
  menu; the `shoppingId = null` "start a list from a recipe" entry waits for `recipe-list-origin`,
  because `ShoppingScreen` has no overflow menu today and a menu whose destination can't yet create
  a list is a dead end. This **supersedes** the earlier draft placement on `ShoppingScreen`.
  T15/T17 updated.

## Effort note

PROD-04 was estimated **M**. After OQ-3 it is **M–L** — driven entirely by the risk-hardening
surface (R7 across four modes, plus R10–R12) and the Quick Add chip regression, not by new feature
work. PROD-05 is unchanged at **S–M**; OQ-4 made it slightly cheaper, since it reuses an existing
overflow menu instead of introducing an affordance.
