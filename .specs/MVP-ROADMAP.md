# MVP Roadmap — Cestou (How-Much)

Status: Phases 0–2 mostly done — see gap list for what's still open
Owner: bruno
Last updated: 2026-09-09 — not re-verified since. See `.specs/BETA-LAUNCH-PLAN.md` revision 4
(2026-09-21) for a material correction: the beta is already live (950 Open-testing users), and G3/G4
(remainder) are closed, not blocked, contradicting anything below that still lists them as open.

Note: the project runs on Firebase's free Spark plan (no billing account) — Cloud Functions
require the Blaze plan even for free-tier usage, so nothing in this plan should depend on them.
Any "server-side" behavior has to be either client-side (Firestore writes from the app, gated by
security rules) or skipped.

## Why this document exists

The core shopping/products/settings/AI features are already built and marked "Verified" in
`.specs/features/*/spec.md`. What's actually blocking an MVP release is not new core
functionality — it's a short list of launch-readiness and compliance gaps, plus landing a very
large branch. This doc lists those gaps and breaks the work into features a user (or Google Play)
would notice, so each one can be picked up, spec'd with the `tlc-spec-driven` skill if needed, and
shipped independently.

## Current state snapshot

- **G1 is resolved.** `feat/new-layout` merged into `develop` via PR #30 — the entire multi-module
  Clean Architecture rewrite (AD-001–AD-007), Wear OS support, and the AI agent are now on
  `develop`. The CI pipeline was also rebuilt around Git Flow with staged checks (PR #47).
  `develop` is the live integration branch; feature branches PR into it, and `develop` → `master`
  is a decision the repo owner makes directly.
- Google Sign-In, product scanning/AI, list sharing, and settings are functionally real (verified
  by reading the code, not just the specs) — they are not on this list.
- A 2026-09-09 audit (this session) reviewed ViewModels, repositories, and Compose screens for
  common bug patterns (coroutine leaks, unguarded `!!`, listener cleanup); see "Bug audit" at the
  end of this doc for findings.

## Gap list

| # | Gap | Why it matters | Est. effort |
|---|---|---|---|
| ~~G1~~ | ~~`feat/new-layout` → `develop` not merged~~ | Done — PR #30. `develop` now carries the full multi-module rewrite. | M–L |
| ~~G2~~ | ~~No in-app account + data deletion~~ | Done — PR #18 | S–M |
| ~~G3~~ | ~~Privacy Policy / Terms only in-app, no hosted URL~~ | Pages drafted — PR #21. Hosting decision + wiring the URL still needs you. | S |
| ~~G4~~ | ~~Store listing assets missing~~ | Descriptions drafted (en/pt-BR/es) — PR #22. Screenshots/feature graphic still need a device. | S |
| ~~G5~~ | ~~Collaboration notifications have no writer~~ | Done, client-side (no Cloud Functions) — PR #20. Firestore rules need a manual check, noted in the PR. | M |
| ~~G6~~ | ~~`lint-rules/` module has uncommitted deleted files~~ | Done — orphaned index state from an abandoned attempt, unstaged. | S |
| ~~G7~~ | ~~Dead `signInWithGoogle`/`signInWithApple`~~ | Done — PR #19 | S |
| ~~G8~~ | ~~Apple Sign-In never offered~~ | Removed the unreachable UI branch rather than implementing it — PR #19 | S |
| G9 | `ShoppingRepositoryImpl.updatePositions` is a no-op (`ShoppingRepositoryImpl.kt:133-139`) — reordering lists by drag updates local state optimistically (`ShoppingListViewModel.kt:269`) but never writes to Firestore, so the order silently reverts on next sync | Feature is already exposed in the UI and looks like it works; found during a 2026-09-04 Tech Lead audit | **Fix pushed, PR #67 open — but not mergeable as of 2026-09-10: Detekt fails on one >120-char line in `ShoppingRepositoryImplTest.kt:220` (so `PR Gate` is red) and the branch is 18 commits behind `develop`. Tracked as T7 in `BETA-LAUNCH-PLAN.md`.** |
| G10 | Cross-feature module coupling: `cart`/`shopping`/`chat`/`ai-agent`/`profile` import `feature.settings` directly, `cart` imports `feature.chat`/`feature.products`, `products` imports `feature.chat`, `shopping` imports `feature.products` | Violates AD-005 (feature modules should only expose a `navigation` entry point); makes each feature module's real dependency graph wider than documented, raising the risk of accidental coupling as the app grows | M — needs a design pass (extract shared contracts to `core/*`), not a quick fix |
| G17 | Beta tester feedback (Felipe Morais, 1★, 07/09/2026, Play Console beta feedback channel): comment is just "difícil", no further detail — possible onboarding/first-use UX friction, root cause unconfirmed | Real signal from an external beta tester rather than an internal audit, but not actionable yet: no repro steps, no flow identified, no persona confidently mapped. See "Beta feedback triage" below for the follow-up plan and process | S (diagnostic) — escalates to a usability session or heuristic review only if he doesn't reply |
| G18 | **Play Store rejection 2026-10-06 ("Falta de credenciais de login", Open testing)**: app only accepts Google Sign-In, so reviewers cannot log in and there is no credential the app accepts. Resolution: phase 1 = e-mail/password login + required first/last name + initials avatar + review credential in Play Console; phase 2 (after Play approval) = phone/SMS. Spec: `.specs/features/email-phone-auth/spec.md` | Blocks publishing the update; second rejection for this reason (first: 2026-09-25, see STATE.md). Phase 1 is P0, above all Phase 3 and "Desirable" items. Phone/SMS (phase 2) may need the Firebase Blaze plan, deferred by bruno | M–L |
| G19 | **Paywall promises a benefit the entitlement system can't enforce.** `PaywallScreen.kt`'s 4th benefit ("Use o Cestou no seu relógio Wear OS") implies Wear access is part of the Pro unlock, but `ProfileSubscriptionViewModel.showWearLink` gates it purely on `WEAR_LOGIN_ENABLED` (a rollout flag, defaults OFF for everyone, Pro or Free, because the Wear companion app isn't published) — it has zero reference to `SubscriptionStatus`. When that flag eventually flips, Wear pairing becomes free for everyone, contradicting what the Paywall sold. Found while formalizing `.specs/features/pro-entitlement/spec.md` (FR-04); the other 3 promised benefits (unlimited chat, share list, QR invite) **are** correctly wired to `SubscriptionRepository` and just idle behind `BILLING_ENABLED`. | Not a launch blocker by itself, but must be fixed *before* `BILLING_ENABLED` is flipped on (see pro-entitlement spec's Rollout Plan) — flipping the flag while this copy is live means real paying users are sold a feature the code structurally cannot gate for them. | S — remove the one string/benefit-list entry now; revisit "Wear as a Pro perk" as its own decision once the companion app ships |
| G20 | **The home-list share action is not Pro-gated, contradicting FR-02's "Implemented" status.** Long-press on a list row in `ShoppingScreen` → "Compartilhar" (`ShoppingItem.kt:156-160` → `ShoppingListViewModel` `ShoppingAction.Share` → `shareShoppingUseCase(...)`, lines 190-197) calls the share use case directly, with no `SubscriptionRepository` check. Only the cart's share (`CartGraph.kt:69-75`, `CartSubscriptionGateViewModel`) routes Free users to the Paywall. Found 2026-10-26 while grounding `.specs/features/purchase-history-export/spec.md`. | Once `BILLING_ENABLED` flips, a Free user can still share from home, so the Paywall's "Compartilhe listas" benefit is bypassable. Same class of promise-vs-code gap as G19. Must be resolved (gate it, or decide on purpose that home-share stays free, which also bears on the open "should sharing be Pro at all" question in the monetization section) **before** the flag flip. Also: the pro-entitlement spec's claim that share/QR/chat "are correctly wired" is only true for the cart path. | S — **Resolution decided 2026-10-26 (tech-lead, AD-013); bruno confirmed it should be gated, controlled by its own micro-flag.** Gate it as capability `ProFeature.SHARE_LIST`, flag `PRO_GATE_SHARE_LIST_ENABLED` (`pro_gate_share_list_enabled`, default off). That's the same flag that gates "share as text" in the cart sheet, because it's the same capability. `ShoppingListViewModel.kt` does **not** gain an entitlement dependency (gate VMs stay separate, mobile-only). The change is in `ShoppingGraph.kt`'s `composable<ShoppingList>`: pass `viewModel.intent.copy(onShare = { s -> if (canShareList) viewModel.intent.onShare(s) else navigator.navigate(Paywall(source = "shopping_list_share")) })`, with `canShareList` from `ShoppingSubscriptionGateViewModel` (`proFeatureAccess.observeAccess(SHARE_LIST)`). Tests: gate VM (Free/Pro x flag on/off) plus the intent wrapping, extracted as a small pure `internal` function so it's JVM-testable. **Depends on AD-013's PR (1)**, ships as its own branch/PR right after it. Owner `android-engineer-features`. |
| G21 | **Finished lists have no finish date, and one finish path records no paid total.** `Shopping` has only `createdAt` (no `finishedAt`), so any by-month history buckets by creation date. The home long-press "Finalizar" (`ShoppingScreen.kt:256-263`) sets only `status = FINISH`: `Shopping.price` stays `0.0` from `ShoppingCreateUseCase`, and `cart_finish_purchase_completed` never fires. Only the cart's `FinishPurchaseViewModel` records a confirmed total (and it overwrites `description` with the establishment). | Makes every spend number wrong for lists finished from home, and mis-buckets month-end purchases. Affects the **free** purchase-history surface the pro-entitlement scope boundary promises, not just Pro export. Also undercounts finish events in GA4. Addressed as a prerequisite in `.specs/features/purchase-history-export/spec.md` (PHE-01: `finishedAt` + read-time total fallback + analytics). Whether long-press finish should open the confirm-total sheet is a separate UX call for bruno/designer. | S–M — **Resolution decided 2026-10-26 (tech-lead, AD-014 parts 1-2); bruno confirmed long-press must open the same confirm-total sheet.** (a) `Shopping.finishedAt: Long? = null` (last param), written only through the pure `finished(...)`/`reopened()` helpers; duplicate resets it; readers trust it only when `status == FINISH`. (b) `FinishPurchaseRoute` becomes public `FinishPurchase(shoppingId, source)` in `core/navigation/mobile`, registered top-level by `feature:cart`, self-loading (no `CartViewModel` parent entry). Finish logic moves into `feature/cart` `FinishShoppingUseCase`, and the home long-press navigates to it (`source = shopping_list_long_press`). No feature->feature import (G10-checked, `KNOWN_FEATURE_TO_FEATURE` stays empty). Two PRs: model+helpers first, then the shared route + long-press. |
| G22 | **AI `update_shopping_list` overwrites the whole list document.** `ShoppingUpdateUseCase.execute` builds a brand-new `Shopping(...)` from the agent arguments, so every field it doesn't pass reverts to its default: `createdAt = now`, `budget/shortCode = null`, `emoji`, `position`, `isFavorite`, `isCategorized`, and (after AD-014) `finishedAt = null`. That `Shopping` is then PUT as the full document. It can also set `status = FINISH` with no total or date (a third finish path). Found 2026-10-26 by tech-lead while reviewing G21's finish paths. | Silent data loss on any AI-driven list edit (wrong month bucketing, lost budget, lost share code). For a non-owner, `shortCode -> null` also violates AD-009 rule (b), so the write fails once rules deploy. Not a beta blocker by itself (AI chat is behind `AI_CHAT_ENABLED`, default off), but must be fixed before that flag ramps. | S — `execute` should `getById` and `copy(...)` only the provided arguments. Route `status = FINISH` through AD-014's `finished(...)` (or reject it and let the user confirm in the sheet). Own branch/PR, `android-engineer-features`. |
| G23 | `RecipeAddToListUseCase.invoke` (`feature/products/.../domain/usecase/RecipeAddToListUseCase.kt:54-63`) wraps a `forEach { saveUseCase(...) }` in `runCatching` but ignores each returned `Result`, so a failed save is swallowed and the whole call reports success. `RecipeAddToListUseCaseTest` uses a `relaxed` mock for `ProductSaveUseCase`, so no test covers a failing save | Found during `recipe-to-list` planning (2026-10-26). Reachable today through the AI chat (`add_recipe_ingredients`) and App Functions (`addRecipeIngredientsToList`): the assistant can say "ingredientes adicionados" when none were. Required by RTL-05 | S — **Resolution decided 2026-10-26 (tech-lead, AD-015): don't patch it, delete it.** The use case and both surfaces that reach it are removed by the AD-015 PR. Its replacement, `ProductBatchAddUseCase` (G27 PR), reports per-item `saved`/`failed`, with a failing-save test required. Also needs **G29**, otherwise the chat path still hears "Sucesso". See `recipe-to-list/design.md` |
| G24 | TheMealDB ingredient mapping (`RecipeRepositoryImpl.kt:107-124`) bakes the cooking measure into the item name (`"2 cups Flour"` → item "2 xícaras Farinha", qty 1.0), and the recipe path calls `ProductSaveUseCase.invoke`, which skips `ProductDuplicateCheckUseCase` (only `execute` and `ProductFormViewModel` run it) | Breaks IAA-02 for every recipe-sourced item (the name never matches "Farinha"), plus category suggestion and any future by-name price history. Spec `recipe-to-list` requires clean names and a duplicate check on this path | S–M — **Resolution decided 2026-10-26 (tech-lead, AD-015/AD-016).** The naming half disappears with the TheMealDB mapping (AD-015 PR). The new model keeps `name` clean and puts the measure in a display-only `note`. The duplicate half moves into the P1 checklist: one `getAllProducts().first()` read, active matches unticked (P1 AC3). The agent batch wrappers do the same one-read check (G27 PR) |
| G25 | TheMealDB is a poor fit for this user base and may not be license-clean: PT-BR queries go raw to an English name search (`search.php?s=`), so "frango"/"bolo" return nothing. One Gemini translation call per result (unbounded fan-out). If the AD-008 key is invalid (photo-scan fallback verified failing on device with `InvalidAPIKeyException`), `translateToPortuguese` silently returns English. Production uses the public test key `1` | Full evaluation in `.specs/features/recipe-to-list/spec.md` § "Data source decision". **bruno confirmed Q1 (2026-10-26):** retire TheMealDB for a curated PT-BR catalog plus paste-text AI extraction | M — **Resolution decided 2026-10-26 (tech-lead, AD-015): full removal, first `recipe-to-list` PR.** Agent actions, App Functions, repository, translation, `app_metadata.xml` text, privacy/terms (pt/en/es) and the Maestro "Recipes" steps all go. The license question goes away with the code. Exact file list in `recipe-to-list/design.md` § "Q3 cleanup". Owed by bruno: confirm the published `ai_system_prompt` doesn't name `search_recipes`/`add_recipe_ingredients` |
| G26 | `BaseCestouAppFunctionService` (`app/.../appfunctions/CestouAppFunctions.kt`) calls agent use cases directly, bypassing the AD-010 dispatch chokepoint (`GeminiAiAgent`/`OpenRouterAiAgent.executeFunctionCall`) | No live leak today, since no action sets `requiresPro` yet. It becomes one the moment any action is gated: the system assistant would run Pro actions for Free users. Must be closed before any `requiresPro = true` ships (`recipe-to-list` RTL-04) | S — **Resolution decided 2026-10-26 (tech-lead, AD-015): closed by policy, no code now.** After the AD-015 PR, the service exposes only Free capabilities (create/list lists, search products, save product). Standing rule: an App Function may only wrap a non-`ProFeature` capability, or must check `ProFeatureAccess` itself. The AD-013 point-8 PR (first `proFeature` on an `AgentAction`) must re-audit the service. P2 extraction is not exposed there |
| G27 | AI batch-add paths skip authorship: `ProductListSaveUseCase` (`add_multiple_products_to_cart`) and `CommonProductAddAllToShoppingUseCase` (`add_common_products_to_cart`) call `ProductRepository.saveProduct` directly, with no `withActivity(ADDED, …)` and no duplicate check | Violates IAA-01 AC1 ("any add path"): items added by the AI in bulk, or via "add all common products", have empty `history`, so no avatar and no "who added" on shared lists. `recipe-to-list` RTL-04 depends on this for the chat path | S — **Resolution decided 2026-10-26 (tech-lead, design.md).** Introduce the shared `internal ProductBatchAddUseCase(items, shoppingId, userId, source)` (per-item `saved`/`failed`, `product_added{source}` per item) plus a `ProductSaveUseCase.invoke(product, shoppingId, userId)` overload. Both agent actions delegate with `session.userId` and return an honest partial-success string. That's the same core P1/P2/P5 use. Own branch/PR right after the AD-015 PR. Owner `android-engineer-features`, tests by `android-engineer-quality` |
| G28 | Spec/plan drift: `item-add-authorship` IAA-03 (Quick Add default + option selector keeping AI/Search/Photo/Suggestions reachable) no longer matches code. `ProductScreen` is `FORM` + `PHOTO` only, and `QuickAddViewModel` is gone. `ANALYTICS-PLAN.md` §4 still lists `product_added{source=recipe\|quick_add\|common_product}`, but no code emits `"recipe"` or `"quick_add"` (the form emits `"form"`) | Doc-honesty gap, not a user bug. Anyone reading IAA-03 or the analytics plan will assume surfaces and funnel slices that don't exist. `tech-lead` to decide whether IAA-03 is formally superseded, and `data-engineer` to correct the source list | S (docs) — **tech-lead half decided 2026-10-26: IAA-03 is superseded** by the shipped `FORM` (default) + camera shortcut shape. `recipe-to-list` P1 adds the recipe shortcut as the 3rd surface, and that's the add sheet's design of record now (marked in `item-add-authorship/spec.md`). `data-engineer` half is still open: replace `recipe`/`quick_add` with the real sources (`form`, `photo_scan`, `ai_chat`, plus `recipe_catalog\|recipe_paste\|recipe_custom` when P1/P2/P5 ship). Best done in the P1 PR. Also stale: `.maestro/flows/product_management_flow.yaml:29-32` asserts a "Recipes" tab (removed in the AD-015 PR) and `ProductDuplicateCheckUseCase`'s KDoc still names `QuickAddViewModel` |
| G29 | **The AI dispatch chokepoint reports failed actions as success.** `GeminiAiAgent.executeFunctionCall` (`feature/ai-agent/.../GeminiAiAgent.kt:134-142`) and `OpenRouterAiAgent.executeToolCall` (`OpenRouterAiAgent.kt:176-181`) both do `action.execute(...).getOrNull()?.toString() ?: "Sucesso"`, so every action that returns `Result.failure` instead of throwing (e.g. `ProductListSaveUseCase` on a failed save, `ProductSaveUseCase.execute` on a missing name or list) tells the model "Sucesso". Only thrown exceptions reach the error branch. Found 2026-10-26 by tech-lead during the `recipe-to-list` architecture pass | This is G23's symptom for every agent action, not just recipes. After AD-015, the chat's "build a list from a recipe" path is `add_multiple_products_to_cart`, so the assistant can still claim items were added when none were. Blocks `recipe-to-list`'s "no surface reports success on a partial add" success criterion for the chat path | S — map `Result.failure` to `"Erro na execução da ação: ${it.message}"` (same text as the catch branch) in both agents. Extend the existing `*GatingTest`s with a failing-action case. Own branch/PR in `feature/ai-agent`, independent of the G27 PR, `android-engineer-features` |

Every item marked done above shipped as its own branch + PR (none merged without review): shared
`StorageService` for `core/auth` (#14), shopping-reminder push notifications (#15), Maestro
regression suite (#16), this doc (#17), account & data deletion (#18), dead social-auth cleanup
(#19), collaboration notifications (#20), hosted legal pages (#21), store listing descriptions
(#22), `feat/new-layout` → `develop` merge (#30), CI rebuild around Git Flow (#47),
`updatePositions` fix (#67, open).

**Still genuinely open, none of them fixable from this environment:**
- **F0.3 / F2.2** — no adb/emulator here, so the Maestro suite and the Google Sign-In flow have
  never actually run. This session added `account_data_flow.yaml` to the suite and fixed the
  `AiChatScreen` settings-icon accessibility gap (F3.4), but still needs a device to execute.
- Screenshots/feature graphic (part of G4) need a device to capture.
- The privacy/terms pages (G3) need a hosting decision before the in-app links can point anywhere.
- **G5 Firestore rules** are no longer unwritten *or* outside the repo: a full rule set covering
  every collection the app uses now lives in `firestore.rules` (PR #84), reasoned through in AD-009
  (`.specs/STATE.md`) and checked by 60 emulator assertions. Production still runs the console
  placeholder, so what's left is yours: review, then
  `firebase deploy --only firestore:rules --project how-much-2a72e`. Read AD-009's "known breakage on
  deploy" first — as written the rules stop join-by-short-code and all Wear OS traffic until the
  client catches up.
- **G9** needs your review/merge of PR #67 — don't duplicate the fix. T7's Detekt fix and the
  18-commit branch update have already landed on the PR; it is green and up to date with `develop`
  again as of 2026-09-10. Merging is still exclusively yours.
- **G10** needs a design decision (which shared contracts move to `core/*`) before it's worth
  spec'ing as its own initiative; flagged here so it doesn't silently grow.

## Beta Launch Priority (owned by `pm`)

Cross-checked against the 12 personas in `.specs/PERSONA-ACTION-PLAN.md` /
`.claude/skills/customer-personas/SKILL.md`. `.specs/BETA-LAUNCH-PLAN.md` (tech-lead's document)
sequences *how* the remaining work gets done and by whom; this section defines *what actually gates*
inviting real customers into the first closed beta vs. what can land after they're already in. A
"beta blocker" here means: a real beta tester hits it on a first-use happy path (login, add a
product, share/join a list, finish a purchase) and it reads as broken, unsafe, or non-compliant — not
just "not yet perfect."

### Beta blocker — must close before inviting real customers

| Item | Persona(s) & dor central afetada | Why it blocks | Status / owner |
|---|---|---|---|
| **G3** — hosted legal pages | Todas — mas sobretudo Dona Célia ("rejeita qualquer feature que exija entender um conceito novo") e Marina (sem paciência para passo extra); um link morto de Termos/Privacidade é a primeira "prova de amadorismo" que qualquer uma delas encontra. | Play Store submission requirement; hoje os links de Privacidade/Termos no app não levam a lugar nenhum. | Needs bruno's hosting decision (`cestou.app` vs. GitHub Pages) |
| **G4 (remainder)** — screenshots/feature graphic | Todas — pré-condição para a listagem existir na Play Store; nenhuma persona chega ao app sem isso. | Play Console requires these to publish even a closed testing track — no listing, no beta. | Needs a device |
| **G5** — Firestore rules for `notifications` writes | Lucas (depende do token de compartilhamento funcionar com integridade), Bianca-e-Diego (lista viva compartilhada) — regra não verificada permite escrever notificação "de" um usuário "para" outro sem checagem. | Primeira fronteira real de confiança que o app cruza com dados de pessoas reais, não dados de teste. Barato de checar, alto downside se errado. | Rules written and emulator-tested — PR #84 (`firestore.rules`, AD-009), still needs bruno's review + manual deploy; the proposal also breaks join-by-short-code and Wear until two client follow-ups land |
| **G9** — `updatePositions` no-op | Dona Marlene ("lista de compras confiável do que realmente falta", rejeita "fluxo que dificulte"), Bianca-e-Diego ("lista viva... sem depender de lembrar de cabeça") — reordenar e ver a lista voltar sozinha é exatamente o tipo de falha que quebra a confiança que essas duas personas mais dependem. | Drag-to-reorder já é uma affordance visível e shipada; reverter silenciosamente no próximo sync lê como perda de dado. Fix pronto e testado (PR #67); é decisão de merge, não trabalho novo. | Needs bruno to review/merge PR #67 |
| **G13** — QR-join scanner has no debounce, spams duplicate notifications | Lucas ("rejeita qualquer feature que só funcione bem com um único usuário"), Bianca-e-Diego ("rejeitam qualquer solução que dependa de um canal separado para ficarem alinhados") — é o ponto de entrada real dessas duas personas na feature-headline do app (carrinho compartilhado). | Segurar o telefone parado sobre um código — comportamento normal — já spamma todo membro no primeiro join. Reprodução quase garantida num fluxo central, não um edge case. | `android-engineer-features` |
| **G15** — Gemini API key compiled into the APK, no rotation path | Não é uma dor de persona específica (é risco de negócio/segurança), mas afeta indiretamente todas as personas que usam IA/scanner (Marina, Dona Marlene, Juliana*, Camila-e-Pedro) se a chave for abusada e o provedor cortar o serviço por excesso de uso. | Chave embutida no APK vira superfície de abuso real assim que o build sai de mãos internas para os dispositivos dos beta testers. Fix já em andamento em `fix/gemini-key-remote-config` (`ProductRepositoryImpl`, `AiAgentFactoryImpl` já leem do Remote Config com a chave compilada como fallback) — quase pronto. | `android-engineer-features`, PR in progress |
| **F0.3** — Maestro suite has never executed on a device | Todas — cobre compra/compartilhamento/login, os fluxos que qualquer persona percorre primeiro. | Um beta com clientes reais é o lugar errado para descobrir que o próprio caminho feliz está quebrado. | Needs bruno, device |
| **F2.2** — Google Sign-In QA pass on a device | Todas, mas Dona Célia é o caso extremo: baixíssima paciência para fricção — se o login falha, ela simplesmente abandona o app, sem tentar de novo. | Login é a porta de entrada literal; config verificada estaticamente, mas o fluxo ao vivo (SHA-1, tela de consentimento OAuth) nunca rodou. Se isso quebra, não há beta. | Needs bruno, device |
| Analytics on key flows (create list, add product, scan, share/join, finish purchase, set budget) | Indireta a todas — sem esses eventos não dá para saber, por persona, onde o beta cohort está travando (ex.: Dona Célia abandona no cadastro? Lucas nunca usa o token?). | Sem eventos chegando ao Firebase não há como observar o que os beta testers realmente fazem — medir é o próprio motivo de rodar um beta em vez de só lançar. | `data-engineer` (see `BETA-LAUNCH-PLAN.md`) |
| Regression coverage on purchase/share/login paths | Todas — mesma lógica do F0.3/F2.2 na camada de teste automatizado. | Não deixar clientes reais serem os primeiros a exercitar esses caminhos de ponta a ponta. | `android-engineer-quality` |

\* Juliana é a persona associada à captura por foto (item 6 do `PERSONA-ACTION-PLAN.md`, ainda em fase de spike) — hoje ela também depende do mesmo `GeminiAiAgent` para qualquer interação de texto com a IA, por isso entra na lista de afetadas indiretamente por G15.

### Desirable, not a beta blocker — fix soon, doesn't gate go/no-go

| Item | Persona(s) & dor central | Why it doesn't block | Owner |
|---|---|---|---|
| **G12** — `CartViewModel` duplicate Flow collectors | Avaliado contra as 12 personas (skill `customer-personas`): **neutro** para todas no horizonte de um beta curto. Marina e Dona Marlene só se importam com o total estar certo ("total sempre correto e visível") — o bug degrada performance/gera leituras redundantes no Firestore, mas não corrompe o total mostrado. Gatilho exige trocar configurações repetidamente, comportamento atípico de um beta tester. | Não corrompe o valor que a persona realmente precisa confiar; baixa probabilidade de disparo num beta pequeno e curto. Monitorar via Crashlytics; corrigir cedo na janela do beta, sem travar o go/no-go. | `android-engineer-features` |
| **G14** — `ProfileViewModel` discards the Firestore profile emission | Avaliado contra as 12 personas: **neutro** para todas — nenhuma delas descreve uso multi-dispositivo simultâneo; todas operam "um celular na mão". | Só afeta um perfil editado num *segundo* dispositivo chegando à UI deste dispositivo — edge case de multi-device, não o fluxo primário de nenhuma das 12 personas. | `android-engineer-features` |
| **G16** — `ProductSearchViewModel` has no debounce/cancellation | Avaliado contra as 12 personas: caso mais duvidoso do lote. Dona Célia (rejeita telas confusas) e Eduardo (rejeita qualquer fluxo impreciso) são os mais sensíveis a resultado errado na tela — mas o usuário ainda confirma explicitamente o item antes de adicioná-lo à lista, então o pior caso é um "flicker", não um item errado persistido sem confirmação. Verdict: **neutro-a-levemente-atrapalha**, não **atrapalha** o suficiente para travar o beta. | Resultado obsoleto só afeta o dropdown antes do toque de confirmação — incômodo, não corrupção silenciosa de uma lista persistida — e exige digitação rápida + rede lenta para disparar. Fix barato, vale fazer logo, só não é um gate. | `android-engineer-features` |
| Beta/store readiness details beyond assets (release notes, disclaimer copy, feedback channel) | Indireta a todas — melhora a experiência de convite/onboarding do beta, não o produto em si. | Necessário antes de *convidar* testers, mas pode terminar em paralelo com o último bloqueador de código. | `marketing` |

### Post-beta (Phase 3 / architecture debt)

| Item | Persona(s) & dor central | Why it's post-beta | Owner |
|---|---|---|---|
| **G10** — cross-feature module coupling | Nenhuma — checado contra as 12 personas do skill, nenhum "o que valoriza"/"o que rejeita" toca fronteira de módulo. Débito puramente interno. | Zero diferença observável para qualquer persona. | `tech-lead` design pass, no rush |
| **F3.1** biometric app-lock, **F3.2** `StorageService` adoption in settings, **F3.3** branded notification icon, **F3.5** G10 implementation | Nenhuma persona lista "trava por biometria" ou "ícone com marca" como algo que valoriza; as personas mais próximas de segurança/confiança (Dona Marlene, Rafael) pedem precisão de orçamento e histórico confiável, não trava de dispositivo. | Polish genuíno; retomar depois que feedback real do beta pedir, em vez de supor agora. | Post-beta backlog |

### Explicitly out of scope for this beta priority pass

The Persona Action Plan's "Agora" items (`item-add-authorship`, `item-row-affordances`,
`recipe-list-origin`) are **product evolution, not beta blockers** — they make the app better for
personas who can already use it end to end today, not a precondition for a first small beta cohort to
complete the core loop (sign in → build a list → shop/share → finish). This matches
`.specs/PERSONA-ACTION-PLAN.md`'s own framing as a companion doc, not a launch-blocker doc. Don't pull
these into beta scope without a deliberate call from bruno.

### Discrepancy flagged for tech-lead

`.specs/BETA-LAUNCH-PLAN.md`'s "Definition of ready for beta" checklist currently lists "G12–G16
closed" as one flat requirement. This PM pass disagrees with holding all five to the same bar: G13 and
G15 read as genuine blockers (headline flow, near-certain reproduction / real security exposure to
external testers), while G12/G14/G16 read as desirable-not-blocking (edge-case trigger conditions, no
data corruption, low tester-visible frequency for a small closed cohort). Flagging for the tech-lead to
either split that checklist line or state explicitly why all five should be held to the same bar before
beta — not deciding it here, since `BETA-LAUNCH-PLAN.md` is the tech-lead's document.

### Gap not tracked

None found in this pass. The dead-end legal links, the QR-join notification spam, and the compiled-in
API key — the three items with the clearest persona-facing "this feels broken/unsafe" angle — are
already tracked as G3, G13, and G15 respectively.

## Monetization (owned by `pm`, 2026-10-23)

Separate from beta-launch gating — this is about `BILLING_ENABLED` (AD-010's kill switch, see
`core/billing/.../PlayBillingSubscriptionRepository.kt`), which has defaulted OFF since it shipped,
so every user reads as `PRO` today regardless of real purchase state. Full decision record, chat
free-message-limit analysis, Pro-feature priority list, and rollout plan: **`.specs/features/
pro-entitlement/spec.md`**. Headline points for anyone scanning this roadmap instead of that spec:

- Three of the four benefits already advertised on the Paywall (unlimited AI chat, share list, QR
  invite) are **fully implemented and wired to `SubscriptionRepository`** — they're idle only
  because of the flag, not missing code. The fourth (Wear OS) is a real mismatch — see **G19**
  above.
- There are real paying users today getting zero differentiation from Free users. Flipping the
  flag is treated as urgent (this sprint), gated on: fixing G19's copy, a device QA pass
  (`android-engineer-quality`) since these gates have never run with the flag actually on, and
  closing a real analytics gap (`data-engineer`) — today only `paywall_subscribe_clicked` and
  `paywall_purchase_failed` exist, with no `paywall_viewed`, no purchase-success event, and no
  gate-blocked event, so the flip would otherwise be unmeasurable.
- The free-chat limit of 1 message (already implemented in `ChatAiGateViewModel`/
  `AiTrialRepository`) is kept as-is — the spec's analysis of the 1.16 messages/user organic
  average concludes it binds only the already most-engaged tail, not the typical user.
- "No ads for Free" (raised as an idea to explore, not a decision) is recommended **deferred**:
  no ad SDK exists in the repo today, the real user base is too small for the revenue to matter
  yet, and introducing ads now would add a second simultaneous variable on top of the already
  undiagnosed post-week-1 retention cliff.

## Monetization feature roadmap — competitive gap action plan (owned by `pm`, 2026-10-26)

Source: a competitive pass against Bring! (5M+ installs, BR market leader), Listonic, AnyList,
OurGroceries, Out of Milk, Samsung Food+, and the Brazilian state nota-fiscal price-comparison
apps (Menor Preço RS/PR/PE, Nota Paraná). None of these are `Gxx` bugs — they're new feature
candidates, sequenced here so each one can be picked up independently. This extends the
Pro-feature priority list already in `.specs/features/pro-entitlement/spec.md` (FR-05); it does
not reopen FR-01–04, which are already implemented and just idle behind `BILLING_ENABLED`.

**Open decision gate, not resolved here:** FR-02 (share list) and FR-03 (QR invite) being Pro is a
deliberate call already made, but it cuts against the dominant market pattern — Bring! and
OurGroceries keep sharing 100% free on purpose, because it's their viral growth loop. With the
real user base at 2 paying accounts and retention collapsing toward zero after week 1
(`project_auth_user_base_small.md`), paywalling the one action that creates network effects
(inviting someone else in) deserves a second look before any more Pro features are built on top of
that boundary. Doesn't block the table below — flagged so it isn't silently load-bearing forever.

**Workflow convention for every row below:** planning and implementation are split into separate
Claude Code sessions by model — `/model opus` for planning (spec/design only, no code), `/model
sonnet-5` for implementation (code + tests, one PR per feature, never bundling two rows into one
PR). Same "own branch, own PR into `develop`" discipline as the rest of this doc (see "Process"
at the end).

| Order | Feature | Free/Pro | Planning deliverable (`/model opus`) | Implementation (`/model sonnet-5`) | PR |
|---|---|---|---|---|---|
| 1 | Purchase-history export / month-end summary (pro-entitlement spec FR-05 #1) — extended 2026-10-26 with a free monthly spend-history screen (month cards + trend + purchases list); only the export action is Pro | History view **Free** / export **Pro** | `.specs/features/purchase-history-export/spec.md`: **spec written 2026-10-26** (PHE-01..03). Needs `design.md` + `tasks.md` before implementation. **Architecture unblocked 2026-10-26 by tech-lead**: AD-013 (export gate = `PRO_GATE_HISTORY_EXPORT_ENABLED`), AD-014 (`finishedAt`, shared finish sheet, `FileProvider` in `:app` + port in `feature/shopping`). See the spec's "Architecture addendum". Visuals still pending `product-designer` | Use case + screen + ViewModel/use-case tests | `feat(export): purchase history export behind Pro gate` |
| 2 | Recipe → list, all paths: curated catalog, paste-text AI extraction, and user-saved custom recipes — **corrected premise 2026-10-26**: the recipe code is already merged into `develop` (`feature/products/.../Recipe*.kt`), wired into AI chat + App Functions, just with no UI, not "unmerged in worktrees" | Catalog browse/add **Free**; paste-text AI extraction **Pro** (1 free try); custom saved recipes **Free**, no count cap (rejected the roadmap's old "browse free / auto-add Pro" placeholder as bait-and-switch — a shopping-list app's whole point is adding items) | `.specs/features/recipe-to-list/spec.md` — **spec written 2026-10-26** (RTL-01..06). Found G23-G28 while grounding it (recipe-add swallows failures, broken item names skip the duplicate check, TheMealDB likely silently serving English in prod via the already-known broken Gemini key, an App Functions path that bypasses the AD-010 Pro-gate chokepoint, AI batch-add paths with no authorship). **bruno confirmed Q1, Q2, Q4, Q6 (2026-10-26).** **Architecture unblocked 2026-10-26 by tech-lead**: Q3 → AD-015 (retire TheMealDB end to end, App Functions only expose non-Pro capabilities, which closes G26 by policy). Q5 → AD-016 (everything in `feature/products`; Profile and cart reach it through the `MyRecipes` / `ShoppingRecipeOrigins` routes in `core/navigation` and `RecipeOriginReader` in `core/domain`). Data model + rules → AD-017 (`users/{uid}/recipes`, and origins as the `shopping/{id}/recipe-origins` subcollection, **not** a field on `Shopping`). Paste extraction → AD-018 (tool-less `AiTextCompletion`). `design.md` **written** (rules text, deletion surface, components). New **G29** found. **Next: `tasks.md` (`sdd-coordinator`)**. P2 additionally waits for AD-013 PR (1) | Revised order (design.md): AD-015 retire → G27 (shared `ProductBatchAddUseCase`) → G29 (any order) → P1 (catalog) → P5 (custom recipes) → P3 (list origin) → `AiTextCompletion` → P2 (paste extraction, dark behind `recipe_paste_enabled`) | `refactor(recipes): retire TheMealDB`, `fix(products): ...` (G27), `fix(ai-agent): ...` (G29), then one PR per story |
| 3 | ~~Recurring list templates — demand validation only (pro-entitlement spec FR-05 #2, explicitly speculative)~~ | ~~N/A yet~~ | **Superseded 2026-10-26.** Row 2's spec found custom saved recipes and reusable list templates are the same data shape (a named, private, reusable item+quantity set) — building both would mean two editors/collections/rule-sets for one concept. Row 2's P5 (RTL-06) is now the only build; this row doesn't get its own implementation. **Needs bruno's explicit sign-off (Q6 above)**, since this flips FR-05 #2 from "Pro candidate, validate first" to "ships Free as part of row 2" without the validation signal FR-05 #2 asked for — pm's own words: "an owner decision isn't a customer signal." | — | — |
| 3b | ~~Recurring list templates — full build, only if 3 validates demand~~ | ~~Pro~~ | Superseded — see row 3. | — | — |
| 4 | Cross-store / unit-price comparison | **Free** — already decided in the pro-entitlement spec's scope boundary, do not reopen | Spike: is any real price-data source viable on the Spark plan (no Cloud Functions)? Brazilian nota-fiscal price APIs are state-scoped, not national — decide feasibility before committing to a build | Only after the spike approves it: implement + tests | `feat(products): cross-store price comparison (free)` — conditional on the spike |
| 5 | Home pantry/stock tracking + expiry alerts (precedent: Samsung Food+) | Pro | `.specs/features/pantry/spec.md` | Implement + tests | `feat(pantry): home stock tracking with expiry alerts (Pro)` |

Fixed rules across every row: tests are mandatory before a PR is considered done
(`android-engineer-quality` covers ViewModel/use-case/Compose per the existing pattern —
`CartBottomBarTest.kt`, `CartScreenAddProductTest.kt`); no `@Suppress` to silence lint/Detekt
findings; and nothing here ever paywalls budget, running total, purchase history, or price
comparison — that boundary from the pro-entitlement spec holds for every future candidate too.

## Beta feedback triage (owned by `pm`)

Processo, daqui pra frente: sempre que um comentário novo aparecer no canal "Feedback de teste" do
Google Play Console e for respondido, se ele apontar um problema real de UX/funcionalidade (não só
elogio), um gap correspondente entra na tabela do "Gap list" acima (próximo `Gxx` disponível),
cross-checado contra as 12 personas em `.specs/PERSONA-ACTION-PLAN.md` /
`.claude/skills/customer-personas/SKILL.md`, seguindo a mesma disciplina do resto deste documento.
Feedback só-elogio não precisa de gap — vira sinal de validação, registrado aqui por referência.

### 2026-09-21 — primeiros dois comentários de beta revisados

| Tester | Nota | Comentário | Dispositivo | Status |
|---|---|---|---|---|
| Felipe Morais | 1★ (07/09/2026) | "difícil" — sem mais detalhe | Galaxy A54 5G, Android 16, app 1.1.1 (build 3) | Rastreado como **G17** acima — respondemos pedindo detalhe, aguardando retorno |
| Mayra Santos | 5★ (02/10/2025) | "ótimo aplicativo, irei usar bastante 👏👏" | Galaxy M62, Android 13 | Sem ação necessária — sinal positivo de validação; respondemos agradecendo e convidando para sugestões |

**Detalhe do G17:** "difícil", sozinho, é um sinal real de fricção mas está subespecificado — pode
ser onboarding, criação de lista, scanner, compartilhamento, ou outra coisa. O skill
`customer-personas` não consegue mapear isso a uma dor de persona específica sem saber qual fluxo.
Não inventar causa raiz. Próximos passos, em ordem:

1. Aguardar a resposta dele ao nosso follow-up (já enviado) — janela de ~1-2 semanas (até
   ~2026-10-05).
2. Se não responder até lá, tratar como não resolvido e agendar ou (a) uma sessão de teste de
   usabilidade pequena (3-5 pessoas, priorizando personas com baixa tolerância a fricção — Dona
   Célia e Marina são as mais próximas do perfil "nota 1, sem paciência para explicar") ou (b) uma
   revisão heurística do fluxo de onboarding/primeiro uso (login → primeira lista → primeiro item),
   o que `pm`/`tech-lead` julgarem mais barato para o sinal disponível.
3. O que for encontrado vira um gap `Gxx` próprio, com "why it matters" concreto assim que houver um
   fluxo + reprodução real, substituindo este placeholder diagnóstico (G17).

## Plan — features broken by user value

### Phase 0 — Unblock (infra, no direct user value, but nothing ships without it)

- ~~**F0.1 — Merge `feat/new-layout` into `develop`.**~~ Done — PR #30.
- ~~**F0.2 — Resolve `lint-rules/` uncommitted state (G6).**~~ Done.
- **F0.3 — Run the Maestro suite (`maestro test .maestro/test_suite.yaml`) on a real device**,
  fix whatever selector drift shows up now that it's finally executable.

### Phase 1 — Play Store launch blockers (must-have)

- ~~**F1.1 — Account & data deletion (G2).**~~ Done — PR #18. `DeleteAccountUseCase` leaves/deletes
  the user's shopping lists, deletes their profile, clears local settings, then deletes the
  Firebase account, in that order. A recent login is checked first (`AuthService.hasRecentLogin()`);
  if stale, nothing is touched and the user is asked to sign in again (sign-out fallback for every
  provider, Google and e-mail alike; a late `FirebaseAuthRecentLoginRequiredException` takes the
  same path). Success ends on Welcome with a confirmation toast.
- ~~**F1.2 — Hosted Privacy Policy + Terms (G3).**~~ Pages drafted — PR #21
  (`docs/legal/privacy.html`, `terms.html`). Not linked from the app yet: needs a hosting decision
  (`cestou.app` vs. GitHub Pages) before wiring the URL into `CustomMethodPickerTerms`, Settings,
  and Play Console.
- ~~**F1.3 — Store listing assets (G4).**~~ Descriptions done for en/pt-BR/es — PR #22
  (`fastlane/metadata/android/`). Screenshots and the feature graphic still need a device.

### Phase 2 — Deliver on what the app already promises

- ~~**F2.1 — Wire real-time collaboration notifications (G5).**~~ Done, client-side — PR #20.
  `ShoppingJoinUseCase` and `FinishPurchaseViewModel` each write a `notifications` document per
  other member instead of relying on a Cloud Function (Spark plan has none). Only covers changes
  made while some device has the app open — not a true push notification. **Needs a manual check**
  that Firestore security rules allow a user to create a notification addressed to someone else.
- **F2.2 — Device QA pass on Google Sign-In.** Still open. Config is verified correct
  (`serverClientId` matches `google-services.json`); what's unverified is the live flow (SHA-1
  fingerprints registered, OAuth consent screen published). Needs an actual device run.
- ~~**F2.3 — Resolve the Apple Sign-In question (G7, G8).**~~ Done — PR #19. Removed the dead
  `signInWithGoogle`/`signInWithApple` methods and the unreachable Apple UI branch rather than
  implementing Apple for real, since this is Android-only.
- **F2.4 — Fix `updatePositions` no-op (G9).** Fix pushed — PR #67 (`fix/shopping-update-positions`),
  open against `develop`, awaiting review/merge.

### Phase 3 — Post-launch polish (not MVP blockers)

- F3.1 — Biometric app-lock (pattern from FriendsSecrets' `core/biometric`).
- F3.2 — Extend shared `StorageService` adoption to `feature/settings` (deferred earlier this
  session — needs combining ~10 preference keys into one reactive contract without losing
  atomicity).
- F3.3 — Replace the placeholder system notification icon with a branded monochrome asset.
- ~~F3.4 — Fix `AiChatScreen`'s settings icon (`contentDescription = null`) for
  accessibility/testability.~~ Done this session — icon now uses
  `ai_chat_settings_content_description` (en/es/pt-BR), and `chat_flow.yaml` asserts it's reachable.
- F3.5 — Address G10 (cross-feature module coupling) once a design pass decides which shared
  contracts move to `core/*`.

## Suggested order

`feat/new-layout` is merged; `develop` is now the live integration branch. What's left needs you
specifically:

1. Review and merge PR #67 (G9 fix) — small, low-risk, already implemented and tested.
2. Once you have a device: run the Maestro suite (`maestro test .maestro/test_suite.yaml`, F0.3,
   now includes `account_data_flow.yaml`), do the Google Sign-In QA pass (F2.2), and capture the
   screenshots/feature graphic (rest of G4).
3. Pick a host for the legal pages (G3) and wire the URL in.
4. Review the proposed `firestore.rules` (G5 / AD-009 / PR #84), decide on the two client follow-ups it
   depends on, then deploy it yourself:
   `firebase deploy --only firestore:rules --project how-much-2a72e`. Nobody else touches production
   rules.
5. When ready, decide on `develop` → `master` for the actual store release — that merge is yours to
   make, same as G1 was.
6. Schedule a design pass for G10 (cross-feature coupling) before it grows further — not a launch
   blocker, but the longer it's left the more feature modules will depend on it.
7. ~~Prioritize **G15** (hardcoded Gemini API key, no remote-rotation path) among the new bug-audit
   items~~ — done, PR #69, along with the rest of G12–G16. What's left of G15 is yours: publish
   `gemini_api_key` in Remote Config and **revoke the old key at the provider** — until then nothing
   is actually mitigated.

## Bug audit — 2026-09-09

A pass over ViewModels, repository implementations, and Compose screens for common bug patterns
(coroutine scope leaks, unguarded `!!`, missing Firestore listener cleanup, `LaunchedEffect`/
`remember` key mistakes). Findings beyond what was already tracked (G9, F3.4):

| # | Gap | File | Fix status |
|---|---|---|---|
| ~~G11~~ | ~~Camera analyzer thread leak~~ — `CameraPreview`'s single-thread `Executor` was created via `remember` but never shut down; every scanner screen visit (open → back → reopen) leaked a background thread | `feature/products/.../components/scanner/CameraPreview.kt:33` | **Fixed this session** — added `DisposableEffect` to shut down the executor |
| ~~G12~~ | ~~`CartViewModel.observeData()` leaks duplicate Flow collectors — its settings `collect{}` calls `observeProducts()`, which launches a *new* `viewModelScope` collector each time instead of using `flatMapLatest`; every DataStore settings write anywhere in the app (theme, language, AI prefs) adds one more permanent product collector, each re-running `sortProductsUseCase`/`resolveMemberProfiles`~~ | `feature/cart/.../viewmodel/CartViewModel.kt:98-127` | **Fixed — PR #78** (`flatMapLatest`). Unit tests only, no device verification. |
| ~~G13~~ | ~~QR-code list join has no scan debounce — `BarcodeAnalyzer` fires `onBarcodeScanned` on every analyzed camera frame with no throttle/one-shot guard, and `ScannerViewModel.onTokenScanned` has no "already processing" flag; holding a code in frame re-triggers `ShoppingJoinUseCase`, which loops a notification write per other member on every duplicate join~~ | `feature/products/.../scanner/BarcodeAnalyzer.kt:22`, `feature/shopping/.../viewmodel/ScannerViewModel.kt:28-35`, `ShoppingJoinUseCase.kt:33-38` | **Fixed — PR #73** (`ScannerViewModel.isJoining` guard, released on failure and held on success, + analyzer throttle). Unit tests only, no device/camera verification. |
| ~~G14~~ | ~~`ProfileViewModel.observeProfile()` subscribes to the Firestore profile but discards the emitted value, always rebuilding state from cached `authService.currentUser` instead — a Firestore-only profile edit never reaches the UI, and the listener runs forever for no effect~~ | `feature/profile/.../viewmodel/ProfileViewModel.kt:47-54` | **Fixed — PR #79** (emission reconciled into state). Unit tests only, no device verification. |
| ~~G15~~ | ~~Gemini API key is compiled into the APK (`BuildConfig.GEMINI_API_KEY`) in three places, and the remote-config key meant for server-side rotation (`RemoteVariableKeys.GEMINI_API_KEY`) is never actually read — so a compromised/abused key can't be revoked without a new release~~ | `feature/products/.../ProductRepositoryImpl.kt:44-47`, `RecipeRepositoryImpl.kt:29-32`, `feature/ai-agent/.../GeminiAiAgent.kt:29-30`, `core/remote-config/.../RemoteConfigKeys.kt:19` | **Code fixed — PR #69** (AD-008: Remote Config + blank-value guard + compiled fallback). **Not yet mitigated in practice:** publishing `gemini_api_key` and revoking the old key in the consoles is still owed by bruno. Rotation is restart-scoped for the two `@Singleton` repositories. |
| ~~G16~~ | ~~`ProductSearchViewModel.search()` has no debounce or request cancellation — every keystroke past 3 chars launches a fresh, untracked coroutine; a slower earlier response can arrive after a faster later one and overwrite `_uiState` with stale results for a query the user no longer typed~~ | `feature/products/.../viewmodel/ProductSearchViewModel.kt:55-90` | **Fixed — PR #77** (debounce + cancel-previous job). Unit tests only, no device verification. |

**Update 2026-09-10:** G12–G16 are all closed — each shipped as its own branch and PR (#78, #73,
#79, #69, #77 respectively), exactly as the paragraph below intended. Recorded plainly: apart from
G11's `androidTest`, none of these fixes has been exercised on a device or emulator — their evidence
is JVM unit tests plus review. Accepted for their risk class (cancellation/flow-plumbing changes that
fail as a stale UI update, not as data corruption), but do not restate them as "verified". G15's code
is done while its console-side rotation/revocation is not; see its row.

Original note, kept as the record of why they were split:

G12–G16 are documented here rather than fixed in this branch on purpose: none of them can be
exercised on a device in this environment, and each touches a different feature's core behavior
(cart observation, QR join, profile sync, AI cost/security, search) — bundling behavioral fixes
like these into one PR is exactly the risk the "Process" section below exists to avoid. G11 was
fixed here because it's a pure resource-cleanup change with zero behavior change.

## Process

Each item above ships as its own branch off `develop`, with its own PR into `develop` — never
committed or merged directly. That keeps every change independently reviewable and revertable.
`develop` → `master` is a separate, deliberate step the repo owner takes. PR numbers are noted next
to each completed item as they land.

**Session notes convention**: `.claude/SESSION-NOTES.md` is a local, gitignored file (never
committed, never shared) where in-progress work gets a short "what / why / status" entry — branch
name, what it is, why it exists, current state. It exists for the messier in-between state that
isn't ready for `STATE.md` or this doc yet: a half-finished branch, uncommitted changes, a decision
made mid-session. If you find uncommitted work with no entry explaining it, that's a process miss —
reconstruct the intent, then add the entry.
