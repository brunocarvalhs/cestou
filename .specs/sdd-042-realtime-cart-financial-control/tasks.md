# Task: Aprimoramento do Modo Carrinho (Total vs. Carrinho)

- **Slug:** realtime-cart-financial-control
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03 (Refinado)
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Ajudar o usuário no supermercado com agilidade e controle financeiro em tempo real, separando visualmente na barra inferior o valor planejado de toda a lista do valor acumulado no carrinho (apenas os itens já marcados/riscados como pegos), permitindo saber o gasto exato antes de chegar ao caixa.
- Referência ao pedido: Refinamento de Concorrentes - Item 5 (Modo Carrinho).
- Rastreabilidade: `feature/cart/`, `feature/shopping/`, `core/ui/`.

## 2. Escopo
- **Inclui:**
  - Barra inferior fixa exibindo métricas em tempo real: "Total Planejado" vs. "Total no Carrinho".
  - Comportamento de risco de item ao marcar checkbox e movimentação automática para seção de carrinho.
  - Edição rápida de item (quantidade, unidade, preço, categoria).
- **Exclui:**
  - Relatórios avançados de despesas (escopo de sdd-044).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/cart`, `feature/shopping`, `core:domain`, `core:ui`.
- Padrão aplicado: Clean Architecture + MVI, Nav3.
- Novos arquivos:
  - `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/components/CartBottomBar.kt` (já existia; ganha a linha de totais)
  - `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/helper/CartTotals.kt` (totais derivados dos produtos)
- Edição rápida: **não** cria `QuickEditItemBottomSheet`; o sheet existente (`EditItemRoute`/`EditItemContent`, em `feature/cart`) já edita nome, categoria, preço e quantidade. `Product` não tem campo de unidade.
- `core:ui`: `CestouCategoryHeader` ganha o parâmetro opcional `title` (aditivo) para a seção "No carrinho".

## 4. Contratos e Interfaces
- Métricas de cálculo de carrinho expostas no estado do ViewModel em `feature/cart`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Exibição de total único sem distinção do que já foi colocado no carrinho | Barra inferior com cálculo em tempo real separando Total Planejado e Carrinho | `feature/cart/...` |

## 6. Critérios de Aceitação
- Performance: Atualização instantânea dos valores ao marcar/desmarcar itens sem jank.
- Feature toggle: `feat_realtime_cart` (default: true).
- Observabilidade: Evento `cart_item_checked`, `cart_total_calculated` (só quando os totais mudam).
  `cart_item_checked` é emitido junto de `product_purchased_toggled` de propósito: o segundo é legado e alimenta as explorações do GA4; o novo é o nome definido por esta spec.
- Flag relida a cada emissão de produtos, então uma mudança remota vale sem reabrir a lista.

## 7. Plano de Testes
- **Unitários:** Testar soma dos itens marcados vs. total geral.
- **UI:** Testes de layout via Robolectric (`CartScreenTest`, padrão do módulo, sem emulador) com `testTag` para a barra inferior; agrupamento em `CartGroupingTest`.
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Itens sem preço definido gerando soma incorreta | Alta | Baixa | Exibir aviso visual ou tratar itens sem preço como R$ 0,00 |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
