# Task: Trial gratuito do chat e gate de acesso

- **Slug:** ai-chat-gate
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

O trial gratuito do chat é consumido **antes** da resposta, até para PRO e em caso de falha (B-12). O gate começa como `true`, permitindo envio antes de os flows carregarem (race condition). O spinner fica infinito quando o provedor não retorna texto (B-13). Erros não aparecem ao usuário. Este SDD corrige o gate para consumir só após resposta bem-sucedida, expor erros e bloquear envio durante carregamento.

**Itens do backlog:** B-12, B-13, B-15 (remove tools), B-16 (remove tools).

## 2. Escopo

**Inclua:**
- Marcar trial como usado só após resposta bem-sucedida (Flow emite texto ou erro).
- Pular incremento se o usuário é PRO.
- Valor inicial do gate: `false` (ou estado de carregamento).
- Bloquear botão de envio enquanto `isLoading = true`.
- Exibir snackbar ou mensagem de erro se Flow não emitir texto.
- Adicionar `onCompletion { isLoading = false }` para evitar spinner infinito.
- Remover tools `compare_prices` e `delete_multi_shopping_list` do registry (B-15, B-16).
- `update_shopping_list` deixa de aceitar `users`/`roles` como argumentos (B-27).

**Exclui:**
- Implementação de novos limites de token (fica para 2.3.0).
- Feature flag para trial (usar remote-config só se necessário).

## 3. Impacto Arquitetural

```
:feature:chat/presentation/viewmodel
  ├── AiChatViewModel.kt
  │   ├── isLoading: Boolean  [novo]
  │   ├── uiError: UiText?  [novo]
  │   ├── onSuccess → consumir trial após resposta completa
  │   ├── onCompletion { isLoading = false }
  │   └── enviarMensagem bloqueado se isLoading
  └── ChatAiGateViewModel.kt
      ├── hasTrialRemaining: Flow<Boolean>  [mudar logica]
      └── valor inicial: false
      
:feature:chat/presentation/screen
  └── AiChatScreen.kt / ChatDetailContent.kt
      ├── botão desabilitado se isLoading
      └── mostrar uiError em snackbar
      
:feature:ai-agent/data/service
  └── AgentRegistry.kt
      ├── remover `compare_prices`
      ├── remover `delete_multi_shopping_list`
      └── atualizar `update_shopping_list` (sem `users`/`roles`)
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `AgentRegistry` | Remove 2 tools, edita 1 | `internal` | prompt remoto pode citar | B-15, B-16, B-27 |
| `AiChatViewModel.isLoading` | Novo | `public` (estado) | bloqueia UI |
| `AiChatViewModel.uiError` | Novo | `public` (estado) | exibe aviso |

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento novo | Origem |
|---|---|---|---|
| 1 | Trial consumido antes da resposta | Consumido só após sucesso; skipped se PRO | B-12 |
| 2 | Gate começa como `true` (race condition) | Começa como `false`; carrega do remote-config | B-12 |
| 3 | Spinner infinito quando vazio | `onCompletion` zera `isLoading` | B-13 |
| 4 | `compare_prices` retorna dados falsos | Tool removida do registry | B-16 |
| 5 | `delete_multi_shopping_list` duplica `delete` | Tool removida do registry | B-15 |
| 6 | IA pode editar papéis e status | `update_shopping_list` não aceita `users`/`roles` | B-27 |

## 6. Critérios de Aceitação

- **Trial FREE:** primeira mensagem custa 1 trial. Falta trial → `hasTrialRemaining = false`, botão desabilitado com mensagem "Upgrade para PRO".
- **Trial PRO:** nunca decrementa.
- **Spinner:** Flow vazio (sem texto) → `onCompletion` zera `isLoading`, exibe erro "Nenhuma resposta recebida".
- **Envio bloqueado:** `sendButton.enabled = !isLoading && hasTrialRemaining`.
- **Valor inicial do gate:** `false` ou `loading`. Carrega do `ChatAiGateViewModel.hasTrialRemaining`.
- **Tools removidas:** `compare_prices`, `delete_multi_shopping_list` não aparecem no resultado de `AgentRegistry.getTools()`.
- **`update_shopping_list`:** assinatura não mais aceita `users` ou `roles` em argumentos.
- **Prompt remoto:** após publicar, validar que refere só às tools restantes.

## 7. Plano de Testes

**Unitários:**
- B-12: FREE, primeira mensagem → trial decrementa após sucesso. Falha → trial mantido. PRO → nunca decrementa.
- B-13: Flow vazio (sem onNext) → `onCompletion` chamado → `isLoading = false`.
- B-15/B-16: `AgentRegistry.getTools()` não contém `compare_prices` ou `delete_multi_shopping_list`.
- B-27: `update_shopping_list` signature não lista `users`/`roles` em params.

**Instrumentados:**
- FREE com trial: enviar mensagem → spinner desaparece → resposta exibe. Trial decrementa.
- FREE sem trial: botão desabilitado com mensagem "Upgrade".
- Falha de rede: erro exibido, trial mantido.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Remover tools quebra prompt remoto | média | médio | Revisar `ai_system_prompt` no Remote Config |
| Flow sem emissão não é loop (é falha) | baixa | baixo | Teste com provedor indisponível |
| PRO sem verificação no servidor | desconhecida | alto | Depende F-09 (P-07) |

## 9. Perguntas em Aberto

- [ ] **P-07a:** Verificação de PRO é local ou no servidor? (hoje é local, B-24 agora)

Se local, marcar como risco.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (B-12, B-13, B-15, B-16, B-27).
- [x] Nenhum código escrito.
- [x] Seção 6 substantiva.
- [x] Não quebra contratos (tools removidas são internas ao registry).
- [x] Testes instrumentados planejados.
