# 04 — Integrações externas

> Fonte de verdade: o código citado. Documento escrito a partir de uma leitura parcial; confirme cada integração no arquivo indicado antes de planejar mudanças.

## 1. Firebase Authentication

- Login por provedores. Google e e-mail vêm ligados por padrão; os demais (telefone, Apple, Microsoft, Facebook, GitHub) são controlados por flags do Remote Config (`core/remote-config/.../model/RemoteConfigKeys.kt`, `FeatureFlagKeys`).
- Credenciais via Credential Manager e Google ID (`core:auth`, `feature:auth`).
- Regras de senha/credencial: `feature/auth/domain/model/CredentialRules.kt`.

## 2. Cloud Firestore

Regras de acesso: `firestore.rules`. Coleções identificadas nas regras:

| Caminho | Uso | Regra resumida |
|---|---|---|
| `/shopping/{shoppingId}` | Listas de compras | Leitura só para membros (`users`). |
| `/shopping/{shoppingId}/products/{productId}` | Produtos da lista (subcoleção) | Acesso conforme membership da lista. |
| `/users/{userId}` | Perfil e configurações do usuário | Por `userId`. |
| `/common-products/{itemId}` | Catálogo de produtos comuns | Ver regra no arquivo. |
| `/shortCodes/{code}` | Índice de código curto para entrar em listas | Leitura para qualquer usuário autenticado (`isSignedIn`); `list` negado para impedir enumeração. |
| `/notifications/{notificationId}` | Notificações entre membros | Leitura só do destinatário (`userId == uid`, inclusive a consulta de não lidas `isRead == false`); criação por qualquer logado com payload fixo (campos opcionais `shoppingId`/`senderId` validados); update só vira `isRead` para `true`; delete negado. Ver regra no arquivo e AD-009. |

Estado atual: a entrada por código usa o índice `shortCodes/{code}` (`ShoppingRepositoryImpl.kt`, `resolveShortCode` e `ensureShortCodeIndex`). Como o índice não existe para listas antigas, há um fallback por consulta em `shopping` que as regras versionadas em `firestore.rules` negam a não-membros. Essas regras ainda não foram publicadas, então o fallback funciona em produção até o deploy da decisão AD-009 (`05-decisoes-arquiteturais.md`), que o interrompe. O comentário em `firestore.rules` (bloco `shopping`) registra o motivo: não é possível liberar a consulta de um único código sem liberar a leitura de todas as listas (decisão G5).

Implementação da entrada por código: `feature/shopping/domain/usecase/ShoppingJoinUseCase.kt` chama `repository.resolveShortCode`.

## 3. Firebase Remote Config

- Flags (`FeatureFlagKeys`): `ai_openrouter_enabled`, `ai_gemini_enabled`, provedores de login e `wear_login_enabled`.
- Variáveis (`RemoteVariableKeys`): `open_router_model`, `gemini_agent_model`, `ai_system_prompt`, e também `open_router_api_key` e `gemini_api_key`.
- Intervalo mínimo de busca: 3600 segundos (`RemoteConfigInitializer.kt`).
- O system prompt tem fallback compilado em `feature/ai_agent/data/service/SystemPrompts.kt`, usado quando o Remote Config ainda não foi buscado.

### Chaves de API no Remote Config

`RemoteVariableKeys` contém `open_router_api_key` e `gemini_api_key`. A motivação declarada pelo time é poder trocar essas chaves sem depender de rollout de versão na loja.

**Onde a chave está exposta:**
- **No APK:** as chaves vêm do `.env` no build (`app/build.gradle.kts`, `feature/ai-agent/build.gradle.kts`, via `propertiesFileName = ".env"`) e são gravadas no `BuildConfig` (`BuildConfig.GEMINI_API_KEY`, `BuildConfig.OPEN_ROUTER_API_KEY`). Qualquer pessoa que decompilar um APK obtém esses valores, independentemente do Remote Config.
- **Em tempo de execução:** o Remote Config sobrepõe o valor do `BuildConfig` quando não está vazio (`AgentFactoryImpl.kt`, `ProductRepositoryImpl.kt`, `RecipeRepositoryImpl.kt`). O valor remoto também chega ao aparelho.
- **Na rede:** a chave do OpenRouter é enviada do aparelho no header `Authorization: Bearer ...` (`OpenRouterAgent.kt`, ~linha 150), direto ao provedor.

**Consequência da troca remota:** trocar a chave no Remote Config só afeta quem recebe o novo valor. APKs antigos continuam com a chave embutida no `BuildConfig` até que ela seja revogada no provedor. Assim, o objetivo de trocar sem rollout é parcialmente atingido: a rotação funciona, mas a chave antiga permanece válida até ser revogada.

**Ao planejar mudanças nessa área, considere:**
- manter a chave no servidor (ex.: Cloud Function que encaminha a chamada ao provedor), de modo que o cliente nunca a receba;
- se a chave permanecer no cliente, restringi-la no provedor (Gemini: restrição por pacote Android e SHA-1; OpenRouter: limite de crédito) para limitar o dano;
- manter no Remote Config apenas o que não é segredo, como nomes de modelo e o system prompt.

## 4. Provedores de IA

- **OpenRouter:** `feature/ai_agent/data/service/OpenRouterAgent.kt`, mapeamento em `OpenRouterMapping.kt`. Ligado pela flag `ai_openrouter_enabled`.
- **Gemini:** `feature/ai_agent/data/service/GeminiAgent.kt`. Ligado pela flag `ai_gemini_enabled`.
- **Fábrica e seleção:** `feature/ai_agent/data/service/AgentFactoryImpl.kt`.
- **Erros:** mensagens em `AiErrorMessages.kt`; indisponibilidade do provedor gera `AiProviderUnavailableException`.
- **Histórico:** `ConversationHistory.kt`.
- **Ferramentas:** ações de negócio expostas ao modelo por `@AiAgentAction` (`core/ai`), executadas via `AgentActionUseCase`.
- **Gate de assinatura:** ações com `requiresPro` só rodam para usuários PRO (ver `02-regras-de-negocio.md`, seção 3).

## 5. Google Play Billing

- Wrapper do `BillingClient` em `core:billing`.
- Contrato `SubscriptionRepository` (em `core:domain`), que expõe apenas `FREE` ou `PRO` para o restante do app.
- Dados de compra (token, expiração) ficam dentro de `core:billing`.
- Tela de assinatura: `feature/subscription`.

## 6. Wear OS (sincronização)

- Pareamento com código de 6 dígitos (100000 a 999999): `core/data/.../wearable/WearableSyncServiceImpl.kt`.
- Pareamento só ocorre se `wear_login_enabled` estiver ligada.

## 7. WorkManager

- Lembretes de listas: `feature/settings/data/worker/ShoppingReminderScheduler.kt`.

## 8. Telemetria

- Firebase Analytics, Crashlytics e Performance Monitoring (citados em `README.md`; confirmar a configuração em `core:analytics` antes de planejar eventos).
- Logs de desenvolvimento via Timber.

## 9. Pendências deste documento

- Não foi verificado: configuração do Firebase App Distribution, detalhes de `core:analytics`, e lista completa de coleções fora de `firestore.rules`.
