# Task: Ferramentas Práticas de Adição e Gestão (Barcode, Dual View, Lixeira)

- **Slug:** practical-add-management-tools
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Introduzir leitor de código de barras tradicional (CameraX) para adição instantânea de produtos, opção de alternar visualização entre "Lista de texto" compacta e "Lista visual" com imagens, e área de lixeira para recuperação de listas excluídas acidentalmente.
- Referência ao pedido: Requisitos de Concorrentes - Item 6.
- Rastreabilidade: `feature/shopping/`, `core/ui/`, `core/common/`.

## 2. Escopo
- **Inclui:**
  - Scanner de código de barras com CameraX integrado à adição de produtos.
  - Alternância de visualização na lista (Modo Texto Compacto vs. Modo Visual com Cards).
  - Tela/Área de Lixeira (Recuperação) para listas excluídas.
- **Exclui:**
  - Backup em nuvem avançado (escopo de sdd-044).

## 3. Impacto Arquitetural
- Módulos afetados: `feature/shopping`, `core:ui`, `core:common`.
- Padrão aplicado: Clean Architecture + MVI, CameraX interop.
- Novos arquivos:
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/BarcodeScannerScreen.kt`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/TrashBinScreen.kt`

## 4. Contratos e Interfaces
- Interfaces de repositório para exclusão lógica e restauração de listas em `core:domain`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Sem leitor de código de barras, visual único e exclusão definitiva | Scanner de barras, visual duplo (texto/visual) e lixeira de recuperação | `feature/shopping/...` |

## 6. Critérios de Aceitação
- Performance: Leitura de código de barras rápida e eficiente via CameraX; transição suave entre visualizações.
- Feature toggle: `feat_practical_tools` (default: true).
- Observabilidade: Evento `barcode_scanned`, `list_restored_from_trash`.

## 7. Plano de Testes
- **Unitários:** Testes de lógica de soft-delete e restauração de listas na lixeira.
- **Instrumentados / UI:** Testes de alternância de view mode com `testTag`.
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Permissão de câmera negada | Média | Média | Tratamento graceful com request de permissão e fallback manual |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
