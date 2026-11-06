# Task: Onboarding Inteligente e Perfilamento (Foco em Economia)

- **Slug:** smart-onboarding-profiling
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Criação de uma experiência de onboarding interativa (quiz de 5 passos) que coleta perfil do usuário (família, restrições alimentares, preferências, tamanho da casa, duração da compra, orçamento alvo) e gera automaticamente uma lista de compras inicial categorizada.
- Referência ao pedido: Requisitos de Concorrentes - Item 1.
- Rastreabilidade: `app/src/main/java/br/com/brunocarvalhs/howmuch/app/FeatureInitializer.kt` e `feature/profile/`.

## 2. Escopo
- **Inclui:**
  - Tela de quiz de 5 passos em `feature/profile` ou `feature/onboarding` com Jetpack Compose.
  - Modelos de domínio para Perfil do Usuário e preferências alimentares.
  - Use case para geração automática da lista inicial baseada nas respostas do quiz.
  - Integração com persistência local (Room/DataStore) e Firestore (se logado).
- **Exclui:**
  - Comparador de preços por rede de supermercados (escopo de sdd-039).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/profile`, `core:domain`, `core:data`, `core:navigation`.
- Padrão aplicado: Clean Architecture + MVI, Nav3 (`NavKey`).
- Novos arquivos:
  - `feature/profile/src/main/java/br/com/brunocarvalhs/howmuch/feature/profile/presentation/OnboardingViewModel.kt`
  - `feature/profile/src/main/java/br/com/brunocarvalhs/howmuch/feature/profile/presentation/OnboardingScreen.kt`
  - `feature/profile/src/main/java/br/com/brunocarvalhs/howmuch/feature/profile/domain/GenerateInitialListUseCase.kt`
- Diagrama ASCII:
  ```
  [OnboardingScreen] ---> [OnboardingViewModel] ---> [GenerateInitialListUseCase]
                                                                |
                                                      [ShoppingRepository] ---> [Room/Firestore]
  ```
- Injeção via Hilt em todo o fluxo.

## 4. Contratos e Interfaces
- Novos modelos em `core:domain`:
  ```kotlin
  public data class UserProfile(
      public val familySize: FamilySize,
      public val dietaryRestrictions: List<DietaryRestriction>,
      public val preferences: List<String>,
      public val homeSize: HomeSize,
      public val purchaseDuration: PurchaseDuration,
      public val targetBudget: Double
  )
  ```
- Interface de repositório e UseCase marcados como `public` com justificativa de contrato cross-module.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | App inicia sem lista pré-preenchida | App executa quiz no primeiro acesso e gera lista inicial categorizada | `app/.../FeatureInitializer.kt` |

## 6. Critérios de Aceitação
- Performance: Sem recomposições excessivas no quiz; cálculo da lista inicial assíncrono em Dispatchers.IO.
- Feature toggle: `feat_smart_onboarding` (default: true).
- Observabilidade: Evento de analytics `onboarding_completed`.
- Compatibilidade: minSdk 28, suporte a temas Material 3.

## 7. Plano de Testes
- **Unitários:** Testar `GenerateInitialListUseCase` com diferentes combinações de perfil e restrições alimentares. Testar `OnboardingViewModel` states.
- **Instrumentados / UI:** Testes de fluxo do quiz com `testTag`.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify`.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Lista inicial muito genérica | Média | Baixa | Permitir customização imediata pelo usuário |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Novo cache/armazenamento justificado.
- [ ] Perguntas em aberto exauridas.
