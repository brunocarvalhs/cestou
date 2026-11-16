# Task: Medir o funil de produto em todos os caminhos

- **Slug:** funil-de-produto-completo
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-16
- **Status:** ready
- **Versão-alvo:** próxima minor (junto com sdd-061)
- **Tipo de mudança:** fix (observabilidade)
- **Impacto público (módulo):** none
- **Prioridade:** 4 de 4 (medição, pré-requisito para avaliar as outras)

## 1. Contexto e Motivação
- `product_added` só é registrado no formulário e na foto de produto. Itens que entram pela IA, por receita ou pelo catálogo não contam, então o funil "criou lista para adicionou produto" (~17%) está subestimado.
- `app_remove` é o evento automático de desinstalação do Firebase, não remoção de listas; a análise de 13/11/2026 o leu errado.
- Rastreabilidade: `feature/products/.../ProductFormViewModel.kt:118`, `feature/products/.../ProductPhotoViewModel.kt:140,182`.

## 2. Escopo
- **Inclui:**
  - Registrar `product_added` (com `source` = `ai`, `recipe`, `catalog`, `appfunction`) em todo caminho que salva produto.
  - `first_product_added` também nesses caminhos.
- **Exclui:**
  - Novos painéis no Firebase.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Só formulário e foto registram `product_added` | Todo caminho de salvar produto registra, com `source` | `feature/products/.../ProductFormViewModel.kt:118` |

## 6. Critérios de Aceitação
- Para cada `source`, um teste confirma o evento.

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha`.
- [x] Nenhum código de produção escrito.
