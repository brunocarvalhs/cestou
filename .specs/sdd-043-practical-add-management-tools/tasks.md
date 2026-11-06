# Task: Ferramentas Práticas, Leitor de Código de Barras e Lixeira (Prevenção de Erros)

- **Slug:** practical-add-management-tools
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03 (Refinado)
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Adicionar o leitor de código de barras clássico (EAN) integrado a um banco de dados de produtos para reposição rápida de itens da despensa, e implementar uma área de "Lixeira" / histórico de exclusão para permitir a restauração fácil de listas deletadas por engano, prevenindo frustrações.
- Referência ao pedido: Refinamento de Concorrentes - Itens 2 (Barcode) e 6 (Lixeira).
- Rastreabilidade: `feature/shopping/`, `core/ui/`, `core/common/`.

## 2. Escopo
- **Inclui:**
  - Scanner rápido de código de barras (EAN) com CameraX para adição instantânea de produtos à lista.
  - Tela de "Lixeira" / Histórico de exclusão para restaurar listas deletadas.
  - Alternância de visualização na lista (Lista compacta vs. Lista visual).
- **Exclui:**
  - Backup em nuvem avançado (escopo de sdd-044).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/shopping`, `core:ui`, `core:common`.
- Padrão aplicado: Clean Architecture + MVI, CameraX interop.
- Novos arquivos:
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/BarcodeScannerScreen.kt`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/TrashBinScreen.kt`

## 4. Contratos e Interfaces
- Interfaces de repositório para exclusão lógica (soft-delete) e restauração em `core:domain`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Exclusão definitiva de listas e ausência de leitor de código de barras | Exclusão lógica com lixeira para restauração e scanner EAN integrado | `feature/shopping/...` |

## 6. Critérios de Aceitação
- Performance: Leitura de código de barras otimizada (<500ms); restauração de lixeira instantânea.
- Feature toggle: `feat_practical_tools` (default: true).
- Observabilidade: Evento `barcode_scanned`, `list_restored_from_trash`.

## 7. Plano de Testes
- **Unitários:** Testar lógica de soft-delete, retenção na lixeira e restauração de listas.
- **Instrumentados / UI:** Testes do scanner de código de barras e navegação na lixeira.
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Permissão de câmera negada | Média | Média | Tratamento graceful com request e fallback manual |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
