# Task: Comparador de Preços por Redes de Supermercado

- **Slug:** supermarket-price-comparator
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Permitir que o usuário informe seu Estado e Cidade para calcular e comparar quanto sua lista de compras atual custaria em diferentes redes locais de supermercados (ex: Assaí, Atacadão, Dia, Guanabara), exibindo um ranking do mais barato para o mais caro e a economia estimada em Reais.
- Referência ao pedido: Requisitos de Concorrentes - Item 2.
- Rastreabilidade: `core/data/` e `feature/shopping/`.

## 2. Escopo
- **Inclui:**
  - Tela/bottom sheet de seleção de localização (Estado e Cidade).
  - Algoritmo de cálculo comparativo de preços baseado em índices regionais de redes de supermercado.
  - Exibição de ranking (mais barato para mais caro) com destaque para a economia estimada.
- **Exclui:**
  - Onboarding inicial (coberto por sdd-038).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/shopping`, `core:domain`, `core:data`, `core:ui`.
- Padrão aplicado: Clean Architecture + MVI, Nav3.
- Novos arquivos:
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/PriceComparatorViewModel.kt`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/PriceComparatorScreen.kt`
  - `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/model/SupermarketPriceComparison.kt`

## 4. Contratos e Interfaces
- Modelos e Use Cases públicos em `core:domain`.
- Integração com repositório de preços locais (`SupermarketRepository`).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Sem comparação de preços por rede | Cálculo dinâmico baseado na lista ativa e localização do usuário | `feature/shopping/...` |

## 6. Critérios de Aceitação
- Performance: Cálculo executado em background thread com cache local de preços por praça.
- Feature toggle: `feat_price_comparator` (default: true).
- Observabilidade: Evento `price_comparison_executed`.

## 7. Plano de Testes
- **Unitários:** Validação do cálculo de ranking e diferença de economia.
- **Instrumentados / UI:** Teste de UI da tela de comparação com `testTag`.
- **Lint/Detekt/Cobertura:** Validação via Gradle tasks.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Dados de preços desatualizados por região | Alta | Média | Fornecer estimativas baseadas em percentuais médios de redes atacadistas vs varejistas |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
