# Task: Evitar efeito colateral duplicado no fallback do agente de IA

- **Slug:** ai-agent-fallback-safety
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.3
- **Tipo de mudança:** fix
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação

Revisão (`code-reviewer`) do `sdd-006-ai-agent-reliability` concluiu **AJUSTES NECESSARIOS** com
dois achados bloqueantes:

1. `FallbackAiAgent.kt:17-24` recorre ao provider secundário para **qualquer** exceção do
   primário, mesmo quando uma tool já executou um efeito colateral real (ex. produto gravado no
   Firestore) nesta mesma rodada. Isso contradiz o próprio critério de aceitação do sdd-006 (§6,
   "Fallback": *"exceto se o primário já executou uma tool nesta rodada"*) e arrisca duplicar a
   ação ao cair no secundário. Piora com o sdd-006: `AiAgentFactoryImpl.selectAgent` agora sempre
   envolve `FallbackAiAgent` quando os dois provedores existem, então esse caminho passou a ser o
   padrão, não um caso raro.
2. `AiSession.clearHistory()` (`core/ai/.../contract/AiSession.kt:12-18`) é um contrato público
   novo introduzido durante a implementação do sdd-006 mas nunca documentado no `tasks.md`
   daquela tarefa — viola o registro exigido por `AGENTS.md` §6.

**Itens não bloqueantes do mesmo review (avisos), explicitamente fora de escopo aqui — ver §2:**
`AiSession.history` continuar público/mutável; ausência de testes no nível `sendMessage` completo
para `MAX_TOOL_ROUNDS`/relançamento de transporte.

## 2. Escopo

**Inclui:**
- `AiSession` ganha `var toolExecutedThisTurn: Boolean` — sinal que `GeminiAiAgent`/
  `OpenRouterAiAgent` setam ao despachar uma tool e `FallbackAiAgent` lê antes de decidir se
  tenta o secundário.
- `GeminiAiAgent`/`OpenRouterAiAgent`: resetam o sinal no início de `sendMessage`; marcam
  `true` no chokepoint de despacho (`executeFunctionCall`/`executeToolCall`), antes de chamar
  `action.execute(...)` — conservador: qualquer tentativa de execução conta, sucesso ou falha,
  já que mesmo uma falha pode ter mutação parcial.
- `FallbackAiAgent` passa a receber `AiSession` no construtor e só tenta o secundário se
  `!session.toolExecutedThisTurn` quando o primário falha; senão relança o erro original (sem
  wrapper — a classificação de rede/genérica em `AiChatViewModel` continua funcionando sem
  mudança lá).
- `AiAgentFactoryImpl.selectAgent` passa `session` para `FallbackAiAgent`.
- Atualizar `specs/sdd-006-ai-agent-reliability/tasks.md` (§3 e §4) para documentar
  `AiSession.clearHistory()` como contrato público já existente, com a justificativa real
  (classpath do SDK do Gemini não chega a `:feature:chat`/`:app`).

**Exclui (registrado, não bloqueante):**
- Mensagem de erro distinta para "ação pode ter sido concluída, mas não confirmei" — exigiria um
  novo tipo de exceção público só para isso; o ganho de UX não justifica o contrato público
  extra agora. A mensagem de rede/genérica atual continua sendo mostrada (não é incorreta: a
  falha de transporte de fato aconteceu).
- Estreitar `AiSession.history` (continua público/mutável — aviso #3 do review, sem risco novo
  introduzido por esta tarefa).
- Testes de `sendMessage` completo com mock do SDK do Gemini/HTTP do OpenRouter (aviso #4 do
  review) — exigem injetar `GenerativeModel`/`HttpClient` como seam de teste, escopo maior;
  fica para um SDD próprio se o maintainer priorizar.

## 3. Impacto Arquitetural

```
:core:ai
  └── contract/AiSession.kt
      └── var toolExecutedThisTurn: Boolean   [novo, public]

:feature:ai-agent/data/service
  ├── GeminiAiAgent.kt
  │   ├── sendMessage(): session.toolExecutedThisTurn = false no início do try
  │   └── executeFunctionCall(): session.toolExecutedThisTurn = true antes de action.execute(...)
  ├── OpenRouterAiAgent.kt
  │   └── mesmo padrão em sendMessage()/executeToolCall()
  ├── FallbackAiAgent.kt
  │   └── construtor +AiSession; catch só chama secondary se !session.toolExecutedThisTurn
  └── AiAgentFactoryImpl.kt
      └── selectAgent(): FallbackAiAgent(primary, secondary, session)

:feature:ai-agent/domain/entity
  └── AiAgentSession.kt
      └── override var toolExecutedThisTurn: Boolean = false
```

Fluxo do fix:
```
sendMessage(prompt) [primary]
  session.toolExecutedThisTurn = false
  ... tool despachada -> session.toolExecutedThisTurn = true
  ... 2ª chamada de rede falha -> throw
FallbackAiAgent.catch
  toolExecutedThisTurn == true -> throw (sem retry; sem duplicar a tool)
  toolExecutedThisTurn == false -> tenta secondary normalmente
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Justificativa |
|---|---|---|---|
| `AiSession.toolExecutedThisTurn: Boolean` | Novo campo mutável | `public` (`core:ai`, interface já pública entre `:core:ai` e `:feature:ai-agent`) | `FallbackAiAgent` (mesmo módulo) precisa ler o sinal que `GeminiAiAgent`/`OpenRouterAiAgent` escrevem; sem isso não há como saber, de fora do agente concreto, se uma tool já rodou nesta tentativa |
| `FallbackAiAgent(primary, secondary, session)` | Novo parâmetro de construtor | `internal` (classe já é `internal`, usada só dentro de `:feature:ai-agent`) | Não é contrato cross-module; `FallbackAiAgentTest.kt` precisa ser atualizado |
| `AiAgentSession.toolExecutedThisTurn` | Implementação do novo membro da interface | `public` (override) | Exigido pela interface |

Nenhuma mudança é breaking: `AiSession` ganha campo aditivo; nenhuma assinatura pública de
`AiAgent`/`AiAgentFactory` muda.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | `FallbackAiAgent` tenta o secundário para qualquer falha do primário, mesmo após uma tool já ter executado | Só tenta o secundário se nenhuma tool executou nesta tentativa do primário | `FallbackAiAgent.kt:17-24`; critério de aceitação do sdd-006 §6 |
| 2 | `AiSession.clearHistory()` existe no código sem registro em nenhum `tasks.md` | Documentado em `specs/sdd-006-ai-agent-reliability/tasks.md` §3/§4 | `core/ai/.../AiSession.kt:12-18` |

## 6. Critérios de Aceitação

- Tool executada com sucesso pelo primário, falha de rede na chamada seguinte (ex. ao enviar o
  resultado da tool de volta): `FallbackAiAgent` **não** chama o secundário; a exceção original
  sobe (mesma classificação IOException/genérica em `AiChatViewModel`, sem mudança lá).
- Primário falha **antes** de qualquer tool rodar (ex. timeout na primeira chamada): comportamento
  atual preservado — `FallbackAiAgent` tenta o secundário normalmente.
- `toolExecutedThisTurn` é resetado no início de cada `sendMessage`, nunca "vaza" de um turno
  para o outro (duas chamadas sequenciais ao mesmo agente, a segunda sem tool, não deve herdar
  `true` da primeira).
- `specs/sdd-006-ai-agent-reliability/tasks.md` passa a citar `AiSession.clearHistory()`
  explicitamente em §3 (diagrama) e §4 (tabela de contratos).

## 7. Plano de Testes

**Unitários:**
- `FallbackAiAgentTest` (atualizar construtor + novos casos): `session.toolExecutedThisTurn = true`
  e primário falha → secundário **nunca** é chamado (verificar via mock, não só o resultado da
  flow), exceção original é relançada. `toolExecutedThisTurn = false` e primário falha →
  secundário é chamado (comportamento já existente, mantém passando).
- `GeminiAiAgentGatingTest` (estender): após `executeFunctionCall` com uma `FakeAgentAction`
  (sucesso ou falha), `session.toolExecutedThisTurn == true`.
- `OpenRouterAiAgentGatingTest` (estender): mesmo para `executeToolCall`.
- Novo teste (`AiAgentSessionTest` ou dentro de um teste existente): `toolExecutedThisTurn`
  começa `false` por padrão.

**Lint/Detekt/Testes:**
```bash
./gradlew lintDebug detekt testDebug
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Esquecer de resetar `toolExecutedThisTurn` no início do turno faz o sinal "vazar" do turno anterior e bloquear fallback indevidamente | baixa | médio | Teste explícito de reset (§7); reset é a primeira linha do `try` em `sendMessage` |
| `AiAgentSession` é `@Singleton` — `toolExecutedThisTurn` é compartilhado entre agentes concorrentes se o usuário mandar duas mensagens em paralelo | baixa (UI não permite enviar 2 msgs ao mesmo tempo hoje — `isLoading` bloqueia o input) | baixo | Fora de escopo; mesma premissa de concorrência que `history` já tem hoje |

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (`FallbackAiAgent.kt`, `AiSession.kt`, `AiAgentFactoryImpl.kt`).
- [x] Nenhum código de produção escrito nesta spec.
- [x] Seção 6 substantiva.
- [x] Mudança pública (`AiSession.toolExecutedThisTurn`) marcada e justificada.
- [x] Perguntas em aberto exauridas.
