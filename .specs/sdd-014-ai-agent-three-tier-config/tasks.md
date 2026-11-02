# Task: Layout de Configuração de Agente de IA em 3 Níveis (Com Gating Pro para Níveis 2 e 3)

- **Slug:** sdd-014-ai-agent-three-tier-config
- **Autor del plano:** Code-Planner (SDD)
- **Data:** 2025-03-09
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- O app Cestou precisa de um layout refinado e intuitivo para a configuração de agentes de IA na tela de preferências (`AiSettingsScreen`), estruturado em 3 níveis com regras de acesso por assinatura (`SubscriptionRepository`):
  1. **Nível 1 (Valor Default):** Gratuito e padrão para todos os usuários (`openrouter/free`). Disponível out-of-the-box.
  2. **Nível 2 (Seleção de Agente Específico):** Exclusivo para assinantes (`SubscriptionStatus.PRO`). Permite escolher modelos built-in/gratuitos específicos.
  3. **Nível 3 (Agentes Pagos com API Key Externa / BYOK):** Exclusivo para assinantes (`SubscriptionStatus.PRO`). Permite ao usuário incluir chaves de API externas (ex.: Claude, GPT-4o) para utilizar modelos pagos com pagamento externo.
- Rastreabilidade: `core/domain/model/AppSettings.kt`, `core/data/repository/SettingsRepositoryImpl.kt`, `feature/settings/presentation/screen/AiSettingsScreen.kt`, `core/domain/repository/SubscriptionRepository.kt`.

## 2. Escopo
- **Inclui:**
  - Atualização do modelo de domínio `AppSettings` (`core/domain/model/AppSettings.kt`) para suportar o tier de configuração (`aiConfigTier`), chave de API customizada (`customApiKey`) e modelo selecionado.
  - Atualização do `SettingsRepository` para persistir e recuperar o tier e a API key.
  - Injeção de `SubscriptionRepository` no `AiSettingsViewModel` para verificar se o usuário é `PRO` antes de permitir a seleção do Nível 2 ou Nível 3.
  - Redesenho da `AiSettingsScreen` (`feature/settings/presentation/screen/AiSettingsScreen.kt`) com abas/cartões claros para os 3 níveis, exibindo banner ou redirecionando para o Paywall caso um usuário `FREE` tente acessar os Níveis 2 ou 3.
  - Atualização da fábrica de agentes `AiAgentFactoryImpl` para respeitar a chave customizada de BYOK quando o Nível 3 estiver ativo.
  - Testes unitários para o ViewModel (`AiSettingsViewModelTest`), Repositório e Gating de Assinatura.
- **Exclui:**
  - Processamento de pagamentos de IA in-app (o Nível 3 é BYOK / chave externa paga por fora; a assinatura Pro do app é gerenciada por `core:billing` e `feature:subscription`).

## 3. Impacto Arquitetural
- Módulos afetados: `core/domain`, `core/data`, `feature/settings`, `feature/ai-agent`.
- Padrão aplicado: Clean Architecture + MVI, Nav3, Gating reativo com `SubscriptionRepository`.
- Novos arquivos:
  - `core/domain/model/AiConfigTier.kt` (`enum class AiConfigTier { DEFAULT, SPECIFIC, CUSTOM_BYOK }`)
- Diagrama ASCII de componentes e fluxo:
  ```
  [AiSettingsScreen] ---> [AiSettingsIntent] ---> [AiSettingsViewModel]
                                                           |
                                      +--------------------+--------------------+
                                      |                                         |
                            [SubscriptionRepository]                 [SettingsRepository]
                            (Verifica se é PRO para                   (Persiste Nível, Modelo
                             Níveis 2 e 3)                             e customApiKey)
  ```

## 4. Contratos e Interfaces
- **`AppSettings` (`core/domain/model/AppSettings.kt`)**: Adição de `val aiConfigTier: AiConfigTier = AiConfigTier.DEFAULT`, `val customApiKey: String? = null`.
- **`AiConfigTier` (`core/domain/model/AiConfigTier.kt`)**: `enum class AiConfigTier { DEFAULT, SPECIFIC, CUSTOM_BYOK }`.
- **`SettingsRepository` (`core/domain/repository/SettingsRepository.kt`)**: Suporte a gravação e leitura do tier de IA e da chave customizada.
- **Gating de Assinatura**: Uso de `SubscriptionRepository#observeStatus()` para bloquear interativamente Níveis 2 e 3 para usuários `FREE` e exibir o paywall.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Seleção livre de modelos na lista | Nível 1 (Default) é livre; Níveis 2 (Específico) e 3 (BYOK) exigem assinatura `SubscriptionStatus.PRO` | `AiSettingsScreen.kt:30` |
| 2 | Chaves fixas via `BuildConfig` | Suporte a chave customizada no Nível 3 para assinantes Pro | `AiAgentFactoryImpl.kt:37` |

## 6. Critérios de Aceitação
- **Performance:** Recomposição otimizada, sem I/O síncrono na Main Thread, observação reativa de status de assinatura e settings.
- **Gating e Paywall:** Usuários `FREE` ao tocarem no Nível 2 ou Nível 3 visualizam o indicador Pro e podem abrir o fluxo de assinatura (`feature:subscription`).
- **Compatibilidade:** minSdk 28, Wear OS, migração retrocompatível para configurações existentes.

## 7. Plano de Testes
- **Unitários:**
  - `AiSettingsViewModelTest`: testa alternância de tiers, gating para usuários `FREE` vs `PRO`, e salvamento de chave customizada.
  - `SettingsRepositoryImplTest`: testa persistência no DataStore.
- **Instrumentados / UI:** Testes de UI na `AiSettingsScreen` validando bloqueio dos níveis 2 e 3 para plano Free.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Usuário Free tentar configurar modelo pago sem Pro | Baixa | Média | Bloqueio visual imediato na UI com redirecionamento para o Paywall do app (`core:navigation` / `feature:subscription`). |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma pergunta bloqueante.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive` em "Impacto público".
- [ ] Perguntas em aberto exauridas.
