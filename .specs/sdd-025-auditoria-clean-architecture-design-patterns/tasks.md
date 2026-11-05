# Task: Auditoria de Conformidade com Clean Architecture e Design Patterns

- **Slug:** auditoria-clean-architecture-design-patterns
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** ready
- **Versão-alvo:** n/a (documento de diagnóstico; versões serão definidas nos SDDs filhos)
- **Tipo de mudança:** refactor
- **Impacto público (módulo):** none (este plano não altera contratos; cada SDD derivado define o próprio impacto)

> **Nota:** este arquivo substitui um rascunho anterior gerado incorretamente nesta mesma sessão por um
> subagente de investigação que saiu do escopo pedido (deveria apenas ler `specs/sdd-024-cobertura-testes-ai-settings/`
> para contexto, e em vez disso escreveu sua própria conclusão de auditoria sem ver os achados dos outros
> 3 subagentes paralelos). O conteúdo abaixo é a síntese real, verificada arquivo a arquivo, dos 4 eixos de
> investigação. O achado mais crítico do rascunho descartado ("projeto já está substancialmente conforme")
> era incompleto — existe pelo menos um achado P1 real (AgentRegistry, ver A-01) que ele não via.

---

## 1. Contexto e Motivação

Pedido do usuário: inspecionar o projeto quanto à conformidade com Clean Architecture (referência:
[Zup — Clean Architecture](https://zup.com.br/blog/clean-architecture-arquitetura-limpa/)) e identificar
Design Patterns do catálogo GoF/refactoring.guru ([refactoring.guru/design-patterns](https://refactoring.guru/design-patterns))
que fazem sentido para o projeto, apontando o que migrar para facilitar manutenção e escalabilidade.

Este não é um pedido de bug funcional (como `sdd-001`/`sdd-023`) — é uma auditoria estrutural: grafo de
dependências entre os 19 módulos (`settings.gradle.kts`), separação de camadas dentro de cada módulo,
padrões de projeto já aplicados vs. ausentes, e guard-rails automatizados que impedem regressão.

Rastreabilidade: `AGENTS.md` §2 (padrões adotados), §5 (convenções/visibilidade), §6 (fluxo para parâmetros
públicos); `architecture/01-visao-geral-arquitetura.md` (anatomia de módulo/feature); `architecture/05-decisoes-arquiteturais.md`
AD-001 (Clean Architecture multi-módulo), AD-005 (visibilidade `internal` por padrão), AD-007 (AI via
`AgentActionUseCase`), AD-010 (contrato de entitlement isolado), AD-011 (segregação `ProductReader`/
`ShareShoppingUseCase`), AD-013 (acoplamento `profile`→`settings`).

## 2. Escopo

**Inclui:**
- Grafo de dependências Gradle real entre todos os módulos vs. a Dependency Rule.
- Separação de camadas (`domain`/`data`/`presentation`/`di`/`navigation`) dentro de cada `feature:*` e dos
  `core:*` de maior risco (`core:ai`, `core:auth`, `core:data`, `core:billing`, `core:remote-config`),
  com cobertura exaustiva de `core:ai`/`feature:ai-agent` (módulo mais crítico do app, ver `sdd-017`).
- Mapeamento de Design Patterns GoF já aplicados (corretamente ou de forma duplicada/inconsistente) e
  oportunidades concretas de introdução, com evidência de código real — não teoria genérica.
- Guard-rails automatizados existentes (Konsist, Detekt, Kover) contra os que faltam.

**Exclui:**
- Revisão de segurança dedicada (coberta por skills próprias — `android-permissions-security`,
  `android-intent-security` — e por `sdd-017`).
- Revisão visual/UX de componentes Compose.
- Auditoria linha a linha dos ~14 `AgentActionUseCase` restantes além de `ProductSaveUseCase` — o achado
  A-02 abaixo é uma amostra confirmada, não uma varredura completa (ver pergunta em aberto 9.1).
- Execução de build/testes instrumentados completos (auditoria estática, leitura direta de código e
  configuração).

## 3. Achados — Grafo de Dependências entre Módulos

| ID | Sev. | Achado | Evidência (`arquivo:linha`) | Por quê |
|---|---|---|---|---|
| **A-06** | P3 | `feature:cart` depende diretamente de `feature:products` e `feature:shopping` (para navegação — `EditShopping`, `ProductPickerRoute`). Tecnicamente **permitido** pela própria regra do Konsist (`ArchitectureTest.kt:84-98` só proíbe import cruzado fora de `.navigation.`), mas, ao contrário do caso equivalente `profile`→`settings`, nunca foi formalizado como AD. | `feature/cart/build.gradle.kts:15-16`; uso em `feature/cart/.../presentation/viewmodel/CartViewModel.kt:21,28`; regra em `app/src/test/.../architecture/ArchitectureTest.kt:84-98`; precedente formalizado em `architecture/05-decisoes-arquiteturais.md` AD-013 | Não é violação da Dependency Rule (é navegação, não lógica de negócio), mas é uma lacuna de rastreabilidade: a próxima pessoa que ler `feature:cart/build.gradle.kts` não tem como saber que isso é intencional sem grepar o Konsist. |
| **A-07** | P4 | `feature:shopping` declara `implementation(project(":feature:settings"))` mas **nenhum** import de `feature.settings.*` existe em `feature/shopping/src/main/java` — dependência Gradle morta, resíduo do cleanup da AD-013 (quando `ShoppingGraph` parou de referenciar a rota `Settings`). | `feature/shopping/build.gradle.kts:17` | Infla o grafo de build e confunde quem lê o módulo; sem risco funcional. |
| **A-08** | informativo | `core:ui` e `core:navigation` dependem de `core:domain` (`UserAvatars.kt`, `MobileRoutes.kt`). Não viola a Dependency Rule (domain é o centro), mas é acoplamento transversal→negócio que reduz a reusabilidade genérica desses módulos se crescer. | `core/ui/build.gradle.kts:15`; `core/navigation/build.gradle.kts:17` | Registrar como ponto de atenção, não como defeito. |

**Achado positivo (não é problema, é evidência de que a base está sólida):** nenhum módulo `core:*` tem
dependência de Android/Firebase indevida; `core:domain` é o módulo mais limpo do grafo (zero dependências
de projeto). O grafo completo módulo→módulo foi verificado linha a linha em todos os 22 `build.gradle.kts`
e não há nenhuma inversão real da Dependency Rule (nada em `domain` depende de `data`/`presentation`/framework).

## 4. Achados — Separação de Camadas dentro dos Módulos

| ID | Sev. | Achado | Evidência (`arquivo:linha`) | Cenário de risco |
|---|---|---|---|---|
| **A-01** | **P1** | `AgentRegistry` é um **service locator global com estado mutável** (`object` + `mutableMapOf`), em desacordo direto com AGENTS.md §5 ("Sem service locator"). Todo `AgentActionUseCase` se auto-registra no `init {}` com `@Suppress("LeakingThis")`, e esse registro populado por side-effect de construtor é exatamente o que `GeminiAgent`/`OpenRouterAgent` consultam para despachar as tool calls vindas do LLM. | `core/ai/.../registry/AgentRegistry.kt:9-30`; auto-registro em `core/ai/.../base/AgentActionUseCase.kt:33-37`; consumo em `feature/ai-agent/.../GeminiAgent.kt:151` e `OpenRouterAgent.kt:199`; Hilt só "decora" o global (`feature/ai-agent/.../di/AgentModule.kt:42`, sem DI real) | É o chokepoint de maior risco de segurança do app (tema central de `sdd-017`) dependendo de estado global populado implicitamente. Testes precisam chamar `AgentRegistry.clear()` manualmente (`core/ai/src/test/.../AgentRegistryTest.kt:26,66`) — sintoma de estado global escondido, com risco real de poluição entre testes/ordem de execução e de qualquer módulo que instancie um `AgentActionUseCase` "sem querer" registrar uma tool globalmente. |
| **A-02** | **P1** | `ProductSaveUseCase` tem duas regras de negócio divergentes para a **mesma ação** ("salvar produto"), dependendo de quem chama: os `invoke()` usados pela UI (linhas 35-56) nunca preferem `session.userId` sobre `AuthService` nem checam duplicado; só `execute()` (linhas 58-100, caminho do agente de IA) aplica as duas regras — "sessão tem prioridade sobre AuthService" (linha 78) e checagem informativa de duplicado (linha 83). Confirmado por leitura direta do arquivo. | `feature/products/.../domain/usecase/ProductSaveUseCase.kt:35-56` (UI) vs. `:58-100` (agente) | Mesma classe de bug já relatada em `specs/sdd-023-auditoria-logica-producao/tasks.md` — regra de negócio que só existe em um caminho de entrada. Qualquer novo `AgentActionUseCase` que replique esse padrão (dois `invoke()` "crus" + um `execute()` "completo") herda o mesmo risco. **Não auditado**: se o mesmo padrão se repete nos ~14 outros `AgentActionUseCase` do projeto (ver pergunta em aberto 9.1). |
| **A-05** | P3 | `feature:subscription` não usa `internal` em nenhum arquivo (`ManageSubscriptionViewModel.kt`, `PaywallViewModel.kt`, `*UiState.kt`, `*Intent.kt`, `*Screen.kt`), em desacordo com AGENTS.md §5 ("internal por padrão"). Comparar com `feature:auth` (disciplina correta) e `feature:profile`, que seguem AD-005. `feature:products` também tem paridade quase 1:1 público/internal sem justificativa de contrato entre módulos. | `feature/subscription/.../ManageSubscriptionViewModel.kt:29`, `PaywallViewModel.kt:21`, demais arquivos do módulo; comparar `feature/auth` (~69 `internal` vs. 3 públicos) | Nenhum desses símbolos é consumido fora do módulo — é superfície pública desnecessária, mais fácil de acoplar por acidente no futuro (exatamente o trade-off que a própria AD-013 já reconheceu como risco ao alargar a visibilidade de `feature:settings`). |

**Achado positivo:** a anatomia reduzida (sem `domain/`/`data/`) de `feature:cart`, `feature:profile` e
`feature:subscription` **não** é uma violação da Dependency Rule — todas consomem apenas interfaces de
`core:domain`, coerente com o papel de "shell de composição" de cada uma. Vale só uma nota: isso diverge
da "Anatomia de uma feature" documentada em `architecture/01-visao-geral-arquitetura.md` §2, e nenhuma AD
formaliza essa divergência como padrão aceito — baixo risco, mas candidato a um parágrafo em `architecture/01`.

## 5. Achados — Design Patterns

### 5.1 Já aplicados corretamente (não tocar sem necessidade real)

| Padrão | Evidência | Nota |
|---|---|---|
| **Command** | `AgentAction.execute()` (`core/ai/.../contract/AgentAction.kt:8-25`) + `AgentActionUseCase` base (`core/ai/.../base/AgentActionUseCase.kt:15-41`) | Toda tool de IA é um Command; dispatch é busca em mapa (`registry.find`), não `if/when` gigante. |
| **Factory Method** | `AgentFactory`/`AgentFactoryImpl` (`feature/ai-agent/.../AgentFactoryImpl.kt:25-113`) | Isola resolução de API key/modelo/feature flag da apresentação. |
| **Strategy** | Interface `Agent` com 3 implementações intercambiáveis (`GeminiAgent`, `OpenRouterAgent`, `NoProviderAvailableAgent`) escolhidas em `AgentFactoryImpl.selectAgent()` (linhas 100-112) | Troca de provider sem `if/else` no ponto de consumo. |
| **Null Object** | `NoProviderAvailableAgent` (`AgentFactoryImpl.kt:110`) | Evita checagem de nulo em todo consumidor de `Agent` quando nenhum provider está disponível. |
| **Decorator** | `FallbackAgent` (`feature/ai-agent/.../FallbackAgent.kt:13-37`) envolve o par primário/secundário com retry via `Flow.catch` | Resolveu um bug real documentado (`sdd-006`/B-08) justamente por estar isolado do resto. |
| **Adapter/Facade** | `NetworkService` sobre Firestore (`ShoppingRepositoryImpl.kt:52-100`); `PlayBillingSubscriptionRepository` sobre `BillingClient` (AD-010) | Único ponto de cada módulo que conhece o tipo da infraestrutura externa — decisão já registrada em AD-010. |
| **Observer** | `BillingLifecycleObserver` sobre `ProcessLifecycleOwner`; MVI inteiro via `StateFlow`/`collectAsStateWithLifecycle` (AD-004) | — |
| **Repository + mapeamento consistente** | `ShoppingMapper.kt:6-40`, `ProductMapper.kt:8-28` — todas as features seguem `toDomain()`/`toModel()` como extension functions | Sem duplicação de padrão entre features; não precisa de ação. |

### 5.2 Aplicados de forma duplicada — candidatos a consolidar

| ID | Sev. | Achado | Evidência | Ganho / Esforço |
|---|---|---|---|---|
| **A-03** | P2 | `GeminiAgent.executeFunctionCall` e `OpenRouterAgent.executeToolCall` duplicam quase byte a byte: gate Pro (`isPro()`), dispatch via `registry.find`, `try/catch` com `crashReporter.recordException`, log e formatação de erro. Confirmado por leitura direta: `isPro()` é **idêntica** nas duas classes. Ambas as classes estão **excluídas do gate de cobertura do Kover** justamente por isso — o cliente do SDK (`GenerativeModel`/`HttpClient`) é construído inline em vez de injetado, e o comentário no próprio arquivo de config já reconhece a dívida. | `GeminiAgent.kt:147-196` vs. `OpenRouterAgent.kt:198-235`; exclusão em `buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:106-112` | **Proposta:** extrair um `BaseAiAgent` (Template Method) com o gate/dispatch/erro comuns, injetar os clientes SDK via Hilt. Esforço: médio. Ganho: elimina duplicação **e** desbloqueia a cobertura hoje excluída do Kover — os dois problemas têm a mesma causa raiz. |
| **A-04** | P2 | `settings.aiProvider` é `String` literal (`"gemini"`/`"openrouter"`) em pelo menos 3 arquivos, enquanto `AiConfigTier` (mesmo domínio) já é um `enum` type-safe. | `core/domain/.../model/AppSettings.kt`; `core/data/.../SettingsRepositoryImpl.kt`; `feature/ai-agent/.../AgentFactoryImpl.kt:38,62,89,101-104` | **Proposta:** introduzir `enum AiProvider`. Esforço: baixo. Ganho: elimina comparação por string (`"gemini"` espalhado por 4+ `if`), evita typo silencioso. **Deve ser resolvido antes de `sdd-014`** (BYOK, 3º+ provider) para não replicar o padrão string-typed com mais um provider. |
| — | baixa | `CartSubscriptionGateViewModel`/`ShoppingSubscriptionGateViewModel` são idênticos (mesma lógica `map { it == SubscriptionStatus.PRO }.stateIn(...)`). A duplicação é **intencional e já documentada em comentário**: `:wear` compila a classe-mãe mas não depende de `core:billing`, então uma `BaseViewModel` compartilhada arriscaria o grafo Hilt do Wear. | `feature/cart/.../CartSubscriptionGateViewModel.kt:22-34`; `feature/shopping/.../ShoppingSubscriptionGateViewModel.kt:21-33` | **Proposta leve:** extrair só a *lógica* (não o `ViewModel`) como extension function em `core:billing` (ex.: `SubscriptionRepository.observeIsPro(scope, timeoutMs)`), que nem `:wear` nem a classe-base precisam importar. Esforço: baixo. Não é bloqueante — é polimento, incluir no mesmo SDD que tocar essa área (ex. `sdd-018-firestore-resilience` toca `*RepositoryImpl.observe*` com padrão de `catch`/degrade parecido; avaliar se faz sentido juntar). |

### 5.3 Ausentes recomendados

Nenhum padrão GoF novo e crítico ficou de fora dos pontos de maior risco do projeto — Command/Strategy/
Factory/Decorator/Facade/Observer já cobrem exatamente os lugares que mais precisariam (dispatch de IA,
billing, resiliência). Não há `if/else`/`when` gigante de despacho por tipo fora do já resolvido via
`AgentRegistry`, nem construtor com excesso de parâmetros opcionais que justifique um `Builder` dedicado
(`AgentDependencies` já atua como parameter object). A introdução recomendada é a consolidação via
**Template Method** (A-03) — não um padrão novo na arquitetura, mas a correção de uma Strategy já boa que
hoje tem a lógica de dispatch duplicada em vez de compartilhada.

## 6. Achados — Guard-rails Automatizados

| ID | Tipo | Achado | Evidência |
|---|---|---|---|
| **Positivo** | guard-rail existente | O projeto **já tem** um teste Konsist único (`ArchitectureTest.kt`, roda em `:app` porque Konsist lê sources, não classpath) cobrindo: camadas permitidas por `feature`/`core`, `domain` não depende de `data`/`presentation` nem de Android/Compose/Firebase (via import **e** nome totalmente qualificado), `feature` não depende de outro `feature` exceto import `.navigation.` (com lista de exceções conhecidas, incluindo a AD-013), e convenção sufixo→pacote (`UseCase`→`domain.usecase`, `ViewModel`→`presentation.viewmodel` etc.). Cada regra tem uma lista de violações conhecidas que só pode **encolher**, nunca crescer. | `app/src/test/java/br/com/brunocarvalhs/howmuch/architecture/ArchitectureTest.kt:1-188` |
| **A-09** | guard-rail ausente | Nenhum guard-rail automatizado cobre: (a) service locators/estado global fora de DI (não pegaria A-01), (b) divergência semântica de regra de negócio entre `invoke()`/`execute()` do mesmo use case (não pegaria A-02), (c) disciplina de visibilidade `internal` por padrão de AD-005 (não pegaria A-05), (d) dependências Gradle mortas (não pegaria A-07 — é nível de build, não de import Kotlin). | `app/src/test/.../ArchitectureTest.kt` (âmbito atual) |

## 7. Roadmap de SDDs Derivados

```
┌──────────────────────────────────────────────────────────────────────────┐
│ SDD-026: AI Provider Consolidation (A-03, A-04)                          │
│  Template Method (BaseAiAgent) + enum AiProvider. Roda ANTES de sdd-014  │
│  (BYOK three-tier) para não replicar a duplicação com um 3º provider.    │
└───────────────────────────────────┬──────────────────────────────────────┘
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ SDD-027: AgentRegistry → DI real (A-01)                                  │
│  Substitui o object/init-registration por registro explícito via Hilt    │
│  Multibinding (Set<AgentAction<*>>, padrão já usado em outro lugar do    │
│  projeto). Maior risco/esforço — sequenciar depois que sdd-017 (branch   │
│  em andamento) estabilizar, por tocar o mesmo chokepoint de dispatch.    │
└───────────────────────────────────┬──────────────────────────────────────┘
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ SDD-028: Paridade de Regra de Negócio nos AgentActionUseCase (A-02)       │
│  Corrige ProductSaveUseCase e audita os ~14 demais AgentActionUseCase    │
│  quanto ao mesmo padrão invoke()-vs-execute() divergente.                │
└───────────────────────────────────┬──────────────────────────────────────┘
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ SDD-029: Limpeza de Visibilidade e Dependências (A-05, A-06, A-07)        │
│  Aplica `internal` em feature:subscription/products; abre AD formalizando│
│  cart→products/shopping (espelhando AD-013); remove dependência Gradle   │
│  morta shopping→settings. Baixo risco, bom para um PR único em lote.     │
└──────────────────────────────────────────────────────────────────────────┘
```

Este roadmap deve ser inserido em `specs/tasks.txt` junto do roadmap já existente (`sdd-016` a `sdd-020`),
preservando a ordem de severidade já registrada lá — `sdd-026`/`sdd-027` (P1/P2 reais) competem por
prioridade com `sdd-018`/`sdd-019` (bugs de lógica em produção já confirmados); recomenda-se não adiantar
este roadmap de arquitetura na frente de bugs de produção P0/P1 já mapeados em `sdd-001`/`sdd-023`, exceto
`sdd-026` por ser pré-requisito direto de `sdd-014`.

## 8. Contratos e Interfaces Afetados

Nenhum contrato público é alterado diretamente por este documento de auditoria. Cada SDD derivado (026 a
029) definirá seus próprios contratos (ex.: `SDD-026` provavelmente introduz `enum AiProvider` em
`core:domain`, público por ser consumido por `core:data`+`feature:ai-agent`; `SDD-027` pode expor um novo
binding Hilt `Set<AgentAction<*>>` em `core:ai`).

## 9. Perguntas em Aberto (não bloqueantes para esta auditoria)

- [ ] **9.1** — O padrão de A-02 (regra de negócio só no `execute()`, ausente nos `invoke()` da UI) se repete
  em quais dos ~14 outros `AgentActionUseCase` do projeto? Não bloqueia esta auditoria (que é diagnóstico),
  mas deve ser o primeiro passo de `SDD-028`.
- [ ] **9.2** — A divergência de anatomia de feature (`cart`/`profile`/`subscription` sem `domain`/`data`)
  deveria ganhar uma AD formal em `architecture/05`, ou é aceitável deixar implícito? Não bloqueia (baixo
  risco), mas afeta a redação de `architecture/01-visao-geral-arquitetura.md` §2.

Nenhum item acima é bloqueante — `status: ready`.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` e `architecture/`/AGENTS.md quando existente.
- [x] Nenhum código de produção foi escrito ou alterado por este plano (achados A-01 a A-09 são apenas
  diagnóstico; a implementação fica para os SDDs 026-029).
- [x] Seção 7 (roadmap) preenchida com sequenciamento e dependência explícita entre SDDs.
- [x] Nenhuma mudança de contrato público proposta diretamente aqui.
- [x] Perguntas em aberto não são bloqueantes.
