# Task: Billing Hardening & Resiliência de Assinaturas PRO

- **Slug:** sdd-016-billing-hardening
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-31
- **Status:** ready
- **Versão-alvo:** 2.2.2 (patch de resiliência e estabilidade sobre a 2.2.1)
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação
Atualmente, o repositório de faturamento Google Play (`PlayBillingSubscriptionRepository`) apresenta três vulnerabilidades críticas diagnosticadas na auditoria geral (`.specs/sdd-001-auditoria-geral/tasks.md`):
1. **B-01 (P0):** Quando `queryPurchasesAsync` falha (ex.: erro transitório de rede, `SERVICE_UNAVAILABLE` ou falha temporária no IPC da Play Store), a lista de compras retornada é vazia e o repositório invoca cegamente `handlePurchases(emptyList())`, rebaixando imediatamente um assinante PRO ativo para o plano `FREE` (`PlayBillingSubscriptionRepository.kt:95-106`).
2. **C-01 (P1):** A função de conexão `connect()` suspende sem timeout através de `suspendCancellableCoroutine` (`PlayBillingSubscriptionRepository.kt:180-192`). Se o processo do Play Services travar ou o callback IPC não responder, a coroutine fica bloqueada para sempre, travando qualquer chamada que dependa de `ensureConnected()`. Além disso, chamadas concorrentes a `startConnection` disparam `IllegalStateException`.
3. **F-05 (P2):** O refresh das assinaturas só é invocado na inicialização da `PaywallViewModel` (`PaywallViewModel.kt:40`). Não há sincronização automática no ciclo de vida do app (`onResume`), de modo que alterações de status feitas na Play Store ou restaurações de compras não são refletidas durante o uso regular.

Referências:
- `core/billing/src/main/java/br/com/brunocarvalhs/howmuch/core/billing/PlayBillingSubscriptionRepository.kt:49-214`
- `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/repository/SubscriptionRepository.kt:14-16`
- `architecture/05-decisoes-arquiteturais.md` (AD-010)
- `.specs/sdd-001-auditoria-geral/tasks.md` (Itens B-01, C-01, F-05)

## 2. Escopo
- **Inclui:**
  - Validação estrita de `BillingResponseCode.OK` em `PlayBillingSubscriptionRepository.refresh()` antes de atualizar o status ou rebaixar para `FREE`.
  - Tratamento de exceções com `try-catch` em todas as chamadas assíncronas do `BillingClient` (`queryPurchasesAsync`, `queryProductDetails`, `launchBillingFlow`, `acknowledgePurchase`).
  - Timeout determinístico (`10_000L` ms) com `withTimeoutOrNull` em `connect()`, retry com backoff exponencial (até 3 tentativas) e sincronização via `Mutex` contra concorrência em `ensureConnected()`.
  - Persistência local (cache) do último status verificado em `SharedPreferences` privado do módulo `:core:billing`, garantindo que o status PRO permaneça intacto ao abrir o app offline.
  - Registro de `Application.ActivityLifecycleCallbacks` no `PlayBillingSubscriptionRepository` para disparar `refresh()` automaticamente sempre que o app retornar ao primeiro plano (`onActivityResumed`), com throttle de segurança (mínimo de 3 segundos entre refreshes automáticos).
  - Testes unitários cobrindo todos os cenários de erro transitório, timeout, reconexão concorrente e ciclo de vida.
- **Exclui:**
  - Alterações no contrato público `SubscriptionRepository` em `:core:domain` (permanece apenas `observeStatus(): Flow<SubscriptionStatus>`).
  - Alterações na UI do `PaywallScreen` ou `ManageSubscriptionScreen`.
  - Alterações no módulo `:wear` (que possui seu próprio `WearSubscriptionModule` estático).

## 3. Impacto Arquitetural
- **Módulo e camada afetados:**
  - `:core:billing` (`PlayBillingSubscriptionRepository.kt` e seus testes unitários em `data`/infraestrutura).
- **Padrão aplicado:** Clean Architecture + Repository Pattern. O módulo `:core:billing` encapsula completamente as regras de integração, resiliência e persistência local da Google Play Billing Library sem expor tipos do SDK externo para o restante do app.
- **Novos arquivos:** Nenhum arquivo novo necessário; modificações cirúrgicas em `PlayBillingSubscriptionRepository.kt` e expansão de `PlayBillingSubscriptionRepositoryTest.kt`.
- **Injeção via Hilt:** Mantém o binding `@Singleton` de `PlayBillingSubscriptionRepository` implementando `SubscriptionRepository` via `BillingModule.kt:23-25`. O repositório já recebe `@ApplicationContext context: Context`, permitindo acessar `SharedPreferences` e `(context.applicationContext as Application).registerActivityLifecycleCallbacks(...)`.

### Diagrama ASCII de Fluxo e Resiliência
```
┌────────────────────────────────────────────────────────────────────────┐
│ App Lifecycle (onResume) / ViewModel Init / Pull-to-refresh             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PlayBillingSubscriptionRepository.refresh()                            │
│  - Mutex.withLock + ensureConnected()                                  │
│  - withTimeoutOrNull(10s) + Exponential Backoff Retry (3x)             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ billingClient.queryPurchasesAsync(SUBS)                                │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │                                 │
         ResponseCode == OK               ResponseCode != OK / Exception
                   │                                 │
                   ▼                                 ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────┐
│ handlePurchases(purchasesList):      │  │ Log Warning via Timber       │
│  - status = purchases.toStatus()     │  │ MANTER status atual!         │
│  - Salvar status no Cache local      │  │ (NÃO rebaixar para FREE)     │
│  - Emitir no Flow _status            │  └──────────────────────────────┘
│  - Acknowledge se não confirmado     │
└──────────────────────────────────────┘
```

## 4. Contratos e Interfaces
Nenhuma interface pública do módulo `:core:domain` é alterada. Os contratos públicos permanecem intactos:

```kotlin
// Em core:domain (inalterado)
package br.com.brunocarvalhs.howmuch.core.domain.repository

interface SubscriptionRepository {
    fun observeStatus(): Flow<SubscriptionStatus>
}
```

```kotlin
// Em core:billing: PlayBillingSubscriptionRepository.kt (assinaturas mantidas, comportamento robustecido)
package br.com.brunocarvalhs.howmuch.core.billing

@Singleton
class PlayBillingSubscriptionRepository @Inject constructor(
    @ApplicationContext context: Context
) : SubscriptionRepository {

    override fun observeStatus(): Flow<SubscriptionStatus>

    suspend fun refresh()

    suspend fun queryFormattedPrice(
        productId: String = BillingConstants.PRO_MONTHLY
    ): String?

    suspend fun purchase(
        activity: Activity,
        productId: String = BillingConstants.PRO_MONTHLY
    ): BillingResult
}
```
- **Visibilidade:** `PlayBillingSubscriptionRepository` permanece `public` para consumo exclusivo em `:app` e `:feature:subscription` (conforme AD-010). Todos os métodos auxiliares (`ensureConnected`, `connectSingleAttempt`, `handlePurchases`, `loadCachedStatus`, etc.) são `private` ou `internal` para testes.
- **Breaking changes:** Nenhuma.
- **Armazenamento:** Arquivo `billing_preferences.xml` via `SharedPreferences` privado (`Context.MODE_PRIVATE`) armazenando `cached_subscription_status: String`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Erro no `queryPurchasesAsync` passa `purchasesList` vazio para `handlePurchases`, setando `FREE` imediatamente. | Se `responseCode != BillingResponseCode.OK`, aborta o processamento, emite log de warning e mantém inalterado o status atual (`PRO` ou `FREE`). | `PlayBillingSubscriptionRepository.kt:95-106` |
| 2 | `connect()` suspende sem timeout; trava coroutines se o IPC do Play Services morrer ou não responder. | `connectSingleAttempt()` executa sob `withTimeoutOrNull(10_000L)`. Em caso de falha/timeout, tenta reconexão com backoff (500ms, 1000ms, max 3 tentativas). | `PlayBillingSubscriptionRepository.kt:180-192` |
| 3 | Chamadas simultâneas a `ensureConnected()` podem disparar `startConnection` em paralelo, causando `IllegalStateException`. | `ensureConnected()` usa `Mutex.withLock` para serializar o fluxo de conexão; se `billingClient.isReady == true`, retorna imediatamente. | `PlayBillingSubscriptionRepository.kt:194-196` |
| 4 | Ao reiniciar o app sem conexão com a internet, o status inicia como `FREE` e a falha de conexão impede o reconhecimento do usuário PRO. | O repositório inicializa `_status` a partir do cache local persistido em `SharedPreferences`. Um usuário PRO offline mantém o entitlement. | `PlayBillingSubscriptionRepository.kt:54` |
| 5 | O refresh de compras não é disparado automaticamente nas retomadas do app em primeiro plano. | Observer de ciclo de vida (`registerActivityLifecycleCallbacks`) dispara `refresh()` automaticamente ao retomar (`onActivityResumed`) com debounce de 3 segundos. | `PaywallViewModel.kt:40` |
| 6 | Falhas não capturadas em chamadas como `acknowledgePurchase` ou `queryProductDetails` podem propagar exceções não tratadas. | Todas as chamadas de SDK são envolvidas em blocos defensivos com log estruturado via `Timber.tag(TAG).w(...)`. | `PlayBillingSubscriptionRepository.kt:169-178` |

## 6. Critérios de Aceitação
- **Performance:**
  - Conexão e queries protegidas por timeout de 10s para nunca bloquear threads do dispatcher `IO` ou o ciclo de vida.
  - Sincronização via `Mutex` garante alocação de apenas um listener de conexão simultâneo.
  - Cache local lido de forma síncrona leve no `init` (único valor `String` em `SharedPreferences`), sem overhead de I/O em loops.
  - Throttle de 3.000 ms no `onActivityResumed` para evitar tempestade de consultas IPC em trocas rápidas de janela ou rotação.
- **Feature toggles:** Não aplicável (core infra/billing nativo).
- **Observabilidade:**
  - Logs estruturados via `Timber.tag("PlayBillingSubscriptionRepository")` com códigos de resposta e debug messages canônicos do Google Play Billing.
  - Nenhum dado pessoal sensível (PII) é logado; tokens de compra são truncados ou omitidos nos logs.
- **Compatibilidade:**
  - Compatível com Android 9+ (API 28 ao 37).
  - Suporte resiliente a modos offline e perda temporária de conectividade.

## 7. Plano de Testes
- **Unitários (`PlayBillingSubscriptionRepositoryTest.kt`):**
  1. `refresh does not downgrade Pro status to Free when queryPurchasesAsync returns SERVICE_UNAVAILABLE or ERROR`: simula falha não-OK e verifica que status permanece `SubscriptionStatus.PRO`.
  2. `refresh does not downgrade Pro status to Free when queryPurchasesAsync throws an exception`: simula exceção e verifica status preservado.
  3. `refresh downgrades to Free only when queryPurchasesAsync returns OK with empty purchase list`: valida que o cancelamento legítimo continua funcionando.
  4. `connect handles timeout gracefully and does not hang indefinitely`: simula ausência de resposta no listener e verifica término após timeout.
  5. `ensureConnected handles concurrent calls cleanly with Mutex without IllegalStateException`: simula chamadas paralelas a `ensureConnected()` e valida que `startConnection` só é chamado de forma serializada.
  6. `initialization restores cached Pro status from SharedPreferences`: valida que o app iniciando offline já inicia com `SubscriptionStatus.PRO` se previamente assinado.
  7. `onActivityResumed triggers auto-refresh when throttled time has elapsed`: valida que a retomada de atividade dispara `refresh()`.
  8. `failed acknowledgePurchase is caught and logged without crashing or dropping Pro`: simula erro na confirmação de compra e assegura estabilidade.
- **Lint / Detekt / Cobertura:**
  - Executar `./gradlew :core:billing:testDebug`
  - Executar `./gradlew :feature:subscription:testDebug`
  - Executar `./gradlew lintDebug`
  - Executar `./gradlew detekt`

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Cache local indicar `PRO` para usuário que reembolsou/cancelou fora do app enquanto esteve offline. | Baixa | Baixa | Na primeira conexão bem-sucedida (`responseCode == OK`), `handlePurchases` sobrescreve o cache com `FREE`. A janela de descompasso limita-se ao período sem conexão. |
| Múltiplos eventos de `onActivityResumed` dispararem chamadas excessivas ao IPC da Play Store. | Média | Baixa | Implementação de throttle com timestamp (`lastAutoRefreshTimestamp`), limitando auto-refreshes a no máximo 1 a cada 3 segundos. |
| Falha no binding de `SharedPreferences` em testes JVM puros sem Android framework. | Baixa | Média | O mock de `Context` em testes relaxados ou injeção defensiva garante instanciação segura tanto em produção quanto em testes unitários. |

## 9. Perguntas em Aberto (bloqueantes)
*Nenhuma pergunta bloqueante.* Todas as regras de negócio, contratos e comportamentos de fallback foram validados no código fonte e na auditoria geral.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `none` em "Impacto público" (contratos inalterados).
- [x] Novo endpoint/remote-config: Não aplicável.
- [x] Novo cache: armazenamento justificado (`SharedPreferences` privado para inicialização síncrona e retenção do status offline contra downgrade indevido).
- [x] Perguntas em aberto exauridas.
