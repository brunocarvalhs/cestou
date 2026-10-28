# 01 — Visão geral da arquitetura

> Fonte de verdade: o código. Este documento resume a estrutura e cita arquivos. Em caso de divergência, corrija este documento.

## 1. Módulos

Definidos em `settings.gradle.kts`:

| Módulo | Papel |
|---|---|
| `:app` | Entrypoint Android. `CestouApplication` (`@HiltAndroidApp`) registra os agentes de IA na inicialização. |
| `:core:common` | Utilitários transversais. |
| `:core:domain` | Modelos e contratos de domínio compartilhados (`Shopping`, `Product`, `SubscriptionStatus`, `User`...). |
| `:core:data` | Infraestrutura compartilhada (inclui `wearable/` para sincronização com o relógio e módulos de DI). |
| `:core:theme` / `:core:ui` | Tema Material 3 e componentes reutilizáveis. |
| `:core:navigation` | Rotas base (`NavRoutes.kt`, `mobile/MobileRoutes.kt`, `wear/WearRoutes.kt`). |
| `:core:auth` | Autenticação e sessão. |
| `:core:ai` | Contrato e base dos agentes de IA (`contract/AgentAction.kt`, `base/AgentActionUseCase.kt`). |
| `:core:remote-config` | Chaves de flags e variáveis remotas (`model/RemoteConfigKeys.kt`). |
| `:core:analytics` | Telemetria. |
| `:core:billing` | Google Play Billing. |
| `:feature:shopping`, `:feature:products`, `:feature:settings`, `:feature:auth`, `:feature:profile`, `:feature:chat`, `:feature:cart`, `:feature:ai-agent`, `:feature:subscription` | Features de negócio. |
| `:wear` | App Wear OS standalone (`CestouWearApplication`), com `applicationId` próprio. |

## 2. Anatomia de uma feature

Cada feature segue a separação por camadas. O diretório Gradle usa hífen, mas o pacote Kotlin de `ai-agent` é `ai_agent`:

```
feature/<nome>/src/main/java/br/com/brunocarvalhs/howmuch/feature/<nome>/
├── domain/        modelos, repositórios (interfaces), use cases
├── data/          implementações de repositório, DTOs, mappers, fontes de dados
├── presentation/  Composables, ViewModels, estado e intents
├── navigation/    rotas (NavKey) e grafo da feature
└── di/            módulos Hilt
```

Exemplos verificados:
- `feature/shopping/domain/usecase/` — `ShoppingCreateUseCase`, `ShoppingJoinUseCase`, `ShoppingDuplicateUseCase`, entre outros.
- `feature/shopping/presentation/viewmodel/ShoppingSubscriptionGateViewModel.kt` — ViewModel de gate de assinatura.
- `feature/settings/navigation/Settings.kt`, `feature/shopping/navigation/ShoppingRoutes.kt` — rotas tipadas por feature.

## 3. Padrões

- **Clean Architecture:** `domain` não depende de Android nem de `data`. As implementações ficam em `data` e são ligadas por Hilt.
- **MVI:** ViewModels expõem estado (`StateFlow`) e recebem intents. O estado é observado com `collectAsStateWithLifecycle` nos grafos (ex.: `feature/shopping/navigation/ShoppingGraph.kt`).
- **Navegação tipada:** rotas são objetos `NavKey`, agrupadas por feature e por plataforma (mobile e wear).
- **Injeção de dependência:** Hilt. Use cases de IA são coletados como `Set<AgentActionUseCase<*>>` no `Application`.
- **Use cases de IA:** operações de negócio anotadas com `@AiAgentAction` e `@AiAgentParameter` (ex.: `ShoppingCreateUseCase.kt`, `ShoppingUpdateUseCase.kt`). Isso permite ao agente chamá-las por nome. Um use case pode exigir assinatura PRO via `AgentAction.requiresPro`.

## 4. Fluxo típico de uma tela

```
Composable (presentation)
   │ intent
   ▼
ViewModel (presentation) ──► UseCase (domain) ──► Repository (interface, domain)
                                                      │
                                                      ▼
                                       RepositoryImpl (data) ──► Firestore / Remote Config / Billing
```

## 5. Entrypoints

- Mobile: `app/src/main/java/br/com/brunocarvalhs/howmuch/CestouApplication.kt`.
- Wear OS: `wear/src/main/java/br/com/brunocarvalhs/howmuch/wear/CestouWearApplication.kt`.

## 6. Divergências conhecidas

- `README.md` menciona um `FeatureInitializer` que orquestra as features. Esse nome **não existe** no código (busca em `*.kt` sem resultado). Verificar a inicialização real em `app/` antes de citá-la.
- Os módulos Gradle usam hífen (`:core:remote-config`), mas os pacotes Kotlin usam `remoteconfig` (`core/remote-config/src/main/java/.../core/remoteconfig`). Isto é intencional em Kotlin, mas vale lembrar ao procurar arquivos.
- `AGENTS.md` e as skills citavam `architecture/01-04` como inexistente; a partir deste documento eles passam a existir (ver `specs/sdd-000-setup-sdd/tasks.md`).
