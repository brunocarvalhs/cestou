# Task: Lista da semana (repetir a última compra) e preço lembrado

- **Slug:** lista-da-semana
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-16
- **Status:** draft
- **Versão-alvo:** a definir
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive
- **Prioridade:** 3 de 4 (hábito)

## 1. Contexto e Motivação
- O app é usado para uma compra pontual e não cria hábito (DAU/MAU ~1%). Compra de mercado é semanal: o caminho mais curto para a próxima compra é repetir a anterior.
- Out of Milk e AnyList são criticados por perder preços a cada semana; o Cestou também não sugere o preço pago antes.
- Rastreabilidade: duplicar lista já existe (`core/appfunctions/.../DuplicateShoppingListFunction.kt:28`); `feature/products/.../ProductFormViewModel.kt` só sugere categoria.

## 2. Escopo
- **Inclui:**
  - Card "Repetir a compra de <data>" na home quando a última lista foi finalizada, que duplica a lista com um toque.
  - Ao adicionar um produto que já foi comprado, sugerir o último preço pago.
  - Lembrete no dia da semana em que o usuário costuma comprar (depende de sdd-061).
- **Exclui:**
  - Comparador de preços entre redes (escopo de sdd-039).

## 3. Impacto Arquitetural
- Módulos afetados: `feature:shopping`, `feature:products`, `core:domain`, `feature:settings`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Duplicar fica no menu da lista | Card de repetir na home após finalizar uma compra | `core/appfunctions/.../DuplicateShoppingListFunction.kt:28` |
| 2 | Formulário sugere só a categoria | Também sugere o último preço pago pelo mesmo produto | `feature/products/.../ProductFormViewModel.kt:69` |

## 6. Critérios de Aceitação
- Eventos `weekly_list_repeated` e `last_price_suggested`.
- Meta: sessões por usuário ativo de 1,1 para 1,5.

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Onde guardar o último preço por produto (por usuário ou por lista compartilhada)?

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha`.
- [x] Nenhum código de produção escrito.
