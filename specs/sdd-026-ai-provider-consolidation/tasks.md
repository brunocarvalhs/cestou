# Task: Consolidar Providers de IA (Template Method + `enum AiProvider`)

- **Slug:** ai-provider-consolidation
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** ready
- **Versão-alvo:** 2.3.0 (minor — refactor interno sem mudança de comportamento visível, mas com
  contrato público alterado em `core:domain`; agrupar no mesmo minor que `sdd-014`, que depende deste)
- **Tipo de mudança:** refactor
- **Impacto público (módulo):** **breaking** (interno ao monorepo) — `AppSettings.aiProvider` e
  `AiModel.provider` trocam de tipo (`String` → `AiProvider`); todos os consumidores são atualizados
  no mesmo PR (ver §4 e lista de consumidores abaixo). Não é breaking para usuário final: nenhuma
  tela, string visível ou dado persistido muda de formato (DataStore continua guardando o `.name` do
  enum como string, igual já faz hoje `AiConfigTier`).

> **Nota pós-implementação (2026-11-02, via `code-reviewer`):** os nomes do pseudocódigo da §4 foram
> ajustados durante a revisão para evitar um verbo ambíguo: `BaseAiAgent` → **`BaseAgent`**;
> `BaseAiAgent.dispatchToolCall` → **`BaseAgent.executeAction`** (é o único lugar que de fato executa
> a `AgentAction`); `GeminiAgent.executeFunctionCall` → **`GeminiAgent.toFunctionResponsePart`** e
> `OpenRouterAgent.executeToolCall` → **`OpenRouterAgent.toToolCallContent`** (os dois agora são só
> adaptadores de formato, não executores — o nome "execute" ficou reservado para onde a execução
> realmente ocorre). Nenhum contrato público nem comportamento mudou, só nomenclatura interna.

---

## 1. Contexto e Motivação

Este SDD resolve os achados **A-03** e **A-04** de `specs/sdd-025-auditoria-clean-architecture-design-patterns/tasks.md`
§5.2 e §7, e deve rodar **antes** de `sdd-014-ai-agent-three-tier-config` (BYOK multi-provider) para não
replicar a duplicação/inconsistência com um 3º provider (recomendação já registrada em `specs/tasks.txt`).

Dois problemas, mesma causa raiz (os dois providers de IA foram escritos como duas implementações
independentes em vez de uma abstração compartilhada):

1. **A-03** — `GeminiAgent.executeFunctionCall` (`feature/ai-agent/.../GeminiAgent.kt:147-192`) e
   `OpenRouterAgent.executeToolCall` (`feature/ai-agent/.../OpenRouterAgent.kt:198-231`) duplicam
   quase byte a byte o gate Pro (AD-010), o dispatch via `AgentRegistry.find`, o `try/catch` com
   `crashReporter.recordException` e o log de erro — `isPro()` (`GeminiAgent.kt:194-196` vs.
   `OpenRouterAgent.kt:233-235`) é **idêntica** nas duas classes. As duas classes constroem seu
   cliente de SDK inline (`GenerativeModel`/`HttpClient` via `by lazy`, `GeminiAgent.kt:43-76`,
   `OpenRouterAgent.kt:56-68`) em vez de receber via injeção — por isso estão **excluídas do gate de
   cobertura do Kover** (`buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:106-112`), com um comentário
   no próprio arquivo já reconhecendo a dívida. Confirmado: não existe hoje nenhum `GeminiAgentTest.kt`
   nem `OpenRouterAgentTest.kt` no projeto.
2. **A-04** — `AppSettings.aiProvider` (`core/domain/.../AppSettings.kt:11`) e `AiModel.provider`
   (`core/domain/.../AiModel.kt:13`) são `String` livre (`"gemini"`/`"openrouter"`, e um 3º valor
   `"auto"` usado apenas em teste — ver §5, regra 3), enquanto `AiConfigTier`
   (`core/domain/.../AiConfigTier.kt`), no mesmo domínio, já é um `enum` type-safe persistido como
   `.name` no DataStore (`SettingsRepositoryImpl.kt:140`). A comparação por string aparece em pelo
   menos 4 pontos de `AgentFactoryImpl.kt` (linhas 38, 62, 73, 101-104).

## 2. Escopo

**Inclui:**
- Extrair `BaseAiAgent` (Template Method) com o gate Pro + dispatch + tratamento de erro compartilhado
  entre `GeminiAgent` e `OpenRouterAgent`.
- Injetar os clientes de SDK (`GenerativeModel`, `HttpClient`) via Hilt, através de uma Factory
  injetável por agente, para tornar as duas classes testáveis sem round-trip de rede real.
- Criar `GeminiAgentTest`/`OpenRouterAgentTest` e remover a exclusão das duas classes em
  `howmuch.kover.gradle.kts`.
- Introduzir `enum AiProvider` em `core:domain` e migrar `AppSettings.aiProvider`/`AiModel.provider`
  e todos os pontos de comparação por string listados em §4.
- Migração segura do valor já persistido no DataStore (`ai_provider`), incluindo valores legados
  desconhecidos (ex. `"nvidia"`, de antes do fix `sdd-006`/B-07) sem lançar excessão.

**Exclui:**
- Qualquer mudança de comportamento do usuário (seleção de modelo, UI de configurações, three-tier
  BYOK) — isso é `sdd-014`.
- Unificar o loop de `sendMessage()`/function-calling inteiro das duas classes num único Template
  Method — os formatos de request/response dos dois SDKs (`Content`/`FunctionCallPart` vs.
  `ChatRequest`/`ChatResponse`) são estruturalmente diferentes demais para compensar o esforço; apenas
  o **dispatch chokepoint** (gate Pro + execução + erro) é compartilhado, que é onde está 100% da
  duplicação real identificada em A-03.
- Corrigir a inconsistência de que `AiProvider.AUTO` nunca é produzido pelos dados reais hoje (só em
  teste) — comportamento preservado 1:1, ver §5 regra 3 e §9.1.
- Resolver `A-01` (`AgentRegistry` como service locator) — é um SDD separado (`sdd-027`, maior
  risco/esforço, sequenciado depois de `sdd-017` estabilizar).

## 3. Impacto Arquitetural

**Módulos/camadas afetados:** `core:domain/model` (novo enum + 2 campos alterados), `core:data/repository`
(persistência), `feature:ai-agent/data/service` (nova classe base + 2 factories + DI), `feature:ai-agent/di`.
Nenhuma mudança em `presentation`/`navigation` — confirmado que nem `feature:settings` (telas de IA,
`AiSettingsViewModel.kt`) nem `:wear` tocam `aiProvider`/`AiModel.provider` diretamente.

**Padrão aplicado:** Template Method (GoF) para o dispatch compartilhado; Factory Method (GoF, mesmo
idioma já usado em `AgentFactoryImpl`) para a injeção dos clientes de SDK — consistência com os
padrões já catalogados como "aplicados corretamente" em `sdd-025` §5.1.

**Novos arquivos:**
- `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/model/AiProvider.kt`
- `feature/ai-agent/src/main/java/br/com/brunocarvalhs/howmuch/feature/ai_agent/data/service/BaseAiAgent.kt`
- `feature/ai-agent/src/main/java/br/com/brunocarvalhs/howmuch/feature/ai_agent/data/service/GeminiModelFactory.kt`
  (interface + implementação default, ambas `internal`)
- `feature/ai-agent/src/main/java/br/com/brunocarvalhs/howmuch/feature/ai_agent/data/service/OpenRouterHttpClientFactory.kt`
  (interface + implementação default, ambas `internal`)
- `feature/ai-agent/src/test/java/br/com/brunocarvalhs/howmuch/feature/ai_agent/data/service/GeminiAgentTest.kt`
- `feature/ai-agent/src/test/java/br/com/brunocarvalhs/howmuch/feature/ai_agent/data/service/OpenRouterAgentTest.kt`

**Arquivos alterados (produção):**
`core/domain/.../AppSettings.kt`, `core/domain/.../AiModel.kt`, `core/data/.../SettingsRepositoryImpl.kt`,
`core/data/.../AiModelRepositoryImpl.kt:51`, `feature/ai-agent/.../GeminiAgent.kt`,
`feature/ai-agent/.../OpenRouterAgent.kt`, `feature/ai-agent/.../AgentFactoryImpl.kt`,
`feature/ai-agent/.../di/AgentModule.kt`, `buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:106-112`
(remove a exclusão).

**Diagrama (antes → depois do dispatch chokepoint):**

```
ANTES
┌─────────────────┐        ┌──────────────────┐
│  GeminiAgent   │        │ OpenRouterAgent │
│ executeFunctionCall()     │ executeToolCall()  │   <- lógica idêntica, 2x
│ isPro()          │        │ isPro()            │   <- lógica idêntica, 2x
│ GenerativeModel (inline)  │ HttpClient (inline) │   <- não injetável, exclui Kover
└─────────────────┘        └──────────────────┘

DEPOIS
                 ┌───────────────────────────┐
                 │       BaseAiAgent          │
                 │  dispatchToolCall()  <- único
                 │  isPro()             <- único
                 └──────────┬────────────────┘
                 ┌──────────┴────────────┐
       ┌─────────▼────────┐    ┌─────────▼─────────┐
       │  GeminiAgent    │    │ OpenRouterAgent  │
       │  providerTag      │    │  providerTag       │
       │  GeminiModelFactory (injetado via Hilt)     │
       │  OpenRouterHttpClientFactory (injetado via Hilt) │
       └───────────────────┘    └────────────────────┘
```

**Injeção via Hilt:** `GeminiModelFactory`/`OpenRouterHttpClientFactory` ligados em `AgentModule`
(`@Binds @Singleton`, mesmo padrão já usado para `AgentFactory`/`AiSession`). `AgentFactoryImpl`
passa a receber as duas factories no construtor (hoje já recebe 6 dependências via `@Inject
constructor`) e repassá-las para `GeminiAgent`/`OpenRouterAgent` junto de `AgentDependencies`.

## 4. Contratos e Interfaces

```kotlin
// core/domain/model/AiProvider.kt — PUBLIC (mesmo tier de AiConfigTier; consumido por
// core:data, feature:ai-agent, e transitivamente por qualquer módulo que leia AppSettings/AiModel).
@Serializable
enum class AiProvider { AUTO, GEMINI, OPENROUTER }

// core/domain/model/AppSettings.kt — campo alterado (BREAKING interno, ver consumidores abaixo)
data class AppSettings(
    // ...
    val aiProvider: AiProvider = AiProvider.OPENROUTER, // antes: String = "openrouter"
    // ...
)

// core/domain/model/AiModel.kt — campo alterado (BREAKING interno)
data class AiModel(
    val id: String,
    val name: String,
    val vendor: String,
    val provider: AiProvider = AiProvider.OPENROUTER, // antes: String = "openrouter"
    val isFree: Boolean = true
)

// feature/ai-agent/data/service/BaseAiAgent.kt — internal
internal abstract class BaseAiAgent(
    protected val dependencies: AgentDependencies
) : Agent {
    /** "gemini" | "openrouter" — usado em Timber.tag e crashReporter extras, substitui os literais
     * hoje espalhados em GeminiAgent.kt:129,175 e OpenRouterAgent.kt:128,220. */
    protected abstract val providerTag: String

    /** Template Method: gate Pro (AD-010) + AgentRegistry.find + execute + try/catch + crashReporter.
     * Corpo igual ao de GeminiAgent.executeFunctionCall/OpenRouterAgent.executeToolCall hoje —
     * só a extração de nome/args de cada SDK fica na subclasse, antes de chamar isto. */
    protected suspend fun dispatchToolCall(
        actionName: String,
        rawArgs: Map<String, Any?>,
        meta: Map<String, Any?>
    ): String // pseudocódigo — implementação real é tarefa do code-implementer

    protected suspend fun isPro(): Boolean // dependencies.subscriptionRepository.observeStatus().first() == PRO
}

// feature/ai-agent/data/service/GeminiModelFactory.kt — internal
internal interface GeminiModelFactory {
    fun create(modelName: String, apiKey: String, systemPrompt: String, tools: List<AgentAction<*>>): GenerativeModel
}
// implementação default internal substitui o `by lazy { GenerativeModel(...) }` hoje inline em GeminiAgent.kt:43-76

// feature/ai-agent/data/service/OpenRouterHttpClientFactory.kt — internal
internal interface OpenRouterHttpClientFactory {
    fun create(): HttpClient
}
// implementação default internal substitui o `HttpClient(OkHttp) { ... }` hoje inline em OpenRouterAgent.kt:56-68

// feature/ai-agent/data/service/GeminiAgent.kt — assinatura alterada
internal class GeminiAgent(
    dependencies: AgentDependencies,
    private val modelFactory: GeminiModelFactory, // NOVO
    private val modelName: String = BuildConfig.GEMINI_AGENT,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) : BaseAiAgent(dependencies) { override val providerTag = "gemini" /* ... */ }

// feature/ai-agent/data/service/OpenRouterAgent.kt — assinatura alterada (mesmo padrão)
internal class OpenRouterAgent(
    dependencies: AgentDependencies,
    private val httpClientFactory: OpenRouterHttpClientFactory, // NOVO
    private val model: String = BuildConfig.OPEN_ROUTER_MODEL,
    private val apiKey: String = BuildConfig.OPEN_ROUTER_API_KEY
) : BaseAiAgent(dependencies) { override val providerTag = "openrouter" /* ... */ }

// feature/ai-agent/data/service/AgentFactoryImpl.kt — selectAgent assinatura alterada
private fun selectAgent(provider: AiProvider, gemini: Agent?, openRouter: Agent?): Agent
// when(provider) { AiProvider.GEMINI -> ...; AiProvider.OPENROUTER -> ...; AiProvider.AUTO -> ... }
```

**Consumidores que precisam ser atualizados no mesmo PR** (breaking interno de `AppSettings.aiProvider`/`AiModel.provider`):
`core/data/.../SettingsRepositoryImpl.kt` (leitura/escrita DataStore), `core/data/.../AiModelRepositoryImpl.kt:51`,
`feature/ai-agent/.../AgentFactoryImpl.kt`, e os testes listados em §7. Confirmado por busca: nenhuma
tela (`feature:settings`, `AiSettingsScreen.kt`/`AiTier2Content.kt`/`AiTier3Content.kt`/`AiSettingsViewModel.kt`)
nem `:wear` leem `aiProvider`/`AiModel.provider` diretamente — zero impacto de UI.

Nenhum endpoint, coleção Firestore, evento de Analytics ou chave de remote-config novo.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Gate Pro + dispatch + erro duplicados byte a byte em 2 classes | Mesmo comportamento; implementação única em `BaseAiAgent.dispatchToolCall`/`isPro` | `GeminiAgent.kt:147-196`, `OpenRouterAgent.kt:198-235` |
| 2 | `aiProvider` persistido como string livre; valor legado/desconhecido cai no heurístico `providerFor(aiModel)` via `when` | Idêntico, mas resolvido com parsing seguro de enum (`AiProvider.entries.firstOrNull { it.name.equals(stored, ignoreCase = true) }`), nunca `AiProvider.valueOf()` direto — `valueOf` lançaria `IllegalArgumentException` em qualquer valor legado (ex. `"nvidia"`) | `SettingsRepositoryImpl.kt:91,101` |
| 3 | `aiProvider == "auto"` nunca é produzido pela leitura real do DataStore hoje (`resolveStoredAiProvider` só retorna `"gemini"`/`"openrouter"`); só é exercitado via `AppSettings(aiProvider = "auto")` em testes, para validar o branch `else` de `selectAgent` | **Preservado 1:1** — `AiProvider.AUTO` continua inatingível pelos dados reais; não é objetivo deste SDD corrigir essa divergência (ver §9.1) | `AppSettings.kt:11` (default `"openrouter"`, nunca `"auto"`); `AgentFactoryImplTest.kt` usa `"auto"` em 3 testes |
| 4 | `GenerativeModel`/`HttpClient` construídos inline dentro do próprio agente | Construídos via `GeminiModelFactory`/`OpenRouterHttpClientFactory` injetadas via Hilt | `GeminiAgent.kt:43-76`, `OpenRouterAgent.kt:56-68` |
| 5 | `GeminiAgent`/`OpenRouterAgent` excluídas do gate de cobertura do Kover | Exclusão removida; classes cobertas por teste unitário real | `buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:106-112` |

## 6. Critérios de Aceitação

- **Performance:** nenhuma alocação adicional por requisição — `GenerativeModel`/`HttpClient` continuam
  construídos uma única vez por agente (via `by lazy` dentro do agente, chamando a factory injetada),
  não por chamada. As factories em si são `@Singleton` sem estado.
- **Toggles:** nenhum novo — é refactor interno, sem comportamento condicional novo.
- **Observabilidade:** `crashReporter.recordException(extras = mapOf("provider" to providerTag, ...))`
  mantém exatamente a mesma string (`"gemini"`/`"openrouter"`) que já chega hoje ao Crashlytics — nenhuma
  métrica/dashboard existente quebra.
- **Compatibilidade:** `minSdk 28` inalterado. Usuários com `ai_provider` já persistido no DataStore
  (valores válidos `"gemini"`/`"openrouter"`, ou legados como `"nvidia"` de antes do `sdd-006`) continuam
  resolvendo para o provider correto sem crash — cobrir explicitamente esse caso no teste de
  `SettingsRepositoryImplTest` (regra 2 acima). `:wear` não é afetado (confirmado, não consome
  `AppSettings.aiProvider`/`AiModel` neste fluxo).

## 7. Plano de Testes

- **Unitários — novos:**
  - `GeminiAgentTest`: gate Pro bloqueia/libera ação (`requiresPro`), dispatch encontra/não encontra
    ação no registry, captura de exceção chama `crashReporter.recordException` com `provider = "gemini"`,
    `modelFactory.create(...)` é chamado com os parâmetros corretos (mockável, já que é uma interface).
  - `OpenRouterAgentTest`: mesmos cenários, substituindo `modelFactory` por `httpClientFactory`.
  - (Opcional, se reduzir duplicação de teste também) `BaseAiAgentTest` com uma subclasse de teste mínima,
    cobrindo só `dispatchToolCall`/`isPro` uma vez — a decidir pelo code-implementer conforme o quanto o
    corpo real divergir entre Gemini/OpenRouter após a extração.
- **Unitários — atualizados** (troca de `String` por `AiProvider`, sem mudança de cenário):
  `AgentFactoryImplTest.kt` (todas as 14 ocorrências de `AppSettings(aiProvider = "gemini"/"openrouter"/"auto")`),
  `SettingsRepositoryImplTest.kt` (6 ocorrências de `assertEquals("openrouter"/"gemini", ...)`, mais um
  cenário **novo** para o valor legado desconhecido da regra 2), `core/domain/.../AiModelTest.kt` (2
  ocorrências de `assertEquals("openrouter", model.provider)`), `CartAssistantUseCaseTest.kt:25`
  (`AppSettings(aiProvider = "gemini")`). `AiSettingsScreenTest.kt:119-120` não precisa mudar (não passa
  `provider=` explicitamente, usa o default).
- **Instrumentados/UI:** nenhum necessário — zero mudança de UI confirmada em §4.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`,
  `./gradlew koverVerify koverVerifyLayout` — **a remoção da exclusão no Kover (regra 5) só deve ser
  commitada depois que os novos testes passarem localmente**, para não quebrar o gate de cobertura no CI.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `AiProvider.valueOf()` usado por engano em vez de parsing seguro, lançando `IllegalArgumentException` para usuários com valor legado (`"nvidia"`) persistido no DataStore | média | alto (crash ao abrir `AiSettingsScreen` para uma base de usuários antiga) | Seguir a regra 2 (§5) à risca; teste unitário explícito com valor legado em `SettingsRepositoryImplTest` antes de considerar a tarefa concluída |
| Novo binding Hilt (`GeminiModelFactory`/`OpenRouterHttpClientFactory`) quebra o grafo de DI em runtime, não detectado em compile-time se a validação do Dagger não cobrir o caminho | baixa | alto (app não inicia) | `./gradlew assembleDebug` local antes do PR; manter os `@Binds` simples, sem `@Provides` condicional |
| Remover a exclusão do Kover antes dos novos testes cobrirem os branches de erro (`catch`/`crashReporter`) deixa o módulo abaixo do threshold de 80% | média | médio (build vermelho em `koverVerify`) | Ordem de execução: escrever testes → rodar `koverHtmlReport` localmente → só então remover a exclusão |
| `sdd-014` (BYOK, já roteirizado em `specs/tasks.txt`) é implementado em paralelo e colide com a troca `String`→`enum` | média | médio (conflito de merge) | Confirmar com o maintainer que este SDD é mergeado em `develop` antes de `sdd-014` avançar (já é a recomendação registrada em `sdd-025` §7) |

## 9. Perguntas em Aberto (não bloqueantes)

- [ ] **9.1** — `AiProvider.AUTO` continuará inatingível pelos dados reais após esta mudança (ver regra 3).
  Vale abrir, em paralelo (não neste SDD), uma decisão de produto sobre se o tier 1 do `sdd-014`
  ("Nível 1: valor default sempre o mesmo, automático e gratuito") deveria de fato persistir `AUTO` em
  vez de `OPENROUTER` como hoje? Não bloqueia esta tarefa — comportamento preservado 1:1.
- [ ] **9.2** — `BaseAiAgentTest` compartilhado vs. dois testes totalmente independentes (ver §7): decisão
  de estilo a critério do `code-implementer`, sem impacto em contrato ou regra de negócio.

Nenhum item acima impede o início da implementação — `status: ready`.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` e `specs/sdd-025-.../tasks.md` como origem do achado.
- [x] Nenhum código de produção escrito — apenas assinaturas/pseudocódigo em §4.
- [x] Seção 6 preenchida de forma substantiva (performance, observabilidade, compatibilidade).
- [x] Mudança em símbolo público (`AppSettings.aiProvider`, `AiModel.provider`) refletida como
  **breaking (interno)** em "Impacto público", com lista de consumidores em §4.
- [x] Nenhum endpoint/remote-config novo.
- [x] Nenhum cache novo.
- [x] Perguntas em aberto exauridas e marcadas como não bloqueantes.
