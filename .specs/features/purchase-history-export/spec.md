# Feature Specification: Monthly Spend History (free) + Purchase-History Export (Pro)

## Status
Draft, PM-authored, 2026-10-26. Planning deliverable for row 1 of `.specs/MVP-ROADMAP.md`
"Monetization feature roadmap — competitive gap action plan" (origin: pro-entitlement spec FR-05
#1). Extended the same day with bruno's request for a monthly spend-history screen in a "month
cards + trend line + transactions list" style (reference: Magalu Cashback / Nubank-style card
statement screen, described below). **Needs a `design.md` + `tasks.md` pass before
implementation** (see "Readiness" at the end).

Supersedes `PERSONA-ACTION-PLAN.md` backlog item #5 (`monthly-spend-report`, "Relatório mensal
de gastos + exportação"). That item was sequenced after #4 (`real-price-history`) so it could reuse
#4's cross-list aggregation. #4 has not been built. This spec builds a smaller per-list
aggregation of its own, and #4 can reuse it later instead of the other way around.

## Problem Statement

The app already records everything Anderson and Rafael need. Every finished list (`Shopping`,
`status = FINISH`) has a user-confirmed paid total (`Shopping.price`, entered in
`FinishPurchaseContent`), an establishment (written into `Shopping.description` by
`FinishPurchaseViewModel.onFinishPurchase`) and an optional budget (`Shopping.budget`). Every item
(`shopping/{id}/products`, read through `core/domain`'s `ProductReader.getAllProducts`) has a name,
category, quantity, unit price, barcode and a `history: List<ProductActivity>` with `PURCHASED`
entries (IAA-01).

None of it can be **seen in aggregate** or **taken out of the app**:

1. **No spend-over-time view exists.** The only place "history" is visible today is the home
   list (`ShoppingScreen`). Finished lists sit mixed in with active ones, and each row shows the
   list's *budget* (`ShoppingItem.kt:112-119`), not what was actually paid. A
   `groupedList`-by-month map is computed in `ShoppingListViewModel.applyFilters()` (line 236),
   but nothing renders it, and it groups by `createdAt`, not by purchase date. The only way to see
   a past purchase is to open each finished list one by one (`CartScreen` in its read-only
   `isLocked` mode). Nobody can answer "how much did I spend this month vs. last month?" without
   opening every list and adding the totals up by hand.
2. **No way to get the data out.** Anderson (MEI, pizzeria) re-keys every paper receipt into
   Excel at month-end. Rafael (reseller) needs exact unit cost per product to set resale prices.
   Both already have this data in Cestou and still re-type it.

## Scope boundary — how this spec respects it (load-bearing)

The pro-entitlement spec's confirmed boundary says running total, budget alerts, **purchase
history** and price comparison stay **free** for every persona. This spec draws the line like
this:

| Capability | Tier | Why |
|---|---|---|
| Viewing the monthly spend history screen: month cards, trend line, current-month total, list of finished purchases, opening any past purchase | **Free** | This *is* "purchase history". It's new UI, but putting it behind Pro would be paywalling the boundary itself. |
| Generating a structured file (CSV) of a month's purchases and items and handing it to the Android share sheet | **Pro** | A new convenience layered on top of free data, which replaces Anderson's manual re-keying into Excel. Nothing that's free today, or free in this spec, becomes Pro. |

No existing free behavior changes. Opening a finished list stays free. The new free screen ships
to everyone, including Free users with `BILLING_ENABLED=true`. The Pro gate sits on **one action
only**: the export/share icon.

## Goals

- [ ] Give every user one screen that answers "how much did I spend this month, and how does it
      compare to the last few months?", built from data the app already has.
- [ ] Let a Pro user export one month of purchases as spreadsheet-ready files in 2 taps from
      that screen, with no re-typing and no rounding or merging of item detail.
- [ ] Fix the two data-integrity gaps that make any history (free or exported) inaccurate today:
      no finish timestamp, and an unpriced quick-finish path (see "Data-model prerequisites").
- [ ] Reuse the existing entitlement contract (`SubscriptionRepository.observeStatus()` through
      the existing gate ViewModel). No new gating mechanism.

## Out of Scope

| Feature | Reason |
|---|---|
| PDF, XLSX or formatted-report export | XLSX needs Apache POI (multi-MB, method-count heavy). PDF via `android.graphics.pdf.PdfDocument` is buildable but means hand-laying out a document, and neither persona asked for a document. Both want rows in a spreadsheet. Revisit PDF only if "send to my accountant" shows up as a real request. |
| Server-side generation, email delivery, scheduled "month closed" reports | Spark plan has no Cloud Functions (`MVP-ROADMAP.md` header note). Everything is generated on-device and handed to the share sheet. |
| Filters on the purchases list (by establishment, by member, by category) | The reference UI has a "Filtros" affordance, but v1's list is one month at a time, typically a handful of entries. Add filters only if months with many purchases (Anderson-scale) show it's needed. |
| Per-category / per-product charts, consumption insights | Belongs to Juliana's `pantry-capture` insight track and to `real-price-history` (#4). This spec does month totals only. |
| A monthly budget (one budget across all lists in a month) | No such entity exists. `budget` is per list. Budget-vs-actual is reported per list and summed across lists that had one (see P1 AC7). A real monthly budget is its own feature. |
| Exporting a single list from inside `CartScreen` | One export entry point in v1 (the history screen, scoped to a month). A finished list's items are already in that month's items file. |
| Help/"?" icon from the reference UI; sticky bottom CTA ("Antecipar pagamento") | No equivalent need in this domain. |
| Compose visual design of the cards and trend line | Separate pass by `product-designer` after this spec settles data and screen boundaries. This spec specifies behavior, not pixels. |
| Backfilling `finishedAt` on existing documents | Not needed. Legacy lists get a derived date at read time (see Assumptions). |

---

## Data-model prerequisites (found while grounding this spec in code)

These are real gaps in the current code. They affect the **free** history too, not just export,
which is why they're part of this spec rather than deferred. Both are also logged in
`MVP-ROADMAP.md` (G21) for the tech lead.

1. **`Shopping` has no finish timestamp.** The model has only `createdAt`
   (`core/domain/model/Shopping.kt`). A list created on Oct 30 and finished on Nov 2 would land in
   October, and a list duplicated from an old one keeps nothing that says when it was bought.
   Month bucketing by `createdAt` would be wrong exactly at month-end, which is when Anderson
   closes his books.
2. **Two finish paths, and only one records a paid total.**
   - `FinishPurchaseViewModel.onFinishPurchase` (cart → "Finalizar compra" sheet) sets
     `price = <user-confirmed total>`, `description = <establishment>` and `status = FINISH`, and
     fires `cart_finish_purchase_completed`.
   - `ShoppingScreen`'s long-press "Finalizar" (`ShoppingScreen.kt:256-263` →
     `ShoppingListViewModel.onUpdate`) only sets `status = FINISH`. `Shopping.price` stays at the
     `0.0` it was created with (`ShoppingCreateUseCase.kt:50`), and no finish analytics fire.
   - Side effect worth naming: the first path **overwrites the user's list description** with the
     establishment text. This spec doesn't change that, but it does mean "establishment" in the
     export is really "description at finish time".

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
|---|---|---|---|
| **Data grain** | **One "purchase" = one finished `Shopping` list.** Month totals are sums of purchases. Item-level data (`Product`) appears only in the export's items file and when opening a purchase. | Matches how users think and how the data is shaped: a finished list *is* a receipt (one trip, one confirmed total, one establishment). Item-level `PURCHASED` timestamps exist, but `Product.price` is very often `0.0` (every AI/suggestion/common-product add path writes `price = 0.0`), so summing items as the primary number would understate spend. The confirmed list total is the most trustworthy number in the system. | [ ] |
| Purchase date | New field `Shopping.finishedAt: Long? = null`, set to `now` by **both** finish paths and cleared to `null` by `ShoppingReopenUseCase`. Read-time fallback for legacy lists: latest `PURCHASED` timestamp among the list's products, else `createdAt`. | Fixes prerequisite 1 without a migration. The fallback is good enough for existing data, and every list finished after release has an exact date. | [ ] |
| Purchase total ("total pago") | `Shopping.price` when `> 0`. Otherwise the sum of `Product.total` over items with `isPurchased == true` (marked "calculado" in the export). | Remains the read-time fallback for lists finished before this ships. Going forward, every finish path (cart and the home long-press) now records a real confirmed total via AD-014 §2's shared finish flow, so the "calculado" fallback should only ever apply to legacy data. | [x] |
| Month boundaries | Calendar month in the device's current time zone, bucketed by the resolved purchase date | Same as how `toMonthYearString()` already buckets dates. | [ ] |
| Months shown | Current month + 5 previous (6 cards). 4 visible at once, horizontally scrollable, opens scrolled to the current month at the right edge. | The reference shows 4. 6 lets Anderson recover if he forgot to close a month or two, without loading an unbounded history. Older finished lists stay reachable on the home list as today. | [ ] |
| Current month ("fatura aberta" equivalent) | The current-month card shows the **month-to-date total of finished lists only**, visually distinct (outlined, open marker, "parcial" label). In-progress lists **do not** add to that number. A separate detail row shows "Em andamento: R$X em N listas abertas" (sum of purchased-item totals on non-finished lists) when N > 0. | Keeps the card number honest ("money already spent") and avoids counting a list twice when it later finishes with a different confirmed total. Still answers bruno's "running total for the in-progress month" in its own row. | [x] — bruno confirmed the default (finished lists only) |
| Shared lists | Count fully, for every member, in their own history. Each purchase shows a "compartilhada" indicator. | The member really did take part in that purchase, and splitting costs is the `Lucas` problem, a separate feature. Edge case documented below. | [ ] |
| Entry point | New icon action in `ShoppingScreen`'s top app bar (next to the existing QR-join action), labeled "Histórico de gastos". Opens a new full-screen route in `feature/shopping`. | Home is where lists live. One icon, no new tab, no extra step for anyone who ignores it (Dona Célia). | [ ] |
| Export format | **CSV, two files per export**, shared together via `Intent.ACTION_SEND_MULTIPLE`: `cestou-AAAA-MM-compras.csv` (one row per purchase) and `cestou-AAAA-MM-itens.csv` (one row per item). Plus a short plain-text summary in `EXTRA_TEXT`. | Cheapest real option client-only: string building + a `FileProvider` + share intent, no new library. Two files instead of one because a single item-level file that repeats the list total on every row breaks Anderson's naive `SOMA()`. Each file is "tidy" for its own job. | [ ] |
| CSV dialect | UTF-8 **with BOM**, ISO dates (`AAAA-MM-DD`). Field separator and decimal mark follow the app language: pt-BR/es → `;` and `,`; en → `,` and `.`. RFC-4180 quoting. Numbers raw (no "R$", no thousands separator). Money rounded to 2 decimals, quantity up to 3 decimals with no trailing zeros. | Excel pt-BR on Windows (Anderson) splits on `;` and expects decimal comma. Without the BOM, accents (Laticínios, Feijão) break. ISO dates are understood by every Excel/Sheets locale. Raw numbers keep Rafael's values exact. | [ ] |
| Unpriced items | Stored `price` `null` or `0.0` → `preco_unitario` and `total_item` exported **empty**, not `0`, and counted in the summary as "itens sem preço: N". | Rafael rejects anything that hides detail. A silent `0,00` would look like a real (free) cost and corrupt his margin math. | [ ] |
| File storage | Written to `cacheDir/exports/` (overwritten per export), exposed through a new `androidx.core.content.FileProvider` (none exists in the repo today) with `FLAG_GRANT_READ_URI_PERMISSION` | No storage permission needed. Cache can be reclaimed by the OS. Nothing persists beyond the share. | [ ] |
| Pro gate mechanism | Reuse **`ShoppingSubscriptionGateViewModel.isPro`** (same module, already `subscriptionRepository.observeStatus().map { it == PRO }`, only requested from the mobile `shoppingGraph`). Same call-site pattern as the QR gate: `if (isPro) intent.onExport() else navigator.navigate(Paywall(source = PAYWALL_SOURCE_HISTORY_EXPORT))`. No new gate class. | AD-010: the only allowed entitlement question is `observeStatus()`. Reusing the existing gate VM keeps `:wear` out of `core:billing` for the same reason as the existing KDoc. | [ ] |
| Paywall copy | Add a benefit line "Exporte seu histórico de compras para planilha" (en/es equivalents) to `PaywallScreen.kt`'s benefit list in the same PR that adds the gate | The Paywall must not sell something it doesn't gate (FR-04/G19 lesson), and the reverse also holds: a gate the Paywall never mentions is a surprise wall. | [ ] |
| Module placement | Screen, ViewModel and summary use case in `feature/shopping`. The CSV builder is a pure Kotlin class (no Android types) so it's JVM-unit-testable. The file write + `FileProvider` + `Intent` live behind a small port, the same way `ShareShoppingUseCase` keeps `Intent` out of `core/domain` (AD-011). Exact split is for `design.md`. | `feature/shopping` already owns the home list and has `ShoppingRepository` + `ProductReader`. No new cross-feature coupling (G10). | [ ] |

**Open questions for bruno — both resolved:**
1. ~~Current-month card: finished lists only (default) vs. include in-progress purchased items.~~
   **Resolved — finished lists only (the default).**
2. ~~Long-press "Finalizar" on the home list: should it open the same confirm-total sheet as the
   cart, so every finish records a paid total?~~ **Resolved — yes.** Implemented via AD-014 §2's
   shared `FinishPurchase(shoppingId, source)` route, not a `feature/shopping`-internal reimplementation.

---

## Persona check (customer-personas skill)

| Persona | Verdict | Note |
|---|---|---|
| **Anderson** (primary) | **Serve** | Month total ready without re-adding receipts (free). The compras CSV opens in his Excel pt-BR with correct columns and `SOMA()` works (Pro). Catches only what went through the app, so paper-only receipts still need manual entry. That's an honest limitation, not a regression. |
| **Rafael** (primary) | **Serve** | Items CSV keeps every product with exact quantity, unit price, barcode and date, no merging or rounding beyond cents, and empty (not zero) for unpriced items. |
| Marina, Dona Marlene, Eduardo | Serve (free part) | "Spent less than last month?" on one screen, at no cost. The trend is free, which is exactly why it doesn't break the boundary. |
| Dona Célia | Neutral | One extra icon on home, ignorable. The screen itself has no jargon ("Histórico de gastos", "Compras do mês"). |
| Lucas, Bianca-e-Diego | Neutral / slight risk | Shared purchases show the full list total for each member (not their share), which can read as "I spent R$400" when they paid half. Mitigated by the "compartilhada" indicator. Splitting is out of scope. |
| Yasmin, Camila-e-Pedro, Juliana, Rodrigo | Neutral | No friction added. Juliana's "what do I consume most" is explicitly out of scope. |

No persona is made worse off. Nothing free moves behind Pro.

---

## User Stories

### P1: Monthly Spend History View (free) ⭐ MVP

**User Story**: As any user (primarily Anderson, also Marina/Dona Marlene), I want to see how much
I spent each month and which purchases make up the total, so I can track my spending without
adding receipts up by hand.

**Why P1**: This is the free half of the boundary. It must exist before the export, because the
export is an action *on* this screen. It also has an Independent Test any persona can observe on
its own.

**Reference layout (bruno, 2026-10-26)**: a credit-card statement screen. Top bar (back, title,
share icon top-right). A horizontal row of month cards with a connecting trend line: past months
show a filled green dot and their total, the current month is outlined in a distinct color with an
open marker. Below that, a header with the selected period's total plus 1–2 detail rows. Then a
"Lançamentos" list grouped by date.

**Acceptance Criteria**:

1. WHEN the user taps the "Histórico de gastos" action in `ShoppingScreen`'s top app bar, THEN a
   new full-screen route SHALL open, with a top app bar holding back navigation, the title, and
   an export/share icon (P2).
2. WHERE the screen is shown, THEN it SHALL display a horizontally scrollable row of **6 month
   cards** (current month + 5 previous, oldest on the left), initially scrolled so the current
   month is visible at the right edge. Each card SHALL show the month label (e.g. "out") and that
   month's total (sum of resolved purchase totals of `FINISH` lists whose resolved purchase date
   falls in that calendar month), formatted with the user's currency (`LocalCurrency`).
3. WHERE the month cards are shown, THEN a trend line SHALL connect the month totals in order.
   The current month SHALL be visually distinct (different marker and emphasis, plus a "parcial"
   label) because its total is month-to-date. Months with no purchases SHALL show `R$ 0,00` and
   still sit on the line (the line dips to zero, not a gap). Exact visuals are `product-designer`'s
   call.
4. WHEN the user taps a month card, THEN that month SHALL become selected and the header and
   purchases list below SHALL switch to it. The screen SHALL open with the current month selected.
5. WHERE a month is selected, THEN a header SHALL show the month name, its total in bold, and
   these detail rows: "N compras" (count of finished lists in the month). Only when at least one
   of those lists had a budget, also "Orçamento: R$X · Gasto: R$Y" (sums over **only** the lists
   that had a `budget`). Only for the current month and only when any non-`FINISH` list has
   purchased items, also "Em andamento: R$Z em N listas abertas" (see Assumptions, not added to
   the card's total).
6. WHERE a month is selected, THEN a "Compras" list SHALL show its finished lists grouped by
   resolved purchase date (date header, newest first). Each row SHALL show the list emoji, title,
   establishment (`description`, if non-blank), resolved total, and a "compartilhada" indicator
   when `users.size > 1`.
7. IF a purchase's total was **calculated** (`Shopping.price <= 0`, fallback to the sum of
   purchased items), THEN the row SHALL mark it as an estimate (e.g. "≈" or "estimado"), so a
   calculated number never looks like a confirmed one.
8. WHEN the user taps a purchase row, THEN the existing read-only cart for that list SHALL open
   (`CartFlow(shopping)`, `isLocked` mode). No new detail screen.
9. IF the selected month has no finished lists, THEN the list area SHALL show an empty state
   ("Nenhuma compra finalizada em <mês>") with no error styling.
10. WHERE the data is loading, THEN cards and list SHALL show a loading placeholder. IF loading
    fails (offline with no cache), THEN an inline error with retry SHALL be shown, never a blank
    or zeroed chart that could be mistaken for "you spent nothing".
11. WHERE the user is FREE or PRO, THEN every item in AC1–AC10 SHALL behave identically. Nothing
    on this screen except the P2 export action reads `SubscriptionStatus`.
12. WHEN a list is finished through **either** finish path, THEN `Shopping.finishedAt` SHALL be
    set to the current time. WHEN a list is reopened (`ShoppingReopenUseCase`), THEN `finishedAt`
    SHALL be cleared, so a later re-finish re-buckets it correctly.

**Independent Test**: Account with three finished lists: Sep 10 confirmed R$ 120,00; Oct 3
confirmed R$ 80,00; Oct 28 finished by long-press with two purchased items of R$ 10,00 and
R$ 5,50. Opening "Histórico de gastos" shows Sep = 120,00, Oct = 95,50 (parcial, with the Oct 28
purchase marked estimated), other months 0,00. Tapping Sep switches the header and list to one
purchase. Tapping that purchase opens the locked cart. A Free account sees exactly the same thing.

---

### P2: Month Purchase-History Export (Pro)

**User Story**: As Anderson / Rafael, I want to export a month of purchases into a spreadsheet so I
stop re-typing receipts and can see exact cost per item.

**Why P2**: The Pro convenience layered on P1. It depends on P1's screen and on the same
resolved purchase dates and totals, so it can't ship first.

**Acceptance Criteria**:

1. WHERE the history screen is shown, THEN the top-app-bar export/share icon SHALL be visible to
   **all** users (Free users see it with a small Pro badge), so the feature is discoverable.
2. WHEN a **Pro** user taps it (gate: `ShoppingSubscriptionGateViewModel.isPro`, which is
   `SubscriptionRepository.observeStatus()` mapped to `== PRO`), THEN the app SHALL generate the
   two CSV files for the **selected month** and open the system share sheet
   (`Intent.createChooser` over `ACTION_SEND_MULTIPLE`, MIME `text/csv`, `content://` URIs from
   `FileProvider`, read-permission granted).
3. WHEN a **Free** user taps it, THEN no file SHALL be generated and the app SHALL navigate to
   `Paywall(source = "shopping_history_export")`. This mirrors the existing QR gate in
   `ShoppingGraph.kt:115-121` exactly. Viewing the screen (P1) SHALL NOT be affected in any way.
4. The **compras** file SHALL have one row per finished list in the month, sorted by date
   ascending, with columns: `data` (resolved purchase date), `lista` (title), `estabelecimento`
   (description), `total_pago`, `origem_total` (`confirmado` | `calculado`), `soma_itens` (sum of
   purchased items' `total`, priced items only), `qtd_itens`, `itens_sem_preco`, `orcamento`
   (empty if none), `diferenca_orcamento` (`orcamento − total_pago`, empty if no budget),
   `compartilhada` (`sim`/`nao`), `moeda` (ISO code from settings).
5. The **itens** file SHALL have one row per product of those lists (no grouping or merging of
   same-named items), with columns: `data`, `lista`, `estabelecimento`, `produto`, `categoria`,
   `quantidade`, `preco_unitario`, `total_item`, `comprado` (`sim`/`nao` from `isPurchased`),
   `comprado_em` (date of last `PURCHASED` entry, empty if none), `codigo_barras` (empty if none),
   `moeda`. Column headers SHALL be localized to the app language, with the same column order in
   all languages.
6. IF a product's `price` is `null` or `0.0`, THEN `preco_unitario` and `total_item` SHALL be
   empty (not `0`).
7. The CSV dialect SHALL follow the Assumptions table (UTF-8 BOM, ISO dates, `;`/`,` for pt-BR
   and es, `,`/`.` for en, RFC-4180 quoting, raw 2-decimal money, quantity up to 3 decimals with
   no trailing zeros). A product name containing the separator, quotes or a line break SHALL
   round-trip intact in Excel/Sheets.
8. The share intent's `EXTRA_TEXT` SHALL hold a short localized summary: month, number of
   purchases, month total, budget-vs-spent line (only if any list had a budget), and "itens sem
   preço: N" (only if N > 0). `EXTRA_SUBJECT` SHALL be "Cestou — compras de <mês/ano>".
9. IF the selected month has no finished lists, THEN the export icon SHALL be disabled for Pro
   users (nothing to export). For Free users it still routes to the Paywall, so the gate is
   observable even on an empty account.
10. WHERE export is generating, THEN a progress indicator SHALL show and repeat taps SHALL be
    ignored. IF generation fails (product read failure, I/O error), THEN a snackbar error SHALL
    show and no partial file SHALL be shared.
11. The use case producing the export SHALL receive the already-resolved purchases for the month
    (the same data P1 rendered), so the exported `total_pago` always equals what the screen
    showed for that purchase. No second, divergent calculation.
12. WHEN the export ships, THEN `PaywallScreen.kt`'s benefit list SHALL include the export
    benefit (pt-BR/en/es strings).

**Independent Test**: Pro account, October selected from the P1 test data. Tap export, pick
Google Drive or Gmail. Two files arrive. In Excel pt-BR, `cestou-2026-10-compras.csv` opens as 2
rows with `total_pago` 80,00 (confirmado) and 15,50 (calculado), and `SOMA` = 95,50, matching the
card. `itens.csv` lists every product, with an item that had no price showing empty cells.
Switching the same account to Free and tapping export opens the Paywall, and the screen above it
is unchanged.

---

### P3: Analytics for the history + export funnel

**User Story**: As the PM, I want to measure whether people open the history screen and whether
the export gate converts, so that Pro decisions are based on data. (Per pro-entitlement Rollout
step 3, every new gate needs a gate-blocked event.)

**Acceptance Criteria** (event names proposed; `data-engineer` owns final naming in
`AnalyticsEvents.kt`, snake_case like `cart_finish_purchase_completed`):

1. WHEN the history screen opens, THEN `trackScreenView("spend_history", ...)` SHALL fire.
2. WHEN a month card is tapped, THEN `spend_history_month_selected` SHALL fire (param:
   `months_ago`).
3. WHEN the export icon is tapped, THEN `spend_history_export_tapped` SHALL fire (param:
   `is_pro`). For Free users this is the gate-blocked event.
4. WHEN export completes (share sheet launched), THEN `spend_history_export_completed` SHALL fire
   (params: `purchase_count`, `item_count`, `unpriced_item_count`). WHEN it fails,
   `spend_history_export_failed` SHALL fire.
5. WHEN a list is finished via the long-press path, THEN the same `cart_finish_purchase_completed`
   event SHALL fire (param `source = shopping_list_long_press`), closing the blind spot in
   prerequisite 2.

---

## Edge Cases

- IF a list is finished, reopened and finished again in a different month, THEN it SHALL appear
  only in the month of its latest finish (`finishedAt` is overwritten, never accumulated).
- IF a legacy finished list has no `finishedAt` and no product with a `PURCHASED` entry, THEN
  its `createdAt` SHALL be used. Users won't see the difference, but the implementation SHALL
  NOT crash or drop the list.
- IF a purchase has `Shopping.price <= 0` **and** no priced purchased items, THEN its total is
  `0,00`, marked estimated in P1 and `calculado` in the export. It still counts in the purchase
  count.
- IF the user changes device time zone, THEN bucketing follows the current zone at render time.
  A purchase near midnight may move between adjacent months. Accepted, not worth storing zones.
- IF the user leaves a shared list (no longer in `users`), THEN its purchases disappear from their
  history, because `ShoppingRepository.getAll()` queries by membership. Accepted: history covers
  lists you belong to, consistent with the rest of the app.
- IF a shared list was finished by another member, THEN it appears in every member's history with
  the same total (see Persona check, Lucas).
- IF the currency setting is changed, THEN amounts are re-labeled, not converted (prices are
  stored as plain numbers). Same behavior as the rest of the app, and the export carries a `moeda`
  column so the file is self-describing.
- IF `BILLING_ENABLED` is OFF (current production state), THEN `observeStatus()` reports everyone
  as PRO and everyone can export. Consistent with FR-01–03, no special-casing.
- IF a month has many lists (Anderson can do dozens), THEN products are read per list
  (`ProductReader.getAllProducts`, one subcollection read each) only when needed: for the
  legacy-date or calculated-total fallback in P1, and for the export in P2. Lists with
  `finishedAt` and `price > 0` need no product reads to render P1. Tech lead previously confirmed
  per-user cross-list aggregation is cheap on Spark (`PERSONA-ACTION-PLAN.md`, reconciliation
  table).

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
|---|---|---|---|
| PHE-01 | P1: Monthly spend history view (free), incl. `finishedAt` + resolved-total prerequisites | Specify | Pending — needs `design.md` |
| PHE-02 | P2: Month purchase-history export (Pro, CSV via share sheet) | Specify | Pending — needs `design.md` |
| PHE-03 | P3: Analytics for history + export funnel | Specify | Pending — event names to confirm with `data-engineer` |

**ID format:** `PHE-[NUMBER]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 3 total, 0 tasks mapped yet (`tasks.md` not written).

---

## Success Criteria

- [ ] Every list finished after release (via either path) has a non-null `finishedAt`.
      Reopening clears it.
- [ ] The history screen's month total for any month equals the sum of the per-purchase totals
      listed under it, and equals `SOMA(total_pago)` of that month's exported compras CSV.
- [ ] A Free user (with `BILLING_ENABLED=true`) can open the history screen, switch months and
      open any past purchase with no Paywall. Only the export icon routes to the Paywall.
- [ ] A Pro user gets both CSVs into Gmail/Drive in ≤2 taps from the history screen (export icon
      → share target), and the files open correctly in Excel pt-BR (accents, columns, decimal
      comma) and Google Sheets.
- [ ] No unpriced item is exported as `0`. No two items are merged.
- [ ] The Paywall lists the export benefit in pt-BR/en/es.
- [ ] Tests (per `MVP-ROADMAP.md` fixed rules, `android-engineer-quality`): unit tests for
      purchase-date and total resolution (all fallbacks), month bucketing, the CSV builder
      (both dialects, quoting, empty prices, BOM), the history ViewModel (Free and Pro
      identical), and the gate call site (Free → Paywall, Pro → export). Compose test for the
      empty month and the estimated-total marker.
- [ ] `./gradlew test` and `./gradlew lint`/Detekt pass with no `@Suppress`.
- [ ] Post-release, `spend_history_export_tapped{is_pro=false}` is non-zero. If it stays at zero,
      the export gate isn't being discovered. Don't conclude "no demand" from silence.

---

## Readiness

**Not ready for a direct implementation session. It needs a `design.md` + `tasks.md` pass first**,
the same as `item-add-authorship` got. Reasons:

1. **Data-model change** (`Shopping.finishedAt` in `core/domain`, `ShoppingModel` + mappers,
   Firestore field, two finish call sites plus reopen). This touches `:wear`'s compiled code path
   too (the `Shopping` model is shared), and the tech lead needs to approve it.
2. **New Android infrastructure with no precedent in the repo**: a `FileProvider` (manifest entry
   + `res/xml` paths, in `:app` or `feature/shopping`?) and a file-writing share port that keeps
   `Context`/`Intent` out of the domain, like AD-011 did for `ShareShoppingUseCase`. Placement is
   an architecture decision (tech lead), not a PM one.
3. **The visual design of the cards and trend line** is pending `product-designer`. The data
   contract the screen needs (6 months × total, selected-month header, grouped purchases,
   estimated flag) is fixed here, but the implementation session shouldn't invent the chart
   visuals.
4. **Two open product questions** for bruno (current-month card composition, long-press finish
   UX). Defaults exist, so they don't block Design, but they should be answered before Tasks.

Suggested split for `tasks.md` (one PR per row, per the roadmap's workflow convention, or the tech
lead may merge PR-a into PR-b): **PR-a** `finishedAt` + resolved-total use case + long-press
analytics (no UI). **PR-b** P1 history screen (free). **PR-c** P2 export + gate + Paywall benefit
line. **PR-d** P3 remaining analytics, if not folded into b/c.

---

## Architecture addendum (tech-lead, 2026-10-26)

This answers the architecture questions in "Readiness" items 1–2 and in the Assumptions table. It
doesn't change any business rule above. Full decision records: `.specs/STATE.md` **AD-013** and
**AD-014**.

| Question raised by this spec | Decision |
|---|---|
| `Shopping.finishedAt` shape (Readiness 1, Assumption "Purchase date") | **Approved, with three additions.** `val finishedAt: Long? = null` (epoch millis) as the **last** constructor parameter of `core/domain/model/Shopping.kt`, mirrored in `ShoppingModel.toMap()` + both mappers. (a) Writes go only through pure helpers `Shopping.finished(price, description, at)` / `Shopping.reopened()`, with no clock inside. (b) `ShoppingDuplicateUseCase` must reset it to `null`. (c) Read invariant: trust `finishedAt` **only when `status == FINISH`**, otherwise use the fallback chain already decided here. Older builds and the AI `update_shopping_list` path (new gap **G22**) can leave stale values. Old clients are safe: every `Json` has `ignoreUnknownKeys`, `update()` keeps unknown fields, and `firestore.rules` has no `/shopping` field allowlist. `:wear` only recompiles. |
| Long-press "Finalizar" (open question 2, G21) | **Decided by bruno: same confirm-total sheet.** Architecture (AD-014 §2): `FinishPurchaseRoute` becomes public `FinishPurchase(shoppingId, source)` in `core/navigation/mobile`, registered top-level by `feature:cart`, and loads its own data via `ShoppingRepository` + `ProductReader`. Logic moves into `feature/cart` `FinishShoppingUseCase`. Home navigates with `source = "shopping_list_long_press"`, so P3 AC5's `cart_finish_purchase_completed{source}` comes for free. No feature->feature import (not new G10 debt). **Consequence for this spec:** the "calculado" total fallback is still needed, but only for legacy lists finished before this ships. New finishes always carry a confirmed total. |
| `FileProvider` + share plumbing placement (Readiness 2, Assumptions "File storage" / "Module placement") | **No new module.** CSV builder: pure Kotlin in `feature/shopping/domain/`. Port: `internal interface FileShareService` in `feature/shopping/domain/services/` (`share(files: List<ShareableFile>, subject, text): Result<Unit>`, no Android types). Impl: `feature/shopping/data/services/AndroidFileShareService` (cacheDir/exports, `FileProvider.getUriForFile`, `ACTION_SEND_MULTIPLE` + `ClipData` + read grant on inner and chooser intents, any exception -> `Result.failure`). The `<provider>` + `res/xml/file_provider_paths.xml` live in **`:app`** (a library manifest would merge into the `:wear` APK). Authority = `packageName + FileShareAuthority.SUFFIX` (`core/common`). Promote the port to `core/domain` only when a second feature needs it. |
| Pro gate mechanism (Assumption "Pro gate mechanism", P2 AC2/AC3) | **Superseded by AD-013.** Not `isPro`: the export action gates on `ProFeature.HISTORY_EXPORT`, flag **`PRO_GATE_HISTORY_EXPORT_ENABLED`** (`pro_gate_history_export_enabled`, default off), exposed as `ShoppingSubscriptionGateViewModel.canExportHistory` = `proFeatureAccess.observeAccess(HISTORY_EXPORT)`. Same call-site shape as the QR gate. Enforced only when `BILLING_ENABLED` **and** this flag are on **and** the user is FREE. In every other combination everyone can export (extends the "BILLING_ENABLED is OFF" edge case). P2 AC1's "Pro badge" for Free users should key off `isEnforced(HISTORY_EXPORT)`, so no badge shows when nothing is gated. |
| Paywall copy (P2 AC12) | Still required, but under AD-013 §7 the benefit row renders only while `isEnforced(HISTORY_EXPORT)` is true. |

**Suggested PR split for `tasks.md`** (refines the split above; one branch + PR each off
`develop`, in this order):
1. AD-013 PR (1): `ProFeature`/`ProFeatureAccess` + migrate the existing gates. Prerequisite
   for PR-c, separate initiative.
2. **PR-a1** `finishedAt` + `finished()`/`reopened()` + duplicate reset + reopen. No UI.
3. **PR-a2** shared `FinishPurchase` route + `FinishShoppingUseCase` + long-press opens the sheet
   + `source` analytics param (closes G21 together with PR-a1).
4. **PR-b** P1 history screen (free) + resolved date/total use case. Needs `product-designer` visuals.
5. **PR-c** P2 export (CSV builder, `FileShareService`, `:app` provider, `HISTORY_EXPORT` gate,
   Paywall row). Needs 1 and 4.
6. **PR-d** remaining P3 analytics, if not folded in.

**Module note:** the export port, impl, CSV builder and gate VM follow the history screen's
module. That's `feature/shopping` per this spec, but uncommitted `SpendHistory*` UI appeared in
`feature/cart` from a parallel session on 2026-10-26. `design.md` must pick one module for
both. Splitting them is not allowed (AD-014 §3).

**Status after this addendum:** architecture unblocked. Ready for a `design.md` + `tasks.md` pass.
Before Tasks, bruno still owes the current-month card answer (open question 1, default applies)
and `product-designer` the card/trend visuals.
