# 03 — Modelos de domínio

> Fonte de verdade: os arquivos citados. Campos foram verificados nos modelos marcados como "campos verificados"; os demais estão apenas listados e devem ser lidos no arquivo antes de uso em planos.

## 1. Modelos compartilhados (`:core:domain`)

Localização: `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/model/`.

### `Shopping` — campos verificados

Arquivo: `Shopping.kt`. Serializável.

| Campo | Tipo | Observação |
|---|---|---|
| `id` | `String` | Identificador da lista. |
| `title`, `description` | `String` | |
| `price` | `Double` | Total da lista. |
| `status` | `Shopping.Status` | `NEW`, `IN_PROGRESS`, `FINISH`. |
| `users` | `List<String>` | Ids dos membros. |
| `roles` | `Map<String, String>` | Id do membro → papel (`OWNER` ou `EDITOR`). |
| `createdAt` | `Long` | Padrão: `System.currentTimeMillis()`. |
| `isFavorite`, `isCategorized` | `Boolean` | |
| `shortCode` | `String?` | Código de 6 caracteres para entrar na lista. |
| `budget` | `Double?` | Orçamento opcional. |
| `position` | `Int` | Ordem na tela. |
| `emoji` | `String` | Padrão `🛒`. |

### `Product` — campos verificados

Arquivo: `Product.kt`. Serializável.

| Campo | Tipo | Observação |
|---|---|---|
| `id`, `name` | `String` | |
| `quantity` | `Double` | |
| `price` | `Double?` | |
| `isPurchased` | `Boolean` | |
| `category` | `String` | Padrão `"Outros"`. |
| `barcode` | `String?` | |
| `history` | `List<ProductActivity>` | Padrão vazio. |

Propriedades derivadas: `total`, `addedBy`, `lastEditedBy`, `purchasedBy`, `lastActivity`. Função de extensão: `withActivity(action, userId)`.

### `SubscriptionStatus` — verificado

Arquivo: `SubscriptionStatus.kt`. Enum com `FREE` e `PRO`, sem payload. Dados de compra (token, expiração) ficam em `core:billing`.

### `User` — verificado parcialmente

Arquivo: `User.kt`. Contém `enum class Role { OWNER, EDITOR }`. Demais campos: ler o arquivo.

### `Notification` — verificado

Arquivo: `core/domain/.../repository/NotificationRepository.kt`. Campos: `id`, `userId` (destinatário), `title`, `message`, `type` (`list_joined` | `list_finished` | `campaign`, esta vinda do FCM e sem `shoppingId`/`senderId`), `isRead`, `timestamp`, `shoppingId?` e `senderId?` (nulos em documentos anteriores ao sdd-028; sem `shoppingId` o toque só marca como lida). `NotificationOrigin(shoppingId, senderId)` é o parâmetro opcional de `NotificationRepository.notify`.

### Demais modelos (ler o arquivo antes de usar)

| Modelo | Arquivo |
|---|---|
| `AiModel` | `core/domain/.../model/AiModel.kt` |
| `AppSettings` | `core/domain/.../model/AppSettings.kt` |
| `AuthenticatedUser` | `core/domain/.../model/AuthenticatedUser.kt` |
| `ChatMessage` | `core/domain/.../model/ChatMessage.kt` |
| `ProductActivity` | `core/domain/.../model/ProductActivity.kt` |
| `ThemeMode` | `core/domain/.../model/ThemeMode.kt` |
| `UserProfile` | `core/domain/.../model/UserProfile.kt` |
| `ProductTest` | `core/domain/.../model/ProductTest.kt` (confirmar se é modelo de produção ou de teste) |

## 2. Modelos das features

| Modelo | Feature | Arquivo |
|---|---|---|
| `CommonProduct` | products | `feature/products/domain/model/CommonProduct.kt` |
| `Recipe` | products | `feature/products/domain/model/Recipe.kt` |
| `CredentialRules` | auth | `feature/auth/domain/model/CredentialRules.kt` |
| `EmailAuthError` | auth | `feature/auth/domain/model/EmailAuthError.kt` |
| `AgentSession` | ai-agent | `feature/ai_agent/domain/entity/AgentSession.kt` |
| `AiProviderUnavailableException` | ai-agent | `feature/ai_agent/domain/exception/AiProviderUnavailableException.kt` |

## 3. Contratos de IA (`:core:ai`)

- `AgentAction` (`core/ai/.../contract/AgentAction.kt`): contrato de uma ação que o agente pode executar. Tem `requiresPro`, que é `false` por padrão.
- `AgentActionUseCase` (`core/ai/.../base/AgentActionUseCase.kt`): base dos use cases de IA.

## 4. Convenções

- Modelos em `core:domain` são `data class` ou `enum class`, sem dependência de Android.
- Modelos que trafegam para o Firestore são `@Serializable` (kotlinx.serialization).
- Alterar campo de modelo público exige seguir o fluxo da seção 6 de `AGENTS.md`.
