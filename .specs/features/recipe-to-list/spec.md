# Feature Specification: Recipe → Shopping List

Slug note: `recipe-to-list` (kebab-case, outcome-named like `item-add-authorship` /
`email-phone-auth`). This spec **absorbs** the Persona Action Plan's "Agora" item #3
`recipe-list-origin` (Yasmin) as story P3. That item was never spec'd separately, and the recipe →
list path is the thing that produces a list's recipe origin, so splitting them would mean two specs
touching `Shopping` and the same add flow.

Status: **Spec, planning phase only.** Two product decisions are still open for bruno (Q1 data
source, Q2 Pro cut), plus Q6 on custom recipes vs. reusable templates. See "Assumptions & Open
Questions". P1's checklist/batch-add core, P5 (custom recipes, added 2026-10-26 at bruno's request)
and the G23/G24 fixes don't depend on Q1/Q2. P2 and P4 do.

Scope: **all the ways a recipe becomes a shopping list**, in one spec, sharing one checklist and one
add path:

| Source | Story | Tier |
|---|---|---|
| Curated PT-BR catalog | P1 | Free |
| Pasted text → AI extraction (one-shot) | P2 | Pro (1 free try) |
| **User's own saved recipes** (manual, persisted, reusable) | P5 | Free |
| Recipe origin remembered on the list (applies to all three) | P3 | Free |
| Same pipeline and gating across UI / AI chat / App Functions | P4 | n/a |

---

## Correction to the roadmap's premise (read first)

The monetization table describes this as "code already exists, **unmerged**, in agent worktree
branches." That's wrong. The recipe code is **already in the main tree** (`feature/products/src/main/…`)
and has been since at least the 2026-09-09 bug audit (G15 cites `RecipeRepositoryImpl.kt:29-32`).
The worktree copies under `.claude/worktrees/agent-*/` are just identical scratch checkouts. What
actually exists and ships today:

| Piece | Where | What it does |
|---|---|---|
| `RecipeRepositoryImpl` | `feature/products/.../data/repository/` | TheMealDB `search.php?s=` / `lookup.php?i=` over `CloudNetwork`, then **one Gemini call per recipe** to translate name/description/instructions/ingredients to PT-BR (`translateAll` runs them in parallel) |
| `Recipe` model | `feature/products/.../domain/model/Recipe.kt` | `ingredients: List<Product>`. Each ingredient becomes a `Product` named `"<measure> <ingredient>"`, `quantity = 1.0`, `price = 0.0` |
| `RecipeSearchUseCase` | `@AiAgentAction("search_recipes")` | Bound into the agent registry (`ProductAgentModule`) |
| `RecipeAddToListUseCase` | `@AiAgentAction("add_recipe_ingredients")` | Loops `ProductSaveUseCase(product, shoppingId)` over every ingredient |
| `CestouAppFunctions` | `app/.../appfunctions/` | Exposes `searchRecipes` + `addRecipeIngredientsToList` to the **system** assistant (Android App Functions, API 36) |
| Privacy/Terms | `docs/legal/privacy.html:90,102,104`, `terms.html:101,104` | Already disclose TheMealDB + Gemini translation |

The app has **no recipe UI at all**. `ProductScreen` is `Options.FORM` + `Options.PHOTO` only. Its
KDoc says "recipe/AI surfaces are moving to their own modules". No `Recipe*Screen`/`ViewModel`
exists in the main tree or in any worktree. Today a user reaches recipes only by asking the AI chat
(Pro after one free message, `ChatAiGateViewModel`) or a system assistant via App Functions.
`ANALYTICS-PLAN.md` §4 lists `product_added{source=recipe}`, but no code emits `"recipe"`, so that
funnel slice is permanently empty.

So this is not "merge the branch and add a gate". It's "decide whether the existing plumbing is
worth putting in front of users, fix what's broken in it, and give it a proper surface."

---

## Problem Statement

Three personas have a recipe-shaped problem that the list can't express today:

- **Yasmin** (primary): she finds a recipe **on a site**, shops for it, then loses the recipe
  between the store and home. Her pain has two halves: getting the ingredients onto a list without
  retyping them, and keeping the "why" (the recipe) attached to the list.
- **Camila e Pedro**: they don't know what to cook or what a week of meals needs. A short list of
  everyday dishes that turn into ingredients is the "starting point" they say they lack.
- **Juliana**: she rejects any list-building that's only typing. Tapping ingredients off a recipe
  is a non-typing path.
- **Anyone with a repertoire** (bruno's request, 2026-10-26): the dishes a household actually cooks
  repeatedly are *their* recipes: grandma's bolo, their own strogonoff, Rodrigo's marmitas,
  Anderson's weekly pizza insumos. No catalog contains them, and pasting them through AI every time
  is wasteful. They want to write a recipe down once and turn it into list items whenever they cook
  it.

The existing code solves a different version of this problem. It searches an **English, international**
recipe database by English dish name, translates every result through Gemini, and dumps every
ingredient (with the measure baked into the item name) onto the list with no review step. Below is
why that fits Cestou's actual users badly (Data source decision). A product-level reading of the
existing shape also matters: it's a one-shot "ingredients → list" helper, not a meal-planning loop
(see Entry point).

## Goals

- [ ] A user can turn a recipe into list items in one reviewed step: see the ingredients, untick
      what's already at home, add the rest. No retyping.
- [ ] Every recipe-sourced item goes through the **same** add path as every other surface
      (`ProductSaveUseCase` → `withActivity(ADDED, …)`, plus the IAA-02 duplicate check), with a
      clean product name (no measure in the name).
- [ ] The list remembers which recipe(s) it came from, and the user can reopen them from the list
      (absorbs `recipe-list-origin`).
- [ ] The recipe content a Brazilian user sees is Brazilian and in Portuguese, not a machine
      translation of British/American dishes.
- [ ] A user can save their own recipes (name + ingredients with shopping quantities) once, manage
      them, and reuse any of them on any list, any number of times.
- [ ] The Free/Pro line follows one rule users can predict: **Pro pays for AI work, never for a
      button over data the app already has.**

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Bottom-tab or top-level recipe destination | It's an add-to-list helper, not a meal-planning loop (see Entry point). Re-evaluate only on the signals listed there. |
| Weekly meal plan / calendar, servings scaling, pantry subtraction ("I already have 500 g of rice") | These are what would make it a planning loop. Not built, not promised. |
| Importing a recipe from a **URL** (fetch + scrape a site) | Scraping third-party sites carries legal and robustness risk. v1 accepts pasted **text** (what Yasmin copies from the page). The link can still be saved as the origin (P3). |
| Step-by-step cooking mode, timers, nutrition | Not a shopping-list job. |
| Recipe UI on Wear OS | Wear stays list/checkbox only. |
| User-authored public recipe sharing / community catalog | Moderation and B2C-social scope. Different product. |
| Sharing a **saved custom recipe** with another user or list member as a live, editable object | Custom recipes are private (`users/{uid}/recipes`, same model as `common-products`). Members of a shared list see a **snapshot** through the list's origin (P3), not the owner's recipe document. |
| Photos, steps/instructions editor, tags/folders for custom recipes | v1 is name + ingredients + an optional free-text note. Anything more is a recipe-manager product, not a list helper. |
| "Save this whole list as a recipe/template" from the list screen | The cheap Rodrigo affordance. Deferred behind a validation signal, see Q6 and "Custom recipes vs. reusable list templates". |
| Fixing IAA-03 drift or the AI batch-add authorship gap | Tracked as G27/G28 in `MVP-ROADMAP.md`. Not done here, though P1 depends on G27 for the AI path (see RTL-05). |

---

## Data source decision: TheMealDB does not serve these users

Evaluated plainly, against the existing code and the personas:

1. **Language.** The search query goes raw to `search.php?s=<query>`, a dish-**name** search over
   English content (the repo's own KDoc says "TheMealDB only publishes content in English").
   Translation only happens *after* the search, on the results. A Brazilian user typing "frango",
   "bolo de cenoura", or "strogonoff" is querying English names. Most PT-BR queries will return
   nothing, and the user sees "no recipes" without knowing why. The one thing a PT-BR user would
   naturally type is the thing the code can't handle.
2. **Cuisine fit.** The dataset is small and skewed toward British/American/international dishes.
   Brazilian everyday cooking (arroz e feijão, feijoada, escondidinho, farofa, bolo de fubá,
   brigadeiro, strogonoff à brasileira) is rare or absent. *Verify before bruno decides*, with a
   five-minute live probe: `search.php?s=feijoada`, `?s=brigadeiro`, `?s=moqueca`,
   `filter.php?a=Brazilian`. Even if a handful exist, "a handful" doesn't serve Camila e Pedro's
   "what do people normally cook" need. To Marina, Dona Marlene or Dona Célia, a catalog of
   "Beef Wellington" and "Treacle Tart" in machine Portuguese feels foreign. It reads as an
   imported feature, not a Cestou one.
3. **Ingredient semantics.** TheMealDB measures are cooking quantities ("2 cups", "1 tbsp"), not
   shopping quantities. Baking them into the item name ("2 xícaras Farinha", qty 1) breaks the
   duplicate check (IAA-02 never matches "Farinha"), category suggestion, and any future by-name
   price history (`real-price-history`, Persona plan #4). See G24.
4. **Cost and reliability.** Every search fans out to one Gemini call **per result**, which is
   unbounded per keystroke-triggered search. The key resolution is the same Remote-Config-or-compiled
   path (AD-008) as the photo-scan fallback, which was **verified failing on a device with
   `InvalidAPIKeyException`**. While that holds, `translateToPortuguese` swallows the error
   (`getOrElse { recipe }`) and silently returns **English** text. So the translation layer that
   was supposed to fix point 1 is probably not running in production right now.
5. **Licensing (verify).** The endpoint uses the public test key `1` (`/api/json/v1/1/`).
   TheMealDB's published guidance asks apps released publicly / on an app store to use a supporter
   (Patreon) key. Check the current terms. If that still holds, shipping this in a Play Store build
   on key `1` is a terms problem bruno has to own, not an engineering detail.

**Recommendation (needs bruno, Q1):** **Retire TheMealDB as the user-facing source.** Replace it
with two sources that both feed one ingredient checklist:

- **(a) A curated PT-BR catalog of everyday Brazilian dishes (Free).** Roughly 30–50 recipes
  written in-house, with ingredients stored as plain grocery names ("Farinha de trigo", "Ovos",
  "Cenoura") plus a display-only measure. No third-party API, no license question, no AI cost, no
  translation. If it's bundled with the app it **works offline**, which Dona Célia needs ("funcionar
  bem mesmo com conexão ruim"). Storage mechanism (bundled asset vs. Remote Config JSON vs. Firestore
  collection) is a `design.md` call. The default is a bundled asset, updated with releases.
  Content must be written in-house, not copied from recipe sites. Ingredient lists are generally
  fine, but prose and steps are not. Owner for content: `pm` drafts, bruno approves (see Q4).
- **(b) "Paste any recipe" → AI extracts the ingredients (Pro).** This is Yasmin's real flow: she
  copies the ingredients block from whatever Brazilian site she's on. The configured AI provider
  reads PT-BR natively, so no TheMealDB and no translation step are needed. It goes through the
  user's selected provider (Gemini/OpenRouter via the existing factory and AD-008 key handling), not
  a new hard-coded `GenerativeModel`.

**What happens to the existing TheMealDB code (tech-lead's call, flagged as Q3):** the AI chat
doesn't need TheMealDB to "add the ingredients for a feijoada". The model knows recipes and can
call the existing `add_multiple_products_to_cart` (`ProductListSaveUseCase`) directly. The
recommended default is to unbind `search_recipes`/`add_recipe_ingredients` from `ProductAgentModule`
and the two App Functions, delete `RecipeRepositoryImpl`'s TheMealDB/translation code, and update
privacy/terms to remove TheMealDB and "recipe translation". The `Recipe` domain model and the
batch-add use case stay, re-shaped for the checklist. If bruno prefers to **keep** TheMealDB, the
minimum acceptable scope is: AI-chat-only (no UI surface), with G23/G24/G25 fixed, query translated
to English *before* search, and the license confirmed. Even then it shouldn't get its own screen.

---

## Free/Pro cut: the roadmap placeholder doesn't hold up

The roadmap placeholder is "browse free, auto-add-to-list Pro". Checked against the pro-entitlement
scope boundary ("running total, budget alerts, purchase history, price comparison stay FREE for
every persona") and against what each tier would actually get:

- **"Browse free" is fine.** Looking at a recipe touches none of the protected-free items, and
  nobody expects to pay to read one.
- **"Auto-add Pro" is a bait-and-switch in this app.** People don't open a shopping-list app to read
  recipes. Turning the recipe into list items *is* the job, not a convenience on top of it. A Free
  user who browses a recipe and then has to retype 10–15 ingredients into the form has been shown
  the value and then charged for the only part that matters. That's exactly the "digitar cada item
  de novo" Anderson and Juliana reject. The purchase-history-export reasoning (export is an
  incremental convenience over data already visible and usable for free) **doesn't carry over**.
  Here the free tier would be left with nothing usable.
- **The rule that does hold**, and matches the established pattern (`ChatAiGateViewModel`: "the AI
  chat is the Pro differentiator, not any individual agent action"; AD-010), is: **Pro pays for AI
  work, never for a button over data the app already has.**

**Decision (pending bruno's confirmation, Q2):**

| Capability | Tier | Why |
|---|---|---|
| Browse/search the curated catalog | **Free** | No marginal cost. Content is ours. |
| Ingredient checklist (untick what you have) + "Adicionar N itens" in one tap, from a catalog recipe | **Free** | The data is already structured. Gating the add button would be artificial. |
| Recipe origin saved on the list + reopen it (P3), including a manually pasted recipe text/link as the origin | **Free** | Yasmin's core "why" pain. No AI involved. Same shape as `budget`, which is free. |
| Paste any recipe text → AI extracts the ingredients into the same checklist (P2b) | **Pro**, with **one free extraction** (default, unconfirmed) | Real per-use AI cost. It mirrors chat's one-free-message trial, so a Free user experiences it once before the paywall. |
| Asking the AI chat to build a list from a recipe | Already Pro (chat gate) | Unchanged. No new gate. |
| **Create / edit / delete / reuse your own custom recipes (P5)**, including "Salvar nas minhas receitas" from a catalog recipe or from a P2 extraction result | **Free, no count cap** | See below. |

**Custom recipes checked against the principle, not just waved through:**

- *AI work?* None. The user types the ingredients. Saving a P2 extraction result as a custom recipe
  is also free: the AI work was already paid for (or used the free try) at extraction time. Saving
  the result is a button over data the app already has.
- *Whose data?* The user's own. Charging to keep or reuse what someone typed into the app is the
  same pattern the scope boundary already rules out for purchase history.
- *Real cost?* Small private Firestore docs, read on demand. Negligible on Spark quotas. There's no
  cost argument for a gate.
- *Count cap (e.g. "3 free, unlimited on Pro")?* Considered and **rejected**. A count cap on your
  own data is the bait-and-switch shape rejected above, just postponed: it works for two weeks and
  then charges you to keep using your own grandma's recipe. Only a **technical** cap applies
  (~200 recipes, ~60 ingredients each), for document size and abuse, with plain copy if hit.
- *Does making it Free cannibalise P2 (Pro)?* Partly, and that's acceptable. A user who types a
  recipe by hand once never needs to pay to extract it again. P2's value is speed on *new* recipes
  from the web, not lock-in on known ones. Pro has to earn its value, not trap it.

Conclusion: **Free.**

Gate mechanics, following the existing pattern rather than inventing a new one:

- UI gate: a small `@HiltViewModel` gate (same shape as `ShoppingSubscriptionGateViewModel` /
  `ChatAiGateViewModel`) observing `SubscriptionRepository.observeStatus()`. Kept out of any
  ViewModel `:wear` might compile. **No new gate class/abstraction.**
- The one free extraction needs a persisted "used" flag. `design.md` decides whether
  `AiTrialRepository` grows a second key or the chat allowance is shared. The default is a separate
  key, because sharing it would make "I tried the chat" silently burn the recipe trial.
- Agent path: if the AI chat ever gets a recipe-extraction action, it sets
  `AgentAction.requiresPro = true` and relies on the existing dispatch chokepoint.
- App Functions: **they bypass the chokepoint today** (G26). Any recipe capability left exposed
  there must check `SubscriptionRepository` itself, or be removed.
- Paywall copy: add a benefit line **only when P2b is actually reachable**. Word it for exactly the
  Pro piece ("Monte a lista a partir de qualquer receita colada", or similar). Never write a generic
  "Receitas" line, which would tell Free users the catalog is paid (G19 lesson: no benefit line
  promising more, or other, than what the gate actually enforces).

---

## Entry point: add-to-list helper, not a meal-planning loop

Applying the call bruno and the PM just made ("not a bottom-tab candidate unless it matures into a
real meal-planning loop"):

What the existing code does, and what this spec builds, is **recipe → ingredients → list, once**.
There is no recurrence (recipes aren't saved or reused across weeks), no plan across days, no
servings, and no pantry awareness. That's an **add-to-list helper**, the same category as the
camera shortcut. So:

- **Entry point:** a secondary shortcut inside the existing add-item sheet (`ProductForm`), placed
  next to the existing camera shortcut, labelled plainly ("Adicionar de uma receita"). It pushes a
  new route in `ProductScreen`'s NavHost (`Options.RECIPE`, alongside `Options.PHOTO`). The form
  stays the default start destination, so Dona Célia's first screen doesn't change.
- **Module:** stays in `feature/products`, where `ProductSaveUseCase`, the duplicate check and the
  `Recipe` model already live. The `ProductScreen` KDoc's "recipe/AI surfaces are moving to their
  own modules" would add a new feature module plus a cross-feature dependency on `ProductSaveUseCase`,
  for one sheet. That widens G10 for no user benefit. Tech-lead to confirm (Q5).
- **AI chat:** no new card or UI there. Chat keeps doing "build a list from a recipe" through its
  existing tools (Pro), as `item-add-authorship`'s "AI chat's role going forward" note anticipated.
- **Custom recipes (P5) use both places, with one job each:**
  - **Use** them from the same recipe picker (`Options.RECIPE`). "Minhas receitas" is the first
    section, above the catalog, with a "+ Nova receita" action. That's how the user builds a list
    from them, and it's where a new recipe naturally gets created ("I'm adding stuff for my bolo,
    let me save it"). **No new `Options` value**: this is a section of the same picker, not a 4th
    or 5th add surface, so the add sheet still has exactly two shortcuts (camera, recipe).
  - **Manage** them from a "Minhas receitas" row in Profile (list → edit / delete), next to the
    existing rows (Wear link, sign out). This is where people look for "my stuff" when they're not
    in the middle of adding items.
  - Both open **the same editor screen**, owned by `feature/products` and exposed as a navigation
    route through `core/navigation`. `feature/profile` only navigates to it and never imports
    `feature/products` (AD-005 / G10). One editor, two doors.
- **Not a tab, not a top-level icon. Saved recipes don't change that call.** Saving and reusing
  your own recipes makes the helper *recurring*, but it's still not a plan: there's no notion of
  days, weeks, servings, or what's already in the pantry. Revisit only if **all** of these show up:
  users regularly add 2+ recipes to the same list, a meaningful share of active users reuse saved
  recipes week over week, and analytics show `source=recipe_*` as ≥ ~15% of `product_added` among
  active users over a month. At that point it's a meal-planning spec, not this one.

---

## Custom recipes vs. reusable list templates (`pro-entitlement` FR-05 #2)

Caveat: `pro-entitlement/spec.md` isn't present in this checkout, so FR-05 #2 is taken as the
coordinator described it: "Recurring/reusable list templates", for Rodrigo, explicitly deferred
pending a cheap validation signal.

**Are they the same feature?** Structurally, yes. A saved custom recipe is a named, user-owned,
private, reusable set of items with quantities that gets turned into list items on demand. A
reusable list template is the same thing. The differences are only:

| | Custom recipe (P5) | List template (FR-05 #2) |
|---|---|---|
| Data | name + items (name, qty, note) | name + items (name, qty, category) |
| Trigger | add *into* an existing list | create a *new* list from it |
| Framing | "a dish" (a few items) | "my monthly checklist" (20–40 items) |

Two entities under two names would mean two editors, two Firestore collections, two sets of rules,
two deletion paths, and users not knowing which one to use. **Decision: one entity, built once, as
P5.** P5 is the implementation of FR-05 #2's *data and reuse* half. No separate template feature
gets built.

- The entity is deliberately **not cooking-specific**: no servings, no steps, no cooking units in
  the schema. "Marmitas da semana" with 25 items, or "Insumos pizzaria" with 15 bulk items, are
  valid custom recipes. Applying one to an empty, freshly created list *is* using a template.
- **What's not claimed:** bruno's request validates custom recipes. It is **not** the customer
  validation FR-05 #2 asked for on Rodrigo's specific need (an owner decision isn't a user signal).
  So FR-05 #2's Rodrigo-specific affordances stay deferred: "Salvar esta lista como receita" from
  the list screen, "Criar lista a partir de uma receita", and any naming beyond "receitas" such as
  "listas prontas". The validation signal now comes free from P5's own usage: the share of saved
  recipes with ≥ 15 items or non-food names, plus `ShoppingDuplicateUseCase` usage (Rodrigo's
  current workaround is duplicating last month's list). If that signal shows up, the follow-up is
  a small UI addition on top of P5's entity, not a new feature.
- Recorded as **Q6** for bruno: confirm that FR-05 #2 is merged into P5 at the model level (and
  `pro-entitlement`'s FR-05 #2 row updated to point here), rather than kept as an independent
  future feature.

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| **Q1** Data source | Curated PT-BR catalog (Free) + paste-text AI extraction (Pro). TheMealDB retired from user-facing paths. | See Data source decision. TheMealDB is English-only, name-searched, culturally foreign, AI-translated with a probably-broken key, and possibly license-restricted on key `1`. | [ ] **bruno** |
| **Q2** Free/Pro cut | "Pro pays for AI work". Catalog + checklist + add + origin Free; paste extraction Pro with 1 free try. | Rejects the placeholder's gated add as bait-and-switch. See Free/Pro cut. | [ ] **bruno** |
| **Q3** Fate of TheMealDB agent actions + App Functions | Unbind `search_recipes`/`add_recipe_ingredients` and the two App Functions, delete the TheMealDB/translation code, update privacy/terms | The LLM can already draft ingredients and call `add_multiple_products_to_cart`. Keeping a second, worse recipe pipeline is maintenance and disclosure cost for nothing. | [ ] **tech-lead** (product side agrees) |
| **Q4** Catalog size, ownership, locale | ~30–50 everyday Brazilian dishes, PT-BR only in v1, shown to all locales; `pm` drafts, bruno approves | Brazil is the market. EN/ES translation of the catalog is a cheap follow-up if analytics show non-PT usage. Content written in-house (no copying site prose). | [ ] **bruno** |
| **Q5** Module placement | `feature/products`, new `Options.RECIPE` route | Avoids a new module plus cross-feature coupling (G10) for one sheet. Contradicts the `ProductScreen` KDoc's "moving to their own modules". | [ ] **tech-lead** |
| Ingredient → list item mapping | Item `name` = clean ingredient name; `quantity = 1`; measure ("2 xícaras") shown in the checklist only, never stored in the name; category via `ProductCategory.suggestFromProductName` | Shopping quantity ≠ cooking quantity. A clean name keeps IAA-02 duplicates, categories, and future price history working. `Product` gets no new field. | [y] product call |
| Review step | Always a checklist. Nothing is added without the user tapping "Adicionar N itens". | Dona Marlene ("não comprar duplicado ou em excesso") and everyone with salt at home. Also the defence against AI-extraction errors. | [y] product call |
| Origin storage | List-level `recipes: List<RecipeOrigin>` on `Shopping` (name, optional source text/link, optional catalog id), not per-item | Yasmin's question is "which recipe was this *list* for". Supports several recipes per monthly list. Same additive, nullable/empty-default pattern as `budget`. Exact shape and `firestore.rules` impact in `design.md`. | [ ] design.md |
| Pasted text handling | Max ~5,000 chars; sent to a **plain completion** (no tool calling), output parsed as a list of ingredient strings only | Pasted text is untrusted. It must never reach the tool-calling agent, where it could trigger `save_product_to_cart` / `clear_purchased_products`. | [y] product call (security) |
| Shared lists | Any member can add from a recipe or attach an origin. Origins are visible to all members. | Same permissions as adding an item. | [y] |
| **Q6** Custom recipes vs. FR-05 #2 templates | One entity (P5). FR-05 #2's data/reuse half is delivered by P5. Rodrigo-specific UI ("save list as recipe", "new list from recipe") stays deferred behind P5 usage signals. | Avoids two overlapping features. See "Custom recipes vs. reusable list templates". If `pro-entitlement` pencilled FR-05 #2 in as a **Pro** candidate, this merge makes it **Free**, on the same principle. That's an explicit change bruno must accept. | [ ] **bruno** |
| Custom recipe storage | Firestore `users/{uid}/recipes/{id}`, strictly private, following the `users/{uid}/common-products` precedent (AD-009 data model). Doc = `id`, `name`, `ingredients[]` (`name`, `quantity`, optional `note`, optional `category`), `createdAt`, `updatedAt`. | Must survive reinstall and work across the user's devices, so not DataStore (`AiTrialRepository`-style local flags don't fit). `core/billing` isn't a model here: it holds no user data and mirrors Play's local purchase cache. `common-products` is the only per-user private-collection precedent, and it's the right one. Rules mirror it: owner-only read/write, `hasOnly` field allowlist, size caps. | [ ] design.md (shape) / [y] location |
| Custom recipe module ownership | Repository, editor and picker section in `feature/products` (next to `CommonProductRepositoryImpl`). Profile reaches the editor/list through a `core/navigation` route only. | Keeps the add path (`ProductSaveUseCase`) and the editor in one module. Profile → products import would widen G10. | [ ] **tech-lead** (with Q5) |
| Account deletion | `UserRepositoryImpl.deleteProfile` must delete the new `recipes` subcollection too. It currently lists subcollections explicitly (`deleteDocumentWithSubCollections("users/$id", "common-products")`). | Firestore doesn't cascade subcollections. Forgetting this leaves private user data behind after "delete my account", which breaks G2/F1.1's promise and the Play data-deletion requirement. | [y] hard requirement |
| Custom recipe quantities | The user enters **shopping** quantities (e.g. "Leite, 2"), which are carried into the list as `quantity`. The optional note holds cooking detail ("2 xícaras"). | Unlike catalog/paste (where cooking measure ≠ what you buy, so qty = 1), here the user authored the numbers for shopping. | [y] product call |
| Gemini key | P2b doesn't ship while the AD-008 key resolves to an invalid key (photo-scan fallback verified failing on device) | Otherwise the Pro feature fails on first use, which is worse than not having it. | [y] dependency |

---

## User Stories

### P1: Recipe → reviewed checklist → list (catalog source) ⭐ MVP

**User Story**: As Camila e Pedro, I want to pick an everyday dish and get its ingredients onto my
list in one step, unticking what we already have, so we stop starting every shopping trip from
zero.

**Why P1**: It's the core loop, and it's independent of the AI and paywall questions. It also
exercises the shared add path (authorship, duplicates) that P2 reuses.

**Acceptance Criteria**:

1. WHEN the user opens the add-item sheet, THEN a secondary "Adicionar de uma receita" shortcut
   SHALL be visible next to the camera shortcut, and the form SHALL remain the default surface.
2. WHEN the user taps it, THEN a recipe picker SHALL show the curated catalog with a search field.
   Search SHALL match PT-BR dish names and ingredient names, case- and accent-insensitive ("feijao"
   finds "Feijão tropeiro").
3. WHEN the user opens a recipe, THEN its ingredients SHALL be shown as a checklist, each row with
   the clean item name plus the display-only measure, all ticked by default. **Exception:** an
   ingredient matching an active (not purchased) item already on the list SHALL be shown unticked,
   labelled "já está na lista" (with the adder's name when known, reusing IAA-02 copy).
4. WHEN the user taps "Adicionar N itens" (N = ticked count, button disabled at 0), THEN each ticked
   ingredient SHALL be saved through `ProductSaveUseCase` (so `history` gets
   `ADDED` by the current user) with `name` = clean name, `quantity = 1`, `price = 0`, category
   suggested from the name.
5. WHEN all saves succeed, THEN the sheet SHALL return to the list with a confirmation ("N itens de
   Strogonoff adicionados"), and the list's running total SHALL be visible as on any other add.
6. IF some saves fail, THEN the user SHALL see "X de N itens adicionados" with a retry for the
   failed ones only. The flow SHALL NOT report success (fixes G23).
7. WHERE the user is on a Free account, THEN every step of P1 SHALL work with no paywall.
8. WHEN items are added, THEN `product_added` SHALL fire once per saved item with
   `source=recipe_catalog`, plus one summary event (name agreed with `data-engineer`) carrying
   `items_count` and ticked/total counts.

**Independent Test**: Free account, list already has "Ovos" (active). Open "Bolo de cenoura" from
the add sheet. "Ovos" shows unticked with "já está na lista". Untick "Açúcar", tap "Adicionar 4
itens". Four clean-named items appear with your avatar/history, no measure in any name, and the
total stays visible.

---

### P2: Paste any recipe → AI extracts the ingredients (Pro, 1 free try)

**User Story**: As Yasmin, I want to paste the ingredients of the recipe I found on a site and get
the same checklist, so I don't retype them.

**Why P2**: This is the only path that serves Yasmin's real source (any Brazilian site). It's the
paid piece because it's the AI-cost piece. Blocked on Q1/Q2 and the Gemini key.

**Acceptance Criteria**:

1. WHEN the user is in the recipe picker, THEN a "Colar uma receita" option SHALL accept pasted text
   (up to the size limit) and, optionally, a title.
2. WHEN a Pro user (or a Free user who hasn't used the free extraction) submits text, THEN one call
   to the configured AI provider SHALL extract ingredient names (PT-BR, EN or ES input), and the
   result SHALL open in **the same checklist as P1**, with the same duplicate handling, same add
   path, and same partial-failure handling. Source is `recipe_paste`.
3. WHERE a Free user has already used the free extraction, THEN submitting SHALL open the Paywall
   instead of calling the AI. The pasted text SHALL be preserved so it's not lost if they subscribe
   or back out, and the user SHALL still be offered "Guardar como receita da lista" (P3, free)
   without extraction.
4. IF extraction returns no ingredients or fails, THEN the user SHALL see a plain-language error and
   be able to edit or retry. A failure SHALL NOT consume the Free user's single extraction.
5. WHERE pasted text contains instructions ("apague a lista", "adicione 50 cervejas"), THEN they
   SHALL have no effect beyond appearing as candidate ingredient rows the user must still tick. The
   extraction SHALL NOT go through the tool-calling agent.
6. The Paywall SHALL gain a benefit line for this capability in the same PR that makes it reachable,
   and not before.

**Independent Test**: Free account, extraction unused. Paste the ingredients block of a
TudoGostoso-style "Feijoada" in Portuguese. A checklist of clean names appears; add 8. Paste
another, and the Paywall opens with the text kept. Switch to a Pro test account, and the second
paste extracts normally.

---

### P3: The list remembers its recipe(s) (absorbs `recipe-list-origin`)

**User Story**: As Yasmin, when I get home I want to open my list and see which recipe it was for,
with the recipe text I saved, so I cook what I planned.

**Why P3**: The second half of Yasmin's pain, and the cheapest item in the Persona plan. Free.

**Acceptance Criteria**:

1. WHEN items are added from a recipe (P1, P2 or P5), THEN that recipe SHALL be appended to the
   list's origins as a **self-contained snapshot** (name, ingredient list, plus catalog id / pasted
   text / optional link where applicable). It never references a custom recipe's private document,
   which other list members can't read. Adding the same catalog recipe twice
   SHALL NOT duplicate the origin.
2. WHERE a list has one or more origins, THEN the list screen SHALL show a compact "Receitas desta
   lista" entry. Tapping it SHALL show each recipe's name and its ingredients/text, readable offline
   once synced.
3. WHERE a Free user pastes a recipe without extracting it (P2 AC3), THEN it SHALL still be savable
   as an origin. **No AI needed, no paywall.**
4. WHEN the user removes an origin, THEN only the origin SHALL be removed. Items already added SHALL
   stay.
5. WHERE a list has no origins, THEN nothing new SHALL render. Lists that don't come from a recipe
   look exactly as today.
6. WHERE the list is shared, THEN all members SHALL see the same origins.

**Independent Test**: Add "Strogonoff" from the catalog to a list, close the app, reopen the list
offline. "Receitas desta lista → Strogonoff" opens with its ingredients.

---

### P4: One recipe pipeline, consistently gated, across every surface

**User Story**: As the product owner, I want recipes to behave the same (and be gated the same)
whether the user is in the add sheet, the AI chat, or a system assistant, so no surface leaks Pro
or adds items differently.

**Why P4**: Today two parallel recipe surfaces (agent actions + App Functions) exist with no UI,
bypass the duplicate check, and in the App Functions case bypass the AD-010 chokepoint. Shipping P1–P3
without reconciling them leaves a worse, inconsistent second pipeline in place.

**Acceptance Criteria**:

1. WHEN Q3 resolves to "retire" (default), THEN `search_recipes`/`add_recipe_ingredients` SHALL be
   unbound from the agent registry, the two recipe App Functions removed, TheMealDB/translation code
   deleted, and `privacy.html`/`terms.html` (pt/en/es) updated in the same PR.
2. WHERE any surface adds recipe-derived items, THEN it SHALL use the same batch-add use case as P1
   (which calls `ProductSaveUseCase` per item). No surface SHALL call `ProductRepository.saveProduct`
   directly for recipe items.
3. WHERE any App Function or agent action exposes a Pro capability from this spec, THEN it SHALL be
   gated (`requiresPro` for agent actions; an explicit `SubscriptionRepository` check for App
   Functions).
4. WHEN the AI chat adds items on the user's behalf from a recipe, THEN `history` SHALL carry the
   human `AiSession.userId` (IAA edge case). That requires G27 for `add_multiple_products_to_cart`.

**Independent Test**: With the default Q3, the agent registry and App Functions metadata
(`app_metadata.xml`) list no recipe actions. Asking chat (Pro) "coloca os ingredientes de um
strogonoff na lista" adds clean-named items with the user's `ADDED` history.

---

### P5: My own recipes: save once, reuse on any list (Free)

**User Story**: As someone who cooks the same dishes over and over (Camila e Pedro building their
repertoire, Yasmin repeating the family's favourite, Rodrigo's marmitas, Anderson's weekly insumos),
I want to save my own recipe with its ingredients once, then put them on any list whenever I need
them, so I never retype or re-paste the same recipe.

**Why P5**: A direct request from bruno. It covers the one source neither the catalog nor paste
covers (the user's own repertoire). It's the reuse half of FR-05 #2 (Q6). No AI, and it doesn't
depend on Q1/Q2, so it can ship right after P1 with the same checklist.

**Acceptance Criteria**:

1. WHEN the user opens the recipe picker, THEN a "Minhas receitas" section SHALL appear first,
   listing their saved recipes, with a "+ Nova receita" action. With no saved recipes, it SHALL
   show only the action plus one line explaining what it's for.
2. WHEN the user creates or edits a recipe, THEN the editor SHALL ask only for a name (required)
   and ingredients. Each ingredient is a name (required), a shopping quantity (default 1), and an
   optional note. Rows are added with a single "+ ingrediente" field that keeps focus, so typing
   "Arroz ⏎ Feijão ⏎ Cebola ⏎" works without extra taps. Category is suggested from the name, as
   in the add form.
3. WHEN the user saves, THEN the recipe SHALL be persisted to their private
   `users/{uid}/recipes/{id}`. It SHALL be available on any of their devices and survive reinstall.
4. WHEN the user opens a saved recipe from the picker, THEN it SHALL open in **the same checklist as
   P1**: ticked by default, active duplicates unticked with "já está na lista", the saved quantities
   pre-filled, the same "Adicionar N itens" through `ProductSaveUseCase`, and the same
   partial-failure handling. Source is `recipe_custom`. P3 records the origin as a **snapshot** (name
   + ingredients at that moment), never as a reference to the private document.
5. WHERE the user is viewing a catalog recipe (P1) or a P2 extraction result, THEN "Salvar nas
   minhas receitas" SHALL copy it into a new, editable custom recipe. Free, with no AI call at save
   time.
6. WHEN the user opens Profile → "Minhas receitas", THEN they SHALL see the same list, and can open
   the same editor or delete a recipe (with confirmation). Deleting a recipe SHALL NOT remove items
   already added to lists or origins already saved on lists (snapshots).
7. WHERE the user is Free, THEN creating, editing, deleting and reusing custom recipes SHALL be fully
   available with **no count-based limit**. Only the technical cap applies (~200 recipes / ~60
   ingredients), with plain copy if reached.
8. WHEN the user deletes their account, THEN all of `users/{uid}/recipes` SHALL be deleted along
   with `common-products` (`UserRepositoryImpl.deleteProfile`).
9. WHEN a custom recipe is created, edited, deleted or used, THEN analytics SHALL record it (names
   with `data-engineer`), with `source` of creation = `manual` / `catalog_copy` / `paste_copy`, and
   item count. No recipe names or ingredient text go in event params.

**Independent Test**: Free account. In the add sheet → recipe → "+ Nova receita", save "Bolo da vó"
with Farinha (1), Ovos (1, note "4 unidades"), Leite (2). Back in the picker, open it: three items
pre-filled with those quantities. Add to list A. Sign in on a second device, open Profile → Minhas
receitas, and "Bolo da vó" is there. Edit Leite to 1 and add it to list B. List A is unchanged, and
B's origin shows the edited version.

---

## Edge Cases

- IF the user is offline, THEN the catalog (bundled) and checklist SHALL still work, and adds SHALL
  queue through Firestore's offline persistence like any other add. Paste extraction (P2) SHALL show
  "precisa de internet" without consuming the free try.
- IF the user double-taps "Adicionar N itens", THEN items SHALL be added once (same in-flight guard
  as `ProductFormViewModel.save`).
- IF the same ingredient appears twice in one recipe (e.g. "Sal" in dough and filling), THEN the
  checklist SHALL show it once.
- IF an ingredient matches an item already on the list that is **purchased**, THEN it SHALL be
  ticked normally (IAA-02 AC3: nothing left to duplicate).
- IF the user unticks everything, THEN the add button SHALL be disabled. Backing out SHALL add
  nothing and save no origin.
- IF a recipe has more than ~25 ingredients (pasted mega-recipes), THEN the checklist SHALL still
  scroll and add in one action. Writes are per item (no Firestore batch requirement for v1).
- IF Pro lapses between opening the paste screen and submitting, THEN the gate evaluated at submit
  time wins (point-in-time read, same as the agent chokepoint).
- IF the device language is EN/ES, THEN the PT-BR catalog SHALL still show (Q4 default) and the UI
  chrome SHALL be localized.
- IF a custom recipe is edited after it was used on a list, THEN that list's origin keeps the old
  snapshot (P5 AC4). Lists record what was planned at the time, not live recipe state.
- IF the user saves a custom recipe offline, THEN it SHALL be queued by Firestore's offline
  persistence and appear immediately in the picker on that device.
- IF the user taps "+ Nova receita" mid-add and backs out of the editor without saving, THEN they
  SHALL return to the picker with nothing saved and nothing added to the list.
- IF a custom recipe has two rows with the same ingredient name, THEN the editor SHALL keep both,
  as the user's choice, and the checklist SHALL merge them into one row, summing the quantities.
- IF the same custom recipe is applied to the same list twice, THEN the second time its
  ingredients show as "já está na lista" (unticked), and the origin isn't duplicated.
- IF a shared-list member opens the origin of a recipe that came from someone else's custom recipe,
  THEN they see the snapshot. They can "Salvar nas minhas receitas" to get their own copy, but
  can't edit the owner's.
- IF a member without permission somehow reaches a list (shared-list edge), THEN the save fails
  through the normal rules, and the partial-failure path (P1 AC6) reports it. No special handling.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| RTL-01 | P1: Recipe → reviewed checklist → list (catalog) | Spec | Pending (catalog part blocked on Q1/Q4) |
| RTL-02 | P2: Paste recipe → AI extraction (Pro, 1 free) | Spec | Pending (blocked on Q1, Q2, Gemini key) |
| RTL-03 | P3: List remembers its recipe(s) | Spec | Pending |
| RTL-04 | P4: One pipeline, consistent gating | Spec | Pending (blocked on Q3) |
| RTL-05 | Prerequisite fixes G23 (silent partial failure) + G24 (clean names / duplicate check) + G27 (AI batch-add authorship) | Spec | Pending, can start before Q1–Q6 resolve |
| RTL-06 | P5: My own recipes (save, manage, reuse; Free). Delivers FR-05 #2's data/reuse half | Spec | Pending (not blocked on Q1/Q2. Q6 confirms the FR-05 #2 merge, not the build) |

**ID format:** `RTL-[NUMBER]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

## Does this need `design.md` / `tasks.md`?

**Yes to both. Not optional here, unlike a pure UI or single-module change.** Reasons:

- A **schema change** on `Shopping` (origins) needs a `firestore.rules` check against AD-009's
  update shapes (members-only update with `affectedKeys` constraints on join/leave paths), plus
  mapper/model work and Wear compatibility (`:wear` reads `Shopping`).
- **Catalog storage** (bundled asset vs. Remote Config vs. Firestore) and its update story is a real
  architecture choice for the tech-lead.
- **Gate placement** touches three surfaces (UI gate VM, agent chokepoint, App Functions), plus the
  trial-flag storage (`AiTrialRepository` key or new).
- **Extraction call** must reuse the provider factory/AD-008 key path without going through the
  tool-calling agent. The design has to say exactly which seam does that.
- P5 adds a **new Firestore collection** (`users/{uid}/recipes`): rules plus emulator assertions in
  `firestore-tests/rules.test.mjs`, the account-deletion change, a cross-module navigation route
  (Profile → products editor), and the doc shape/size caps.
- The work splits into at least six independently mergeable PRs (G23/G24/G27 fixes → P1 → P5 → P3 →
  P4 cleanup → P2), which needs a `tasks.md` PR plan like `item-add-authorship`'s. P5 sits right
  after P1 because it reuses P1's checklist and needs no external decision. P3 follows so origins
  can snapshot all three sources from the start.

Sequencing: `design.md` can start on RTL-01/03/05/06 now. RTL-02/04 sections wait for bruno's Q1/Q2
and tech-lead's Q3/Q5. The G23/G24/G27 fixes don't need to wait for design. They are pre-existing
bugs in shipped code.

---

## Success Criteria

- [ ] Every item added from a recipe has an `ADDED` history entry and a name with no measure in it.
      Duplicates of active items are flagged before adding (unit tests on the batch use case).
- [ ] A Free user can go from add sheet → recipe → items on the list in ≤ 4 taps without seeing a
      paywall.
- [ ] No surface reports success on a partial add (test with one failing save).
- [ ] With Q3 default: zero TheMealDB requests and zero recipe-translation Gemini calls in a build.
      Privacy/terms no longer mention either.
- [ ] A custom recipe saved on one device is usable on another. Deleting the account leaves no
      document under `users/{uid}/recipes` (emulator test).
- [ ] No Free-tier limit on custom recipes other than the technical cap (test: 4th, 10th, 50th
      recipe all save on a FREE account).
- [ ] Analytics (with `data-engineer`): `product_added{source=recipe_catalog|recipe_paste|recipe_custom}` and a
      recipe-add summary event fire. The dead `source=recipe` entry in `ANALYTICS-PLAN.md` is
      replaced.
- [ ] Product hypothesis to read 4 weeks after release, **not a launch gate**: among new lists whose
      first add comes from a recipe, the share that reaches a 2nd add session is higher than the
      app-wide baseline. This feeds the list-created → first-product-add drop (the app's biggest
      activation leak), without claiming to fix it.

---

## Persona check (customer-personas skill)

| Persona | Verdict | Why |
|---|---|---|
| **Yasmin** (primary) | **Serve** | P3 (origin) is exactly her pain, and it's free. P2 (paste) is her real source. Trade-off made consciously: automatic extraction is Pro after one try. A Free Yasmin can still save the pasted recipe as the list's origin, tick catalog dishes, or type it once as her own recipe (P5) and reuse it free. |
| **Camila e Pedro** | **Serve** | The free PT-BR catalog of everyday dishes *is* the "ponto de partida" they lack. This is why the catalog must be Brazilian, not TheMealDB. P5 lets them grow it into *their* repertoire ("catalog copy → edit"). |
| **Juliana** | Serve (light) | Non-typing path to a list (tap a dish, untick, add). P5 requires typing once, then never again for that recipe. |
| **Dona Marlene** | Neutral → serve | The checklist with "já está na lista" avoids duplicate and excess buying. A dump-everything add (today's code) would have **atrapalhado**. |
| **Marina** | Neutral | Opt-in shortcut. The total stays visible after the add. Adds no step to her default flow. |
| **Dona Célia** | Neutral, **conditional** | Only because it's a secondary shortcut and the form stays default. It would atrapalhar as a tab, or if she hit a paywall without a clear "Pro" label on the paste option. |
| **Lucas, Bianca e Diego** | Neutral | Authorship and duplicate flags apply to recipe adds. Origins are visible to all members. (List sharing itself is Pro today. Unchanged.) |
| **Rodrigo** | Serve (partial) with P5, otherwise neutral | P5 stores exactly his fixed checklist ("Marmitas da semana", 25 items), reusable monthly with no paper. It's only partial: he has to think of it as a "receita", and there's no "save this list as…" or "new list from…" yet (deferred, Q6). Count him as served only once P5 usage shows template-shaped recipes. |
| **Anderson** | Serve (light) with P5 | A saved "Insumos pizzaria" set re-added each week cuts retyping, which is exactly the double-typing he rejects. Spend consolidation (his main pain) isn't touched. |
| **Dona Célia** (P5 specifically) | Neutral | The editor is a name plus one ingredient field with Enter-to-add. Profile gains one row. Nothing changes unless she goes looking. |
| **Rafael, Eduardo** | Neutral | Bulk/business/unit-price needs aren't touched. Clean names help future price history, nothing more. |

No persona is made worse **given** the entry point and review-step decisions above. Both are
load-bearing for that verdict.
