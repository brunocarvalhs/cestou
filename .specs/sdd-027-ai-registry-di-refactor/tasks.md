# Task: Substituir o Service Locator do AgentRegistry por Injeção de Dependência Real

- **Slug:** ai-registry-di-refactor
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** ready
- **Versão-alvo:** 2.3.0 (minor — mesmo grupo de `sdd-026`, que já troca `core:ai`/`feature:ai-agent`;
  refactor interno sem mudança de comportamento visível, mas com remoção de símbolos públicos)
- **Tipo de mudança:** refactor
- **Impacto público (módulo):** **breaking** (interno ao monorepo) — `AgentRegistry` deixa de ser
  `object` e perde os métodos públicos `register`/`registerProvider`/`clear`; `AgentActionProvider`
  é removida. Nenhum consumidor externo confirmado além dos listados em §4 (busca exaustiva no
  repositório inteiro). Não é breaking para usuário final: nenhum comportamento observável do app
  muda — as mesmas 15 tools de IA continuam disponíveis para o LLM, com o mesmo `id`/`description`.

## 1. Contexto e Motivação

Resolve o achado **A-01** de `specs/sdd-025-auditoria-clean-architecture-design-patterns/tasks.md`
§4 e §7: `AgentRegistry` (`core/ai/.../registry/AgentRegistry.kt:9-30`) é um `object` com
`mutableMapOf` global, em desacordo direto com AGENTS.md §5 ("Sem service locator"). Toda subclasse
de `AgentActionUseCase` se auto-registra nele via side-effect de construtor
(`core/ai/.../base/AgentActionUseCase.kt:33-37`, com `@Suppress("LeakingThis")`), e esse registro é
exatamente o que `GeminiAiAgent`/`OpenRouterAiAgent` consultam para despachar as tool calls vindas
do LLM — o ponto de maior risco de segurança do app (tema central de `sdd-017`, **já mergeado** em
`develop` via PR #242, satisfazendo o pré-requisito de sequenciamento registrado em `sdd-025` §7 e
em `specs/tasks.txt`).

**Descoberta chave que define o escopo deste plano:** o app já tem a infraestrutura de Hilt
Multibinding que resolveria isso sem o `object` — ela só não é usada para o propósito certo.
`CestouApplication.kt:12-13` já injeta `Set<@JvmSuppressWildcards AgentActionUseCase<*>>`, agregado
via `@Binds @IntoSet` em `ShoppingAgentModule.kt` (8 bindings) e `ProductAgentModule.kt` (7
bindings) — as 15 tools de IA do app inteiro. Hoje esse `Set` só serve para **forçar a instanciação
eager** de cada `AgentActionUseCase` no boot, para que o `init {}` de cada uma dispare o registro no
`object` global (`CestouApplication.kt:17`, log "N Agentes de IA registrados"). Ou seja: a fonte de
verdade correta (o `Set` do Hilt) já existe e já é confiável — o `object AgentRegistry` é uma camada
redundante e arriscada por cima dela.

## 2. Escopo

**Inclui:**
- Transformar `AgentRegistry` de `object` em `@Singleton class` com `@Inject constructor(actions:
  Set<@JvmSuppressWildcards AgentActionUseCase<*>>)`, construindo o mapa `id → action` uma única vez,
  a partir do mesmo `Set` que `CestouApplication` já consome hoje. `find`/`getAll` continuam com a
  mesma assinatura pública; `register`/`registerProvider`/`clear` são removidos (não há mais
  necessidade de mutação — o `Set` do Hilt já é a fonte de verdade completa no momento da construção).
- Remover o `init {}` de auto-registro e o `@Suppress("LeakingThis")` de `AgentActionUseCase`
  (`core/ai/.../base/AgentActionUseCase.kt:33-37`) — vira uma classe puramente de extração de
  metadados das anotações `@AiAgentAction`/`@AiAgentParameter`, sem side-effect nenhum no construtor.
- Remover `AgentActionProvider` (`core/ai/.../contract/AgentActionProvider.kt`) e o `override fun
  toAgentAction()` de `AgentActionUseCase` — confirmado por busca exaustiva (`grep` no repositório
  inteiro) que `AgentActionUseCase` é o único implementador existente; a indireção não tem outro
  consumidor e só existia para suportar o padrão de self-registration que está sendo removido.
- Remover o `@Provides fun provideAgentRegistry(): AgentRegistry = AgentRegistry`
  (`feature/ai-agent/.../di/AiAgentModule.kt:40-42`) — com `@Inject constructor`, o Hilt resolve
  `AgentRegistry` automaticamente, sem módulo nenhum.
- Remover o valor default `= AgentRegistry` de `AiAgentDependencies.registry`
  (`feature/ai-agent/.../AiAgentDependencies.kt:17`) — confirmado por busca que **nenhum** call site
  de produção ou teste depende desse default hoje; todos já passam `registry = registry`
  explicitamente.
- Atualizar `CestouApplication` para injetar `AgentRegistry` diretamente em vez do `Set` bruto,
  preservando o log de diagnóstico de boot (mesmo texto, nova fonte: `agentRegistry.getAll().size`).
- Reescrever `AgentRegistryTest.kt` para construir `AgentRegistry` via o novo construtor em vez de
  chamar métodos estáticos do `object`.

**Exclui:**
- Qualquer mudança nas 15 classes `AgentActionUseCase` concretas (`ShoppingCreateUseCase`,
  `ProductSaveUseCase` etc.) ou nos dois módulos `@IntoSet` existentes (`ShoppingAgentModule`,
  `ProductAgentModule`) — já estão corretos e não precisam de nenhuma alteração.
- Corrigir `A-02` (`ProductSaveUseCase` com regra de negócio divergente entre `invoke()`/`execute()`)
  — é `sdd-028`, já roteirizado em `specs/tasks.txt`.
- Qualquer mudança de comportamento do usuário — zero toggle, zero tela, zero string nova.

## 3. Impacto Arquitetural

**Módulos/camadas afetados:** `core:ai/registry`, `core:ai/base`, `core:ai/contract` (remoção),
`feature:ai-agent/di`, `feature:ai-agent/data/service` (só a remoção do default), `app` (ponto de
injeção em `CestouApplication`). Nenhuma mudança em `ShoppingAgentModule`/`ProductAgentModule`
(`feature:shopping`/`feature:products`) nem em qualquer `presentation`/`navigation`.

**Padrão aplicado:** substitui Service Locator por Injeção de Dependência real via Hilt Multibinding
— o mesmo padrão que `CestouApplication` já usa hoje só para forçar o warm-up, agora usado para a
coisa certa (ser a própria fonte de verdade do registro).

**Novos arquivos:** nenhum.

**Arquivos removidos:**
- `core/ai/src/main/java/br/com/brunocarvalhs/howmuch/core/ai/contract/AgentActionProvider.kt`

**Arquivos alterados (produção):**
`core/ai/.../registry/AgentRegistry.kt`, `core/ai/.../base/AgentActionUseCase.kt`,
`feature/ai-agent/.../di/AiAgentModule.kt`, `feature/ai-agent/.../AiAgentDependencies.kt`,
`app/.../CestouApplication.kt`.

**Diagrama (antes → depois):**

```
ANTES
AgentActionUseCase.init{} ──side-effect──▶ object AgentRegistry (mutableMapOf, global)
     ▲ instanciado só se                        ▲
     │ algo mais no grafo                        │ GeminiAiAgent/OpenRouterAiAgent leem
     │ pedir essa classe                         │ (via AiAgentDependencies.registry)
CestouApplication injeta Set<AgentActionUseCase<*>> SÓ para forçar essa instanciação no boot

DEPOIS
ShoppingAgentModule/ProductAgentModule (@IntoSet, já existentes, inalterados)
                    │
                    ▼
      Set<AgentActionUseCase<*>>  (mesmo Set de hoje)
                    │
                    ▼  @Inject constructor
      @Singleton class AgentRegistry (byId = actions.associateBy { it.id }, imutável após construção)
                    │                                   │
                    ▼                                   ▼
      CestouApplication (log de boot)      GeminiAiAgent/OpenRouterAiAgent (via AiAgentDependencies)
```

**Injeção via Hilt:** `AgentRegistry` não precisa mais de `@Provides`/`@Binds` — o próprio `@Inject
constructor` resolve, puxando o `Set<AgentActionUseCase<*>>` que já é montado pelos módulos
`@IntoSet` existentes em `feature:shopping`/`feature:products`. `AiAgentFactoryImpl` continua
recebendo `registry: AgentRegistry` no construtor exatamente como hoje (`feature/ai-agent/.../AiAgentFactoryImpl.kt:28`)
— só a implementação por trás do tipo muda.

## 4. Contratos e Interfaces

```kotlin
// core/ai/registry/AgentRegistry.kt — PUBLIC (cross-module: feature:ai-agent consome o tipo)
// Assinatura pública de find/getAll inalterada; register/registerProvider/clear REMOVIDOS (BREAKING
// interno — ver §9, sem consumidor externo confirmado).
@Singleton
class AgentRegistry @Inject constructor(
    actions: Set<@JvmSuppressWildcards AgentActionUseCase<*>>
) {
    private val byId: Map<String, AgentActionUseCase<*>> = actions.associateBy { it.id }

    fun find(id: String): AgentAction<*>? = byId[id]
    fun getAll(): List<AgentAction<*>> = byId.values.toList()
}

// core/ai/base/AgentActionUseCase.kt — assinatura pública inalterada (id/description/parameters),
// só o supertype AgentActionProvider e o init{} de registro são removidos.
abstract class AgentActionUseCase<R> : AgentAction<R> {
    // extração de @AiAgentAction/@AiAgentParameter inalterada — sem init{}, sem self-registration.
}

// core/ai/contract/AgentActionProvider.kt — REMOVIDO (BREAKING interno, zero consumidor externo
// confirmado por busca).

// feature/ai-agent/data/service/AiAgentDependencies.kt — default removido (nenhum call site usava)
internal data class AiAgentDependencies(
    val session: AiSession,
    val crashReporter: CrashReporter,
    val subscriptionRepository: SubscriptionRepository,
    val registry: AgentRegistry, // antes: = AgentRegistry (default inválido após a mudança)
    val systemPrompt: String = SystemPrompts.CESTOU_ASSISTANT
)

// feature/ai-agent/di/AiAgentModule.kt — método removido:
// @Provides @Singleton fun provideAgentRegistry(): AgentRegistry = AgentRegistry  (DELETADO)

// app/CestouApplication.kt — ponto de injeção alterado
@HiltAndroidApp
class CestouApplication : Application() {
    @Inject lateinit var agentRegistry: AgentRegistry // antes: Set<@JvmSuppressWildcards AgentActionUseCase<*>>
    override fun onCreate() {
        super.onCreate()
        Timber.d("🚀 ${agentRegistry.getAll().size} Agentes de IA registrados na inicialização.")
    }
}
```

**Consumidores confirmados por busca exaustiva (`grep` no repositório inteiro) que precisam mudar:**
`core/ai/.../base/AgentActionUseCase.kt`, `feature/ai-agent/.../di/AiAgentModule.kt`,
`feature/ai-agent/.../AiAgentDependencies.kt`, `app/.../CestouApplication.kt`,
`core/ai/src/test/.../AgentRegistryTest.kt`. **Nenhum outro arquivo** no repositório referencia
`AgentRegistry.register`/`.registerProvider`/`.clear`/`AgentActionProvider`/`toAgentAction` fora
desta lista — confirmado linha a linha.

Nenhum endpoint, coleção Firestore, evento de Analytics ou chave de remote-config novo.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | `AgentActionUseCase` se auto-registra no `init {}` com `@Suppress("LeakingThis")`, side-effect de construtor | Removido; cada ação já chega pronta no `Set` via `@Binds @IntoSet` (inalterado); `AgentRegistry` só lê o `Set`, nunca escreve | `AgentActionUseCase.kt:33-37` |
| 2 | `AgentRegistry` é um `object` global mutável (`register`/`find`/`getAll`/`clear`) | `@Singleton class` imutável após construção; só `find`/`getAll` sobrevivem | `AgentRegistry.kt:9-30` |
| 3 | `AgentActionProvider`/`toAgentAction()` existem para suportar um 2º tipo de "fonte de ação" além de `AgentActionUseCase` | Removidos — zero implementador confirmado além de `AgentActionUseCase`, que já é um `AgentAction` direto | `AgentActionProvider.kt`, `AgentActionUseCase.kt:40` |
| 4 | `CestouApplication` injeta o `Set<AgentActionUseCase<*>>` bruto só para forçar o boot de cada `init {}` | Injeta `AgentRegistry` diretamente; mesmo log, nova fonte (`getAll().size`) — a instanciação eager deixa de ser necessária para correção (o `Set` já é completo quando `AgentRegistry` é construído), só é mantida para preservar o diagnóstico de boot | `CestouApplication.kt:12-17` |
| 5 | Em caso de dois `AgentActionUseCase` com o mesmo `id` (não deveria ocorrer; nenhum caso real hoje), o `object` mutável aplicava "o último a se registrar vence", numa ordem de instanciação incidental e não garantida | `actions.associateBy { it.id }` também aplica "o último do `Set` vence", numa ordem também não garantida pelo Dagger — **mesma classe de risco, não piora, não é escopo deste SDD corrigir** | `AgentRegistry.kt` (novo), comportamento pré-existente |

## 6. Critérios de Aceitação

- **Performance:** `byId` é construído uma única vez, no momento em que o Hilt resolve
  `AgentRegistry` (primeira injeção, hoje via `CestouApplication` no boot) — não há mais 15
  side-effects de construtor espalhados, só uma função pura (`associateBy`) sobre um `Set` que já
  existe. Sem alocação adicional por chamada de `find`/`getAll` (mapa pré-computado).
- **Toggles:** nenhum — refactor interno sem comportamento condicional novo.
- **Observabilidade:** o log de boot em `CestouApplication` mantém exatamente o mesmo texto
  (`"🚀 N Agentes de IA registrados na inicialização."`), só troca a fonte do `N`.
- **Compatibilidade:** `minSdk 28` inalterado. `:wear` não depende de `core:ai`/`feature:ai-agent`
  neste fluxo (confirmado na auditoria `sdd-025`) — zero impacto.

## 7. Plano de Testes

- **Unitários — reescritos:** `AgentRegistryTest.kt` (`core/ai/src/test/.../registry/`) —
  substitui as chamadas estáticas (`AgentRegistry.register/registerProvider/clear`) por construção
  direta via o novo construtor (`AgentRegistry(actions = setOf(fakeAction1, fakeAction2))`),
  cobrindo: `find` encontra por id; `find` retorna `null` para id desconhecido; `getAll` retorna
  todas as ações do `Set` recebido; duas ações com o mesmo `id` no `Set` resultam em uma só entrada
  (mantém o cenário de "overwrite" já testado hoje, adaptado para `associateBy`). Não precisa mais de
  `@After { clear() }` — cada teste constrói sua própria instância isolada, eliminando o próprio
  motivo do achado A-01 (estado global entre testes).
- **Unitários — sem mudança de cenário, só de mock:** `AiAgentFactoryImplTest`, `GeminiAiAgentTest`,
  `OpenRouterAiAgentTest`, `GeminiAiAgentGatingTest`, `OpenRouterAiAgentGatingTest` já mockam
  `AgentRegistry` como instância (`mockk<AgentRegistry>(relaxed = true)`), nunca como `object` — não
  deveriam precisar de nenhuma alteração; confirmar isso é o próprio critério de aceitação do
  `code-implementer` antes de considerar a tarefa concluída.
- **Instrumentados/UI:** nenhum necessário — zero mudança de UI.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`,
  `./gradlew koverVerify koverVerifyLayout` — rodar em todo o projeto (não só `core:ai`), já que
  `app/CestouApplication.kt` também muda.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Existir algum consumidor de `AgentRegistry.register`/`.registerProvider`/`.clear`/`AgentActionProvider` não capturado pela busca deste plano | baixa | alto (erro de compilação) | A busca foi exaustiva (grep no repositório inteiro, resultados citados em §4); `./gradlew compileDebugKotlin` em todos os módulos revela qualquer remanescente imediatamente |
| Dagger falhar ao resolver `Set<AgentActionUseCase<*>>` no ponto de injeção de `AgentRegistry` (`core:ai`) por algum motivo de ordem de módulos/componente | baixa | alto (app não compila/não inicia) | O mesmo `Set` já é injetado hoje com sucesso em `CestouApplication` (`:app`); `AgentRegistry` só adiciona mais um consumidor do mesmo binding agregado no `SingletonComponent`, não um grafo novo — `./gradlew assembleDebug` confirma antes do PR |
| Remover o `init{}` de auto-registro mas esquecer de remover o `@Suppress("LeakingThis")` ou deixar `AgentActionProvider` como import morto | baixa | baixo (lint/detekt pega) | `./gradlew detekt` com a regra de import não utilizado; checklist de conformidade §10 |
| `sdd-028` (correção de `ProductSaveUseCase`, A-02) tocar os mesmos arquivos em paralelo | baixa | baixo (conflito de merge trivial, arquivos diferentes: este SDD não toca nenhuma classe `AgentActionUseCase` concreta) | Nenhuma — os dois SDDs não compartilham arquivo nenhum |

## 9. Perguntas em Aberto (não bloqueantes)

Nenhuma. O único pré-requisito de sequenciamento (`sdd-017` estabilizado) já está satisfeito —
confirmado: PR #242 (`fix/ai-agent-safety-sdd-017`) está **mergeado** em `develop`. A decisão de
manter o log de diagnóstico em `CestouApplication` (em vez de removê-lo, já que a instanciação eager
deixa de ser necessária para correção) foi tomada neste plano (§5, regra 4) para preservar
comportamento observável 1:1 — não é uma pergunta em aberto.

`status: ready`.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` e `specs/sdd-025-.../tasks.md` como origem do achado (A-01).
- [x] Nenhum código de produção escrito — apenas assinaturas/pseudocódigo em §4.
- [x] Seção 6 preenchida de forma substantiva (performance, observabilidade, compatibilidade).
- [x] Remoção de símbolos públicos (`AgentRegistry.register/registerProvider/clear`,
  `AgentActionProvider`) refletida como **breaking (interno)** em "Impacto público", com lista de
  consumidores confirmada em §4.
- [x] Nenhum endpoint/remote-config novo.
- [x] Nenhum cache novo (o mapa `byId` não é um cache no sentido de invalidação — é derivado uma vez
  de um `Set` imutável após a construção do grafo Hilt).
- [x] Perguntas em aberto exauridas (nenhuma).
