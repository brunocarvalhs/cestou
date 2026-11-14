# Task: Fluidez entre Telas, Transições e Polimento de Layout (Compose & Navigation 3)

- **Slug:** fluidity-transitions-layout
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** refactor / feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Garantir fluidez visual extrema e experiência nativa polida em todo o Cestou através de transições de navegação suaves (Navigation 3), suporte completo a edge-to-edge (insets de barra de status e navegação), animações de gesto preditivo (Predictive Back / NavigationEvent), otimização de performance de rolagem em listas (LazyColumn), e espaçamentos/touch targets consistentes em Material 3.
- Referência ao pedido: Solicitação do usuário por SDD de fluidez, layout e transições.
- Rastreabilidade: `core/navigation/`, `core/ui/`, `core/theme/`, e módulos `feature:*`.

## 2. Escopo
- **Inclui:**
  - Configuração de transições fluidas de entrada e saída (Slide + Fade) entre destinos no Nav3 (`core:navigation`).
  - Padronização de Edge-to-edge em todas as telas com tratamento correto de `WindowInsets` (`core:ui`).
  - Suporte a animações de Predictive Back (`androidx.navigationevent` / `BackHandler`).
  - Otimização de performance de renderização em listas (`LazyColumn` com chaves estáveis e prevenção de recomposição desnecessária).
  - Verificação de touch targets mínimos (48dp) e feedback visual (ripple effects) em componentes interativos.
- **Exclui:**
  - Alterações nas regras de negócio de backend ou repositórios de dados.

## 3. Impacto Arquitetural
- Módulos afetados: `core:navigation`, `core:ui`, `core:theme`, `feature:*`.
- Padrão aplicado: Clean Architecture + Jetpack Compose M3 + Navigation 3 Scenes.
- Novos arquivos:
  - `core/navigation/src/main/java/br/com/brunocarvalhs/howmuch/core/navigation/TransitionExtensions.kt`
  - `core/ui/src/main/java/br/com/brunocarvalhs/howmuch/core/ui/modifiers/EdgeToEdgeModifiers.kt`
- Diagrama ASCII:
  ```
  [Nav3 Graph] ---> [TransitionExtensions] ---> [Slide/Fade Animations]
  [Screens]    ---> [EdgeToEdgeModifiers]   ---> [WindowInsets Handling]
  ```
- Injeção via Hilt onde aplicável.

## 4. Contratos e Interfaces
- Extensões de transição públicas e utilitários de insets em `core:navigation` e `core:ui`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Transições de tela padrão e tratamento de insets inconsistente entre features | Transições suaves fluidas, edge-to-edge padronizado e Predictive Back habilitado | `core/navigation/...` e `core/ui/...` |

## 6. Critérios de Aceitação
- Performance: Taxa de quadros estável (60/120fps) sem quedas de frame durante navegação e scroll em `LazyColumn`.
- Feature toggle: `feat_fluid_transitions` (default: true).
- Observabilidade: Métricas de performance de renderização via Jetpack Macrobenchmark / Perfetto.
- Compatibilidade: minSdk 28+, suporte a gestos do sistema Android 15/16.

## 7. Plano de Testes
- **Unitários:** Testes de utilitários de transição e cálculo de insets.
- **Instrumentados / UI:** Testes de transição de navegação e verificação de limites de layout (`testTag`).
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify`.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Conflito de insets em telas com teclado virtual (IME) | Média | Média | Uso adequado de `imePadding()` e `navigationBarsPadding()` |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
