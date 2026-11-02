# Task: Navigator singleton e deep linking

- **Slug:** navigator-deeplink
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** implemented
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

`Navigator` é `@Singleton` (Hilt) e sobrevive à recriação da Activity, mas `MainActivity` não declara `launchMode` (padrão `standard`) e não implementa `onNewIntent`. Um link de convite (`https://how-much-2a72e.web.app/join/...`, intent-filter em `AndroidManifest.xml:78-83`) abre uma **segunda** instância da Activity, que faz `bind()` no corpo da composição (`MainActivity.kt:100`). Quando essa segunda instância é fechada, seu `DisposableEffect` chama `navigator.unbind(navController)` (`MainActivity.kt:107-109`); como a checagem de identidade em `Navigator.unbind` (`Navigator.kt:39-43`) só impede um *unbind* tardio de uma instância **antiga**, ela não impede este caso — a segunda instância era, de fato, a última a vincular — e a primeira instância (retomada) fica sem `NavController`. Qualquer chamada subsequente a `navigator.navigate()`/`goBack()` acessa a propriedade `navController` (`Navigator.kt:22-23`), que lança `IllegalStateException("Navigator not bound to a NavController")` — já visto no Crashlytics (C-01).

Além disso, o deep link só é lido uma vez, via `LaunchedEffect(intent)` no corpo do Composable (`MainActivity.kt:186-193`), que navega direto para `JoinList(token)` independente do estado de autenticação. `JoinList` não implementa `RouteProtocol` (`MobileRoutes.kt:22`) e é registrado como `dialog<JoinList>` (`ShoppingGraph.kt:173`) fora da hierarquia de `ShoppingList`/`AiChat`/`Profile`, então o gate de autenticação em `MainActivity.kt:147-153` não a trata como protegida e não redireciona para `Welcome`. O diálogo abre por cima da tela atual e `JoinListViewModel.init` (`JoinListViewModel.kt:52-54`) chama `joinByToken` imediatamente; `ShoppingJoinUseCase` (`ShoppingJoinUseCase.kt:20`) chama `authService.getOrCreateUserId()`, que lança `IllegalStateException("No authenticated user. Sign-in is required.")` quando não há sessão (`FirebaseAuthService.kt:88-109`). Esse erro cru é exibido como `UiText.DynamicString(message)` (`JoinListViewModel.kt:86-92`) e o token nunca é reprocessado — ele só existe na route args do diálogo já aberto e falho (B-30). Girar a tela também pode reempilhar `JoinList`, pois a chave do `LaunchedEffect(intent)` é a instância do `Intent`, não um estado consumido explicitamente (relacionado a B-14, fora do escopo aqui).

O `Navigator` já expõe um acessor nulo-seguro, `navControllerOrNull` (`Navigator.kt:19-20`, cobertura em `NavigatorTest.kt:40-49`), mas `navigate()`/`goBack()` ainda usam a propriedade que lança (`Navigator.kt:45-55`) — ou seja, a base para tolerar "não vinculado" já existe e só precisa ser usada nesses dois métodos.

**Itens do backlog:** C-01, B-30 (`.specs/sdd-001-auditoria-geral/tasks.md:34,74,91`).

## 2. Escopo

**Inclui:**
- `android:launchMode="singleTask"` em `MainActivity` (`app/src/main/AndroidManifest.xml:68-84`), garantindo uma única instância.
- `MainActivity.onNewIntent(Intent)`: chama `setIntent(intent)` e delega a extração do deep link à `MainViewModel`.
- Extração do deep link (`InviteLink.matches`/`tokenFrom`) movida de `LaunchedEffect(intent)` (removido) para um ponto único, chamado tanto em `onCreate` (intent inicial) quanto em `onNewIntent` (intents subsequentes).
- `MainViewModel`: novo `SavedStateHandle` injetado; `pendingJoinToken: StateFlow<String?>` persistido nele; `fun onDeepLink(intent: Intent)` grava o token pendente; `fun consumePendingJoinToken(): String?` lê e limpa (consumo único).
- Em `MainActivity`, `LaunchedEffect(pendingJoinToken, isAuthenticated)` navega para `JoinList(token)` **somente quando autenticado**, chamando `consumePendingJoinToken()` no mesmo passo; enquanto não autenticado, o token permanece pendente (não navega, não dispara `joinByToken` com usuário anônimo/inexistente).
- `Navigator.navigate`/`Navigator.goBack` (`Navigator.kt:45-55`) passam a usar `navControllerOrNull`; se nulo, registram `Timber.w` e retornam sem lançar exceção.

**Exclui:**
- Migração de `Navigator` para `@ActivityRetainedScoped` (refactor futuro).
- Novas rotas ou features de deep link (fica para o produto).
- Mensagem de erro crua em `JoinListViewModel` (`JoinListViewModel.kt:86-92`) quando `joinByToken` falha por outro motivo (token inválido, lista inexistente) — comportamento inalterado; o caso "sem sessão" deixa de ocorrer via deep link porque o diálogo só abre após autenticação, mas entrada manual de token com usuário deslogado é hoje inacessível pela navegação atual (todas as rotas que abrem `JoinList` exigem estar autenticado), então não há regressão a cobrir aqui.
- Correção de B-14 (reempilhamento de `JoinList` ao girar a tela) além do que o consumo único do token já resolve como efeito colateral.

## 3. Impacto Arquitetural

```
:app
  ├── AndroidManifest.xml
  │   └── MainActivity launchMode="singleTask"              [mudança]
  ├── MainActivity.kt
  │   ├── onCreate: processa intent inicial (1x)             [novo]
  │   ├── onNewIntent(intent): setIntent + processa           [novo]
  │   ├── remove LaunchedEffect(intent) ad-hoc                [remoção]
  │   ├── LaunchedEffect(pendingJoinToken, isAuthenticated)   [novo]
  │   │     -> navigator.navigate(JoinList) + consome token
  │   └── navigator.bind/unbind (identidade) — inalterado
  └── MainViewModel.kt
      ├── SavedStateHandle injetado                           [novo]
      ├── pendingJoinToken: StateFlow<String?>                [novo]
      ├── onDeepLink(intent: Intent)                          [novo]
      └── consumePendingJoinToken(): String?                  [novo]

:core:navigation
  └── Navigator.kt
      └── navigate()/goBack() usam navControllerOrNull,
          logam (Timber.w) em vez de lançar               [mudança]
```

Fluxo (deep link com app fechado, usuário deslogado):

```
Browser/outro app --VIEW--> MainActivity.onCreate
                                  │
                                  ▼
                     MainViewModel.onDeepLink(intent)
                                  │ grava token em SavedStateHandle
                                  ▼
                    pendingJoinToken = "abc123" (sobrevive a recriação)
                                  │
                      (usuário faz login em Welcome)
                                  │
                 LaunchedEffect(pendingJoinToken, isAuthenticated)
                                  │ isAuthenticated == true
                                  ▼
                 navigator.navigate(JoinList("abc123"))
                 viewModel.consumePendingJoinToken() -> limpa
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `Navigator.navigate(route, builder)` | Usa `navControllerOrNull`; não lança mais se não vinculado, loga `Timber.w` | `public` (`core:navigation`) | Assinatura inalterada; comportamento mais tolerante — compatível com todos os call sites atuais |
| `Navigator.goBack()` | Idem | `public` | Idem |
| `MainActivity.onNewIntent(intent: Intent)` | Novo override | n/a (Activity lifecycle) | Chama `setIntent(intent)` antes de delegar |
| `MainViewModel.pendingJoinToken: StateFlow<String?>` | Novo | `internal` (classe já é `internal`) | Backed by `SavedStateHandle.getStateFlow` |
| `MainViewModel.onDeepLink(intent: Intent)` | Novo | `internal` | Único ponto de extração do token (via `InviteLink`) |
| `MainViewModel.consumePendingJoinToken(): String?` | Novo | `internal` | Lê e limpa atomicamente no `SavedStateHandle` |

Nenhum contrato cruza módulos de forma nova além de `Navigator` (já público, sem mudança de assinatura) — não se aplica o fluxo do §6 do AGENTS.md para novo parâmetro público.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Uma segunda instância de `MainActivity` pode ser criada por um deep link | `singleTask` garante uma única instância; `onNewIntent` processa deep links nela | `AndroidManifest.xml:68-84` |
| 2 | Token de convite só é lido no intent de abertura e perdido se o usuário não estiver logado | Token guardado em `SavedStateHandle` (sobrevive a recriação/login); consumido uma única vez, só após autenticação | `MainActivity.kt:186-193`, `JoinListViewModel.kt:38,52-54` |
| 3 | `navigate()`/`goBack()` lançam `IllegalStateException` se `Navigator` não estiver vinculado | Registram aviso (`Timber.w`) e retornam sem lançar — app não quebra em uma janela de "ainda não vinculado" | `Navigator.kt:22-23,45-55` |
| 4 | `JoinList` abre e tenta entrar na lista mesmo sem sessão, expondo erro interno (`"No authenticated user..."`) | `JoinList` só é navegado após `isAuthenticated == true`; o caminho sem sessão deixa de ser alcançado via deep link | `ShoppingJoinUseCase.kt:20`, `FirebaseAuthService.kt:88-109`, `JoinListViewModel.kt:86-92` |

## 6. Critérios de Aceitação

- **`launchMode="singleTask"`:** uma só instância de `MainActivity` em todo o ciclo de vida do app; `onNewIntent` é chamado para intents subsequentes (validável via log/teste instrumentado).
- **`onNewIntent`:** extrai token do intent (`InviteLink.matches`/`tokenFrom`) e grava no `pendingJoinToken` via `MainViewModel.onDeepLink`; não navega diretamente.
- **Intent inicial:** processado pelo mesmo caminho (`onDeepLink`) a partir de `onCreate`, não mais por `LaunchedEffect(intent)`.
- **`SavedStateHandle`:** `pendingJoinToken` sobrevive a recriação de processo e a rotação de tela.
- **Token consumido uma vez:** após `consumePendingJoinToken()`, uma nova emissão de `pendingJoinToken` é `null`; `JoinList` não é reempilhado ao girar a tela com o token já consumido.
- **Gate de autenticação:** `JoinList` só é navegado quando `isAuthenticated == true`; deslogado, o token permanece pendente até login.
- **Navigator seguro:** nenhuma `IllegalStateException` em `navigate()`/`goBack()` quando não vinculado; `navController` (propriedade direta) continua lançando para quem acessa explicitamente (comportamento intencional, cobertura mantida em `NavigatorTest.kt:51-57`).
- **Performance:** nenhuma alocação ou I/O adicional por frame — `onDeepLink` roda só em `onCreate`/`onNewIntent` (eventos raros), e a leitura de `SavedStateHandle.getStateFlow` é O(1) e já observada via `collectAsStateWithLifecycle`.

Sem feature toggle — fix de comportamento existente, sem necessidade de flag de rollback (reversível via revert de PR).

**Observabilidade:** nenhum evento novo de Analytics necessário; manter os eventos existentes (`SHOPPING_LIST_JOINED`/`SHOPPING_LIST_JOIN_FAILED` em `JoinListViewModel.kt:66-69,74-79`). Logar via `Timber.w` o caso "Navigator.navigate chamado sem bind" para facilitar diagnóstico futuro no Crashlytics (breadcrumb), sem enviar dados pessoais.

**Compatibilidade:** minSdk 28 — `singleTask` e `onNewIntent` são suportados desde API 1, sem gap de versão. Não há dado local/Firestore migrado por esta mudança. Wear OS não é afetado (módulo `:wear` tem sua própria `MainActivity`, confirmado — nenhum `PendingIntent`/`Intent` do módulo `:app` referencia a Activity do Wear ou vice-versa).

## 7. Plano de Testes

**Unitários:**
- `NavigatorTest` (estender `core/navigation/src/test/.../NavigatorTest.kt`): `navigate()` sem bind não lança (hoje só existe teste de que a propriedade `navController` lança — `navigate()`/`goBack()` precisam de testes próprios usando um `Navigator()` sem `bind()`).
- `goBack()` sem bind não lança, idem.
- `MainViewModelTest` (novo ou existente): `onDeepLink(intent)` com link válido grava `pendingJoinToken`; com link inválido/não é `InviteLink`, não grava nada.
- `consumePendingJoinToken()` retorna o valor e limpa (`pendingJoinToken` volta a `null`); chamado duas vezes, a segunda retorna `null`.
- Usar `SavedStateHandle()` real (androidx) ou fake em teste de ViewModel — padrão já usado em `JoinListViewModel` (`JoinListViewModelTest.kt`), replicar abordagem.

**Instrumentados (seletores por `testTag`, nunca texto — AGENTS.md §5):**
- App aberto (autenticado) → recebe deep link de convite (via `am start` ou `Intent` simulado) → `JoinList` abre e processa sem crash.
- App aberto → abre deep link em outro app (Browser) → volta ao app (`onNewIntent`) → navega normalmente sem `IllegalStateException`.
- Deslogado → toca convite → `JoinList` **não** abre imediatamente (token pendente) → login → `JoinList` abre e processa o convite automaticamente.
- Girar a tela com convite pendente e deslogado → token preservado → após login, processado uma única vez (não duplicado).
- `JoinListContent` (`feature/shopping/.../JoinListContent.kt`) não tem `testTag` hoje — adicionar durante a implementação para os elementos relevantes ao teste instrumentado (diálogo visível, estado de erro/sucesso), seguindo o padrão do resto do app.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug
./gradlew koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `singleTask` muda comportamento de outros intents que abrem `MainActivity` | baixa | médio | Verificado: nenhum `PendingIntent`/notificação do módulo `:app` referencia `MainActivity` hoje (grep confirmado); único intent externo é o próprio deep link `/join` |
| `onNewIntent` não chamado em algum fluxo (ex.: `singleTask` com `FLAG_ACTIVITY_NEW_TASK` de certos launchers) | baixa | médio | Teste instrumentado cobrindo abrir link com app em background e em foreground |
| `SavedStateHandle` indisponível em edge case (processo morto sem bundle) | baixa | baixo | `pendingJoinToken` nasce `null`; sem crash, apenas convite perdido (regressão apenas para esse caso raro, não pior que o atual) |
| Teste instrumentado de deep link é inerentemente mais lento/flaky (depende de intents entre apps) | média | baixo | Priorizar os cenários no emulador local; cenário "Browser real" pode ficar manual se Maestro não suportar bem intents cross-app |

## 9. Perguntas em Aberto

Nenhuma bloqueante.

- [ ] (Não bloqueante) Vale abrir um SDD futuro para trocar a mensagem de erro crua (`"No authenticated user. Sign-in is required."`) por um texto localizado em `JoinListViewModel.kt:86-92`? Hoje esse caminho fica inalcançável via deep link após esta correção, mas permanece como código morto potencialmente alcançável por outras entradas futuras.

## 11. Revisão (code-reviewer)

`/code-review` encontrou 4 problemas na implementação; 3 corrigidos, 1 descartado (verificado como incorreto por teste):

1. **(Corrigido)** `MainActivity.onCreate` chamava `viewModel.onDeepLink(intent)` sem limpar o intent: `getIntent()` mantém o MESMO intent através de recriações por config change (rotação, dark mode, idioma), então uma rotação após o convite já ter sido consumido o lia de novo e reempilhava `JoinList` — exatamente o B-14 que a Seção 1 original afirmava (erroneamente) já estar coberto. Corrigido com `consumeDeepLinkIntent()`: lê e imediatamente `setIntent(Intent())`, em `onCreate` e `onNewIntent`.
2. **(Corrigido)** `Navigator.navigate` logava `route` via `Timber.w("...%s...", route)`, o que serializa o token de `JoinList(token=...)` para o breadcrumb do Crashlytics — viola AGENTS.md §5 ("não logar dados pessoais ou tokens"). Corrigido para logar `route::class.simpleName`.
3. **(Corrigido)** O `LaunchedEffect(pendingJoinToken, isAuthenticated)` e o gate de nome obrigatório podiam disparar na mesma virada de `isAuthenticated` e navegar para destinos diferentes (`JoinList` vs `CompleteName`); o que rodasse por último vencia, e o token já estava consumido — perdendo o convite silenciosamente para uma conta nova sem nome. Corrigido adicionando `!requiresName` à condição/chave do efeito do convite, para só navegar depois que o gate de nome resolver.
4. **(Descartado)** Sugestão de trocar `get` + `set(null)` por `savedStateHandle.remove<String>(key)` em `consumePendingJoinToken`. Testado e reproduzido: `remove()` não atualiza o `StateFlow` já obtido via `getStateFlow()` no mesmo ciclo (`MainViewModelTest > consumePendingJoinToken returns the stored token once, then null` falhou com essa troca). Revertido para `get`/`set(null)`, com comentário explicando o motivo.

Testes/lint/detekt/cobertura reexecutados por completo após as correções — `./gradlew testDebug detekt lintDebug koverVerify koverVerifyLayout` com BUILD SUCCESSFUL.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (C-01, B-30) e o código atual verificado nesta revisão.
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas/diagramas).
- [x] Seção 6 preenchida de forma substantiva (performance, observabilidade, compatibilidade).
- [x] Não quebra contratos públicos existentes (`Navigator` mantém assinatura).
- [x] Testes instrumentados planejados, com nota sobre `testTag` ausente em `JoinListContent`.
- [x] Perguntas em aberto exauridas (nenhuma bloqueante).
