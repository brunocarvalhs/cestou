# Task: Receber campanhas via FCM e gravá-las no feed de notificações

- **Slug:** fcm-campanhas
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** ready
- **Versão-alvo:** 2.3.0 (minor — feature; mesmo grupo do `sdd-028`, de que depende)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive — `NotificationRepository` e um modelo novo em `core:domain` ganham símbolos; coleção Firestore `notifications` ganha o `type` `campaign`.

> **Dependência:** este plano parte do código do `sdd-028` (PR #250: `NotificationOrigin`, `observeUnreadCount`, tela de Notificações, regras com `shoppingId`/`senderId`). Implementar **depois** que o #250 estiver em `develop`.

## 1. Contexto e Motivação

- O app só tem notificações geradas por membros (`list_joined`, `list_finished`), gravadas direto no Firestore pelo cliente porque não há servidor (`firestore.rules:303-311`, `architecture/05-decisoes-arquiteturais.md:149-153`). Não existe FCM no projeto: nenhuma dependência `firebase-messaging`, nenhum `FirebaseMessagingService`, nenhuma Cloud Function (busca em `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `firebase.json`).
- Pedido do usuário: usar o **FCM para campanhas**, **sem servidor próprio** — a campanha sai do console do Firebase; ao chegar, o app captura os campos do FCM, completa o que falta (id, userId, isRead, timestamp, type) e grava em `notifications`, alimentando o sino, o badge e a tela do `sdd-028`.
- Restrição técnica que molda o desenho: o FCM só envia a partir de um servidor/console. O app **não** envia push a outros aparelhos; este plano trata só de **receber** campanhas.
- Rastreabilidade: `app/src/main/AndroidManifest.xml:30-33,103-120` (Application, `MainActivity` `singleTask`), `app/.../MainActivity.kt:62-95` (`consumeDeepLinkIntent`), `app/.../MainViewModel.kt:150-156` (`onDeepLink`), `core/data/.../network/FirestoreClient.kt` `post` (`set(data)` com `data["id"]` como id do documento), `feature/settings/.../ShoppingReminderWorker.kt:60-78` (canal e checagem de `POST_NOTIFICATIONS`).

## 2. Escopo

- **Inclui:**
  - Dependência `firebase-messaging` e um `FirebaseMessagingService` (`CestouMessagingService`) no `:app`.
  - Captura de campanhas nos dois caminhos do FCM: `onMessageReceived` (app em primeiro plano) e extras da `Intent` de abertura (usuário tocou na notificação da bandeja com o app em segundo plano ou fechado).
  - Contrato de autoria da campanha (campos de dados customizados no console) e preenchimento dos campos que faltam.
  - Gravação idempotente em `notifications` (id derivado do `message_id` do FCM), `type = campaign`.
  - Regras do Firestore para o novo `type`, restritas a "para si mesmo".
  - Exibição no feed (ícone próprio, entra em "Todas" e fora de "Listas"), badge automático via `observeUnreadCount`.
  - Canal de notificação e metadados padrão do FCM (ícone, cor, canal).
  - Analytics, testes, documentação do contrato de campanha.
- **Exclui:**
  - Qualquer envio de push pelo app, Cloud Functions, backend ou chave de servidor no APK.
  - Push segmentado/transacional (ex.: "Ana finalizou a lista") — continua só no feed, como hoje.
  - Armazenar token FCM (`onNewToken` não grava nada): campanhas do console por app/segmento/tópico não exigem token do nosso lado.
  - Abrir a tela de Notificações ou uma lista ao tocar (ver §9.2).
  - Pedido de permissão `POST_NOTIFICATIONS` em tempo de execução (ver §8 e §9.1).
  - Wear OS.

## 3. Impacto Arquitetural

- **Módulos/camadas:**
  - `:app` — `service` (entrada FCM e `Intent`), `di`; é o único módulo com manifesto e `MainActivity`.
  - `:core:domain` — `domain` (modelo, contrato de repositório, use case); mantém-se sem Android.
  - `:core:data` — `data` (implementação de `save`).
  - `:core:analytics` — constantes de eventos.
  - `:feature:shopping` — apresentação do novo tipo no feed.
  - Raiz — `firestore.rules`, `firestore-tests/rules.test.mjs`, `architecture/`.
- **Padrão:** Clean Architecture; o Android (`RemoteMessage`, `Intent`) fica na borda do `:app` e vira um `PushPayload` puro antes de entrar no domínio.
- **Novos arquivos:**
  - `app/src/main/java/br/com/brunocarvalhs/howmuch/push/CestouMessagingService.kt`
  - `app/src/main/java/br/com/brunocarvalhs/howmuch/push/PushPayloadExtractor.kt` (`RemoteMessage`/`Intent` → `PushPayload`)
  - `app/src/main/java/br/com/brunocarvalhs/howmuch/push/PushChannel.kt` (cria o canal)
  - `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/model/PushPayload.kt`
  - `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/usecase/SavePushNotificationUseCase.kt`
  - Testes espelhando cada item (§7).
- **Alterados:** `gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt`, `MainViewModel.kt`, `NotificationRepository.kt`, `NotificationRepositoryImpl.kt`, `AnalyticsEvents.kt`, `firestore.rules`, `firestore-tests/rules.test.mjs`, `NotificationModels.kt` e `NotificationsUiState.kt`/`NotificationRow.kt`/`NotificationsViewModel.kt` do `sdd-028`, `strings.xml` (3 locales de `app` e `feature:shopping`), `architecture/03`, `04`, `05`.

```
 Console do Firebase (campanha: notification + dados customizados title/message)
        │
        ▼ FCM
 ┌───────────────────────────────────────────────────────────────────────────┐
 │ App em primeiro plano ─► CestouMessagingService.onMessageReceived          │
 │ App em 2º plano/morto ─► bandeja do sistema ─toque─► MainActivity(Intent)  │
 │                                          └► MainViewModel.onPushIntent     │
 └───────────────┬───────────────────────────────────────────────────────────┘
                 ▼  PushPayloadExtractor  (Android → PushPayload puro)
          SavePushNotificationUseCase
            ├─ valida: título e mensagem não vazios; trunca 200 / 500
            ├─ completa: id="fcm-<message_id>", userId=AuthService.currentUser,
            │            isRead=false, type="campaign", timestamp=sentTime ?: agora
            └─ NotificationRepository.save(notification)
                     └─ Firestore: set(notifications/fcm-<message_id>)   (idempotente)
                              ▼ snapshot listener (sdd-028)
                   badge do sino  +  tela de Notificações (ícone de campanha)
```

- **Hilt:** `CestouMessagingService` com `@AndroidEntryPoint` (Hilt suporta `Service`); injeta o use case e o `AnalyticsTracker`. Nenhum service locator. O Firebase é inicializado por `FirebaseInitializer` (`core/data/.../initializer/FirebaseInitializer.kt`, via androidx.startup), pois `FirebaseInitProvider` está removido do manifesto (`AndroidManifest.xml:92-96`); o serviço roda depois de `Application.onCreate`, então o Firestore já está disponível (confirmar na implementação, ver §8).

## 4. Contratos e Interfaces

**`core:domain` — `public`** (consumidos por `:app` e implementados em `:core:data`; não podem ser `internal`)

```kotlin
// model/PushPayload.kt — dados crus de uma mensagem FCM, sem tipos do Android
data class PushPayload(
    val messageId: String?,          // google.message_id / RemoteMessage.messageId
    val sentTimeMillis: Long?,       // google.sent_time / RemoteMessage.sentTime
    val notificationTitle: String?,  // RemoteMessage.notification.title (só no 1º plano)
    val notificationBody: String?,   // RemoteMessage.notification.body  (só no 1º plano)
    val data: Map<String, String>    // pares customizados da campanha
)

// repository/NotificationRepository.kt
interface NotificationRepository {
    // ...membros existentes do sdd-028...
    /** Grava [notification] com o id já definido; [SaveResult.AlreadySaved] se o documento já existia. */
    suspend fun save(notification: Notification): Result<SaveResult>
}
enum class SaveResult { Saved, AlreadySaved }

// usecase/SavePushNotificationUseCase.kt
class SavePushNotificationUseCase @Inject constructor(
    authService: AuthService, notificationRepository: NotificationRepository, clock: Clock
) {
    suspend operator fun invoke(payload: PushPayload): PushSaveOutcome
}
sealed interface PushSaveOutcome { Saved; AlreadySaved; Ignored(reason); Failed(cause) }
```

- `Notification.type` continua `String`; nova constante `NotificationTypes.CAMPAIGN = "campaign"` em `feature/shopping/.../domain/model/NotificationModels.kt` e valor `CAMPAIGN` em `NotificationType` (`internal`).
- **Contrato de autoria da campanha** (campos "dados customizados" do Compositor de notificações do Firebase):

  | Chave | Obrigatória | Uso |
  |---|---|---|
  | `title` | sim | título gravado no feed (máx. 200 caracteres, o excedente é cortado) |
  | `message` | sim | texto gravado no feed (máx. 500) |

  Por quê dados customizados e não o bloco "Notificação": quando o app está em segundo plano ou fechado, o sistema desenha a notificação e entrega ao app **apenas os dados customizados** e as chaves `google.*` na `Intent` de abertura — título e corpo do bloco "Notificação" **não** chegam. Em primeiro plano, `title`/`body` do bloco "Notificação" são usados como fallback se as chaves não existirem.
- **Firestore:** documento `notifications/fcm-<message_id sanitizado>` com o mesmo formato de hoje (`id, userId, title, message, type, isRead, timestamp`), `type = "campaign"`, sem `shoppingId`/`senderId`. Sanitização: caracteres fora de `[A-Za-z0-9_-]` viram `_` (ids de documento não aceitam `/`).
- **Analytics** (`AnalyticsEvents.kt`): `PUSH_RECEIVED = "push_received"` (param `source`: `foreground` | `tap`), `PUSH_SAVED = "push_saved"`, `PUSH_IGNORED = "push_ignored"` (param `reason`), `PUSH_SAVE_FAILED = "push_save_failed"`. Nenhum conteúdo da campanha nos parâmetros.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Só existem notificações de membros (`list_joined`/`list_finished`) | Nova origem: `campaign`, vinda do FCM; entra no feed e no badge como qualquer outra | `firestore.rules:~345` (allowlist de `type`) |
| 2 | Rules: `type in ['list_joined','list_finished']` e qualquer logado grava para qualquer `userId` | `type == 'campaign'` só é aceito se `userId == uid()` e sem `shoppingId`/`senderId`. Impede que alguém envie "campanha" falsa a outro usuário | `firestore.rules:303-361` |
| 3 | Tocar numa notificação marca lida e, se há `shoppingId`, abre o carrinho | Campanha não tem `shoppingId`: o toque só marca como lida | `NotificationsViewModel` (sdd-028) |
| 4 | Filtro "Listas" mostra só tipos de lista | Campanha aparece em "Todas" e não em "Listas" | `GroupNotificationsUseCase` (sdd-028) |
| 5 | Sem sessão, `observeNotifications` não emite | Campanha recebida sem usuário logado é **descartada** (`Ignored(NO_USER)`): as rules exigem login e o app exige login para uso (`FirebaseAuthService.kt:88-109`) | `FirebaseAuthService.kt:107` |
| 6 | — | Idempotência: o mesmo `message_id` chega por dois caminhos (1º plano e toque) ou é reenviado; o segundo `set` vira `update` e as rules o negam (só `isRead` pode mudar) → tratado como `AlreadySaved`, sem erro para o usuário | `firestore.rules:~362`, `FirestoreClient.post` |
| 7 | — | Mensagem sem `title` e sem `message` (nem fallback) → `Ignored(EMPTY)`; sem `message_id` → id aleatório (`fcm-<uuid>`), sem deduplicação possível | novo |
| 8 | `timestamp` é o `System.currentTimeMillis()` do remetente | `timestamp = sentTime` do FCM quando existir (data real da campanha), senão agora | `NotificationRepositoryImpl.notify` |
| 9 | App em primeiro plano não exibe notificação do FCM | Nada na bandeja; o usuário vê o badge subir em tempo real. Não se desenha notificação local | comportamento do FCM |

## 6. Critérios de Aceitação

- **Funcional**
  - Campanha enviada pelo console com `title` e `message` aparece no feed com o ícone de campanha e incrementa o badge, nos três estados do app: primeiro plano, segundo plano (após tocar na notificação) e fechado (após tocar).
  - A mesma campanha chegando pelos dois caminhos gera **um** documento.
  - Campanha com texto acima do limite é cortada, não rejeitada.
  - Sem usuário logado: nada é gravado e nada quebra.
- **Segurança:** nenhum segredo/chave de servidor no repositório ou APK; `onMessageReceived` não loga título/mensagem (AGENTS.md §5); as rules impedem `campaign` para outro `userId`.
- **Performance / threading**
  - `onMessageReceived` roda em thread de fundo com ~20 s de janela: a gravação usa `runBlocking(Dispatchers.IO)` com `withTimeout(10 s)`; estourar o tempo **não perde** a campanha — o Firestore já enfileirou a escrita no cache local e a reenvia depois (offline persistence habilitada por padrão).
  - Um único `set`; sem leitura prévia (leitura de documento inexistente é negada pelas rules).
  - Caminho da `Intent` roda no `viewModelScope`, uma vez por intent (a intent é limpa após o consumo, como em `MainActivity.kt:92-95`).
  - Sem alocação por frame; nenhuma mudança de recomposição além de um novo ícone no `NotificationRow`.
- **Feature toggles:** nenhum. Justificativa: a feature é passiva — só age se alguém enviar uma campanha; reverter é parar de enviar campanhas ou reverter a release.
- **Observabilidade:** eventos de §4; falhas com `Timber` (só ids/códigos, nunca o texto) e Crashlytics apenas para falhas inesperadas.
- **Compatibilidade:** minSdk 28; canal de notificação criado em API ≥ 26; `POST_NOTIFICATIONS` (API 33+) — ver §8; Wear OS inalterado; documentos antigos inalterados.
- **Deploy:** publicar `firestore.rules` **antes** da versão do app (a versão antiga do app não grava `campaign`, mas o app novo falha se as rules antigas ainda estiverem no ar).

## 7. Plano de Testes

- **Unitários** (JUnit + coroutines-test + mockk, como no restante)
  - `SavePushNotificationUseCaseTest`: payload completo; sem `data` e com fallback `notification.title/body`; título/mensagem vazios → `Ignored`; truncamento em 200/500; sem `message_id` → id aleatório; `sentTime` ausente → relógio; sem usuário → `Ignored(NO_USER)`; repositório devolve `AlreadySaved`; repositório falha → `Failed`.
  - `PushPayloadExtractorTest` (Robolectric): `RemoteMessage` em primeiro plano; `Intent` com extras `google.message_id`, `google.sent_time` e dados customizados; `Intent` sem extras do FCM (ex.: abertura normal ou deep link de convite) → `null`.
  - `NotificationRepositoryImplTest`: `save` envia `POST` com id fixo; `PERMISSION_DENIED` → `AlreadySaved`; outra falha → `Result.failure`; cancelamento preservado.
  - `MainViewModelTest`: `onPushIntent` chama o use case uma vez; intent sem FCM não chama; não interfere em `onDeepLink`.
  - `GroupNotificationsUseCaseTest`/`NotificationsViewModelTest`: `campaign` em "Todas", fora de "Listas"; toque só marca lida.
  - Teste do serviço (Robolectric): `onMessageReceived` chama o use case e registra o evento; exceção do use case não derruba o serviço.
- **Rules (emulador, `firestore-tests/rules.test.mjs`):** `campaign` para si mesmo → ok; para outro `userId` → negado; com `shoppingId`/`senderId` → negado; `isRead: true` → negado; segundo `set` no mesmo id → negado (duplicata).
- **UI:** `NotificationRow` com `NotificationType.CAMPAIGN` por `testTag`.
- **Manual (obrigatório antes do release, não automatizável aqui):** enviar uma campanha de teste pelo console (dispositivo de teste via token) nos três estados do app.
- **Comandos:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`. Sem `@Suppress`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Em segundo plano/fechado, a campanha só entra no feed se o usuário **tocar** na notificação (o console envia sempre o bloco "Notificação", e o app não é chamado nesse caso; mensagem só de dados exigiria API/servidor) | alta | médio | Aceito e documentado no contrato de campanha; o badge e o feed refletem apenas campanhas "vistas" (tocadas ou recebidas em primeiro plano) |
| `POST_NOTIFICATIONS` (API 33+) nunca é pedida em tempo de execução: só há `checkSelfPermission` no lembrete (`ShoppingReminderWorker.kt:71-79`) e nenhum `RequestPermission` para ela no repositório. Sem a permissão a bandeja não mostra a campanha em segundo plano e o fluxo "tocar" nunca acontece | alta | alto | Ver §9.1. Documentado; sem decisão do usuário, não se adiciona prompt novo |
| Campos do console divergem do contrato (ex.: autor esquece `title`/`message`) | média | médio | `Ignored(EMPTY)` + evento `push_ignored`; fallback para `notification.*` em primeiro plano; guia de autoria em `architecture/04` |
| `set` duplicado vira `update` e a regra o nega: `PERMISSION_DENIED` é lido como "já salvo" e esconde uma negação real (ex.: rules fora do ar) | baixa | médio | Tratar só `PERMISSION_DENIED` como duplicata; o evento `push_saved` vs `push_ignored` e o teste de rules cobrem o caminho feliz; negações recorrentes aparecem como queda de `push_saved` |
| Firestore ainda não pronto quando o serviço acorda o processo (init via androidx.startup) | baixa | médio | Teste manual com app morto; se necessário, forçar `FirebaseApp.getInstance()` no início do serviço |
| Estouro da janela de ~20 s do `onMessageReceived` | baixa | baixo | `withTimeout(10 s)`; a escrita já está enfileirada localmente |
| Mensagem com texto de terceiros no feed (campanha forjada por quem controla o projeto do console) | baixa | baixo | Só quem tem acesso ao console envia; as rules negam `campaign` para outro `userId` |
| Aumento do APK (`firebase-messaging` + `iid`) | baixa | baixo | O pipeline mede tamanho (`.github/pipeline-config.yaml`, baseline `size_check`); atualizar o baseline se necessário |

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma bloqueante. Não bloqueantes (o plano segue com o default indicado):

- [ ] 9.1 **Permissão `POST_NOTIFICATIONS`:** sem pedi-la em tempo de execução, usuários da API 33+ que nunca a concederam não verão campanhas na bandeja em segundo plano. Default: **fora do escopo** (nada de prompt novo). Se quiser incluir, a proposta é pedir uma vez, em contexto, ao abrir a tela de Notificações, com texto explicando o porquê.
- [ ] 9.2 **Tocar na campanha:** default = abre o app como hoje (sem navegar). Alternativa: abrir direto a tela de Notificações (`Notifications` NavKey) — exige tratar a navegação em `CestouApp`, hoje só tratada para convites (`MainViewModel.kt:149-168`).
- [ ] 9.3 **Tópico:** default = não assinar tópicos (envio pelo console por app/segmento). Se quiser campanhas por tópico (ex.: `all`), basta assinar em `onNewToken`/login; fica como follow-up.
- [ ] 9.4 **Imagem/ação na campanha:** o bloco "Notificação" aceita imagem; default = ignorada no feed.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/05` AD-009).
- [x] Nenhum código de produção escrito (apenas assinaturas e diagramas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` em "Impacto público".
- [x] Sem novo endpoint de servidor nem remote-config; toggle justificado como inexistente; observabilidade planejada.
- [x] Sem novo cache: persistência offline do Firestore cobre a escrita pendente (§6).
- [x] Perguntas bloqueantes exauridas; pendências não bloqueantes em §9.
- [ ] Ao implementar: atualizar `architecture/03`/`04`/`05`, `.github/PIPELINE.md` se mudar o baseline de tamanho, `CHANGELOG.md` via release e `fastlane/metadata` se afetar a loja.
