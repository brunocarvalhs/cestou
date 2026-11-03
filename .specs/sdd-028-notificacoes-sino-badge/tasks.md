# Task: Sino de notificações com badge de não lidas e tela de notificações redesenhada

- **Slug:** notificacoes-sino-badge
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** ready
- **Versão-alvo:** 2.3.0 (minor — feature visível; mesmo grupo de `sdd-026`/`sdd-027`)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive — `Notification` e `NotificationRepository.notify` (`core:domain`) ganham campos/parâmetros opcionais; coleção Firestore `notifications` ganha 2 campos opcionais.

## 1. Contexto e Motivação

- O design aprovado (canvas Cestou, artboards "1 · Listas com sino", "3 · Notificações" e "F1 · Fluxo") pede: sino na barra de topo de Listas com badge de não lidas, e tela de Notificações com filtros, agrupamento Hoje/Ontem, tempo relativo, "Marcar lidas" e toque que abre o carrinho da lista.
- Hoje a rota `Notifications` e a tela existem, mas **ninguém navega até elas**: o único item de entrada é um `DropdownMenuItem` cujo `onClick` só fecha o menu (`feature/shopping/.../presentation/screen/ShoppingScreen.kt:139-151`). Não existe contagem de não lidas em lugar nenhum.
- A tela atual tem defeitos que o redesign precisa corrigir (ver §5): tempo hardcoded `"Há pouco"` (`NotificationsViewModel.kt:56`), mapeamento de tipo que nunca casa (`NotificationsViewModel.kt:57-58`), ícones repetidos (`NotificationsScreen.kt:~140-146`), sem estado vazio nem loading.
- Pedido: "adicionar o ícone de notificações, lógica de badge e tela de notificações". Decisões do usuário nesta sessão: (a) escopo **só UI + badge**, sem novos produtores de notificação; (b) **evoluir o schema** com `shoppingId` e `senderId` opcionais para o toque abrir o carrinho.
- Rastreabilidade principal: `core/domain/.../repository/NotificationRepository.kt:6-23`, `core/data/.../repository/NotificationRepositoryImpl.kt:24-79`, `feature/shopping/.../viewmodel/NotificationsViewModel.kt`, `feature/shopping/.../navigation/ShoppingGraph.kt:69-79`, `feature/shopping/.../viewmodel/ShoppingListViewModel.kt:263`, `firestore.rules:303-361`, `architecture/05-decisoes-arquiteturais.md:149-153` (AD-009).

## 2. Escopo

- **Inclui:**
  - Ícone de sino na `TopAppBar` de `ShoppingScreen` com badge (oculto em 0, `9+` acima de 9), com `contentDescription` que inclui a contagem e `testTag`.
  - Contagem de não lidas em tempo real (listener Firestore filtrado por `isRead == false`).
  - Tela de Notificações: chips Todas/Listas, seções Hoje/Ontem/Anteriores, tempo relativo localizado, bolinha de não lida, estados loading/vazio/erro, ação "Marcar lidas".
  - Toque no aviso: marca como lido e abre `CartFlow` da lista (quando há `shoppingId`).
  - Campos opcionais `shoppingId` e `senderId` em `Notification`/`NotificationModel`/`notify`; produtores existentes (`list_joined`, `list_finished`) passam a preenchê-los.
  - Avatar do remetente na linha, igual ao do perfil/carrinho (`UserAvatar(profile)` de `core:ui`, foto ou iniciais), resolvido por `senderId` com `UserRepository.getUserProfile` uma vez por id distinto (mesmo padrão de `CartViewModel.resolveMemberProfiles`, `CartViewModel.kt:141-161`). Sem `senderId` (docs antigos) ou perfil não resolvido: ícone por tipo.
  - Rules: valida membership (remetente e destinatário são membros de `shoppingId`) quando `shoppingId` presente — fecha parte do OPEN ISSUE AD-009.
  - Atualização de `firestore.rules` (additive) e testes.
  - Strings em `values`, `values-pt-rBR` e `values-es` (o módulo já tem os três).
- **Exclui:**
  - Novos tipos/produtores do design (item comprado, item adicionado, orçamento 80%): plano separado.
  - Chip "Lembretes" e itens de lembrete no feed (decidido pelo usuário): o lembrete é notificação **local do sistema** (`feature/settings/.../ShoppingReminderWorker.kt:97`), não passa pela coleção `notifications`.
  - Menu ⋮ (decidido: removido; reintroduzir quando houver conteúdo).
  - Tela de notificações no Wear OS (artboard S2).
  - Push FCM, exclusão de notificações, paginação, i18n server-side do texto (título/mensagem continuam gravados já traduzidos pelo remetente — comportamento atual).
  - Tornar `shoppingId`/`senderId` **obrigatórios** nas rules: fica para release posterior, após adoção do app novo (ver §8).

## 3. Impacto Arquitetural

- **Módulos/camadas:**
  - `:core:domain` — `domain` (modelo + contrato).
  - `:core:data` — `data` (model, mapper, repo impl).
  - `:core:analytics` — constantes de eventos.
  - `:feature:shopping` — `presentation`, `domain`, `data/text`, `di`, strings.
  - `:feature:cart` e `ShoppingJoinUseCase` (em `feature:shopping`) — apenas preencher os novos campos nos produtores.
  - Raiz — `firestore.rules`, `CHANGELOG.md` (via release).
- **Padrão:** Clean Architecture + MVI (intents como data class, estado imutável); Nav3 reaproveitando `Notifications : NavKey` (`core/navigation/.../MobileRoutes.kt:25`) e `CartFlow` (`MobileRoutes.kt:~42`). Sem novas dependências entre features: `feature:shopping` já navega a `CartFlow` via `core:navigation` (`ShoppingListViewModel.kt:263`).
- **Novos arquivos** (base `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/`):
  - `domain/model/NotificationModels.kt` (`NotificationFilter`, `NotificationSectionKey`, `NotificationGroup`, `NotificationTypes`; ficam no domain para o use case não depender de `presentation`)
  - `domain/usecase/ObserveUnreadNotificationsCountUseCase.kt`
  - `domain/usecase/GroupNotificationsUseCase.kt` (puro: filtro + seções + ordenação + teto de 100; `Clock` injetável)
  - `domain/usecase/FormatNotificationTimeUseCase.kt` (tempo relativo)
  - `domain/usecase/MarkAllNotificationsReadUseCase.kt`
  - `domain/text/NotificationTexts.kt` (interface) e `data/text/ResourceNotificationTexts.kt` — mesmo padrão de `ShoppingTexts`/`ResourceShoppingTexts`
  - `presentation/components/notifications/NotificationBellButton.kt` (sino + badge)
  - `presentation/components/notifications/NotificationRow.kt` (usa `UserAvatar`), `NotificationFilterChips.kt`, `NotificationsEmptyState.kt`
  - Testes espelhando cada item (§7).
- **Arquivos alterados:** `NotificationRepository.kt`, `NotificationModel.kt`, `NotificationMapper.kt`, `NotificationRepositoryImpl.kt`, `ShoppingScreen.kt`, `ShoppingListUiState.kt`, `ShoppingListViewModel.kt` (+ intent), `NotificationsScreen.kt`, `NotificationsViewModel.kt`, `NotificationsUiState.kt`, `NotificationsIntent.kt`, `ShoppingGraph.kt`, `ShoppingJoinUseCase.kt`, `FinishPurchaseViewModel.kt`, `AnalyticsEvents.kt`, `strings.xml` x3, `firestore.rules`.

```
 ShoppingScreen (TopAppBar)
   [QR] [🔔 badge N] ──onNotificationsClick──► Navigator.navigate(Notifications)
        ▲ unreadCount                                   │
 ShoppingListViewModel                                  ▼
   └─ ObserveUnreadNotificationsCountUseCase    NotificationsScreen ◄── NotificationsUiState
        └─ NotificationRepository                 ▲        │ intents: onFilter / onMarkAllRead /
           .observeUnreadCount(uid)               │        │          onNotificationClick / onBack
              └─ Firestore: notifications         NotificationsViewModel
                 where userId==uid && isRead==false   ├─ observeNotifications(uid)  (listener completo)
                                                      ├─ GroupNotificationsUseCase  (flowOn Default)
                                                      ├─ markAsRead / markAllAsRead
                                                      └─ tap ► ShoppingGetByIdUseCase ► CartFlow(shopping)
```

- **Hilt:** `NotificationRepository` já é `@Binds @Singleton` (`core/data/.../di/DataModule.kt:63-65`); use cases com `@Inject constructor`; `Clock` fornecido por `@Provides` no `di/` de `feature:shopping` (default `Clock.systemDefaultZone()`); `NotificationTexts` por `@Binds`. Nenhum service locator.

## 4. Contratos e Interfaces

Tudo abaixo só assinaturas.

**`core:domain` — `public` (contrato entre `core:data`, `feature:shopping`, `feature:cart`)**

```kotlin
// NotificationRepository.kt
data class Notification(
    val id: String, val userId: String, val title: String, val message: String,
    val type: String, val isRead: Boolean, val timestamp: Long,
    val shoppingId: String? = null,   // NOVO, opcional (docs antigos não têm)
    val senderId: String? = null      // NOVO, opcional
)

interface NotificationRepository {
    fun observeNotifications(userId: String): Flow<List<Notification>>
    fun observeUnreadCount(userId: String): Flow<Int>                      // NOVO
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun markAllAsRead(notificationIds: List<String>): Result<Unit> // NOVO
    suspend fun notify(
        userId: String, title: String, message: String, type: String,
        origin: NotificationOrigin? = null                                 // NOVO, default null
    ): Result<Unit>
}

/** NOVO (public, mesmo arquivo): lista de origem e membro que disparou. Agrupa os dois campos porque os produtores sempre os enviam juntos e `notify` com 6 parâmetros reprova o detekt `LongParameterList` (limite 6, sem `@Suppress`). */
data class NotificationOrigin(val shoppingId: String, val senderId: String)
```

- **Por que `public`:** a interface (e `NotificationOrigin`) é consumida por 3 módulos (`feature:shopping`, `feature:cart`, `core:data`); não pode ser `internal`. Mudança **additive**: parâmetro com default, campos com default. Consumidores: `NotificationsViewModel`, `ShoppingJoinUseCase`, `FinishPurchaseViewModel`, `NotificationRepositoryImpl` (única implementação). Fakes/mocks nos testes existentes continuam compilando.
- Fluxo obrigatório de AGENTS.md §6: busca de usos feita (acima); testes em §7; docs a atualizar: `architecture/03-modelos-de-dominio.md`/`04` (coleção `notifications`) e `architecture/05` AD-009; `CHANGELOG.md` via release; bump `2.3.0` via `.github/scripts/version.sh` junto do grupo sdd-026/027; revisão de um maintainer.

**`core:data` — `internal`/módulo:** `NotificationModel` ganha `shoppingId: String? = null`, `senderId: String? = null`; `toDomain()`/`toMap()` mapeiam (omitir chaves nulas no `toMap()` para não violar `hasOnly` em clientes velhos / rules).

**`feature:shopping` — `internal`**

```kotlin
internal data class NotificationsUiState(
    val sections: List<NotificationSection> = emptyList(),
    val filter: NotificationFilter = NotificationFilter.ALL,
    val hasUnread: Boolean = false,
    val isLoading: Boolean = true,
    val error: UiText? = null,           // padrão já usado em cart (UiText)
    val senderProfiles: Map<String, UserProfile> = emptyMap()  // senderId -> perfil (core:domain)
)
// NotificationFilter { ALL, LISTS } e NotificationSectionKey { TODAY, YESTERDAY, EARLIER } vivem em domain/model (ver §3)
internal data class NotificationSection(val key: NotificationSectionKey, val items: List<NotificationItem>)
internal data class NotificationItem(
    val id: String, val title: String, val description: String,
    val relativeTime: String, val type: NotificationType,
    val isRead: Boolean, val shoppingId: String?,
    val senderId: String?
)
internal enum class NotificationType { LIST_JOINED, LIST_FINISHED, OTHER }   // substitui ACTION/REMINDER/FEATURE

internal data class NotificationsIntent(
    val onNotificationClick: (String) -> Unit = {},
    val onFilter: (NotificationFilter) -> Unit = {},
    val onMarkAllRead: () -> Unit = {},
    val onErrorShown: () -> Unit = {},
    val onBack: () -> Unit = {}
)
// ShoppingListUiState: + val unreadNotifications: Int = 0
// ShoppingIntent:      + val onNotificationsClick: () -> Unit
```

- `NavKey`: sem alteração (`Notifications` e `CartFlow` já existem).
- **Firestore:** coleção `notifications` — novos campos opcionais `shoppingId` (string ≤ 128) e `senderId` (string, igual a `request.auth.uid`). Regra adicional: se `shoppingId` presente, `uid()` e `userId` ∈ `members(listById(shoppingId))` (1 `get()` extra cobrado por criação). Sem índice composto novo (consulta só com igualdades: `userId` + `isRead`). Sem remote-config.
- **Analytics** (`AnalyticsEvents.kt`): `NOTIFICATIONS_OPENED = "notifications_opened"` (param `unread_count` via `AnalyticsParams`, novo `UNREAD_COUNT`), `NOTIFICATION_CLICKED = "notification_clicked"` (param `type`, novo `NOTIFICATION_TYPE`), `NOTIFICATIONS_MARK_ALL_READ = "notifications_mark_all_read"` (`items_count` já existe). Sem dados pessoais.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Entrada para a tela é um item de menu sem navegação | Sino na `TopAppBar` navega para `Notifications`; item de menu e o `DropdownMenu`/⋮ saem (decisão do usuário) | `ShoppingScreen.kt:139-151` |
| 2 | Não há contagem de não lidas | `unreadCount` = nº de docs com `userId == uid && isRead == false`; badge oculto em 0; texto `N` até 9, `9+` acima; atualiza em tempo real | novo; `FirestoreClient.kt:219-244` (listener com filtros de igualdade) |
| 3 | Usuário sem sessão: `observeNotifications` retorna sem emitir | Contagem 0 e lista vazia sem erro | `NotificationsViewModel.kt:39` |
| 4 | Falha de rede em `observeNotifications` é engolida e emite lista vazia (indistinguível de "sem avisos") | Mantém emissão de vazio no repo (compat.), mas a ViewModel não exibe erro de listener; erro só em ações (mark read / abrir lista) via `UiText` | `NotificationRepositoryImpl.kt:32-35` |
| 5 | `type` string → enum por `valueOf` nunca casa com `list_joined`/`list_finished`, tudo cai em `FEATURE` | Mapa explícito: `list_joined → LIST_JOINED`, `list_finished → LIST_FINISHED`, qualquer outro → `OTHER` (aparece em Todas, não em Listas) | `NotificationsViewModel.kt:57-58`, `ShoppingJoinUseCase.kt:43`, `FinishPurchaseViewModel.kt:27` |
| 6 | Tempo exibido é literal `"Há pouco"` | Tempo relativo localizado: <1 min "agora", <60 min "há N min", <24 h "há N h", senão data curta; seção por data local: Hoje, Ontem, Anteriores | `NotificationsViewModel.kt:56`; AGENTS.md §5 (strings) |
| 7 | Ordem = a do snapshot (sem `orderBy`) | Ordenação por `timestamp` desc no `GroupNotificationsUseCase` (client-side, evita índice) | `NotificationRepositoryImpl.kt:24-37` |
| 8 | Toque só chama `markAsRead` | Toque: `markAsRead(id)` e, se `shoppingId != null`, carrega a lista (`ShoppingGetByIdUseCase`) e navega `CartFlow(shopping)`. Sem `shoppingId` (docs antigos): só marca lida. Lista ausente/sem permissão (saiu da lista): mantém na tela, marca lida e mostra snackbar "Lista indisponível" | `NotificationsViewModel.kt:63-67`; `ShoppingListViewModel.kt:263`; F1 do design |
| 9 | Não existe "marcar todas" | `markAllAsRead(ids)` só dos **não lidos visíveis** (filtro selecionado e teto de 100 aplicados; `hasUnread` segue o mesmo critério); atualiza uma vez por doc (rules só permitem flip de `isRead` por doc); botão desabilitado/oculto sem não lidos | `firestore.rules:346-349` |
| 10 | Rules fixam as chaves exatas do payload (`hasOnly`/`hasAll` das 7 chaves) | Chaves base obrigatórias; `shoppingId`/`senderId` opcionais; se `senderId` presente, deve ser `== uid()`; se `shoppingId` presente, remetente e destinatário devem ser membros da lista; `type` continua restrito a `list_joined`/`list_finished` | `firestore.rules:333-343` |
| 11 | Produtores gravam sem referência à lista/autor | `ShoppingJoinUseCase` e `FinishPurchaseViewModel` passam `shopping.id` e o id do ator | `ShoppingJoinUseCase.kt:38`, `FinishPurchaseViewModel.kt:101` |
| 12 | Filtros do design: Todas, Listas, Lembretes | Implementa Todas e Listas; "Lembretes" fora (decisão do usuário) |
| 13 | Linha sem avatar do remetente | `UserAvatar` do remetente via `senderId` → `getUserProfile`; cache em memória por ViewModel, uma busca por id distinto, sem busca por linha | `UserAvatars.kt` (`core:ui`), `CartViewModel.kt:141-161`; `firestore.rules` `/users` get liberado a logados | design B3; `ShoppingReminderWorker.kt:97` |

## 6. Critérios de Aceitação

- **Funcional**
  - Sino visível na tela Listas; com 0 não lidas não há badge; com 3 mostra "3"; com 12 mostra "9+".
  - Tocar no sino abre a tela; voltar retorna a Listas e o badge reflete as leituras feitas.
  - Tela mostra seções Hoje/Ontem/Anteriores, ordenadas do mais recente ao mais antigo, bolinha nas não lidas, chips Todas/Listas filtrando.
  - Toque em aviso com `shoppingId` abre o carrinho da lista e o aviso fica lido; "Marcar lidas" zera o badge.
  - Estados: loading (primeira emissão), vazio (ilustração/texto, com e sem filtro), erro de ação em snackbar.
- **Performance**
  - Um listener leve para o badge (apenas não lidas) na ViewModel de Listas e um listener completo só enquanto a tela de Notificações estiver na pilha; ambos coletados com `stateIn(SharingStarted.WhileSubscribed(5_000))` / ciclo da ViewModel, sem listener duplicado por recomposição.
  - Agrupamento/formatação feitos na ViewModel (Main) sobre no máximo 100 itens — **desvio deliberado**: `flowOn(Dispatchers.Default)` exigiria injetar dispatcher, e `@Inject` não aceita parâmetro com default (padrão de `ShoppingRepositoryImpl` não se aplica a ViewModel Hilt); `LazyColumn` com `key = id` e `contentType`; itens `@Stable`; lambdas estáveis para evitar recomposição de todas as linhas ao marcar uma como lida.
  - `markAllAsRead` envia as escritas em paralelo limitado (ex.: `async` por doc, concorrência 10) fora da Main; sem I/O duplicado (não reler a lista após marcar — o snapshot já atualiza).
  - Sem cache próprio: o Firestore já mantém cache offline; a contagem não é persistida.
- **Feature toggles:** nenhum. Mudança é aditiva e o risco de rules é coberto pela ordem de deploy (§8). Justificativa: não existe remote-config para UI; reverter = reverter release.
- **Observabilidade:** eventos de §4 disparados via `core:analytics`; falhas de markAsRead/abrir lista registradas com `Timber` sem texto da notificação (pode conter nome de usuário — AGENTS.md §5).
- **Acessibilidade/i18n:** `contentDescription` do sino com plural ("Notificações, 3 não lidas"); badge `clearAndSetSemantics`; áreas de toque ≥ 48 dp (design usa 44 dp — usar 48 dp do Material); todas as strings em pt-BR, en e es; sem texto literal em Composable.
- **Compatibilidade:** minSdk 28 (`java.time` disponível); documentos antigos sem `shoppingId`/`senderId` decodificam (`ignoreUnknownKeys` + defaults nulos, `NetworkServiceImpl.kt:~25`); app novo contra rules antigas **falha na criação** (chaves extras) → rules devem ser publicadas antes (§8); Wear OS inalterado.

## 7. Plano de Testes

- **Unitários (JUnit + coroutines-test + Turbine/mockk, já usados em `NotificationRepositoryImplTest`)**
  - `NotificationRepositoryImplTest`: `observeUnreadCount` emite tamanho da lista filtrada por `isRead=false`; emite 0 em erro/`null`; `markAllAsRead` chama PUT por id e agrega falha parcial em `Result.failure`; `notify` inclui `shoppingId`/`senderId` quando presentes e **omite** quando nulos; `toDomain` com campos ausentes.
  - `NotificationMapperTest`: ida e volta com e sem campos novos.
  - `GroupNotificationsUseCaseTest`: relógio fixo; fronteiras 23:59/00:00 e fuso; ordenação; filtro `LISTS` exclui `OTHER`; lista vazia; tipo desconhecido.
  - `NotificationsViewModelTest`: loading→conteúdo; vazio; usuário nulo; toque com `shoppingId` navega a `CartFlow` e marca lida; toque sem `shoppingId`; lista inexistente → erro `UiText` e marcada lida; `onMarkAllRead` só com não lidas; troca de filtro; não repete listener ao recompor.
  - `ShoppingListViewModelTest`: `unreadNotifications` reflete o fluxo; `onNotificationsClick` navega para `Notifications`; sem sessão = 0.
  - `ShoppingJoinUseCaseTest` e `FinishPurchaseViewModelTest`: verificam `notify(..., shoppingId = shopping.id, senderId = actor)` (ajustar mocks existentes).
  - Teste de formatação do `ResourceNotificationTexts` (agora/min/h/data) por locale.
- **UI (Robolectric/Compose, seletores por `testTag`; app roda em pt-BR ou en — sem assert de texto):** `notifications_bell`, `notifications_badge` (existe com N>0, ausente em 0), `notifications_list`, `notification_item_<id>`, `notification_unread_dot_<id>`, `notifications_mark_all_read`, `notifications_filter_all`/`_lists`, `notifications_empty`; atualizar `NotificationsScreenTest` e `ShoppingComponentsTest` onde cobrir o header.
- **Rules:** casos no emulador/arquivo de teste de rules se houver (`firestore.rules` tem comentário de coleção; verificar suíte existente ao implementar): create com/sem campos opcionais, `senderId` ≠ uid negado, `type` fora da allowlist negado, update só `isRead`.
- **Comandos:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`; instrumentados de UI se tocar `core:ui`. **Não** usar `@Suppress` (corrigir `LongMethod`/`CyclomaticComplexMethod` extraindo composables).
- **Maestro (futuro, memória do projeto):** fluxo deve usar os `testTag`s acima, nunca texto.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| App novo grava `shoppingId`/`senderId` antes das rules novas serem publicadas → `create` negado, usuários deixam de notificar | média | alto | Publicar `firestore.rules` **antes** do release; campos opcionais mantêm clientes antigos válidos; nota no PR e em `.github/PIPELINE.md` se o deploy de rules for manual |
| Badge não decrementa/duplica listeners (ViewModel de Listas viva na pilha + tela) | baixa | médio | Listener de contagem restrito a não lidas; teste de ViewModel; `WhileSubscribed` |
| Coleção sem paginação/limite: usuário com muitos avisos baixa tudo a cada abertura | baixa (base real minúscula, ver memória do projeto) | médio | Ordenar e **cortar para os N mais recentes** na camada de apresentação (N=100) e registrar paginação como follow-up; verificar se `FirestoreClient` suporta `limit` antes de implementar |
| `markAllAsRead` com muitas escritas parciais | baixa | baixo | Concorrência limitada, `Result.failure` agregado, snapshot reconcilia o estado real |
| Texto gravado já traduzido pelo remetente (aparece em idioma errado para o destinatário) | alta (já existe) | baixo | Fora de escopo; registrar follow-up (guardar chaves + argumentos em vez de texto) |
| Docs sem `shoppingId` (anteriores ao release) | alta | baixo | Toque só marca lida; sem erro |
| Remover o menu ⋮ diverge do design | — | baixo | Decisão do usuário; reintroduzir quando houver conteúdo |
| Membership nas rules só vale quando `shoppingId` vem no payload: cliente malicioso pode omiti-lo e continuar enviando avisos arbitrários a um uid conhecido | baixa | médio | Aceito nesta release para não quebrar clientes antigos; follow-up torna os dois campos obrigatórios (base de usuários reais é minúscula, ver memória do projeto) |
| Custo extra de 1 `get()` por criação de notificação (por destinatário) nas rules | baixa | baixo | Volume baixo; plano Spark tem franquia diária — conferir no console após o release |
| N buscas de perfil ao abrir a tela | baixa | baixo | Uma por `senderId` distinto, em paralelo, cache na ViewModel; fallback para ícone se falhar |
| `ShoppingHeader` (bell sem ação, `ShoppingHeader.kt:~85-92`) sugere uma segunda entrada | baixa | baixo | Verificado: só é usado em preview/teste (`ShoppingComponentsTest.kt:151`), código morto; não tocar (cai na refatoração de componentes já planejada) |

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma. Decisões do usuário em 2026-11-02:

- Escopo só UI + badge; evoluir schema com `shoppingId`/`senderId`.
- Menu ⋮ removido; chip "Lembretes" fora; teto do badge `9+`.
- Avatar do remetente igual ao do perfil, via `senderId`.
- Rules: validar membership quando `shoppingId` presente (recomendação aceita pelo fluxo; tornar campos obrigatórios em release futura).

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/05` AD-009 / `firestore.rules`).
- [x] Nenhum código de produção escrito (apenas assinaturas e diagrama).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` em "Impacto público".
- [x] Novo evento de Analytics com nome canônico; sem novo endpoint nem remote-config (toggle justificado como inexistente).
- [x] Sem novo cache: o cache offline do Firestore é suficiente (justificado em §6).
- [x] Perguntas bloqueantes exauridas; pendências não bloqueantes registradas em §9.
- [ ] Ao implementar: atualizar `architecture/03`/`04`/`05`, `CHANGELOG.md` (release) e conferir `fastlane/metadata`.
