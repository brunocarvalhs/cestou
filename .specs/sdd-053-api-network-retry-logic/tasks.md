# Task: Implementar Retry Logic com Exponential Backoff para APIs

- **Slug:** api-network-retry-logic
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-08
- **Status:** in-progress
- **Versão-alvo:** 2.3.0+
- **Tipo de mudança:** feature (infraestrutura de resiliência)
- **Impacto público (módulo):** additive (novos componentes, zero breaking changes)

## 1. Contexto e Motivação

**Problema:** Gemini Vision fallback está quebrado (InvalidAPIKeyException) com zero retry. Além disso, network timeouts causam app crashes sem recuperação automática. Apps modernos precisam de resiliente retry com exponential backoff.

**Demanda:** Implementar retry layer genérico com:
- Exponential backoff (1s → 1.5s → 2.25s, etc)
- Configurações dinâmicas (sdd-052): API_REQUEST_TIMEOUT_SECONDS, API_RETRY_MAX_ATTEMPTS, API_RETRY_BACKOFF_MULTIPLIER
- Fallback inteligente: Gemini → OpenRouter (se Gemini falha)
- Observabilidade: GA4 + Crashlytics (trace retries, backoffs, final failures)

**Rastreabilidade:**
- Issue: Photo-scan Gemini fallback broken (known live issue)
- Config base: RemoteConfigKeys.kt (sdd-052, linhas 313-325)
- RemoteConfigDefaults.kt (sdd-052, linhas 125-131)
- Gemini consumer: `feature:ai-agent` chat + photo scan
- OpenRouter consumer: `feature:chat` LLM calls

## 2. Escopo

- **Inclui:**
  - Retry interceptor (OkHttp ou Ktor layer) com exponential backoff
  - Dynamic config consumer (API_REQUEST_TIMEOUT_SECONDS, API_RETRY_MAX_ATTEMPTS, API_RETRY_BACKOFF_MULTIPLIER)
  - Fallback strategy: Gemini → OpenRouter (for vision requests)
  - Retry state + analytics (GA4 events: retry_attempt, backoff_delay, final_failure)
  - Crashlytics breadcrumbs (retry counts, timeouts, failure reasons)
  - Unit tests: happy path, exhausted retries, timeout, backoff timing
  - Kotlin coroutines + suspend functions (not callbacks)

- **Exclui:**
  - Client-side caching (belongs in sdd-055)
  - Circuit breaker pattern (future SDD if needed)
  - Request deduplication (belongs in sdd-055)
  - Adaptive retry (based on error type — future optimization)

## 3. Impacto Arquitetural

**Módulos afetados:**
- `core:remote-config` (read API_RETRY_* configs)
- `core:data` (new RetryInterceptor, RetryPolicy)
- `core:analytics` (log retry events)
- `feature:ai-agent` (consume RetryPolicy for Gemini)
- `feature:chat` (consume RetryPolicy for OpenRouter)

**Camadas:**
- Data: `RetryInterceptor` (OkHttp interceptor or Ktor plugin)
- Domain: `RetryPolicy` (interface) + `ExponentialBackoffRetryPolicy` (impl)
- Presentation: no changes (transparent to UI)

**Diagrama (Fluxo de Retry):**

```
┌────────────────────────────────────────────────┐
│ API Call (Gemini Vision for photo-scan)       │
└────────────────────────────┬───────────────────┘
                             ↓
         ┌───────────────────────────────────────┐
         │ RetryInterceptor                      │
         ├───────────────────────────────────────┤
         │ 1. Read configs (RemoteConfig):       │
         │    - API_REQUEST_TIMEOUT_SECONDS      │
         │    - API_RETRY_MAX_ATTEMPTS           │
         │    - API_RETRY_BACKOFF_MULTIPLIER    │
         │ 2. Execute request (with timeout)     │
         │ 3. If fails → check if retryable      │
         │ 4. If yes → exponential backoff       │
         │ 5. Retry (up to max attempts)         │
         │ 6. Log to GA4 + Crashlytics           │
         └────────┬────────────────────────────┬─┘
                  │                            │
            Success             Exhausted Retries
                  ↓                            ↓
         ┌─────────────────┐      ┌─────────────────────────┐
         │ Return result   │      │ Fallback:               │
         │ GA4: success    │      │ - Gemini → OpenRouter?  │
         │                 │      │ - If yes → retry again  │
         └─────────────────┘      │ - If no → throw error   │
                                  │ GA4: final_failure      │
                                  │ Crashlytics: breadcrumb │
                                  └─────────────────────────┘
```

**Injeção (Hilt):**

```kotlin
// core:data/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Singleton
    @Provides
    fun provideRetryPolicy(
        remoteConfig: RemoteConfigService
    ): RetryPolicy = ExponentialBackoffRetryPolicy(remoteConfig)
    
    @Singleton
    @Provides
    fun provideOkHttpClient(
        retryPolicy: RetryPolicy
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(RetryInterceptor(retryPolicy))
        .build()
}

// Consumidores:
// feature:ai-agent/data/GeminiVisionService.kt
class GeminiVisionService @Inject constructor(
    private val client: OkHttpClient,  // injected with retry
    private val openRouterService: OpenRouterService,  // fallback
    private val analytics: AnalyticsTracker
)

// feature:chat/data/OpenRouterService.kt
class OpenRouterService @Inject constructor(
    private val client: OkHttpClient  // injected with retry
)
```

---

## 4. Contratos e Interfaces

### RetryPolicy (Domain Contract)

```kotlin
// core:data/contract/RetryPolicy.kt
internal interface RetryPolicy {
    /**
     * Determine if error is retryable.
     * Retryable: network timeout, connection error, 5xx, 429 (rate limit)
     * Not retryable: 4xx (except 429), invalid API key, parse error
     */
    fun isRetryable(error: Throwable): Boolean
    
    /**
     * Calculate backoff delay in milliseconds for given attempt.
     * attempt=1 → delay1, attempt=2 → delay2, etc.
     * Formula: initialDelayMs * (multiplier ^ (attempt - 1))
     */
    fun getBackoffDelayMs(attempt: Int): Long
    
    /**
     * Max retry attempts (from remote config).
     */
    fun getMaxAttempts(): Int
    
    /**
     * Request timeout in milliseconds (from remote config).
     */
    fun getTimeoutMs(): Long
}

// core:data/impl/ExponentialBackoffRetryPolicy.kt
internal class ExponentialBackoffRetryPolicy(
    private val remoteConfig: RemoteConfigService,
    private val analytics: RemoteConfigAnalytics
) : RetryPolicy {
    
    companion object {
        private const val INITIAL_DELAY_MS = 1000  // 1 second
    }
    
    override fun isRetryable(error: Throwable): Boolean {
        // Retryable: SocketTimeoutException, ConnectException, IOException
        // Not retryable: HttpException (4xx), InvalidAPIKeyException
        return when (error) {
            is SocketTimeoutException -> true
            is ConnectException -> true
            is HttpException -> error.code >= 500 || error.code == 429
            else -> false
        }
    }
    
    override fun getBackoffDelayMs(attempt: Int): Long {
        val multiplier = remoteConfig.getDouble(
            RemoteVariableKeys.API_RETRY_BACKOFF_MULTIPLIER
        )
        return (INITIAL_DELAY_MS * multiplier.pow(attempt - 1)).toLong()
    }
    
    override fun getMaxAttempts(): Int {
        return remoteConfig.getInt(
            RemoteVariableKeys.API_RETRY_MAX_ATTEMPTS
        )
    }
    
    override fun getTimeoutMs(): Long {
        return remoteConfig.getLong(
            RemoteVariableKeys.API_REQUEST_TIMEOUT_SECONDS
        ) * 1000
    }
}
```

### RetryInterceptor (OkHttp)

```kotlin
// core:data/network/RetryInterceptor.kt
internal class RetryInterceptor(
    private val retryPolicy: RetryPolicy,
    private val analytics: RemoteConfigAnalytics
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: Exception? = null
        var attempt = 0
        
        while (attempt < retryPolicy.getMaxAttempts()) {
            attempt++
            try {
                val request = chain.request()
                    .newBuilder()
                    .header("X-Retry-Attempt", attempt.toString())
                    .build()
                
                val startTime = System.currentTimeMillis()
                val response = chain.proceed(request)
                val duration = System.currentTimeMillis() - startTime
                
                // Success
                if (response.isSuccessful) {
                    analytics.logRetrySuccess(
                        attempt = attempt,
                        durationMs = duration,
                        statusCode = response.code
                    )
                    return response
                }
                
                // Non-retryable 4xx
                if (response.code in 400..499 && response.code != 429) {
                    analytics.logRetryFailed(
                        attempt = attempt,
                        statusCode = response.code,
                        reason = "non_retryable_4xx"
                    )
                    return response
                }
                
                // Retryable error
                lastException = HttpException(response)
                
            } catch (e: Exception) {
                if (!retryPolicy.isRetryable(e)) {
                    analytics.logRetryFailed(
                        attempt = attempt,
                        statusCode = null,
                        reason = e.javaClass.simpleName
                    )
                    throw e
                }
                lastException = e
            }
            
            if (attempt < retryPolicy.getMaxAttempts()) {
                val backoffMs = retryPolicy.getBackoffDelayMs(attempt)
                Thread.sleep(backoffMs)  // or use coroutines delay
                analytics.logRetryAttempt(
                    attempt = attempt,
                    backoffMs = backoffMs
                )
            }
        }
        
        // Exhausted retries
        analytics.logRetryExhausted(
            maxAttempts = retryPolicy.getMaxAttempts(),
            lastError = lastException
        )
        throw lastException ?: IOException("Max retries exhausted")
    }
}

// For suspend functions (coroutines):
internal suspend inline fun <T> withRetry(
    retryPolicy: RetryPolicy,
    analytics: RemoteConfigAnalytics,
    crossinline block: suspend () -> T
): T {
    var lastException: Exception? = null
    var attempt = 0
    
    while (attempt < retryPolicy.getMaxAttempts()) {
        attempt++
        try {
            return block()
        } catch (e: Exception) {
            if (!retryPolicy.isRetryable(e)) {
                throw e
            }
            lastException = e
        }
        
        if (attempt < retryPolicy.getMaxAttempts()) {
            val backoffMs = retryPolicy.getBackoffDelayMs(attempt)
            delay(backoffMs)
            analytics.logRetryAttempt(attempt, backoffMs)
        }
    }
    
    throw lastException ?: IOException("Max retries exhausted")
}
```

### Analytics Events

```kotlin
// core:analytics/RemoteConfigAnalytics.kt (add methods)
internal fun logRetryAttempt(attempt: Int, backoffMs: Long) {
    trackEvent("retry_attempt", mapOf(
        "attempt_number" to attempt,
        "backoff_delay_ms" to backoffMs
    ))
}

internal fun logRetrySuccess(attempt: Int, durationMs: Long, statusCode: Int) {
    trackEvent("retry_success", mapOf(
        "attempt_number" to attempt,
        "total_duration_ms" to durationMs,
        "status_code" to statusCode
    ))
}

internal fun logRetryExhausted(maxAttempts: Int, lastError: Exception?) {
    trackEvent("retry_exhausted", mapOf(
        "max_attempts" to maxAttempts,
        "error_type" to (lastError?.javaClass?.simpleName ?: "unknown")
    ))
}
```

### Crashlytics Breadcrumbs

```kotlin
// Before each attempt
crashlytics.log("Retry attempt $attempt for API call")
crashlytics.setCustomKey("retry_max_attempts", maxAttempts)
crashlytics.setCustomKey("api_request_timeout_s", timeoutSeconds)

// On final failure
crashlytics.recordException(IOException(
    "API call failed after $maxAttempts retries. Last error: ${lastError?.message}"
))
```

---

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento com Retry | Origem |
|---|---|---|---|
| 1 | Gemini timeout | Retry 3x com exponential backoff (1s, 1.5s, 2.25s) | AI_VISION_TIMEOUT_SECONDS config |
| 2 | Network error | Retryable: SocketTimeout, ConnectException, 5xx, 429 | HttpException handling |
| 3 | Invalid API key | NOT retryable (4xx) → fail immediately | HttpException.code check |
| 4 | Fallback strategy | Gemini fails 3x → try OpenRouter (for vision) | GeminiVisionService fallback |
| 5 | Config-driven | All retry params from remote config (tuneable) | RemoteVariableKeys |
| 6 | Observability | Every retry attempt logged (GA4 + Crashlytics) | Analytics integration |

---

## 6. Critérios de Aceitação

### Retry Logic

- ✓ Exponential backoff: delay = initialDelayMs × (multiplier ^ (attempt-1))
  - Example: 1s × (1.5 ^ 0) = 1s, 1s × (1.5 ^ 1) = 1.5s, 1s × (1.5 ^ 2) = 2.25s
- ✓ Max attempts configurable: API_RETRY_MAX_ATTEMPTS (default: 3)
- ✓ Timeout configurable: API_REQUEST_TIMEOUT_SECONDS (default: 30s)
- ✓ Backoff multiplier configurable: API_RETRY_BACKOFF_MULTIPLIER (default: "1.5")

### Retryability Rules

- ✓ Retryable: SocketTimeoutException, ConnectException, 5xx, 429
- ✓ NOT retryable: 4xx (except 429), HttpException (invalid API key)
- ✓ Fallback: Gemini fails → try OpenRouter (if vision request)

### Feature Toggles

| Toggle | Default | Behavior OFF | Behavior ON |
|--------|---------|---|---|
| `feat_ai_vision_fallback_enabled` | true | No fallback (fail immediately) | Gemini fails → OpenRouter |
| (no toggle for retry itself) | - | Retry logic always active | - |

### Observability

- ✓ GA4 events: `retry_attempt`, `retry_success`, `retry_exhausted`, `retry_failed`
- ✓ Crashlytics breadcrumbs: attempt count, timeouts, error types
- ✓ Crashlytics custom key: `remote_config_version` (included in all exceptions)

### Performance

- ✓ No I/O loops (retry happens async with delay, not CPU spinning)
- ✓ Thread pool: respect Dispatcher.IO for coroutines
- ✓ Memory: no unbounded collections (attempt count bounded by max_attempts ≤ 10)

### Compatibility

- ✓ minSdk 28+ (OkHttp 4.x available)
- ✓ Wear OS: RetryPolicy injectable, testable
- ✓ Offline: retry layer doesn't bypass offline detection (throw error after retries)

---

## 7. Plano de Testes

### Unitários

**RetryPolicy:**
- ✓ `isRetryable()`: SocketTimeoutException → true, HttpException(401) → false, HttpException(500) → true
- ✓ `getBackoffDelayMs()`: attempt 1 → 1000ms, attempt 2 → 1500ms, attempt 3 → 2250ms
- ✓ `getMaxAttempts()`: returns from remote config (mocked)

**RetryInterceptor:**
- ✓ Happy path: 1st attempt succeeds → no retry
- ✓ Retry succeeds: 1st fails, 2nd succeeds → total 2 attempts
- ✓ Exhausted: 3 failures → throw final exception
- ✓ Non-retryable: HttpException(401) → throw immediately
- ✓ Timeout: request > timeout → retry with backoff

**withRetry() suspend:**
- ✓ Happy path: suspend function succeeds on 1st call
- ✓ Retry succeeds: fails then succeeds
- ✓ Exhausted: all attempts fail → throw

### Instrumentados / E2E

- ✓ Real Gemini call (mock API): timeout on 1st, succeed on 2nd
- ✓ Real OpenRouter fallback: Gemini fails all → OpenRouter called
- ✓ GA4 event capture: verify `retry_attempt` event for each retry

### Lint/Detekt

- ✓ `./gradlew lintDebug` — zero findings
- ✓ `./gradlew detekt` — zero findings
- ✓ `./gradlew testDebug` — all tests pass

---

## 8. Dependências e Riscos

| Risco | Prob | Impacto | Mitigação |
|---|---|---|---|
| Infinite retry loop (bug in backoff calc) | Baixa | Alto | Unit tests verify backoff formula, max attempt check |
| Network still offline (retry after offline → still fails) | Baixa | Médio | Retry layer respects offline detection (fails fast) |
| OkHttp version conflict | Baixa | Médio | Already using OkHttp 4.x (check build.gradle) |
| Gemini fallback not implemented | Média | Alto | Blocker: fallback logic must be in GeminiVisionService |
| Config values invalid (negative, zero) | Baixa | Médio | Add validation: API_RETRY_MAX_ATTEMPTS > 0, multiplier > 1 |
| Memory leak in retry callback | Baixa | Médio | Use suspend (not callbacks), respect coroutine scope |

---

## 9. Perguntas em Aberto (bloqueantes)

- [ ] **Q1: Gemini fallback implementation.** Who implements fallback logic in GeminiVisionService? Is OpenRouter available as fallback, or only retry?
  - **A:** Fallback logic is part of sdd-053 scope (see §3 diagram, GeminiVisionService consumer). OpenRouter is fallback for vision requests.

- [ ] **Q2: OkHttp vs Ktor.** Use OkHttp interceptor or Ktor plugin? Current codebase uses?
  - **A:** Check app/build.gradle.kts for HTTP client. Assume OkHttp (standard). If Ktor, create Ktor plugin instead of OkHttp interceptor (similar logic).

**Resolution:** Both bloqueantes answered. No blocking questions remain.

---

## 10. Checklist de Conformidade

- [x] Decisões citam código/origem (sdd-052 configs, RemoteConfigKeys.kt, RemoteConfigDefaults.kt, RemoteConfigAnalytics.kt)
- [x] Nenhum código de produção escrito (apenas pseudocódigo, interfaces, assinaturas)
- [x] Seção 6 preenchida de forma substantiva (retry rules, observability, performance)
- [x] Sem breaking changes (additive: new RetryPolicy, RetryInterceptor, analytics events)
- [x] Feature toggles documentados (FEAT_AI_VISION_FALLBACK_ENABLED behavior)
- [x] Observabilidade planejada (GA4 + Crashlytics)
- [x] Perguntas em aberto exauridas

---

## Apêndice A: Configurações Consumidas (sdd-052)

```kotlin
// RemoteVariableKeys (from sdd-052)
const val API_REQUEST_TIMEOUT_SECONDS = "api_request_timeout_seconds"        // default 30
const val API_RETRY_MAX_ATTEMPTS = "api_retry_max_attempts"                   // default 3
const val API_RETRY_BACKOFF_MULTIPLIER = "api_retry_backoff_multiplier"       // default "1.5"
const val AI_VISION_TIMEOUT_SECONDS = "ai_vision_timeout_seconds"             // default 10
const val PHOTO_SCAN_RETRY_COUNT = "photo_scan_retry_count"                   // default 2

// RemoteConfigDefaults (from sdd-052)
const val API_REQUEST_TIMEOUT_SECONDS_DEFAULT = 30
const val API_RETRY_MAX_ATTEMPTS_DEFAULT = 3
const val API_RETRY_BACKOFF_MULTIPLIER_DEFAULT = "1.5"
const val AI_VISION_TIMEOUT_SECONDS_DEFAULT = 10
const val PHOTO_SCAN_RETRY_COUNT_DEFAULT = 2
```

---

## Apêndice B: Exemplo de Uso (GeminiVisionService)

```kotlin
// feature:ai-agent/data/service/GeminiVisionService.kt
internal class GeminiVisionService @Inject constructor(
    private val geminiClient: GeminiClient,  // OkHttp with RetryInterceptor
    private val openRouterService: OpenRouterService,  // fallback
    private val retryPolicy: RetryPolicy,
    private val analytics: RemoteConfigAnalytics
) {
    
    internal suspend fun scanPhotoWithFallback(
        imageBytes: ByteArray
    ): String = withRetry(retryPolicy, analytics) {
        try {
            // Try Gemini first
            geminiClient.vision(imageBytes)
        } catch (e: Exception) {
            if (isGeminiFallbackEnabled() && e.isRetryable()) {
                // Fallback to OpenRouter
                openRouterService.vision(imageBytes)
            } else {
                throw e
            }
        }
    }
    
    private fun isGeminiFallbackEnabled(): Boolean {
        return remoteConfig.getBoolean(
            FeatureFlagKeys.FEAT_AI_VISION_FALLBACK_ENABLED
        )
    }
}
```

---

## Apêndice C: GA4 Dashboard Query

```
Event: retry_attempt, retry_success, retry_exhausted
Dimensions: event_date, event_parameter(error_type), event_parameter(attempt_number)
Metrics: event_count
Timeline: Last 30 days
Breakdown: by event_parameter(error_type)

Result: Identify which errors are most common, whether retries succeed, retry exhaustion rate
```

---

## Implementação — Status Atual

### ✅ Fase 1: Infraestrutura (COMPLETA — commit 786d2e9f)

- ✅ core:network módulo criado (evita circular dependency)
- ✅ RetryPolicy interface (internal)
- ✅ ExponentialBackoffRetryPolicy (internal, desacoplado de configs)
- ✅ withRetry() suspend function (interno)
- ✅ Testes unitários em core:network (100% passing)
- ✅ Lint + detekt + testes all passing
- ✅ Zero circular dependencies: core:data, core:remote-config, feature:* podem depender de core:network

### 📋 Fase 2: Integração em Features (PRÓXIMO)

**feature:ai-agent (OpenRouterAgent + GeminiAgent):**
1. Criar RetryConfigAdapter local (lê configs remotas, cria RetryPolicy)
2. Injetar RetryConfigAdapter em OpenRouterAgent
3. Usar withRetry() ao redor de executeRequest() (linhas 159-176)
4. Adicionar GA4 events + Crashlytics breadcrumbs
5. Testar com real Gemini + OpenRouter fallback

**feature:chat (se houver LLM calls diretamente):**
1. Similar ao OpenRouterAgent

### 📋 Fase 3: Analytics + Fallback (FUTURE)

1. Integrar RetryAnalytics para GA4 tracking
2. Implementar Gemini → OpenRouter fallback em GeminiVisionService
3. Upload configs ao Firebase Remote Config (staging first)
4. Setup GA4 Explorations dashboard

## Apêndice D: Timeline & Next SDDs

```
2026-11-08: sdd-053 Phase 1 (core:network) — ✅ COMPLETO
2026-11-15: sdd-053 Phase 2 (Feature integration)
2026-11-22: sdd-054 (UX/Onboarding improvements)
2026-11-29: sdd-055 (Wear OS sync tuning + client-side cache)
```
