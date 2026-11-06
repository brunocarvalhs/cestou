# Task: Catálogo Visual e Enriquecido de Produtos

- **Slug:** visual-enriched-catalog
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Facilitar a adição manual de produtos através de uma aba de "Catálogo" com grade visual (cards grandes) separados por seções coloridas, filtros rápidos por tags (#Vegan, #Keto, #Sem glúten, #Rico em proteína), ficha detalhada do produto (tabela nutricional, dicas de armazenamento, benefícios e riscos à saúde) e aba de itens recentes.
- Referência ao pedido: Requisitos de Concorrentes - Item 4.
- Rastreabilidade: `feature/products/`, `core/ui/`.

## 2. Escopo
- **Inclui:**
  - Aba de Catálogo Visual com grade de cards e seções coloridas.
  - Filtros rápidos por tags nutricionais e dietéticas.
  - Tela/BottomSheet de detalhes do produto com tabela nutricional, dicas de armazenamento e saúde.
  - Aba "Recentes" para adição rápida de produtos frequentes.
- **Exclui:**
  - Leitor de código de barras (escopo de sdd-043).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/products`, `core:domain`, `core:data`, `core:ui`.
- Padrão aplicado: Clean Architecture + MVI, Nav3.
- Novos arquivos:
  - `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/presentation/CatalogScreen.kt`
  - `feature/products/src/main/java/br/com/brunocarvalhs/howmuch/feature/products/presentation/ProductDetailBottomSheet.kt`
  - `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/model/ProductDetails.kt`

## 4. Contratos e Interfaces
- Modelos públicos de produtos e detalhes nutricionais em `core:domain`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Lista de produtos básica baseada em texto/busca simples | Catálogo visual rico com tags, ficha nutricional e histórico de recentes | `feature/products/...` |

## 6. Critérios de Aceitação
- Performance: Carregamento otimizado de imagens nos cards com Coil e paginação/lazy loading.
- Feature toggle: `feat_visual_catalog` (default: true).
- Observabilidade: Evento `catalog_product_viewed`, `catalog_tag_filtered`.

## 7. Plano de Testes
- **Unitários:** Testar filtragem por tags e recuperação de produtos recentes.
- **Instrumentados / UI:** Testes de navegação no catálogo e abertura do bottom sheet de detalhes.
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Volume de dados de catálogo pesado | Baixa | Média | Paginação local em Room e cache eficiente de imagens |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
