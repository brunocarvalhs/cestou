# Task: Monetização, Relatórios de Despesas e Personalização Pro

- **Slug:** monetization-personalization-pro
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Consolidar recursos Pro/Premium: backup automático em nuvem e sincronização entre dispositivos, relatórios de despesas (gráficos por produtos/seções), temas de interface customizáveis (Padrão, Oceano, Safira, Lavanda), ajuste de tamanho de fonte, remoção de anúncios e assistente de IA ilimitado.
- Referência ao pedido: Requisitos de Concorrentes - Item 7.
- Rastreabilidade: `feature/subscription/`, `feature/settings`, `core/billing/`, `core/theme/`.

## 2. Escopo
- **Inclui:**
  - Fluxos de assinatura mensal/anual integrados ao Google Play Billing.
  - Relatórios de despesas com gráficos analíticos.
  - Seletor de temas de interface (Padrão, Oceano, Safira, Lavanda) e ajuste de tamanho de fonte nas configurações.
  - Sincronização em nuvem e privilégios Pro (sem anúncios, IA ilimitada).
- **Exclui:**
  - Funcionalidades básicas de compras (já cobertas em módulos anteriores).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/subscription`, `feature/settings`, `core:billing`, `core:theme`.
- Padrão aplicado: Clean Architecture + MVI, Nav3.
- Novos arquivos:
  - `feature/subscription/src/main/java/br/com/brunocarvalhs/howmuch/feature/subscription/presentation/ExpenseReportsScreen.kt`
  - `core/theme/src/main/java/br/com/brunocarvalhs/howmuch/core/theme/ThemePalettes.kt`

## 4. Contratos e Interfaces
- Contratos de verificação de status Pro e preferências de tema em `core:domain` e `core:theme`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Sem relatórios detalhados nem temas customizáveis avançados | Relatórios gráficos de despesas, múltiplos temas de cor e assinaturas Pro completas | `core/billing/...` |

## 6. Critérios de Aceitação
- Performance: Renderização fluida de gráficos de despesas usando Compose Canvas/Graphics.
- Feature toggle: `feat_pro_features` (default: true).
- Observabilidade: Evento `subscription_purchased`, `theme_changed`.

## 7. Plano de Testes
- **Unitários:** Testes de lógica de verificação de status Pro e agregação de dados para gráficos de despesas.
- **Instrumentados / UI:** Testes de mudança de temas e navegação nos relatórios.
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Falha na validação de recibo do Google Play Billing | Baixa | Alta | Uso robusto de `core:billing` e cache de estado de assinatura |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
