# Task: Confiabilidade do agente de IA (seleção de modelo, fallback, tool calls)

- **Slug:** ai-agent-reliability
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação

O seletor de modelo de IA não funciona: o provider salvo é o fabricante (nvidia, google, etc.) e a fábrica só aplica o modelo escolhido quando é openrouter ou gemini (B-07). O fallback nunca executa porque agentes capturam exceção e emitem mensagem de erro (B-08). Tool calls retornam `"Sucesso"` ou `"kotlin.Unit"` em vez de erro/OK (B-09), causando alucinação do modelo. O loop de tool calls não tem limite (B-10). A exceção `CancellationException` é capturada silenciosamente (B-33). Histórico se mantém entre listas (B-11, privacidade). Este SDD corrige modelo, fallback, tool calls e privacidade.

**Itens do backlog:** B-07, B-08, B-09, B-10, B-11, B-33, B-34.

## 2. Escopo

**Inclui:**
- Separar `vendor` (exibição) de `provider` (openrouter/gemini) em `AiModel`.
- Migrar valor de `AI_PROVIDER` já salvo no DataStore.
- Fallback só executa se nenhuma tool foi executada (evitar efeitos colaterais).
- Exceções em transporte lançam `AiProviderUnavailableException`.
- Tool call result: `Result.failure` → `"Erro: <message>"`, `Unit` → `"OK"`, `Result.success(data)` → representação textual.
- `MAX_TOOL_ROUNDS = 5` com resposta de fallback ao atingir.
- `CancellationException` relançado (não capturado como `Exception`).
- `AiAgentSession` limpo ao mudar `shoppingId` ou fazer logout.
- Histórico cortado em fronteiras de turno (B-34).

**Exclui:**
- Remoção de tools (B-15, B-16 ficam para sdd-007).
- Acesso remoto ao prompt (`ai_system_prompt`), só validação local (fica para Remote Config).

## 3. Impacto Arquitetural

```
:core:domain
  └── model/AiModel.kt
      ├── vendor: String  [novo]
      ├── provider: String  [novo, migrado]
      └── @Deprecated providerId  [removido ou marcado]
      
:feature:ai-agent/data/service
  ├── AiAgentFactoryImpl.kt
  │   └── providerOf(AiModel): AiProvider  [novo]
  ├── GeminiAiAgent.kt
  │   ├── MAX_TOOL_ROUNDS = 5
  │   ├── mapear Result → feedback textual
  │   ├── CancellationException relançado
  │   ├── cortar histórico em fronteiras
  │   └── fallback só se !toolExecuted
  ├── OpenRouterAiAgent.kt
  │   ├── mesmo que acima
  │   └── catch (Exception) → relançar CancellationException
  ├── FallbackAiAgent.kt
  │   └── só se nenhuma tool executada
  └── AiAgentOrchestrator.kt
      ├── novo shoppingId → clearHistory()
      └── logout → clearHistory()
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `AiModel` | Novo `vendor`, `provider` | `public` | migração de `AI_PROVIDER` |
| `AiProviderUnavailableException` | Novo | `public` | sinalizador de fallback |
| `MAX_TOOL_ROUNDS` | Novo | `internal` | por agente |
| `AiAgentSession.clearHistory()` | Já existe | `public` | novo chamador (logout, shoppingId) |

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento novo | Origem |
|---|---|---|---|
| 1 | Modelo escolhido é ignorado | Provider derivado de `AiModel.provider`; modelo sempre vale | B-07 |
| 2 | OpenRouter cai → usuário vê erro sem tentar Gemini | Lança `AiProviderUnavailableException` → fallback tenta Gemini | B-08 |
| 3 | Tool call retorna `kotlin.Unit` | Mapeado a `"OK"`, não `"Sucesso"` | B-09 |
| 4 | Loop infinito de tool calls | Máximo de 5 rodadas, depois resposta de fallback | B-10 |
| 5 | Histórico vaza entre contas | Limpo no logout e ao trocar `shoppingId` | B-11 |
| 6 | CancellationException silenciosa | Relançada (não capturada em `catch (Exception)`) | B-33 |
| 7 | Histórico de conversa longa quebra API | Cortado em fronteiras de turno do usuário | B-34 |

## 6. Critérios de Aceitação

- **`vendor` vs `provider`:** todo modelo tem ambos definidos. Enum de providers: `OPENROUTER`, `GEMINI`.
- **Migração:** `AI_PROVIDER = "nvidia"` → `{vendor: "Nvidia", provider: "openrouter"}`. Idempotente.
- **Fallback:** OpenRouter lança `AiProviderUnavailableException` → Gemini tenta. Se OpenRouter executou uma tool, não há fallback.
- **Tool feedback:** `Result.failure(Exception("denied"))` → `"Erro: denied"`. `Unit` → `"OK"`. `Result.success(id)` → `"ID: $id"`.
- **MAX_TOOL_ROUNDS:** após 5 rodadas, emite `"Desculpe, não consegui."` e termina.
- **CancellationException:** relançada em `GeminiAiAgent` (já faz) e `OpenRouterAiAgent`.
- **Histórico:** limpo em `MainViewModel.signOutWatchOnSignOut` e em `ShoppingDetailsViewModel` ao trocar `shoppingId`.
- **Corte de histórico:** começa em turno inteiro (não no meio de `functionCall`/`functionResponse`).

## 7. Plano de Testes

**Unitários:**
- B-07: Cada modelo em `freeModels` → agente retorna o id correto (não padrão).
- B-08: OpenRouter lança `AiProviderUnavailableException` sem tool executada → Gemini responde. Com tool → sem fallback.
- B-09: `Result.failure(Ex("not found"))` → `"Erro: not found"`. `Unit` → `"OK"`. `Result.success(123)` → contém "123".
- B-10: Loop com 6 tool calls → para na rodada 5, resposta genérica.
- B-11: Logout → `session.history.clear()`. Trocar `shoppingId` → limpa.
- B-33: Cancelar chat → `CancellationException` relançada, não capturada.
- B-34: Histórico com 25 turnos → cortado em turno inteiro, não quebra API.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Mudar feedback de tool quebra o modelo | média | médio | Testar com ambos os provedores |
| Cortar histórico separa turno | baixa | médio | Testar com conversa longa |
| Migração de `AI_PROVIDER` muda efetivo | média | baixo | Evento de analytics com modelo novo |

## 9. Perguntas em Aberto

Nenhuma bloqueante.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (B-07 a B-11, B-33, B-34).
- [x] Nenhum código escrito.
- [x] Seção 6 substantiva.
- [x] `AiModel` é aditivo.
- [x] Testes unitários planejados.
