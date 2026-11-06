# Task: Assistente de IA Proativo e Visão Computacional Refinada

- **Slug:** contextual-ai-assistant-actions
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03 (Refinado)
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Eliminar a tela vazia do chat através de "chips" de prompts rápidos ("Ideias de jantar para dois", "Frutas da estação", "Lista para churrasco") e ações diretas nos itens sugeridos (botão "+" por item ou "Adicionar todos à lista"). Além disso, refinar o prompt da visão computacional na câmera para ignorar "lixo" (tabelas nutricionais, códigos aleatórios como "1a00") e extrair apenas nomes comerciais e ingredientes úteis.
- Referência ao pedido: Refinamento de Concorrentes - Itens 1 e 2 (Chat Proativo e Visão Computacional).
- Rastreabilidade: `core/ai/`, `feature/chat/`, `feature/ai-agent/`.

## 2. Escopo
- **Inclui:**
  - Chips de sugestão rápida flutuantes acima da barra de digitação.
  - Botão de ação rápida ("+" por item e "Adicionar todos à lista") nas respostas estruturadas da IA.
  - Refinamento de system prompt para visão computacional (filtro estrito de ruído: ignorar tabela nutricional e códigos irrelevantes, extraindo apenas nomes comerciais/ingredientes).
- **Exclui:**
  - Histórico de conversas (já coberto por sdd-016).

## 3. Impacto Arquitetural
- Módulos afetados: `core:ai`, `feature:chat`, `feature:ai-agent`, `feature:shopping`.
- Padrão aplicado: Clean Architecture + MVI, Nav3.
- Novos arquivos:
  - `feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/presentation/components/AiSuggestionChips.kt`
  - `core/ai/src/main/java/br/com/brunocarvalhs/howmuch/core/ai/prompt/VisionExtractionPrompt.kt`

## 4. Contratos e Interfaces
- Atualização dos parsers de resposta da IA em `core:ai` para suportar adição em lote de ingredientes sugeridos.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Chat estático sem sugestões e visão extraindo textos irrelevantes | Chat proativo com chips e ações de adicionar; visão filtrando apenas itens comerciais/ingredientes | `core/ai/...` |

## 6. Critérios de Aceitação
- Performance: Processamento de imagem otimizado; prompt restritivo para evitar alucinação ou inclusão de texto espúrio.
- Feature toggle: `feat_proactive_ai` (default: true).
- Observabilidade: Evento `ai_suggestion_clicked`, `ai_batch_items_added`.

## 7. Plano de Testes
- **Unitários:** Testes de extração e filtragem de imagem simulada (garantindo exclusão de tabelas nutricionais).
- **Instrumentados / UI:** Testes de interação com chips e botão "Adicionar todos".
- **Lint/Detekt/Cobertura:** Validação padrão via Gradle.

## 8. Dependências e Riscos
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| IA ainda retornar texto espúrio na foto | Média | Média | Validação e limpeza programática adicional pós-extração |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha`.
- [ ] Nenhum código de produção escrito.
- [ ] Seção 6 preenchida.
- [ ] Mudança em símbolo público reflete `additive`.
- [ ] Perguntas em aberto exauridas.
