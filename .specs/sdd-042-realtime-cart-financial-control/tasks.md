# Task: Carrinho e Controle Financeiro em Tempo Real

- **Slug:** realtime-cart-financial-control
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Aprimorar a experiência de compras no corredor do supermercado dividindo visualmente a lista ou oferecendo rodapés fixos com métricas em tempo real ("Total" planejado vs. "Carrinho" dos itens já pegos), marcação de checkbox com riscado e movimentação automática para o carrinho, além de edição rápida de unidade de medida, preço, categoria e notas/fotos.
- Referência ao pedido: Requisitos de Concorrentes - Item 5.
- Rastreabilidade: `feature/cart/`, `feature/shopping/`, `core/ui/`.

## 2. Escopo
- **Inclui:**
  - Barra inferior fixa com métricas "Total" e "Carrinho" em tempo real.
  - Movimentação automática de itens riscados para a seção de carrinho.
  - Modal/BottomSheet de edição rápida de item (unidade de medida: unidade, dúzia, litro, kg, pacote, caixa, preço, categoria, observações e foto).
- **Exclui:**
  - Relatórios avançados de despesas (escopo de sdd-044).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/cart`, `feature/shopping`, `core:domain`, `core:ui`.
- Padrão aplicado: Clean Architecture + MVI, Nav3.
- Novos arquivos:
  - `feature/cart/src/main/java/br/com/brunocarvalhs/howmuch/feature/cart/presentation/CartBottomBar.kt`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/QuickEditItemBottomSheet.kt`

## 4. Contratos e Interfaces
- Atualização dos use cases de atualização de item para suportar unidades de medida customizadas e fotos em `core:domain`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Carrinho e lista sem divisão clara em tempo real e métricas fixas | Barra de resumo financeiro fixada e separação dinâmica entre lista e carrinho | `feature/cart/...` |

## 6. Critérios de Aceitação
- Performance: Atualização instantânea dos cálculos de total e carrinho sem jank em listas longas.
- Feature toggle: `feat_realtime_cart` (default: true).
- Observabilidade: Evento `cart_item_checked`, `item_quick_edited`.

## 7. Plano de Testes
- **Unitários:** Testes de cálculo de soma total vs carrinho e conversão de unidades.
- **Instrumentados / UI:** Testes de marcação de checkbox e abertura da edição rápida.
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Latência na sincronização offline-first do carrinho | Baixa | Baixa | Persistência reativa com Flow e Room |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
