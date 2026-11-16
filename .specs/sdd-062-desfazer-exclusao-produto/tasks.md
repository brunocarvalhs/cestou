# Task: Desfazer ao excluir produto

- **Slug:** desfazer-exclusao-produto
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-16
- **Status:** ready
- **Versão-alvo:** próxima minor
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive
- **Prioridade:** 2 de 4 (confiança)

## 1. Contexto e Motivação
- Deslizar um produto o apaga na hora, e quantidade zero também apaga sem aviso. É uma das dores mais citadas nos concorrentes (Listonic: "swipe-to-delete with no undo"; Bring!: toque remove item sem querer).
- Referência: doc "dores dos concorrentes" (16/11/2026), tema "Gestos destrutivos".
- Rastreabilidade: `feature/cart/.../components/ProductListItem.kt:71`, `feature/cart/.../viewmodel/CartViewModel.kt` (`deleteProduct`, `updateQuantity`).

## 2. Escopo
- **Inclui:**
  - Snackbar "Produto removido" com ação "Desfazer" (cerca de 5 s) após excluir por gesto, por botão ou por quantidade zero.
  - Desfazer regrava o produto com os mesmos dados (nome, quantidade, preço, categoria, status de comprado).
  - Evento `product_delete_undone`.
- **Exclui:**
  - Lixeira de listas (escopo de sdd-043).

## 3. Impacto Arquitetural
- Módulos afetados: `feature:cart`, `core:analytics`.
- Padrão aplicado: MVI; o ViewModel guarda o último produto removido e expõe um efeito de snackbar.

## 4. Contratos e Interfaces
- Nenhum contrato público novo; nova intent `onUndoDelete` em `CartIntent`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Excluir apaga sem volta | Excluir mostra "Desfazer" por cerca de 5 s | `feature/cart/.../viewmodel/CartViewModel.kt` (`deleteProduct`) |
| 2 | Quantidade zero apaga em silêncio | Quantidade zero apaga e mostra "Desfazer" | `feature/cart/.../viewmodel/CartViewModel.kt` (`updateQuantity`) |

## 6. Critérios de Aceitação
- Desfazer restaura o item na mesma posição e com o mesmo status em listas compartilhadas.
- Observabilidade: razão `product_delete_undone` / `cart_product_deleted`.

## 7. Plano de Testes
- **Unitários:** excluir e desfazer no `CartViewModel`; desfazer depois do tempo não faz nada.
- **Instrumentados / UI:** snackbar aparece após deslizar.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Outro membro edita a lista entre excluir e desfazer | Baixa | Baixa | Desfazer regrava só o item removido |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha`.
- [x] Nenhum código de produção escrito.
