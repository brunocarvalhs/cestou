# Task: Assistente de IA Contextual com Sugestões e Visão Computacional

- **Slug:** contextual-ai-assistant-actions
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Eliminar a "Síndrome da Tela em Branco" no chat de IA através de chips flutuantes de sugestão rápida ("O que está na época agora?", "Dê 3 ideias de jantares rápidos", "Alimentos ricos em proteína"), respostas acionáveis com botão "+" para adicionar itens diretamente à lista com um toque, e suporte a visão computacional (envio de foto de produto para extração e adição em lote).
- Referência ao pedido: Requisitos de Concorrentes - Item 3.
- Rastreabilidade: `core/ai/`, `feature/chat/`, `feature/ai-agent/`.

## 2. Escopo
- **Inclui:**
  - Chips de prompts prontos na `AiChatScreen`.
  - Botão de ação rápida ("+") em itens sugeridos pela IA para adição direta à lista de compras.
  - Funcionalidade de upload/captura de imagem (Vision API / multimodal) para extração de itens e botão de adição em lote ("Adicionar X produtos").
- **Exclui:**
  - Histórico de conversas (já coberto por sdd-016).

## 3. Impacto Arquitetural
- Módulos afetados: `core:ai`, `feature:chat`, `feature:ai-agent`, `feature:shopping`.
- Padrão aplicado: Clean Architecture + MVI, Nav3.
- Novos arquivos:
  - `feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/presentation/components/AiSuggestionChips.kt`
  - `feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/presentation/components/VisionProductExtractorDialog.kt`

## 4. Contratos e Interfaces
- Extensão dos contratos multimodais em `core:ai` para processamento de imagem e extração estruturada de produtos.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Chat de IA apenas textual e sem sugestões iniciais | Chat com sugestões em chips, adição com 1 clique e extração por imagem | `core/ai/...` |

## 6. Critérios de Aceitação
- Performance: Processamento de imagem otimizado com compressão prévia antes de enviar à API de IA.
- Feature toggle: `feat_ai_contextual_actions` (default: true).
- Observabilidade: Eventos `ai_suggestion_clicked`, `ai_vision_products_extracted`.

## 7. Plano de Testes
- **Unitários:** Testes de parsing de resposta multimodal da IA para extracão de itens.
- **Instrumentados / UI:** Testes de interação com chips e botão "+".
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Falha na extração estruturada de imagem | Média | Média | Fallback para prompt textual e edição manual dos itens extraídos |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
