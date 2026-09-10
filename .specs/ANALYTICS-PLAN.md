# Analytics Plan — Beta Observability (Cestou / How-Much)

Status: Draft — instrumentation landed, ready for `tech-lead` review.
Owner: `data-engineer`
Last updated: 2026-09-09

Companion to `.specs/BETA-LAUNCH-PLAN.md` (Analytics foundation workstream). That document says
*what order* to do the work in; this document says *which events/funnels to actually look at* once
the beta is live, and *why* each one matters. It does not modify `BETA-LAUNCH-PLAN.md` — that file is
owned by `tech-lead`.

## Constraint that shapes everything below

Firebase project is on the **Spark (free) plan**: no Cloud Functions, no automatic BigQuery export, no
custom backend pipeline. This is not a blocker for analytics itself — **Firebase Analytics (the GA4
SDK bundled in the app) is free regardless of Spark/Blaze**; only Cloud Functions and the BigQuery
export/streaming feature require Blaze. So every event described here is already flowing into the
standard Firebase console at zero cost. What we *don't* get without Blaze:

- No server-side processing of events (no Cloud Function triggered by an event).
- No raw per-event BigQuery table to run custom SQL against.
- No custom internal dashboard — we use the Firebase console's own UI.

This plan is scoped to what the built-in console gives us: **DebugView**, the **Events** report, and
the **Explorations** module (funnel/retention/path exploration — this is a standard GA4-linked feature,
not a paid add-on). Plus **Crashlytics** for stability, which is separate from Analytics but answers
"is the beta usable at all" alongside "are people using it".

## Event inventory

### Already existed (`AnalyticsEvents.kt`, before this change)

| Event | Fired from | Params |
|---|---|---|
| `app_open` | `MainViewModel` init | — |
| `shopping_list_created` | `ShoppingListViewModel` | `shopping_id` |
| `shopping_list_deleted` | `ShoppingListViewModel` | `shopping_id` |
| `shopping_list_shared` | `ShoppingListViewModel` (share sheet action) | `shopping_id` |
| `cart_product_deleted` | `CartViewModel` | `shopping_id`, `product_id` |
| `cart_finish_purchase_started` | `CartViewModel` (opens the finish-purchase sheet) | `shopping_id` |
| `product_search_performed` | `ProductSearchViewModel` | `search_mode`, `query_length`, `result_count` |
| `product_selected` | `ProductSearchViewModel` (pick from search results) | `shopping_id`, `product_id` |
| `ai_chat_message_sent` | `AiChatViewModel` | — |
| `settings_language_changed` / `settings_currency_changed` | `SettingsViewModel` | `language` / `currency` |
| `auth_sign_in_failed` | `WelcomeViewModel` | `reason` |
| `profile_sign_out` | `ProfileViewModel` | — |

### Added by this change (gaps found in the audit below)

| Event | Fired from | Params | Gap it closes |
|---|---|---|---|
| `shopping_list_joined` | `ScannerViewModel`, `JoinListViewModel` | `join_method` (`qr_scan` / `manual_token` / `deep_link`) | Joining a shared list had **zero** instrumentation — `ShoppingJoinUseCase` was called from two ViewModels and neither tracked success. |
| `shopping_list_join_failed` | same | `join_method`, `reason` | Same gap, failure side — needed to see how often tokens/QRs fail to resolve. |
| `shopping_budget_set` | `EditShoppingViewModel.update()` | `shopping_id`, `has_budget` (bool) | "Definir limite de gastos" (PM checklist item) had no event at all. Fires only when the budget value actually changes on save, not on every edit. |
| `cart_finish_purchase_completed` | `FinishPurchaseViewModel.onFinishPurchase()` | `shopping_id`, `amount` | Only the *start* of finishing a purchase was tracked (`cart_finish_purchase_started`); the actual completion — the beta's core "did they buy" signal — was missing. |
| `product_added` | `ProductSearchViewModel` (recipe ingredients), `CommonProductViewModel` (favorite/common items), `QuickAddViewModel` (free-text quick add), `ProductPhotoViewModel` (AI photo scan confirm) | `shopping_id`, `source` (`recipe` / `common_product` / `quick_add` / `photo_scan`), optional `items_count` for batch adds, optional `product_id` | Search-driven adds were the only add path tracked (`product_selected`). Three other add entry points (recipe, common-product picker, quick-add bar) plus the AI photo-scan confirm step had no event, so "how do people actually add products" was invisible. One event name + a `source` param instead of four new event names, so the whole "how are items added" funnel is a single breakdown in the console. |
| `product_photo_scan_performed` | `ProductPhotoViewModel.analyzeImage()` (success) | `shopping_id`, `result_count` | The AI photo-scan feature (closest equivalent to "escanear código de barras" in the current codebase — see note below) had no visibility into whether the AI actually finds products in the photo. |
| `product_photo_scan_failed` | same (failure) | `reason` | Same gap, failure side. |

New `screen_view` calls added alongside the above so the console's default screen funnel also covers
these flows: `shopping_join_scanner`, `shopping_join`, `shopping_edit`, `cart_finish_purchase`,
`product_photo_scan`, `common_products`.

### Note on "escanear código de barras"

**Correction (tech-lead review):** the original version of this section claimed `BarcodeAnalyzer` was
dead code. That was wrong — it is live and is the actual QR-join scanner, and it is also G13. The
codebase has two different "scan" concepts:

1. `BarcodeAnalyzer` (ML Kit) is declared `internal` under
   `feature/products/.../presentation/components/scanner/`, but it is consumed cross-module: 
   `feature/shopping`'s `QrCodeScanner.kt:53` wires `onBarcodeScanned = onTokenScanned`, which feeds
   `ScannerViewModel` — this is the QR-code-to-join-a-shared-list flow, and it fires per camera frame
   with no debounce (G13, the scanner spam bug already tracked in the roadmap and now instrumented via
   `shopping_list_joined`/`shopping_list_join_failed` above). This cross-module `internal` access is
   also a live instance of the G10 module-coupling issue (AD-005) — flagged for `tech-lead`, not this
   doc's problem to fix.
2. `ProductPhotoViewModel` (`ProductPhotoForm` + `CameraCaptureView`) is the "point the camera at
   products/a price tag and let AI add them" flow used in the product picker — a materially different
   capability (AI vision vs. SKU lookup) from the QR-join scanner above. `product_photo_scan_performed`
   / `_failed` / `product_added(source=photo_scan)` covers this one.

Both flows now have events: the QR-join scanner via `shopping_list_joined` (`join_method=qr_scan`) and
the photo-scan flow via the events in the table above. No new event is needed for G13 itself — once
`android-engineer-features` adds the debounce, the existing `shopping_list_join_failed` volume should
simply drop.

## Funnels to watch during the beta, and the decision each one informs

1. **First-list activation**: `app_open` → `shopping_list` screen_view → `shopping_list_created` →
   `product_added` / `product_selected` → `cart_finish_purchase_started` →
   `cart_finish_purchase_completed`.
   Answers: are new beta testers completing the core loop (create → fill → finish) at all, or dropping
   off partway? This is *the* go/no-go signal for whether the app is usable end to end.

2. **Purchase completion rate**: ratio of `cart_finish_purchase_completed` to
   `cart_finish_purchase_started`, broken down over time.
   Answers: is the finish-purchase sheet itself a drop-off point (confusing UI, validation issue)?
   `amount` on the completed event also gives a rough sense of real basket sizes beta testers are
   entering, useful for sanity-checking the feature is used for real shopping, not just poking around.

3. **Shared-list collaboration**: `shopping_list_shared` → `shopping_list_joined` /
   `shopping_list_join_failed`, broken down by `join_method`.
   Answers: is the multi-person use case (the app's actual differentiator) working? If
   `join_method=qr_scan` fails disproportionately vs. `manual_token`, that's a camera/QR-generation bug,
   not a backend one — directs where `android-engineer-features` should look next.

4. **How products actually get added**: `product_selected` (search) vs. `product_added` broken down by
   `source` (`recipe`, `common_product`, `quick_add`, `photo_scan`).
   Answers: which of the five entry points to "add a product" beta testers actually use. If
   `photo_scan` or `recipe` never fire, that's a strong signal those features aren't discoverable or
   aren't worth the maintenance cost post-beta — a product prioritization input for `pm`, not just an
   engineering one.

5. **AI photo-scan reliability**: `product_photo_scan_performed` (`result_count` distribution,
   especially `result_count = 0`) vs. `product_photo_scan_failed` (`reason`).
   Answers: is the AI vision feature actually reliable enough to keep front-and-center in the picker
   UI, or does it need a fallback/manual-entry nudge. This is the one feature in the app calling an
   external AI API on every use — worth watching for failure-rate spikes tied to `AiAgentFactoryImpl`
   changes (see G15 workstream) or Gemini quota issues.

6. **Spending-limit adoption**: proportion of `shopping_budget_set` events with `has_budget = true`
   relative to total lists created (`shopping_list_created`).
   Answers: is the budget/spending-limit feature used at all? If adoption is near zero, `pm` may
   deprioritize budget-related polish for the post-beta roadmap.

7. **Sign-in friction**: `auth_sign_in_failed` volume and `reason` distribution on the `welcome`
   screen.
   Answers: whether Google Sign-In issues (still unverified on a real device — see F2.2 in
   `BETA-LAUNCH-PLAN.md`) are actually blocking beta testers from getting in at all. If this event is
   noisy, F2.2 becomes urgent, not just "nice to verify".

8. **Stability, via Crashlytics (not Analytics)**: crash-free users %, and specifically watch for
   exceptions tagged `AnalyticsException` (a crash *in* analytics itself would mean `trackEvent` calls
   are silently failing and everything above is undercounting — the try/catch in
   `FirebaseAnalyticsTracker` reports these to Crashlytics instead of crashing, so they're visible
   there, not in Analytics).

## How the team actually watches this, given Spark

- **During internal QA / dogfooding (bruno's own device, before/alongside the beta)**: enable
  `adb shell setprop debug.firebase.analytics.app br.com.brunocarvalhs.howmuch`, then use
  **Firebase console → Analytics → DebugView** to confirm, in real time, that a given flow (e.g. join a
  shared list via QR) actually fires the expected event with the expected params. This is how new
  events from this change should be verified once a device is available — see "What's still unverified"
  below. DebugView is *not* how we watch the external beta cohort — we have no adb access to testers'
  phones.
- **During the actual beta (external testers)**: **Firebase console → Analytics → Events** for raw
  counts, and **Analytics → Explore → Funnel exploration** (free, GA4-linked, no Blaze needed) built
  from the funnels listed above. Mark the highest-signal events as **Conversions** in the console
  (`shopping_list_created`, `cart_finish_purchase_completed`, `shopping_list_joined`) so they surface on
  the default Analytics overview without building a custom report every time. Expect standard 24–48h
  latency for the non-DebugView dashboard — this is a daily/weekly check, not a live monitor.
  Setting `AnalyticsTracker.setUserId` is **not currently wired anywhere in the app** — Firebase still
  gives per-installation pseudonymous cohort/retention reporting without it, so this isn't a blocker for
  the funnels above, but it does mean we can't stitch a tester's activity across a device reinstall
  or manual/mobile-pairing device switch. Flagging as a future improvement, not doing it in this change
  (out of scope — no flow currently calls it and adding one is a UX/privacy decision, not just plumbing).
- **Stability**: **Firebase console → Crashlytics** dashboard, daily during the beta window.

## What's still unverified (needs a real device — cannot be done from this environment)

- None of the new events have been confirmed in DebugView yet — the instrumentation is code-complete
  and unit-tested (mocked `AnalyticsTracker`, asserting the right event/params fire), but nobody has
  watched a real `shopping_list_joined` or `product_photo_scan_performed` show up live. Whoever picks up
  F0.3/F2.2 (Maestro + real-device pass) should walk each funnel above once and cross-check DebugView,
  the same way those items already flag Maestro/Sign-In as "authored, not verified".
- Firebase console **Conversion** toggles for the three events above need to be set manually in the
  console by whoever has project access — not something committed in this repo.
