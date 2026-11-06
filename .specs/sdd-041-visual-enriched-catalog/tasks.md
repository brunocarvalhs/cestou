# Task: Catálogo Visual, Enriquecido e Guiado

- **Slug:** visual-enriched-catalog
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03 (Refinado)
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Substituir a adição puramente baseada em texto por uma aba "Catálogo" com grandes categorias visuais (Açougue, Hortifruti, Limpeza, Congelados) e fichas completas de produtos (dicas de armazenamento, tags nutricionais), transformando o app em um guia de compras inteligente.
- Referência ao pedido: Refinamento de Concorrentes - Item 4 (Catálogo Visual e Enriquecido).
- Rastreabilidade: `feature/products/`, `core/ui/`.

## 2. Escopo
- **Inclui:**
  - Aba "Catálogo" com navegação por categorias visuais e ícones marcantes.
  - Ficha detalhada do produto ao selecionar um item (dicas de armazenamento, melhor conservação e tags nutricionais).
  - Filtros rápidos por tags dietéticas.
- **Exclui:**
  - Leitor de código de barras (escopo de sdd-043).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/products`, `core:domain`, `core:data`, `core:ui`.
- Padrão aplicado: Clean Architecture + MVI, Nav3.
- Novos arquivos:
  - `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/presentation/CatalogScreen.kt`
  - `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/presentation/ProductDetailBottomSheet.kt`

## 4. Contratos e Interfaces
- Modelos públicos de categorias visuais e fichas de produtos em `core:domain`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Adição de produtos por texto simples e sem orientações | Catálogo visual por categorias e fichas de orientação de armazenamento/nutrição | `feature/products/...` |

## 6. Critérios de Aceitação
- Performance: Carregamento fluido de ícones e imagens com Coil e lazy loading.
- Feature toggle: `feat_visual_catalog` (default: true).
- Observabilidade: Evento `catalog_category_selected`, `product_detail_opened`.

## 7. Plano de Testes
- **Unitários:** Testar categorização e recuperação de fichas de produtos.
- **Instrumentados / UI:** Testes de navegação no catálogo visual e abertura do bottom sheet.
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Falta de dados de armazenamento para alguns produtos | Média | Baixa | Fallback para dicas genéricas por categoria |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
