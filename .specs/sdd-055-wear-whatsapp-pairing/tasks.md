# Task: Pareamento do Wear OS em estilo WhatsApp (códigos iguais nos dois aparelhos)

- **Slug:** wear-whatsapp-pairing
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-10
- **Status:** ready, com ajustes pendentes da revisão (`AJUSTES NECESSARIOS`, seção 11). Não mergear antes dos itens bloqueantes.
- **Versão-alvo:** 2.6.0 (feature). Atenção: o template pede `major` para breaking; a quebra de protocolo entre o app do celular e o `:wear` é breaking. Ver pergunta não bloqueante P-3 (seção 9).
- **Tipo de mudança:** breaking-change (protocolo de pareamento entre `:app` e `:wear`), com feature nova.
- **Impacto público (módulo):** breaking. `WearableSyncService`, `WearablePaths`, `PairingCodeHolder` (vira `PairingSessionHolder`), `MobileRoutes` (`PairingCode` removido, `WearPairing` criado) e o deeplink `cestou://wear/pair`.

## 1. Contexto e Motivação

- Pedido do usuário (2026-11-10, `/code-planner`): abrir o app no relógio; o relógio se comunica com o celular; por deeplink o celular abre o fluxo de sincronização. Se der certo, **os dois aparelhos mostram o mesmo código**; se não, o relógio mostra uma mensagem explicando o erro. Depois, botão **Parear** no celular e no relógio; em seguida sincroniza, o relógio recebe o token do usuário logado e passa a abrir direto na home.
- Referência visual (capturas em `/Users/bruno/Desktop/login - wear os - whastapp/`): fluxo "Conectando" → erro com "Tentar novamente" → código de 8 letras (2 × 4) igual nos dois aparelhos com botão "Confirmar" → "Carregando" → erro com "Sair". **Usar só o padrão de fluxo.** Textos, ícones e marca são do WhatsApp; os textos novos devem ser do Cestou.
- Hoje o pareamento é por código de 6 dígitos digitado no celular:
  - Relógio gera o código e guarda só em memória: `core/common/.../wearable/PairingCodeHolder.kt:14`, `core/data/.../WearableSyncServiceImpl.kt:78-86` e `:226-232`.
  - Celular busca o nó e confirma o código por mensagem com timeout de 5 s: `WearableSyncServiceImpl.kt:88-157`, chamado por `feature/auth/.../viewmodel/LinkWearViewModel.kt:66`.
  - Envio do par `uid + idToken` e login no relógio: `WearableSyncServiceImpl.kt:159-175` (`sendAuthPairToNode`), `LinkWearViewModel.kt:74`, `wear/.../service/WearAuthListenerService.kt:34-44`.
- Já existe `openPhoneApp()` no relógio (`WearableSyncServiceImpl.kt:194-224`, `RemoteActivityHelper`), mas **sem chamador de produção** (busca em `app`, `core`, `feature`, `wear`: só interface, impl e teste). É a base do passo "relógio abre o celular".
- Deeplink do celular já existe para outros fins: `app/src/main/AndroidManifest.xml:102-108` (`cestou://chat`) e `MainViewModel.kt:186-197` (`onDeepLink`). Padrão de espera por login: `MainActivity.kt:238-247` (`pendingJoinToken`).
- Gate de Pro: o CHANGELOG diz "gate ... Wear pairing behind Pro" (`CHANGELOG.md:416`). No código, a checagem está só na linha do Perfil (`feature/profile/.../screen/ProfileScreen.kt:94-98`). O deeplink contorna essa tela, então o gate precisa existir também no celular ao receber o pedido (ver regra 8 da seção 5).
- Relação com `sdd-054-wear-bluetooth-sync` (não versionado, `status: blocked`): trata de trocar o código por Bluetooth. Este plano **substitui a parte de descoberta e confirmação do código** do `sdd-054`. Recomendo marcar o `sdd-054` como superado ao aprovar este plano. Não foi tocado.

Rastreabilidade adicional:
- Watch: `wear/src/main/java/br/com/brunocarvalhs/howmuch/wear/presentation/MainActivity.kt:35-39` (início em `ShoppingList` se há usuário, senão `LinkPhone`) e `wear/src/main/AndroidManifest.xml:58-75` (listener com `/auth` e `/pairing`).
- Telas e rotas do relógio: `feature/auth/.../presentation/wear/screen/LinkPhoneWearScreen.kt:20-28` (navega para `PairingCode`), `PairingCodeWearScreen.kt:34-40` (vai para `ShoppingList` quando `isLinked`), `feature/auth/.../navigation/AuthWearGraph.kt:13-28`.
- Perfil do relógio: `feature/profile/.../presentation/wear/screen/ProfileWearScreen.kt:33` e `feature/profile/.../viewmodel/ProfileViewModel.kt:43-49` (`onLinkMobileDevice` navega para `PairingCode`).
- Sign-out: `app/.../MainViewModel.kt:70-85` chama `notifySignOut()`, que derruba a sessão do relógio (`WearableSyncServiceImpl.kt:177-192`).
- Arquitetura (parcial, a validar): `architecture/04-integracoes-externas.md:74-77` (pareamento de 6 dígitos) e `architecture/05-decisoes-arquiteturais.md:87-95` (AD-009). Na AD-009 a doc diz que o pareamento só copia o uid, mas o código envia `idToken` (`WearAuthListenerService.kt:34-44`). Usar o código como fonte.

## 2. Escopo

- **Inclui**
  1. Relógio inicia o pareamento: gera par de chaves efêmero e abre o celular por deeplink (`cestou://wear/pair?sid=&k=&n=`) via `RemoteActivityHelper`.
  2. Celular recebe o deeplink, valida pré-requisitos (flag, login, conta Google, Pro) e mostra a tela de pareamento, ou responde com erro para o relógio.
  3. Ambos derivam o **mesmo código de 8 letras** (2 × 4) a partir de ECDH P-256 + HKDF-SHA256 sobre a transcrição das duas chaves públicas (seção 3.2). Os dois aparelhos mostram o código.
  4. Botão **Parear** nos dois aparelhos. O token só é enviado depois que **os dois** confirmam.
  5. Celular envia `uid + idToken` (mesmo par de hoje) vinculado ao `sid`. Relógio faz `signInWithGoogleIdToken` e vai direto para `ShoppingList`.
  6. Mensagens de erro no relógio (seção 5, regras 9 e 10) e botões "Tentar novamente" e "Sair".
  7. Sessões seguintes: relógio abre direto na home quando já há usuário (já é o comportamento de `MainActivity.kt:35-39`; manter e testar).
  8. Remoção do fluxo de código de 6 dígitos (`updatePairingCode`, `findNodeByPairingCode`, `generatePairingCode`, `PAIRING_VERIFY`, `PAIRING_CONFIRM`, `PairingCodeWearScreen` com código gerado no relógio, campo de código do celular em `LinkWearDeviceScreen`).
- **Exclui**
  - Bluetooth próprio (BLE, GATT, socket). O transporte é a Wearable Data Layer e `RemoteActivityHelper` (Play services escolhe Bluetooth ou Wi-Fi). Não há permissão `BLUETOOTH_*`. Se for necessário Bluetooth direto, vira o `sdd-054`.
  - Mudança no login Google do celular e nas regras do Firestore (AD-009).
  - Celular iniciar o pareamento sozinho (a partir do Perfil do celular). Só o relógio inicia. Ver P-4.
  - Sincronização de listas para o relógio (`syncShoppingLists`, `syncAuthStatus` ficam como estão, sem chamador; ver seção 9, P-5).
  - Persistir chaves ou código em disco.

## 3. Impacto Arquitetural

### 3.1 Módulos e camadas

| Módulo | Camada | Mudança |
|---|---|---|
| `:core:common` | contrato / utilitário puro | `WearablePaths` (novos caminhos, payloads com `sid`); `WearableSyncService` (interface reduzida); `PairingCodeHolder` → `PairingSessionHolder` |
| `:core:data` | `data` (infra do canal) | `WearableSyncServiceImpl`: remove código de 6 dígitos; adiciona envio e recebimento por sessão; `openPhoneApp` → `requestPhonePairing` |
| `:core:navigation` | rotas | `MobileRoutes.kt`: remove `PairingCode`; adiciona `WearPairing(sessionId, wearPublicKey, wearNodeId)` |
| `:feature:auth` | `domain` (pura, internal), `presentation` | Handshake (`internal`), `PairingViewModel` (relógio, MVI), `PhoneWearPairingViewModel` (celular, MVI), telas e estados |
| `:wear` | apresentação / serviço | `WearAuthListenerService` roteia mensagens para `PairingSessionHolder`; valida `sid` e confirmação antes de `signInWithGoogleIdToken`. **`applicationId` igual ao do `:app`** (`br.com.brunocarvalhs.howmuch`), decisão da seção 11. O `namespace` continua `br.com.brunocarvalhs.howmuch.wear` |
| `:app` | entrypoint | `AndroidManifest.xml` (intent-filter `cestou://wear/pair`); `MainViewModel.onDeepLink` (novo caso); `MainActivity` (navega para `WearPairing` quando autenticado) |

- Padrão: Clean Arch + MVI nas duas telas, Hilt em todas as ViewModels. Nenhuma instância manual de repositório.
- A regra de negócio (flag, Pro, conta Google) fica na ViewModel do celular, que chama `FeatureFlagService` e `SubscriptionRepository` (via o módulo que já expõe o status). Não ir para o Composable.

### 3.2 Protocolo (sem código de produção)

Todas as mensagens usam Wearable `MessageClient`. Chaves são efêmeras (uma por sessão), em memória, descartadas ao fim da sessão.

```
Relógio (:wear + feature:auth)                          Celular (:app + feature:auth)
  PairingViewModel                                        MainViewModel
  1. gera sid (128 bits, base64url) e par EC P-256        
  2. RemoteActivityHelper ──── cestou://wear/pair?sid=&k=wearPub&n=nodeId ───►  onDeepLink
                                                          3. valida flag, login, conta Google, Pro
                                                             (falha ⇒ passo 9)
                                                          4. abre WearPairing(sid, k, n)
                                                          5. gera par EC P-256; secret = ECDH(priv_phone, wearPub)
                                                             code = SAS(secret, wearPub || phonePub)
  ◄──────── /pairing/phone-key (sid, phonePub) ──────────  6. envia (MessageClient, nodeId n)
  7. secret = ECDH(priv_wear, phonePub)
     code = SAS(secret, wearPub || phonePub)   ← igual ao do celular
  8. UI: código 2 × 4 + Confirmar                          UI: código 2 × 4 + Parear
  ── /pairing/wear-confirm (sid) ───────────────────────►  9. aguarda os dois botões
                                                          10. se phoneConfirmed && wearConfirmed:
  ◄──────── /auth/pair (sid, uid, idToken) ──────────────     envia /auth/pair
  11. valida sid ativo, wearConfirmed, sid ok
      signInWithGoogleIdToken(idToken, uid)
  12. currentUser != null ⇒ isLinked ⇒ ShoppingList (popUpTo 0)

  9 (erro, qualquer lado) ──── /pairing/error (sid, reason) ──► mostra mensagem (seção 5)
```

Diagrama de componentes:

```
:wear                                        :app                                 :feature:auth (ambos)
WearAuthListenerService ──► PairingSessionHolder ◄── PairingViewModel (relógio)
  /pairing/*  → offer()        (core:common)            ▲
  /auth/pair  → valida sid                              │ WearableSyncService (core:common, impl :core:data)
                                                        │
MainViewModel.onDeepLink ──► MainActivity ──► PhoneWearPairingViewModel (celular)
  cestou://wear/pair                             PairingHandshake (internal, puro, JVM)
```

Canal físico: Wearable Data Layer. O deeplink usa `RemoteActivityHelper` (o intent é aberto no celular, Play services resolve o transporte).

### 3.3 Novos arquivos (caminhos completos)

- `core/common/src/main/java/br/com/brunocarvalhs/howmuch/core/common/wearable/PairingSessionHolder.kt` (substitui `PairingCodeHolder.kt`)
- `core/common/src/main/java/br/com/brunocarvalhs/howmuch/core/common/wearable/WearPairingMessage.kt` (sealed: `PhoneKey`, `Error`, `WatchConfirmed`)
- `feature/auth/src/main/java/br/com/brunocarvalhs/howmuch/feature/auth/domain/wear/PairingHandshake.kt` (`internal`)
- `feature/auth/src/main/java/br/com/brunocarvalhs/howmuch/feature/auth/domain/wear/PairingFailure.kt` (`internal`, enum com os motivos da seção 5)
- `feature/auth/src/main/java/br/com/brunocarvalhs/howmuch/feature/auth/presentation/wear/viewmodel/PhoneWearPairingViewModel.kt` (celular)
- `feature/auth/src/main/java/br/com/brunocarvalhs/howmuch/feature/auth/presentation/wear/state/PhoneWearPairingUiState.kt`
- `feature/auth/src/main/java/br/com/brunocarvalhs/howmuch/feature/auth/presentation/wear/screen/PhoneWearPairingScreen.kt` (celular)
- `feature/auth/src/main/java/br/com/brunocarvalhs/howmuch/feature/auth/presentation/wear/intent/PhoneWearPairingIntent.kt`
- `feature/auth/src/main/java/br/com/brunocarvalhs/howmuch/feature/auth/presentation/wear/screen/PairingWearScreen.kt` (relógio: estados Conectando, Código, Sincronizando, Erro)
- `feature/auth/src/main/java/br/com/brunocarvalhs/howmuch/feature/auth/presentation/wear/router/WearPairingRouter.kt` (`internal`, função pura que decide o que o listener faz com cada mensagem)
- `app/src/main/java/br/com/brunocarvalhs/howmuch/deeplink/WearPairingLink.kt` (`internal`, parse de `cestou://wear/pair`)
- Testes correspondentes (seção 7).

Arquivos removidos: `core/common/.../wearable/PairingCodeHolder.kt`, `feature/auth/.../wear/screen/PairingCodeWearScreen.kt` (substituído pela tela de código do relógio em `PairingWearScreen.kt`).

### 3.4 Injeção e concorrência

- Hilt: `PairingSessionHolder` é `@Singleton` (como `PairingCodeHolder` hoje). `PairingHandshake` é objeto puro, sem injeção.
- Geração de par EC e ECDH: `Dispatchers.Default`. Nunca no `Dispatchers.Main`.
- Chamadas `MessageClient`/`CapabilityClient`: `suspend` com `await()`, em `Dispatchers.IO` por `WearableSyncServiceImpl` (sem mudança de padrão).
- Listener de mensagens do celular: adicionado no `viewModelScope` quando a tela abre e removido no `finally` / `onCleared` (mesmo padrão de `WearableSyncServiceImpl.kt:139-144`). Um listener por sessão.
- Estado da UI exposto como `StateFlow` imutável, coleções imutáveis.

## 4. Contratos e Interfaces

Assinaturas, sem implementação. `public` apenas onde há contrato entre módulos.

### 4.1 `core:common` (public)

```
// WearablePaths (object, public - :app, :feature:auth, :wear)
// REMOVIDOS: PAIRING_VERIFY, PAIRING_CONFIRM                              [BREAKING]
const val PAIRING_PHONE_KEY = "/pairing/phone-key"   // celular -> relógio: "sid\nphonePubB64"
const val PAIRING_WEAR_CONFIRM = "/pairing/wear-confirm" // relógio -> celular: "sid"
const val PAIRING_ERROR = "/pairing/error"           // qualquer lado -> outro: "sid\nreason"
const val AUTH_PAIR = "/auth/pair"                   // payload muda: "sid\nuid\nidToken"  [BREAKING]
fun encodeAuthPair(sid: String, userId: String, googleIdToken: String): ByteArray
fun decodeAuthPair(data: ByteArray): Triple<String, String, String>?   // null se malformado
fun encodePhoneKey(sid: String, phonePubB64: String): ByteArray
fun decodePhoneKey(data: ByteArray): Pair<String, String>?
fun encodeError(sid: String, reason: String): ByteArray
fun decodeError(data: ByteArray): Pair<String, String>?

// PairingSessionHolder (@Singleton, public - :wear e :feature:auth)      [substitui PairingCodeHolder]
class PairingSessionHolder @Inject constructor() {
    val incoming: StateFlow<WearPairingMessage?>        // última mensagem (consumida pela VM)
    val activeSessionId: String?                         // sid da sessão em curso
    fun startSession(sid: String)                        // zera estado anterior
    fun markWatchConfirmed(sid: String): Boolean         // só se sid == activeSessionId
    fun isConfirmed(sid: String): Boolean
    fun offer(message: WearPairingMessage)                // chamado pelo WearAuthListenerService
    fun endSession()                                     // limpa tudo; chamado ao sair da tela
}

// WearPairingMessage (sealed, public)
sealed interface WearPairingMessage {
    val sessionId: String
    data class PhoneKey(override val sessionId: String, val phonePubB64: String) : WearPairingMessage
    data class Error(override val sessionId: String, val reason: String) : WearPairingMessage
}

// WearableSyncService (public)  [interface reduzida]
interface WearableSyncService {
    suspend fun syncAuthStatus(isAuthenticated: Boolean, userId: String?)    // inalterado
    suspend fun syncShoppingLists(shoppingListsJson: String)                  // inalterado
    suspend fun notifySignOut()                                               // inalterado
    // Relógio:
    suspend fun requestPhonePairing(sessionId: String, wearPubB64: String): Boolean  // substitui openPhoneApp
    suspend fun sendWearConfirm(nodeId: String, sessionId: String)
    // Celular:
    suspend fun sendPhoneKey(nodeId: String, sessionId: String, phonePubB64: String)
    suspend fun sendAuthPair(nodeId: String, sessionId: String, userId: String, googleIdToken: String)
    suspend fun sendPairingError(nodeId: String, sessionId: String, reason: String)
}
// REMOVIDOS: updatePairingCode, findNodeByPairingCode, generatePairingCode, openPhoneApp,
//            sendAuthPairToNode (renomeado para sendAuthPair com sid)               [BREAKING]
```

Consumidores a atualizar com a mudança de assinatura: `LinkWearViewModel` (remover), `PairingViewModel`, `MainViewModel` (só `notifySignOut`, sem mudança), `WearAuthListenerService`, testes `LinkWearViewModelTest`, `PairingViewModelTest`, `PairingCodeHolderTest`, `WearablePathsTest`, `AuthWearScreensTest`.

`WearableSyncService.kt` está `public` (interface sem modificador) e é contrato entre `:core:data`, `:feature:auth`, `:app` e `:wear`. Justificativa: é o único ponto de acesso ao canal.

### 4.2 `core:navigation` (public)

```
// MobileRoutes.kt
@Serializable data class WearPairing(val sessionId: String, val wearPublicKey: String, val wearNodeId: String) : NavKey, RouteProtocol
    routeType = RouteType.PROTECTED   // não abre sem login; ver regra 4
// REMOVIDO: data object PairingCode : NavKey                                 [BREAKING para feature:profile e wear]
// MANTIDO:  data object LinkWearDevice : NavKey (tela passa a só instruir)
// MANTIDO:  data object LinkPhone : NavKey
```

`WearPairing` é public: `:app` (`MainActivity`) navega até ele, e `:feature:auth` o registra. Não pode ser `internal`.

### 4.3 `feature:auth` (internal)

```
internal object PairingHandshake {
    fun newSessionId(): String                                   // SecureRandom, 16 bytes, base64url
    fun newKeyPair(): KeyPair                                    // EC secp256r1
    fun encodePublic(pair: KeyPair): ByteArray                   // X.509 SubjectPublicKeyInfo
    fun sharedSecret(privateKey: PrivateKey, peerPublicEncoded: ByteArray): ByteArray   // ECDH
    fun deriveCode(sharedSecret: ByteArray, wearPublic: ByteArray, phonePublic: ByteArray): String   // "XXXX XXXX"
}
internal enum class PairingFailure { DISABLED, NOT_SIGNED_IN, NO_GOOGLE_ACCOUNT, ACCOUNT_MISMATCH, NOT_PRO,
    PHONE_UNREACHABLE, TIMEOUT, CANCELED, INVALID_KEY, SIGN_IN_FAILED }
internal class PairingViewModel @Inject constructor(...)   // relógio, MVI
internal class PhoneWearPairingViewModel @Inject constructor(...)  // celular, MVI
```

### 4.4 `:app` (internal)

- `MainViewModel.onDeepLink`: novo caso `WearPairingLink.parse(data)` antes de `AiChatLaunch` e `InviteLink`. Guarda `pendingWearPairing` em `SavedStateHandle` (mesmo padrão de `KEY_PENDING_JOIN_TOKEN`).
- `AndroidManifest.xml`: novo `intent-filter` em `MainActivity` com `android:scheme="cestou"` `android:host="wear"` `android:path="/pair"` (sem `autoVerify`, igual `cestou://chat`, linhas `102-108`).

### 4.5 Analytics, remote config, Firestore

- Analytics (`core/analytics/.../model/AnalyticsElements.kt`): substituir `WEAR_LINK_CODE_SUBMIT` (linha `54`) por `WEAR_PAIR_START` (`wear_pair.start`, relógio), `WEAR_PAIR_CONFIRM` (`wear_pair.confirm`, celular), `WEAR_PAIR_SUCCESS` (`wear_pair.success`), `WEAR_PAIR_ERROR` (`wear_pair.error`, param `reason` com o nome de `PairingFailure`). Parâmetro `reason` a criar em `AnalyticsParams` se não existir. **Nunca** enviar `sid`, chave, uid ou token.
- Remote config: reutilizar `WEAR_LOGIN_ENABLED` (`core/remote-config/.../RemoteConfigKeys.kt:62`, default `true` em `RemoteConfigDefaults.kt:41`). Verificada só no celular, no recebimento do deeplink (regra 3). Nenhum toggle novo.
- Firestore: sem coleção nova, sem mudança em `firestore.rules`. O login do relógio continua por `signInWithGoogleIdToken` (AD-009).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Pareamento começa com código de 6 dígitos gerado no relógio e digitado no celular. | Pareamento começa **no relógio**, por deeplink. Código de 8 letras derivado das chaves, igual nos dois. Sem digitação. | `WearableSyncServiceImpl.kt:78-86, 226-232`; `LinkWearViewModel.kt:51-54` |
| 2 | Login do relógio é pelo token Google, nunca pelo uid. | Mantido. O envio agora inclui `sid` e só sai após as duas confirmações. | `WearAuthListenerService.kt:34-44`; `LinkWearViewModel.kt:9-10`; AD-009 |
| 3 | Flag `wear_login_enabled` não é lida pelo fluxo de pareamento do relógio (é lida só em `LinkWearViewModel.kt:47`, que vai ser removido). | Celular checa a flag ao receber o deeplink. Se OFF: erro `DISABLED` no relógio. Relógio não consulta remote config. | `LinkWearViewModel.kt:47-50`; `RemoteConfigKeys.kt:62` |
| 4 | Pareamento só com usuário logado no celular. | Se `authService.currentUser == null` ⇒ erro `NOT_SIGNED_IN` (sem abrir `WearPairing`). | `LinkWearViewModel.kt:70` |
| 5 | Conta Google do relógio tem que ser a do app (e-mail igual, sem diferença de caixa). | Mantido, checado no celular antes de liberar o botão Parear. Divergência ⇒ erro `ACCOUNT_MISMATCH` no celular e no relógio. | `LinkWearViewModel.kt:71-72` |
| 6 | Sign-out no celular derruba a sessão do relógio e limpa `WearListStore`. | Mantido. Sem mudança. | `MainViewModel.kt:70-85`; `WearAuthListenerService.kt:46-49`; `WearableSyncServiceImpl.kt:177-192` |
| 7 | Relógio só entra depois de `signInWithGoogleIdToken` com sucesso. | Mantido. Antes de chamar, valida `sid == activeSessionId` e `isConfirmed(sid)`. Mensagem sem confirmação é descartada. | `WearAuthListenerService.kt:34-44`; `PairingViewModel.kt:35-43` |
| 8 | Gate de Pro só na tela de Perfil (`ProfileScreen.kt:94-98`). | Pro é checado também no celular ao receber o deeplink. Sem Pro ⇒ erro `NOT_PRO` no relógio. | `ProfileScreen.kt:94-98`; `ProfileGraph.kt:38-42`; `CHANGELOG.md:416` |
| 9 | Falha de pareamento não tem mensagem para o relógio (não há ack; `LinkWearViewModel` só mostra erro no celular). | Toda falha gera `/pairing/error` e a tela do relógio mostra motivo + "Tentar novamente" + "Sair". Ver mapeamento abaixo. | `LinkWearViewModel.kt:66-70`; `WearableSyncServiceImpl.kt:188-190` |
| 10 | Ao abrir o app depois de pareado, a home é o destino inicial quando há usuário. | Mantido: relógio abre direto em `ShoppingList` sempre que `authService.currentUser != null`. Pareamento não é refeito. | `wear/.../MainActivity.kt:35-39` |
| 11 | Fechar a tela de pareamento no celular depois das duas confirmações (etapa `SYNCING`) não cancela o relógio, e o envio do token não é cancelado junto com a tela. Só antes da confirmação (`CODE`) o celular envia `CANCELED`. | Ajustado após teste manual (seção 11). | `PhoneWearPairingViewModel.cancel()` e `authSendScope` |

Mapeamento de motivos (`PairingFailure` ⇒ mensagem no relógio, em `strings.xml` pt-BR e en-US):

| Motivo | Quem detecta | Mensagem no relógio (ideia) |
|---|---|---|
| `PHONE_UNREACHABLE` | relógio (`requestPhonePairing` falha) | "Não foi possível conectar ao celular. Confira se o Cestou está instalado e conectado." |
| `TIMEOUT` | relógio (30 s sem `PhoneKey`, ou 30 s sem sign-in após confirmar) | "O celular não respondeu. Aproxime o celular e tente de novo." |
| `DISABLED` | celular | "Vincular o relógio está indisponível no momento." |
| `NOT_SIGNED_IN` | celular | "Entre na sua conta no celular e tente de novo." |
| `NO_GOOGLE_ACCOUNT` | celular | "Use uma conta Google no celular para vincular o relógio." |
| `NOT_PRO` | celular | "O relógio é um recurso do plano Pro." |
| `ACCOUNT_MISMATCH` | celular | "Escolha a mesma conta Google que você usa no app." |
| `CANCELED` | celular ao cancelar | "Pareamento cancelado." |
| `INVALID_KEY` | relógio (chave do celular inválida) | "Não foi possível confirmar o código. Tente novamente." |
| `SIGN_IN_FAILED` | relógio (`signInWithGoogleIdToken` falhou) | "Não foi possível entrar com a sua conta. Tente novamente." |

Limites de tempo: relógio espera `PhoneKey` 30 s e sign-in 30 s após confirmar. Celular espera confirmação do relógio 60 s após "Parear" (mostra "Aguardando o relógio"). Todos os timers são cancelados ao sair da tela.

### 5.1 Especificação do código (crypto)

- Curva: EC P-256 (`secp256r1`). Acordo: ECDH (`KeyAgreement "ECDH"`). Chaves públicas em X.509 (`encoded`). Só APIs padrão do JVM (`java.security`, `javax.crypto`). Sem biblioteca nova.
- Transcrição: `T = wearPub ‖ phonePub` (sempre nessa ordem, bytes encodados).
- `secret = ECDH(privLocal, pubPeer)`. Os dois lados chegam ao mesmo `secret`.
- Derivação: HKDF-SHA256 (extract + expand com `javax.crypto.Mac`), `ikm = secret`, `salt = vazio`, `info = "cestou-wear-pairing-sas-v1" ‖ T`, tamanho 16 bytes.
- Mapeamento para letras: alfabeto de 21 consoantes `BCDFGHJKLMNPQRSTVWXYZ`. Para cada byte, aceitar só se `byte < 252` (21 × 12) e usar `byte % 21`. Isso evita viés de módulo. Oito letras, formato `XXXX XXXX`. Entropia ~35 bits.
- Chaves e `secret` nunca saem da memória, nunca vão para log, nunca para analytics.

### 5.2 Formato das mensagens

- Separador `\n`. `sid` em base64url sem `\n`. Payloads com campos vazios ou com `\n` a mais ⇒ `null` no decode (descarte, sem erro para o usuário).

## 6. Critérios de Aceitação

- **Performance**
  - Keygen e ECDH em `Dispatchers.Default`, uma vez por sessão. Nenhum recálculo a cada recomposição: código calculado no `init` / handler, guardado em `StateFlow`.
  - Sem I/O duplicado: `RemoteActivityHelper` chamado uma vez por tentativa; `getCapability` não é usado no novo fluxo (nó vem do deeplink, `n=`).
  - Listener do `MessageClient` único por tela, removido ao sair. Nenhum listener vazando em rotação (ViewModel sobrevive).
  - Recomposição: telas recebem `state` + `intent` (lambdas estáveis). Coleções imutáveis.
- **Feature toggles**

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `wear_login_enabled` | `true` | remote-config (celular) | Deeplink responde `DISABLED`; relógio mostra erro | Fluxo completo |

- **Observabilidade**: eventos `wear_pair.start` (relógio), `wear_pair.confirm` (celular), `wear_pair.success`, `wear_pair.error{reason}`. Erros não fatais em Crashlytics sem dados pessoais nem token. Log com Timber só com `sid` truncado (8 chars) e motivo.
- **Compatibilidade**
  - `minSdk` 28 no app, `minSdk` 30 no `:wear` (`wear/build.gradle.kts:43`). Nenhuma API nova de Bluetooth. `RemoteActivityHelper` já está em uso no código.
  - Protocolo quebra com o app antigo: relógio com versão antiga não pareia com celular novo e vice-versa. Liberar os dois apps juntos (ver P-3).
  - Relógios já pareados continuam logados (sessão Firebase própria do relógio, AD-009); só o novo pareamento usa o protocolo novo.
  - Sem migração de dados locais. `WearListStore` não muda.
- **Acessibilidade e UI**: botões e códigos com `contentDescription` (código lido letra por letra); tamanho de texto do relógio com `MaterialTheme.typography`; sem texto literal em Composable (AGENTS §5).

## 7. Plano de Testes

- **Unitários (JVM, `feature:auth`)**
  - `PairingHandshakeTest`:
    - Dois lados com chaves diferentes produzem o mesmo código (cenário positivo).
    - Chave pública do celular trocada ⇒ código diferente (MITM simulado).
    - Mesma entrada ⇒ mesmo código (determinismo). Vetor fixo de regressão.
    - Formato `XXXX XXXX`, só consoantes do alfabeto, 8 letras.
    - Rejeição de bytes `>= 252` coberta por teste direto da função de mapeamento.
  - `PairingViewModelTest` (relógio, reescrito): `TestDispatcher` com tempo virtual.
    - Caminho feliz: `Connecting` → `PhoneKey` → `Code` → confirmar → `Syncing` → `isLinked` → navegação.
    - `requestPhonePairing` falha ⇒ `PHONE_UNREACHABLE`.
    - Sem `PhoneKey` em 30 s ⇒ `TIMEOUT`.
    - `Error` do celular ⇒ mapeia `reason` para `PairingFailure`.
    - "Tentar novamente" gera novo `sid` e zera a sessão anterior.
    - `isLinked` nunca fica `true` sem sign-in (sem falso positivo).
  - `PhoneWearPairingViewModelTest` (celular, reescrito):
    - Flag OFF ⇒ `DISABLED`; sem login ⇒ `NOT_SIGNED_IN`; sem e-mail/Google ⇒ `NO_GOOGLE_ACCOUNT`; e-mail diferente ⇒ `ACCOUNT_MISMATCH`; sem Pro ⇒ `NOT_PRO`. Cada um envia `/pairing/error` para o nó correto.
    - Ordem de confirmação: celular antes do relógio, e relógio antes do celular. Em ambos, `sendAuthPair` sai uma única vez.
    - Relógio não confirma em 60 s ⇒ `TIMEOUT`, sem `sendAuthPair`.
    - Cancelar ⇒ `CANCELED`, sem token enviado.
    - Falha de `GoogleIdTokenProvider` não vaza token nem chave em estado.
  - `WearablePathsTest` (`core/common`): encode/decode triplo, duplo e vazio; `sid` com `\n` rejeitado; payload antigo (5 campos, código de 6 dígitos) ⇒ `null`.
  - `PairingSessionHolderTest` (substitui `PairingCodeHolderTest`): `startSession` zera; `markWatchConfirmed` com `sid` errado ⇒ `false`; `endSession` limpa.
  - Roteador do listener (extraído para função pura `WearPairingRouter` em `feature:auth`, testável no JVM): `AUTH_PAIR` sem confirmação ou com `sid` antigo ⇒ não chama `signInWithGoogleIdToken`. Esse é o teste de segurança mais importante.
  - `WearPairingLink.parse` (`:app`): URI válida, faltando `sid`/`k`/`n`, `k` inválido ⇒ `null`. Deeplink de chat e de convite continuam funcionando (`MainViewModelTest`).
- **Instrumentados / UI (testTag, não texto, AGENTS §5)**
  - Tags: `wear_pair_start`, `wear_pair_code`, `wear_pair_confirm`, `wear_pair_retry`, `wear_pair_exit`, `phone_pair_code`, `phone_pair_confirm`, `phone_pair_cancel`.
  - `AuthWearScreensTest` atualizado; telas de erro do relógio com preview para cada `PairingFailure`.
  - Maestro: não cobre dois aparelhos. Testes de ponta a ponta são manuais em hardware real (celular + Wear OS). Emulador não cobre Data Layer de forma confiável.
- **Manual (obrigatório antes do merge)**
  - Pareamento feliz com os dois botões (ambas as ordens).
  - Celular fora de alcance ⇒ `PHONE_UNREACHABLE` ou `TIMEOUT` no relógio.
  - Celular em segundo plano ⇒ `TIMEOUT` (esperado, ver risco R-2).
  - Conta Google diferente ⇒ mensagem nos dois.
  - Reabrir o relógio ⇒ abre direto na home.
  - Sign-out no celular ⇒ relógio sai (regra 6).
- **Lint/Detekt/Cobertura**: `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`. `feature/auth/config/detekt/baseline.xml` e `core/data/config/detekt/baseline.xml:5,19` (`WearableSyncServiceImpl`) não recebem entradas novas.
- **Arquitetura**: `ArchitectureTest` (Konsist, `app/src/test/.../architecture/ArchitectureTest.kt`) precisa passar: `feature:auth/domain/wear` sem `android.*`; `PairingHandshake` sem dependência de Android.

## 8. Dependências e Riscos

| # | Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|---|
| R-1 | `RemoteActivityHelper` com URI + query não abre o intent no celular como esperado (intent remoto com dados, não só `MAIN/LAUNCHER`). | média | alto | **Spike no início da implementação** com dois aparelhos reais. Se falhar, a alternativa é mandar `k`/`n` por extras do intent. |
| R-2 | Celular em segundo plano: a tela de pareamento não está viva para receber `/pairing/wear-confirm` (sem `WearableListenerService` no `:app`). | média | médio | Aceitar `TIMEOUT` com mensagem clara. Se for problema real, criar `WearableListenerService` no `:app` que repassa para `PairingSessionHolder`. Decisão de P-6. |
| R-3 | Protocolo quebra com relógio antigo ou celular antigo. | alta (certa, se não coordenar) | alto | Liberar os dois apps juntos. Notas de release. Ver P-3. |
| R-4 | Bypass de Pro via deeplink. | baixa | médio | Checagem de Pro no celular (regra 8). Teste unitário dedicado. |
| R-5 | Erro de implementação crypto (ordem da transcrição, HKDF errado) ⇒ códigos diferentes e pareamento sempre falha. | média | alto | Vetor fixo de teste; spike cedo; sem biblioteca nova; revisão de maintainer. |
| R-6 | Código de 8 letras fraco contra adversário ativo (~35 bits). | baixa | médio | Adversário precisa estar no mesmo grupo de nós da Data Layer; token só sai após confirmações. Aceito para este escopo. |
| R-7 | Token ou chave em log/analytics. | baixa | alto | Teste que verifica que `sid` e token não aparecem em `Timber` (não logar). Revisão do diff. |
| R-8 | Conflito com `sdd-054` (não versionado). | alta | médio | Marcar `sdd-054` como superado ao aprovar. Não implementar os dois em paralelo. |
| R-9 | Flag `wear_login_enabled` é a mesma que o relógio ignora hoje; desligar no celular não avisa o relógio ao vivo. | média | baixo | Erro `DISABLED` enviado ao relógio (regra 3). |
| R-11 | `applicationId` do relógio igual ao do celular: o relógio antigo `.wear` deixa de existir, e a Play trata os dois formatos como um único app. | alta (certa, com a troca) | médio | Desinstalar o `.wear` do relógio de teste. Listagem Play única (Wear OS já aparece na listagem do Cestou). Ver P-8 para o `versionCode`. |
| R-10 | Documentação desatualizada (`architecture/04` §6, `05` AD-009) leva a desenho errado no futuro. | alta | médio | Atualizar as duas no mesmo PR (seção 10). |

Dependências externas: Wearable Data Layer (`libs.play.services.wearable`, `gradle/libs.versions.toml:200`), `androidx.wear.remote.interactions.RemoteActivityHelper` (já usado em `WearableSyncServiceImpl.kt:199`).

## 9. Perguntas em Aberto

Nenhuma pergunta bloqueante. As decisões abaixo foram tomadas com base no pedido e no código. Confirmar na revisão do maintainer (passo 3 do SDD). Se alguma for rejeitada, o plano volta a `draft`.

- [x] **A-1 (decidida):** "Tem acesso direto à home sempre" = depois de pareado, o relógio abre direto na home em toda abertura, sem refazer o pareamento (regra 10). Sign-out no celular continua derrubando a sessão do relógio (regra 6), como hoje.
- [x] **A-2 (decidida):** Transporte é Data Layer + `RemoteActivityHelper`. Não há Bluetooth próprio (ver exclusões). O pedido não cita Bluetooth; a opção de Bluetooth direto continua no `sdd-054`.
- [x] **A-3 (decidida):** Fluxo antigo de código de 6 dígitos é **substituído**, não mantido em paralelo.
- [x] **A-4 (decidida):** Pareamento só começa no relógio. O botão "Vincular relógio" do Perfil do celular mostra instruções ("Abra o Cestou no relógio e toque em Vincular") e não inicia nada.
- [x] **A-5 (decidida):** Idioma: pt-BR e en-US obrigatórios (AGENTS §5). `values-es` existe no módulo: atualizar as mesmas chaves para não cair no inglês. Não bloqueante.

Não bloqueantes, para o maintainer decidir:

- [ ] **P-1:** Pareamento só em conta Pro (regra 8) vale também para quem já tem relógio pareado e reabre? (O plano assume que não: a regra só vale para novo pareamento.)
- [ ] **P-2:** Mensagens de erro de `PairingFailure` ficam em pt-BR e en-US com os textos da tabela da seção 5, ou o maintainer quer outros?
- [ ] **P-3:** Versão-alvo: `2.6.0` (feature, assumindo que relógio e celular são atualizados juntos) ou `3.0.0` (o template pede `major` para breaking)? Afeta o `CHANGELOG.md` e o bump em `app/build.gradle.kts`.
- [ ] **P-4:** O celular deve poder iniciar o pareamento sozinho (sem o relógio)? Hoje o plano não cobre isso.
- [ ] **P-5:** `syncShoppingLists` e `syncAuthStatus` (`WearableSyncServiceImpl.kt:37-75`) não têm chamador. Remover em um PR separado ou manter? O plano mantém.
- [ ] **P-6:** Celular em segundo plano durante o pareamento (R-2): aceitar `TIMEOUT` nesta versão, ou criar `WearableListenerService` no `:app` agora?
- [ ] **P-8 (bloqueante, seção 11):** `versionCode` do `:wear` (17) é diferente do `:app` (20). Com o mesmo pacote, a Play exige códigos únicos e crescentes no app inteiro. Escolher o valor e ajustar `.github/scripts/version.sh` e `1c29aad0` (`keep wear versionCode in step`).
- [ ] **P-7:** Ao atualizar o `:wear` e o app, relógios antigos sem novo app ficam sem pareamento até atualizar. Aviso na loja (`fastlane/metadata`) ou só no CHANGELOG?

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existe).
- [x] Nenhum código de produção escrito (só pseudocódigo, assinaturas e tabelas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público marcada como `breaking` em "Impacto público", com consumidores listados (seção 4.1).
- [x] Novo deeplink e nova rota `WearPairing` marcados `public` com justificativa (seção 4.2 e 4.4).
- [x] Novo endpoint / remote-config: toggle reutilizado (`wear_login_enabled`), observabilidade definida (seção 4.5 e 6).
- [x] Novo cache: nenhum. Chaves em memória, descartadas no fim da sessão (justificativa: segredo efêmero, não persistir).
- [x] Perguntas em aberto: nenhuma bloqueante; itens não bloqueantes em P-1 a P-7. P-3 e P-8 passam a ser bloqueantes para o merge (seção 11).
- [ ] Atualizar `architecture/04-integracoes-externas.md` §6 e `architecture/05-decisoes-arquiteturais.md` (AD-009 e uma AD nova para o pareamento por SAS) no mesmo PR.
- [ ] Atualizar `CHANGELOG.md` no processo de release (AGENTS §6 item 5) e bump de versão (P-3).
- [ ] Marcar `sdd-054-wear-bluetooth-sync` como superado (R-8).

## 11. Revisão de código (code-reviewer, 2026-11-10)

Revisão da branch `feat/wear-whatsapp-pairing` contra `develop`, incluindo o commit `41708729`. Resultado: **AJUSTES NECESSARIOS**.

### 11.1 Decisões tomadas durante a implementação

- **`applicationId` do relógio = `br.com.brunocarvalhs.howmuch`** (commit `41708729`). Motivo: a Wearable Data Layer entrega mensagens pelo nome do pacote. Com um `applicationId` próprio, as mensagens do celular para `/pairing/phone-key` e `/auth/pair` não chegavam ao `WearAuthListenerService`, e o relógio ficava em "Conectando". Evidência: log do GMS no relógio com `Failed to deliver message to AppKey[<hidden>, e38239ae...]`, e o serviço nunca era instanciado. Ver também R-11.
- **Rota de detalhe do relógio é tipada** (`ShoppingWearGraph.kt`, `composable<ShoppingDetail>`). Motivo: a rota por string não casava com `toRoute<ShoppingDetail>()` no ViewModel, e o app fechava ao abrir a última lista.
- **Intent do deeplink com `CATEGORY_BROWSABLE`** (`WearableSyncServiceImpl.requestPhonePairing`). Motivo: `RemoteActivityHelper` rejeita `ACTION_VIEW` sem essa categoria.

### 11.2 Itens bloqueantes para o merge

- [ ] **B-1 Versão e registro de release** (P-3 e P-8). Quebra de protocolo sem bump em `app/build.gradle.kts` (`versionName = "2.5.1"`) e sem entrada no `CHANGELOG.md` (AGENTS §6, itens 5 e 6). Decisão do maintainer: `2.6.0` ou `3.0.0`.
- [x] **B-2 Teste de redação de log (R-7).** `core/data/.../WearableSyncLogRedactionTest.kt`: falhas de envio de `/auth/pair` e `/pairing/error` não logam `sid`, token nem uid, na mensagem nem na exceção. Cobre o celular e o caminho de envio. O listener do relógio (`WearAuthListenerService`) é um `Service` Android e não está coberto por este teste.
- [x] **B-3 Teste de regressão do `requestPhonePairing`.** A montagem do intent virou `buildPhonePairingIntent` (`WearableSyncServiceImpl.kt`). `core/data/.../BuildPhonePairingIntentTest.kt` verifica `ACTION_VIEW`, `CATEGORY_BROWSABLE`, `setPackage(PHONE_APPLICATION_ID)` e os parâmetros `sid`, `k`, `n` do deeplink. Robolectric com `@Config(sdk = [34])`, como o resto do projeto.

### 11.3 Avisos (não bloqueantes)

- [ ] **A-1** AD-009 em `architecture/05-decisoes-arquiteturais.md` diz que o pareamento copia só o uid, mas o código envia `idToken`. Atualizar junto com a seção 10.
- [ ] **A-2** `authSendScope` em `PhoneWearPairingViewModel` é um escopo fora de Hilt. Trocar por um escopo de aplicação injetado, se o projeto tiver um.
- [ ] **A-3** Cliente `br.com.brunocarvalhs.howmuch.wear` do Firebase ficou sem uso. Remover quando for seguro.

### 11.4 Verificações que passaram

- Fluxo antigo de 6 dígitos sem referências restantes (`updatePairingCode`, `PAIRING_VERIFY`, `PairingCodeHolder`).
- Sem `@Suppress` novo, sem texto literal em Composables novos, sem token em log.
- `values`, `values-pt-rBR` e `values-es` com as mesmas 120 chaves em `feature/auth`.
- `./gradlew lintDebug testDebug` e `./gradlew detekt` com exit 0 na árvore do commit `41708729`.
