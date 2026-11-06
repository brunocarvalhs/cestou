# Task: Onboarding de Perfilamento e Geração Mágica da Lista

- **Slug:** smart-onboarding-profiling
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03 (Refinado)
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- O Cestou atualmente joga o usuário recém-chegado diretamente em uma tela de lista em branco. Para gerar valor imediato (foco em economia e organização), implementamos um onboarding interativo na primeira abertura que pergunta o tamanho da família, orçamento desejado e restrições alimentares (ex: vegano, intolerante à lactose), gerando automaticamente a primeira compra do mês.
- Referência ao pedido: Refinamento de Concorrentes - Item 3 (Onboarding de Perfilamento).
- Rastreabilidade: `app/.../FeatureInitializer.kt`, `feature/profile/`, `core/domain/`.

## 2. Escopo
- **Inclui:**
  - Questionário rápido de primeira abertura (tamanho da família, orçamento desejado, restrições alimentares).
  - Use case de geração automática da lista inicial (`GenerateInitialListUseCase`).
  - Persistência local do perfil do usuário (`Room`/`DataStore`) para futuras recomendações e IA.
- **Exclui:**
  - Comparador de preços por rede (escopo de sdd-039).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/profile`, `core:domain`, `core:data`, `core:navigation`.
- Padrão aplicado: Clean Architecture + MVI, Nav3.
- Novos arquivos:
  - `feature/profile/src/main/java/br/com/brunocarvalhs/howmuch/feature/profile/presentation/OnboardingViewModel.kt`
  - `feature/profile/src/main/java/br/com/brunocarvalhs/howmuch/feature/profile/presentation/OnboardingScreen.kt`
  - `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/usecase/GenerateInitialListUseCase.kt`

## 4. Contratos e Interfaces
- `public data class UserProfile(...)` e interface de repositório em `core:domain`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Primeira abertura exibe lista vazia | Primeira abertura exibe quiz de perfil e gera lista inicial personalizada | `app/.../FeatureInitializer.kt` |

## 6. Critérios de Aceitação
- Performance: Geração assíncrona da lista em background thread sem bloquear a UI.
- Feature toggle: `feat_smart_onboarding` (default: true).
- Observabilidade: Evento `onboarding_completed`.

## 7. Plano de Testes
- **Unitários:** Testar `GenerateInitialListUseCase` com restrições e orçamentos variados.
- **Instrumentados / UI:** Teste de navegação do quiz com `testTag`.
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Usuário querer pular o quiz | Média | Baixa | Permitir opção de "Pular" gerando uma lista padrão básica |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
