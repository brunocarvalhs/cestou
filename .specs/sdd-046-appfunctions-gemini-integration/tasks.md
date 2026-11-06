# Task: Integração com Android AppFunctions (Gemini & System Assistant)

- **Slug:** appfunctions-gemini-integration
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Expor as principais funcionalidades do Cestou (como criar listas, adicionar itens, consultar totais e buscar produtos) como **Android AppFunctions** (`@AppFunctionDeclaration`), permitindo que assistentes de sistema baseados em IA (como o Gemini no Android 16+) descubram e executem ações diretamente no dispositivo, sem a necessidade de abrir a interface do app.
- Referência ao pedido: Integração com AppFunctions / Gemini assistant.
- Rastreabilidade: `core/ai/`, `feature/ai-agent/`, e novo módulo ou pacote `core/appfunctions/` / `feature/appfunctions/`.

## 2. Escopo
- **Inclui:**
  - Configuração de dependências do SDK AppFunctions (com `compileSdk 37`, `targetSdk 36+`).
  - Definição de funções annotadas com `@AppFunctionDeclaration` para fluxos principais: `AddShoppingItemFunction`, `GetShoppingListSummaryFunction`, `CreateShoppingListFunction`.
  - KDoc altamente otimizado para que o modelo de IA compreenda os parâmetros, tipos e comportamentos das tools.
  - Testes instrumentados e verificação de registro via ADB (`adb shell appfunctions ...`).
- **Exclui:**
  - Alterações no chat interno (já coberto em `feature/chat`).

## 3. Impacto Arquitetural
- Módulos afetados: `core:domain`, `core:data`, e novo módulo ou subpacote `core/appfunctions` ou `feature/ai-agent`.
- Padrão aplicado: Clean Architecture + AppFunctions Jetpack Library.
- Novos arquivos:
  - `core/appfunctions/src/main/java/br/com/brunocarvalhs/howmuch/core/appfunctions/CestouAppFunctionsService.kt`
  - `core/appfunctions/src/main/java/br/com/brunocarvalhs/howmuch/core/appfunctions/functions/AddShoppingItemAppFunction.kt`
- Diagrama ASCII:
  ```
  [Android System / Gemini] ---> [AppFunctionDiscovery] ---> [CestouAppFunctionsService]
                                                                        |
                                                          [UseCases / ShoppingRepository]
  ```
- Injeção via Hilt para prover UseCases aos AppFunctions.

## 4. Contratos e Interfaces
- Declarações públicas de AppFunctions em `core/appfunctions` com documentação KDoc estruturada para descoberta por agentes de sistema.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Ações do Cestou executáveis apenas via UI do app | Ações expostas via AppFunctions executáveis por voz ou Gemini sem abrir a UI | Arquitetura de Assistentes Android |

## 6. Critérios de Aceitação
- Performance: Execução rápida em background thread (<200ms) sem impacto na bateria ou UI.
- Feature toggle: `feat_app_functions` (default: true).
- Observabilidade: Eventos de analytics para cada AppFunction invocada com sucesso ou erro.
- Compatibilidade: Android 16+ (API 36+) com suporte Jetpack Backports.

## 7. Plano de Testes
- **Unitários:** Testar a lógica de execução de cada AppFunction isolando os use cases de domínio.
- **Instrumentados / ADB:** Validação de descoberta e execução via ADB (`adb shell ...`).
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify`.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Permissão de execução ou contexto indisponível | Baixa | Alta | Validação de sessão e tratamento de exceções seguras |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
