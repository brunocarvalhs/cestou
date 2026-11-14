# Task: Redesenhar a home do Wear OS (listas) inspirada no Spotify e no WhatsApp

- **Slug:** wear-home-redesign
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-10
- **Status:** ready (revisão 6: todas as perguntas respondidas. Q18 descartada pelo usuário: sem atalho para a última lista aberta)
- **Versão-alvo:** 2.3.0 (sugestão; o bump é do processo de release, ver §6)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive (um `NavKey` público em `core:navigation/mobile` e um contrato de abertura do celular; ver §4.1)

## 1. Contexto e Motivação
- A home do relógio é "Minhas Listas": cabeçalho e um botão por lista (`ShoppingWearScreen.kt:53-75`). Sem hierarquia, sem histórico, sem ação de criar.
- Hoje o app pula sozinho para a última lista ao abrir (`ShoppingWearGraph.kt:26-33`). Decisão (Q2): abrir **direto na home**.
- Referência WhatsApp Wear OS: `/Users/bruno/Desktop/home - wear os - whastapp/` (5 capturas). Usada para o menu e o botão de ação.
- Referência Spotify: `/Users/bruno/Desktop/home - waer os - spotify/` (4 capturas). Padrões adotados:
  1. Topo sem título de texto, com **duas pílulas** lado a lado (`Screenshot_20261110_124750.png`).
  2. Linhas com **capa quadrada arredondada**, título em destaque e subtítulo (`..._124731.png`).
  3. **Barras de esqueleto** no carregamento (`..._124750.png`).
  4. Fim da lista com **botão de contorno "Configurações"** (`..._124813.png`).
- Decisões do usuário: Q12 = linha (sem cartão grande). Q14 = "Configurações" no fim, seguindo o Spotify. Q13 = pesquisar mostra a **lista completa**. Pílula de biblioteca vira **análise do histórico**. Configurações tem o **perfil**. Home mostra as **5 listas recentes**.
- Dados já disponíveis (sem fonte nova):
  - `Shopping` (`core/domain/.../model/Shopping.kt:7-22`): `isFavorite`, `price`, `budget`, `status`, `emoji`, `createdAt`, `finishedAt`.
  - Favorito é alternado no celular (`ShoppingListViewModel.kt:261`) e já é filtro (`ShoppingListViewModel.kt:286`).
  - Histórico mensal já calculado no módulo de compras: `GroupHistoryByMonthUseCase` (`feature/shopping/.../domain/usecase/GroupHistoryByMonthUseCase.kt:16-38`).
  - Tela de perfil do relógio já existe: `ProfileWearScreen` (nome, "vincular celular", "Sair") em `feature/profile/.../presentation/wear/screen/ProfileWearScreen.kt:26-95`, registrada por `profileWearGraph` (`feature/profile/.../navigation/wear/ProfileWearGraph.kt:11-16`) na rota `Profile` (`core:navigation/mobile/MobileRoutes.kt`).
  - Produtos da lista: `ProductsUseCase` (`ShoppingDetailViewModel.kt:42`, `:67-72`).
  - Última lista salva no relógio: `WearListStore` (`WearListStore.kt:13-30`).

### 1.1 Como o celular calcula valores
| Valor no celular | Onde | Fórmula | Origem |
|---|---|---|---|
| Total da lista aberta (carrinho) | `CartScreen` | `Σ product.total` | `CartScreen.kt:113` |
| Total usado no orçamento | `computeCartBudget` | `Σ product.total` | `CartBudget.kt:30` |
| Total de um produto | `Product` | `price × quantity` | `Product.kt:17` |
| Orçamento no card da lista | `ShoppingItem` | `shopping.budget` (não o total) | `ShoppingItem.kt:114-118` |
| Valor pago de lista finalizada | `FinishPurchaseViewModel` | grava `price` ao finalizar | `FinishPurchaseViewModel.kt:66-69` |
| Histórico mensal | `GroupHistoryByMonthUseCase` | `Shopping.price` | `GroupHistoryByMonthUseCase.kt:38` |

- O relógio já usa `Σ product.total` (`ShoppingDetailViewModel.kt:113`, `:124`). A divergência está só no **resumo salvo** (`WearListStore`), que só atualiza com a lista aberta no relógio.

## 2. Escopo
- **Inclui:**
  - **Home** (abre direto nela, Q2):
    - Topo: **duas pílulas** lado a lado, no padrão Spotify:
      - **Pesquisar** (ícone de lupa): abre a lista completa (ver abaixo);
      - **Histórico** (ícone de gráfico): abre a análise do histórico (substitui a pílula de biblioteca).
    - **5 linhas** de listas recentes (Q15, respondida): as **5 listas ativas criadas por último** (`createdAt`, mais nova primeiro). Não é a última aberta.
      - fixadas (`isFavorite`) vêm primeiro, depois as demais por `createdAt`;
      - máximo de 5 linhas, sem repetir a mesma lista.
    - Linha: **capa quadrada arredondada** com o emoji (gradiente para a primeira, se for a lista fixada), título, subtítulo com **orçamento** (igual ao card do celular, `ShoppingItem.kt:114-118`) e ícone de fixada quando `isFavorite`.
    - Após as linhas: botão **"Criar lista no celular"** (Q3, posição confirmada na Q16). Abre o app no celular direto em criar lista.
    - Botão **"Mais"** (Q19, leitura padrão): mostra na própria home as demais listas ativas, sem trocar de tela. Alterna para "Menos" depois. Não repete a Pesquisar, que é a lista completa com busca.
    - Fim da home: botão de contorno **"Configurações"** (Q14), logo depois de "Mais".
    - Carregamento: **barras de esqueleto** (padrão Spotify).
  - **Pesquisar** (Q13): tela com a **lista completa** de listas, ativas primeiro e arquivadas depois.
    - Campo de busca por título (default; ver Q17).
    - Linha de arquivada mostra o valor pago (`Shopping.price`), igual ao histórico do celular.
  - **Histórico** (substitui a biblioteca; default aceito): análise mensal do que já foi finalizado.
    - Lista de meses com o total pago, a partir de `GroupHistoryByMonthUseCase`.
    - Sem gráfico e sem detalhe por mês nesta entrega (Q17 em aberto para o usuário).
  - **Configurações** (Q14): reutiliza a tela de perfil existente (`ProfileWearScreen`), acrescida de:
    - **Abrir no celular**: abre o app do celular na home;
    - **Sair** com alerta de confirmação do Wear OS antes de sair (Q4). Sair é **só do relógio**.
  - Estados vazio, carregando e sem sessão.
  - Strings novas em pt-BR e en-US.
  - Testes unitários e de UI com `testTag`.
- **Exclui:**
  - Criar a lista **dentro** do relógio (a criação acontece no celular, Q3).
  - Busca por produto, por item ou por texto livre além do título da lista.
  - Gráficos do histórico e a tela de análise de gastos completa (SpendingInsights).
  - Mudanças no pareamento (`WearAuthListenerService`, `WearableSyncService*`), escopo de sdd-054 e sdd-055.
  - Mudanças na lógica de finalizar compra e no cálculo de `Shopping.price`.
  - `CHANGELOG.md` e `versionName` (processo de release, §6).
  - Eventos de Analytics novos (Q5).
  - Tile (`LastListTileService`) com total ao vivo (Q9).

## 3. Impacto Arquitetural
- **Módulos e camadas:**
  - `:feature:shopping` → `presentation/wear` (home, pesquisar, histórico) e `navigation/wear` (rotas internas).
  - `:feature:shopping` → `domain` reaproveita `ProductsUseCase` e `GroupHistoryByMonthUseCase`. Nenhuma regra nova: a fórmula de total é a do celular (§1.1).
  - `:feature:profile` → `presentation/wear/screen/ProfileWearScreen.kt`: ganha "Abrir no celular" e o alerta de "Sair".
  - `:core:navigation` → `mobile/MobileRoutes.kt`: `NewShopping` (novo `NavKey` público, §4.1). `Profile` já existe e é reutilizado.
  - `:core:domain` / `:core:data`: contrato `OpenPhoneRoute` (§4.1).
  - `:app`: deep link `cestou://shopping/new` (manifest e `MainViewModel`, como o pareamento).
  - `:wear`: sem mudança além de já iniciar em `ShoppingList` (`MainActivity.kt:35-39`).
- **Padrão:** MVI com estado imutável. Intents em `presentation/wear/intent`.
- **Nota de fronteira:** `feature:shopping` **não** depende de `feature:profile`. A navegação até Configurações usa a rota `Profile` de `core:navigation`, que já é registrada no relógio pelo grafo do perfil.
- **Novos arquivos (caminhos completos):**
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/wear/state/WearHomeUiState.kt`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/wear/intent/WearHomeIntent.kt`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/wear/viewmodel/WearHomeViewModel.kt`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/wear/screen/WearSearchScreen.kt`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/wear/screen/WearHistoryScreen.kt`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/navigation/wear/WearInternalRoutes.kt` (`WearSearch`, `WearHistory`, `internal`)
  - `feature/shopping/src/test/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/wear/viewmodel/WearHomeViewModelTest.kt`
  - `core/navigation/src/main/java/br/com/brunocarvalhs/howmuch/core/navigation/mobile/NewShopping.kt` (ou em `MobileRoutes.kt`, como `PairingWear`)
  - `app/src/main/java/br/com/brunocarvalhs/howmuch/deeplink/ShoppingCreateLink.kt` (mesmo padrão de `WearPairingLink.kt`)
  - `app/src/test/java/br/com/brunocarvalhs/howmuch/deeplink/ShoppingCreateLinkTest.kt`
- **Arquivos alterados:**
  - `feature/shopping/.../presentation/wear/screen/ShoppingWearScreen.kt` (home nova; hoje usa `ShoppingListViewModel`, `:27-36`).
  - `feature/shopping/.../navigation/wear/ShoppingWearGraph.kt` (remove o `LaunchedEffect` de `:26-33`; registra `WearSearch` e `WearHistory`).
  - `feature/shopping/.../presentation/wear/viewmodel/LastListViewModel.kt` (lógica de "última lista" passa para `WearHomeViewModel`; removido se não houver outro uso, verificar antes).
  - `feature/profile/.../presentation/wear/screen/ProfileWearScreen.kt` e o `ProfileViewModel` (botão "Abrir no celular" e alerta de "Sair").
  - `feature/shopping/src/main/res/values/strings.xml` e `values-pt-rBR/strings.xml`. Também `feature/profile` (strings do alerta e do botão).
  - `feature/shopping/.../presentation/viewmodel/ShoppingListViewModel.kt`: abre o sheet de criação ao receber `NewShopping` (hoje em `:86-88`).
  - `app/src/main/AndroidManifest.xml` (intent-filter `cestou://shopping/new`, `BROWSABLE`, como `cestou://wear/pair` em `:105-107`).
  - `app/.../MainViewModel.kt` (trata o deep link).

- **Diagrama (ASCII):**

```
 RELÓGIO
 ───────
 MainActivity ──startDestination=ShoppingList──► WearHomeViewModel
                                                   ├─ ShoppingListViewModel.uiState  (listas, já existe)
                                                   ├─ WearListStore.lastList (Flow)  (id da "Continuar")
                                                   └─ ProductsUseCase(continuar.id)  (total ao vivo, Q7)
                                                           │
                                                           ▼
 WearHomeUiState: recentes(≤5) | isLoading | phoneUnavailable
                                                           │
 Home ─┬─ [Pesquisar] ─────────► WearSearch  (lista completa, filtro por título)
       ├─ [Histórico] ─────────► WearHistory (GroupHistoryByMonthUseCase, meses e total pago)
       ├─ linhas (≤5) ─────────► ShoppingDetail(id)
       ├─ [Criar lista no celular] ─► OpenPhoneRoute(cestou://shopping/new) ─► app celular: criar lista
       ├─ [Mais] ──────────────► mostra/oculta as demais listas ativas (na própria home)
       └─ [Configurações] ─────► Profile (ProfileWearScreen)
                                   ├─ [Abrir no celular] ─► OpenPhoneRoute(cestou://)
                                   ├─ [Vincular celular] (já existe)
                                   └─ [Sair] + alerta ─► sign-out local do relógio
```

- **Injeção:** Hilt em todos os pontos. `WearListStore` já é `@Singleton` (`WearListStore.kt:26`). Nenhum repositório instanciado à mão.
- **Navegação:** `WearSearch` e `WearHistory` são rotas **internas** do módulo `feature:shopping` (`internal`). Não viram contrato público. Confirmar que o `Navigator` aceita rota interna; se não aceitar, a rota sobe para `core:navigation/wear` (aí é `public` e segue §6).

## 4. Contratos e Interfaces

### 4.1 Contratos públicos (additive) — exigem §6 do AGENTS.md
- **`NewShopping`** (`NavKey`, `data object`) em `core:navigation/mobile`. Precedente: `PairingWear` e `WearPairing` (`MobileRoutes.kt`).
  - **Por que `public`:** `:app` navega até ele (deep link) e `:feature:shopping` o registra. Cruza módulos, então não pode ser `internal`.
  - **Assinatura:** `@Serializable data object NewShopping : NavKey, RouteProtocol { override val routeType = RouteType.PROTECTED }`
  - `PROTECTED`: sem login, o app pede sign-in antes de criar (mesmo guard das outras rotas).
  - **Consumidores:** `:app` (`ShoppingCreateLink`, `MainViewModel`) e `:feature:shopping` (`ShoppingListViewModel`).
- **`OpenPhoneRoute`** (abertura do celular a partir do relógio):
  - `interface OpenPhoneRoute` em `:core:domain/services` (public, additive). Assinatura: `suspend operator fun invoke(uri: String): Result<Unit>`.
  - Implementação em `:core:data/wearable`, com `RemoteActivityHelper.startRemoteActivity(intent, phoneNodeId)`, como em `WearableSyncServiceImpl.kt:94-108`.
  - Intent com `Intent.CATEGORY_BROWSABLE` e **sem** `ACTION_VIEW`, porque `RemoteActivityHelper` rejeita `ACTION_VIEW` (`WearableSyncServiceImpl.kt:175`; teste em `BuildPhonePairingIntentTest.kt:13`).
  - **Por que `public`:** `:feature:shopping` e `:feature:profile` chamam, e `:core:data` implementa.
  - **Consumidores:** `WearHomeViewModel` (botão "Criar lista no celular") e `ProfileViewModel` (botão "Abrir no celular").
- **Deep link** `cestou://shopping/new`: novo `intent-filter` em `app/src/main/AndroidManifest.xml`, `scheme=cestou`, `host=shopping`, `path=/new`.

### 4.2 Interno (internal, sem contrato)
```kotlin
// presentation/wear/state — internal
@Stable
internal data class WearHomeUiState(
    val recent: StableList<Shopping> = StableList(),   // até 5: fixadas primeiro, depois createdAt desc
    val allActive: StableList<Shopping> = StableList(), // todas as ativas, mesma ordem (usada por "Mais")
    val showAll: Boolean = false,                       // "Mais" / "Menos"
    val isLoading: Boolean = false,
    val phoneUnavailable: Boolean = false              // falha ao abrir o celular (Q10)
)

// presentation/wear/state — internal (tela Pesquisar)
@Stable
internal data class WearSearchUiState(
    val query: String = "",
    val active: StableList<Shopping> = StableList(),   // ativas, fixadas primeiro
    val archived: StableList<Shopping> = StableList()  // finalizadas, valor pago
)

// presentation/wear/state — internal (tela Histórico)
@Stable
internal data class WearHistoryUiState(
    val months: StableList<HistoryMonth> = StableList() // de GroupHistoryByMonthUseCase
)

// presentation/wear/intent — internal
internal sealed interface WearHomeIntent {
    data object CreateListOnPhone : WearHomeIntent     // "Criar lista no celular"
    data object ToggleShowAll : WearHomeIntent         // "Mais" / "Menos"
}

// presentation/wear/viewmodel — internal
@HiltViewModel
internal class WearHomeViewModel @Inject constructor(
    shoppingListState: ShoppingListViewModel /* ver nota */,
    productsUseCase: ProductsUseCase,
    openPhoneRoute: OpenPhoneRoute
) : ViewModel() {
    val uiState: StateFlow<WearHomeUiState>            // stateIn(WhileSubscribed(5_000))
    fun onIntent(intent: WearHomeIntent)
}

// função pura para teste — internal
internal fun pickRecentWearLists(
    lists: List<Shopping>,      // já sem FINISH
    limit: Int = 5              // fixadas primeiro; depois createdAt desc
): List<Shopping>

// função pura para teste — internal (mesma fórmula do celular, §1.1)
internal fun wearHeroTotals(products: List<Product>, budget: Double?): WearHeroTotals
```

- **Nota:** `ShoppingListViewModel` é reaproveitado (já compartilhado com o celular, `ShoppingWearGraph.kt:23`). Não se adiciona campo a `ShoppingListUiState` (`ShoppingListUiState.kt:10-29`), que é do celular.
- **Performance:** `pickRecentWearLists` e `wearHeroTotals` rodam em `StateFlow.map`/`combine`, nunca no corpo do Composable. Hoje o `filter` roda a cada recomposição (`ShoppingWearScreen.kt:46`).

### 4.3 Configurações e "Sair" (Q4, Q14)
- "Configurações" abre a rota `Profile` (`core:navigation/mobile/MobileRoutes.kt`), que já é registrada no relógio por `profileWearGraph` (`ProfileWearGraph.kt:11-16`).
- "Sair" já existe em `ProfileWearScreen` (`:74-79`, `onLogout` → `viewModel.intent.onSignOut()`). Ele **não tem confirmação**. A mudança é acrescentar o alerta do Wear OS antes de chamar `onSignOut()`.
- Verificar em implementação que o `onSignOut` do perfil limpa `WearListStore` (`WearListStore.kt:36`). Se não limpar, o plano acrescenta essa limpeza no mesmo fluxo.
- Sair é **local ao relógio**. Após sair, `MainActivity` manda para `LinkPhone` (`MainActivity.kt:35-39`).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Listas `FINISH` não aparecem na home. | Ficam em **Pesquisar** (arquivadas) e em **Histórico**. | `ShoppingWearScreen.kt:45-46` |
| 2 | Abre a última lista sozinho ao iniciar. | **Removido** (Q2). Abre na home. | `ShoppingWearGraph.kt:26-33` |
| 3 | Última lista aberta = salva no `WearListStore`; senão, a não finalizada mais recente. | **Não é mais usada na home**: a home usa a data de criação (Q15). `WearListStore` segue só para o tile e para a limpeza no sair. | `LastListViewModel.kt:17-21`, `WearListStore.kt:30` |
| 4 | Título `"$emoji $title".trim()`. | Emoji na capa quadrada; título limpo. | `ShoppingWearScreen.kt:73` |
| 5 | Sem favoritos no relógio. | `isFavorite` = fixada: entra primeiro nas recentes, com ícone. | `ShoppingListViewModel.kt:261`, `:286` |
| 6 | Lista vazia mostra "Nenhuma lista". | Mantido, fora do carregamento. | `ShoppingWearScreen.kt:77-84` |
| 7 | Sem subtítulo nas linhas. | Linha mostra o **orçamento**, igual ao card do celular. | `ShoppingItem.kt:114-118` |
| 8 | Total do relógio = `Σ product.total`. | **Detalhe da lista** mostra `Σ product.total`, igual ao carrinho. A home não mostra total (só orçamento, regra 7). | `CartScreen.kt:113`, `CartBudget.kt:30`, `ShoppingDetailViewModel.kt:113` |
| 9 | Restante/excedente: texto já existe no relógio. | Reaproveita `shopping_wear_remaining` / `shopping_wear_over_budget`, com a regra de `CartBudget`. | `strings.xml:117-118`, `CartBudget.kt:33-40` |
| 10 | Arquivadas não têm total de produtos "novo". | Em Pesquisar e Histórico, o valor é o **pago** (`Shopping.price`). | `FinishPurchaseViewModel.kt:66-69`, `GroupHistoryByMonthUseCase.kt:38` |
| 11 | Sair não tem confirmação no relógio. | Alerta antes de sair; sai **só do relógio**. | `ProfileWearScreen.kt:74-79`, `AuthService.kt:10` |
| 12 | Criar lista só no celular. | Botão abre `cestou://shopping/new`; o celular abre o sheet de criar (ou pede login). | `ShoppingListViewModel.kt:86-88`, `WearableSyncServiceImpl.kt:94-108` |
| 13 | Home mostrava todas as listas. | Home mostra **até 5** recentes. "Mais" mostra as demais na home; Pesquisar mostra todas com busca. | `ShoppingWearScreen.kt:64-75` (antes) |

## 6. Critérios de Aceitação
- **Valores iguais ao celular (Q7):**
  - Total da linha "Continuar" e do detalhe = `Σ product.total` (`CartScreen.kt:113`). Teste com preço e quantidade diferentes.
  - Linha da home: orçamento. Arquivadas e histórico: valor pago.
  - `WearListStore` só dá o id da "Continuar". Não dá valor.
- **Recentes (Q15):** a home nunca mostra mais de 5 linhas, nem repete a mesma lista. Ordem: fixadas, depois `createdAt` desc. Teste com 0, 3, 5 e 8 listas, e com a lista criada mais nova sendo a última aberta (não deve mudar a ordem).
- **Criar e Configurações (Q16):** "Criar lista no celular" fica entre as linhas e "Configurações".
- **Performance:**
  - Recentes e totais em `StateFlow`/`combine`, sem `filter` no corpo do Composable.
  - Uma observação de produtos ativa (a da "Continuar"). Nenhuma leitura por linha.
  - Pesquisar filtra em memória sobre a lista já observada (sem nova consulta por tecla).
  - `stateIn(WhileSubscribed(5_000))`.
  - `collectAsStateWithLifecycle` (lifecycle `2.11.0`, `gradle/libs.versions.toml:11-13`). Hoje é `collectAsState` (`ShoppingWearScreen.kt:31`).
  - Capa só com emoji e cor: sem Coil, sem rede.
  - `StableList` nas coleções expostas (`ShoppingListUiState.kt:12-17`). `key = { it.id }` nos `items`.
- **Criar lista no celular (Q3, Q10):**
  - Sucesso: `RemoteActivityHelper` retorna sem erro.
  - Sem celular ou falha: `phoneUnavailable = true`, texto "Não encontramos o celular", botão continua visível.
- **Sair (Q4):** o alerta aparece antes de qualquer ação. "Cancelar" não faz nada.
- **Feature toggles:** nenhum por padrão.
- **Observabilidade:** sem evento novo (Q5). Falhas de `RemoteActivityHelper` vão para Timber/Crashlytics, sem dados pessoais nem tokens (`AGENTS.md` §5).
- **Compatibilidade:**
  - Wear OS: `TransformingLazyColumn`, `ScreenScaffold`, `Button` (`ShoppingWearScreen.kt:9-19`).
  - Telas de 384 e 454 px: títulos truncados (`maxLines`), pílulas caberem em uma linha.
  - minSdk 28; sem API nova do Android.
  - Sem migração de dados (`WearListStore` mantém `last_list`, `WearListStore.kt:39`).
- **Strings (AGENTS §5):** tudo em `values/strings.xml` (en) e `values-pt-rBR/strings.xml` (pt-BR). Sem literal no Composable (a linha atual `Text("${shopping.emoji} ${shopping.title}".trim())`, `ShoppingWearScreen.kt:73`, vira um Composable de linha com parâmetros).

## 7. Plano de Testes
- **Unitários:**
  - `pickRecentWearLists`: fixadas antes das demais; ordem por `createdAt` desc; limite de 5; ignora finalizadas; não depende de abertura (lista aberta por último não muda a ordem).
  - `wearHeroTotals`: `Σ price × quantity`; `budget` nulo → sem restante; excedente → "excedeu"; comprados fora de pendentes.
  - `WearHomeViewModel`:
    - positivo: `CreateListOnPhone` chama `OpenPhoneRoute` com `cestou://shopping/new`;
    - positivo: `ToggleShowAll` alterna `showAll`; com `showAll` = true a home mostra todas as ativas, não só 5;
    - negativo: `OpenPhoneRoute` falha → `phoneUnavailable = true`;
    - negativo: lista sem produtos → total zero, sem derrubar a home.
  - `WearSearchUiState`: filtro por título (case-insensitive, sem acento se o app já normaliza); arquivadas depois das ativas.
  - Histórico: `GroupHistoryByMonthUseCase` com lista vazia → `months` vazio.
  - `ProfileViewModel` (feature:profile): "Sair" só chama `onSignOut()` depois da confirmação; cancelar não chama.
  - `ShoppingCreateLinkTest` (app): `cestou://shopping/new` → `NewShopping`; outros URIs → nulo.
  - `MainViewModel` (app): `NewShopping` sem login passa pelo login antes de criar (guard `PROTECTED`).
  - MockK e `StandardTestDispatcher`, como em `LastListViewModelTest.kt`.
- **Instrumentados / UI:**
  - `ShoppingWearScreensTest.kt`: trocar asserts de texto (`:58-137`) por tags.
  - Tags: `wear_home_search_pill`, `wear_home_history_pill`, `wear_home_row_<id>`, `wear_home_row_pinned_<id>`, `wear_home_create_on_phone`, `wear_home_settings_button`, `wear_home_skeleton`, `wear_search_field`, `wear_history_month_<n>`, `wear_profile_open_phone`, `wear_profile_sign_out`, `wear_profile_sign_out_confirm`.
  - Cenários: home com 5 linhas e fixada; pílulas abrem as telas; esqueleto; "Criar lista no celular" antes de "Configurações"; "Configurações" leva ao perfil; alerta de "Sair"; `phoneUnavailable`.
  - Previews `@WearPreviewDevices` continuam compilando.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`. Sem `@Suppress`.
- **Validação visual:** emulador Wear ou `mcp__maestro`, comparando com as capturas de referência. Não é gate de CI.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Sem "Continuar", o relógio não tem atalho para a lista aberta por último (Q2 abre na home). | média | médio | Atalho pela própria home: a lista aberta costuma estar entre as 5 mais recentes. Se não bastar, é nova decisão (Q18). |
| `RemoteActivityHelper` sem celular conectado. | média | médio | `phoneUnavailable` e texto claro. |
| Celular sem login ao receber `NewShopping`. | média | baixo | Guard `PROTECTED`. |
| `Navigator` pode não aceitar rota interna (`WearSearch`, `WearHistory`). | baixa | baixo | Se não aceitar, a rota vai para `core:navigation/wear` (público, §6). |
| "Sair" do perfil não limpa `WearListStore`. | média | baixo | Verificar em implementação; acrescentar a limpeza se faltar. |
| Tile (`LastListTileService`) segue com o resumo salvo. | média | médio | Q9. Fora desta entrega. |
| Conflito com sdd-054 / sdd-055 (ambos citam `LastListViewModel` e `ShoppingWearGraph`, `sdd-054:34`, `sdd-055:398`). | média | médio | Rebasear antes de implementar. Não mexer no pareamento. |
| Novo `NavKey` público (`NewShopping`) e contrato `OpenPhoneRoute`. | baixa | médio | §6 do AGENTS: testes, revisão de maintainer, registro no release. |
| `RemoteActivityHelper` rejeitar o intent se usar `ACTION_VIEW`. | baixa | alto | `CATEGORY_BROWSABLE` como em `WearableSyncServiceImpl.kt:175`. Teste de regressão. |

## 9. Perguntas em Aberto

Nenhuma bloqueante. Respondidas: Q1, Q2, Q3, Q4, Q6, Q7, Q12, Q13, Q14.

- [x] **Q1: Spotify.** Recebido, aplicado (§1, §2).
- [x] **Q2: abrir na home.** Sem salto automático.
- [x] **Q3: criar lista pelo celular.** Botão "Criar lista no celular" abre o app no celular em criar lista.
- [x] **Q4: "Sair" com alerta.** Alerta do Wear OS, só do relógio (§4.3). Pedido de "Abrir no celular" e "Sair" ficou dentro de Configurações.
- [x] **Q7: valor igual ao celular.** Total = `Σ product.total`, linha = orçamento, arquivadas = valor pago (§1.1).
- [x] **Q12: "Continuar" em linha.**
- [x] **Q13: Pesquisar = lista completa.** Campo de busca por título (padrão; ver Q17).
- [x] **Q14: "Configurações" no fim da home.** Abre o perfil com "Abrir no celular" e "Sair".
- [x] **Q15: o que é "recentes"?** Respondida: as listas criadas por último (`createdAt`), não as abertas. Fixadas antes, como padrão.
- [x] **Q16: onde fica "Criar lista no celular"?** Respondida: antes de "Configurações", e o botão manda para o app do celular.
- [x] **Q19: "Mais" antes de "Configurações".** Respondida. Leitura adotada: mostra as demais listas ativas na própria home. Se a intenção for outra (ex.: abrir Pesquisar ou um menu de ações), ajustar §2 e §4.2.
- [x] **Q18: atalho para a última lista aberta.** Descartado pelo usuário: não é necessário. A "Continuar" não existe mais na home; `WearListStore` fica só para o tile e para a limpeza ao sair.
- [ ] **Q17 (não bloqueante): histórico e busca.** Default: histórico só com meses e total pago; busca só pelo título. Se quiser gráfico ou detalhe por mês, é nova entrega.
- [ ] **Q4b (não bloqueante): "Sair" avisa o celular?** Default: não. Só o relógio sai.
- [ ] **Q4c (não bloqueante): texto do alerta de sair.** Default: "Sair do relógio? Para ver suas listas aqui de novo, será preciso parear com o celular."
- [ ] **Q5 (não bloqueante): Analytics.** Default: nenhum evento novo.
- [ ] **Q8 (não bloqueante): fixada = favorito.** Default: `isFavorite`.
- [ ] **Q9 (não bloqueante): tile.** Default: não nesta entrega.
- [ ] **Q10 (não bloqueante): celular indisponível.** Default: texto "Não encontramos o celular", botão visível.
- [ ] **Q11 (não bloqueante): celular sem login ao criar.** Default: login primeiro, depois a tela de criar.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` em "Impacto público" (§4.1).
- [x] §6 do AGENTS (novo parâmetro público): necessidade validada no código (§4.1), `public` justificado, testes previstos (§7), integrações afetadas listadas (`core:navigation`, `core:domain`, `core:data`), registro no release (Notas).
- [x] Novo endpoint/remote-config: não se aplica.
- [x] Novo cache: não se aplica (reaproveita `WearListStore`; valores não são cacheados).
- [ ] Perguntas em aberto exauridas (Q18 não bloqueante, Q4b, Q4c, Q5, Q8, Q9, Q10, Q11, Q17 aguardando).

## Notas de release (não executar nesta PR)
- `CHANGELOG.md` e `versionName` são responsabilidade do processo de release (`AGENTS.md` §3 e §6). A versão-alvo é sugestão.
- O `NavKey` público `NewShopping` e o contrato `OpenPhoneRoute` entram no registro de release junto com o deep link `cestou://shopping/new`.
- `fastlane/metadata` só se a mudança afetar a descrição da loja.
