# MVP Roadmap — Cestou (How-Much)

Status: Phases 0–2 mostly done — see gap list for what's still open
Owner: bruno
Last updated: 2026-09-09

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

Every item marked done above shipped as its own branch + PR (none merged without review): shared
`StorageService` for `core/auth` (#14), shopping-reminder push notifications (#15), Maestro
regression suite (#16), this doc (#17), account & data deletion (#18), dead social-auth cleanup
(#19), collaboration notifications (#20), hosted legal pages (#21), store listing descriptions
(#22), `feat/new-layout` → `develop` merge (#30), CI rebuild around Git Flow (#47),
`updatePositions` fix (#67, open).

**Still genuinely open, none of them fixable from this environment:**
- **F0.3** — first real execution (2026-09-09, Samsung SM-A146M, Android 15, wireless adb) found
  two suite-authoring bugs, both fixed on `test/maestro-e2e-coverage`: (1) `home_flow.yaml`'s
  `launchApp: clearState: true` logs the device out of Google Sign-In instead of landing on an
  authenticated empty home — split into a separate `onboarding_flow.yaml` (clearState allowed,
  excluded from `test_suite.yaml`) and a `home_flow.yaml` that assumes an existing session; (2) all
  8 flows asserted hardcoded strings against the device — first "fixed" to hardcoded pt-BR (still
  fragile: would've broken again on a CI emulator defaulting to en-US), then corrected to a
  `Modifier.testTag`-based selector strategy so flows don't depend on device locale either way
  (~25 Compose files touched, scoped to only what the flows touch; see `.maestro/README.md`
  "Language / locale"). `onboarding_flow.yaml` ran and passed in full, both before and after the
  testTag rework. The rest of the suite is blocked on a human logging back into the device (this
  session's own test run cleared the session) — see `.specs/STATE.md` handoff for the exact ask.
  F2.2 (Google Sign-In QA) still needs a manual pass; Maestro can't drive the Google account picker.
- Screenshots/feature graphic (part of G4) need a device to capture.
- The privacy/terms pages (G3) need a hosting decision before the in-app links can point anywhere.
- **G5 Firestore rules** are no longer unwritten *or* outside the repo: a full rule set covering
  every collection the app uses now lives in `firestore.rules` (PR #84), reasoned through in AD-009
  (`.specs/STATE.md`) and checked by 60 emulator assertions. Production still runs the console
  placeholder, so what's left is yours: review, then
  `firebase deploy --only firestore:rules --project cestou-86785`. Read AD-009's "known breakage on
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

## Plan — features broken by user value

### Phase 0 — Unblock (infra, no direct user value, but nothing ships without it)

- ~~**F0.1 — Merge `feat/new-layout` into `develop`.**~~ Done — PR #30.
- ~~**F0.2 — Resolve `lint-rules/` uncommitted state (G6).**~~ Done.
- **F0.3 — Run the Maestro suite (`maestro test .maestro/test_suite.yaml`) on a real device**,
  fix whatever selector drift shows up now that it's finally executable.

### Phase 1 — Play Store launch blockers (must-have)

- ~~**F1.1 — Account & data deletion (G2).**~~ Done — PR #18. `DeleteAccountUseCase` leaves/deletes
  the user's shopping lists, deletes their profile, clears local settings, then deletes the
  Firebase account, in that order. No re-auth flow for `FirebaseAuthRecentLoginRequiredException`
  — surfaces as a generic error if Firebase demands a recent login.
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
   `firebase deploy --only firestore:rules --project cestou-86785`. Nobody else touches production
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
