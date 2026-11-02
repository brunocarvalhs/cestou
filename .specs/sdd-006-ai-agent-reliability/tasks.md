# Task: Confiabilidade do agente de IA (seleção de modelo, fallback, tool calls)

- **Slug:** ai-agent-reliability
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** additive

> Este arquivo já existia (gerado em 2026-10-27) e foi **sobrescrito** nesta revisão: o plano
> anterior citava os itens do backlog corretamente, mas não verificava o código linha a linha e
> continha duas suposições que o código atual contradiz (ver §9 e §3, "Correções em relação ao
> plano anterior"). Esta versão revalida tudo contra `develop`/branch atual e corrige o desenho.

## 1. Contexto e Motivação

O seletor de modelo de IA não funciona: o campo `AiModel.provider` guarda o fabricante exibido na
UI (`AiSettingsScreen.kt:109`), não o provider técnico, então `SettingsRepositoryImpl.kt:94-95`
grava um `aiProvider` que quase nunca é `"gemini"`/`"openrouter"` (B-07). O fallback entre
provedores nunca executa porque `GeminiAiAgent`/`OpenRouterAiAgent` capturam toda exceção de
transporte e **emitem** uma mensagem de erro em vez de propagá-la, então o `.catch` de
`FallbackAiAgent` nunca é alcançado (B-08 — confirmado: `FallbackAiAgentTest` já prova que o
`FallbackAiAgent` em si funciona; o bug está nos agentes concretos). Falha de tool call é
informada ao modelo como `"Sucesso"` e sucesso `Unit` como `"kotlin.Unit"` (B-09). O loop de tool
calls não tem limite (B-10). `OpenRouterAiAgent` captura `CancellationException` sem relançar
(B-33 — `GeminiAiAgent` já relança corretamente). O corte de histórico corta por contagem fixa,
podendo separar `functionCall` de `functionResponse` ou abrir com um turno que não é do usuário
(B-34). O histórico de IA é um **singleton `@Singleton`** (`AiAgentModule.kt:21-27,30-38`) nunca
limpo (B-11).

**Itens do backlog:** B-07, B-08, B-09, B-10, B-11, B-33, B-34 (`specs/sdd-001-auditoria-geral/tasks.md:51-55,77-78`).

## 2. Escopo

**Inclui:**
- Separar `vendor` (exibição, ex. "NVIDIA") de `provider` (`"openrouter"`/`"gemini"`) em `AiModel`.
- Migrar o valor de `AI_PROVIDER` já salvo no DataStore (idempotente).
- Corrigir `AiAgentFactoryImpl.selectAgent` para que a escolha explícita de provider também
  envolva o outro provider como fallback de transporte (ver §3 — gap novo encontrado nesta
  revisão: sem isso, corrigir B-07 **elimina** o único caminho que hoje aciona o `FallbackAiAgent`).
- `GeminiAiAgent`/`OpenRouterAiAgent` deixam de converter erro de transporte em texto e voltam a
  propagar a exceção (loga e relança); `CancellationException` sempre relançada primeiro.
- Mapear o resultado de tool call: `Result.failure` → `"Erro: <mensagem>"`; `Result.success(Unit)`
  → `"OK"`; `Result.success(valor)` → representação textual do valor.
- `MAX_TOOL_ROUNDS = 5` por agente, com resposta final de fallback ao atingir o limite.
- `ConversationHistory.trimToRecentHistory` corta no primeiro turno `role == "user"` a partir do
  corte por contagem, nunca no meio de um turno.
- Limpar `AiSession.history` ao detectar logout (`MainViewModel`) e ao mudar o `shoppingId` do chat
  (`AiChatViewModel.setShoppingContext`).
- `AiChatViewModel` passa a mostrar uma mensagem de erro (string de `feature:chat`, pt-BR/en-US)
  quando a cadeia inteira de agentes falha — hoje esse caminho é silencioso (`AiChatViewModel.kt:79-82`
  só esconde o spinner).
- Remover `AiErrorMessages.kt`/`AiErrorMessagesTest.kt` (a responsabilidade migra para
  `AiChatViewModel`, camada de apresentação).

**Exclui:**
- Remoção de tools (B-15, B-16 ficam para `sdd-007-ai-chat-gate`).
- Externalizar `SystemPrompts.kt` para `strings.xml`/multilíngue e qualquer outro texto já
  hardcoded pt-BR fora do que esta tarefa toca — isso é o item **F-07**
  (`specs/sdd-001-auditoria-geral/tasks.md:90`), uma SDD própria, não esta.
- A mensagem de "atingi o limite de tentativas" (novo texto do `MAX_TOOL_ROUNDS`) fica como
  literal pt-BR no agente, no mesmo padrão (e mesma dívida, rastreada por F-07) de
  `SystemPrompts.kt` — não cria um novo módulo de recursos Android em `:feature:ai-agent` só para
  essa string. Decisão registrada para não ficar implícita (ver §9).
- `AiAgentOrchestrator` **não é usado em produção** (ver §3) e não será religado por esta tarefa —
  fica registrado como limpeza fora de escopo (ver §8/§9).
- Acesso remoto ao prompt (`ai_system_prompt`) — já existe, não alterado aqui.

## 3. Impacto Arquitetural

### Correções em relação ao plano anterior (achados desta revisão)

1. **`AiAgentOrchestrator` é código morto.** `grep -rl "AiAgentOrchestrator"` no repositório só
   retorna o próprio arquivo (`feature/ai-agent/.../domain/orchestrator/AiAgentOrchestrator.kt`).
   O caminho real de produção é `CartAssistantUseCase.kt:21-29` → `AiAgentFactory.create()` →
   `AiAgent.sendMessage()`, sem passar pelo orquestrador. O plano anterior propunha limpar o
   histórico ali (e citava `ShoppingDetailsViewModel`, que nem referencia IA) — isso não teria
   efeito nenhum em produção. Os pontos de limpeza corretos são os que a UI real usa: `MainViewModel`
   (logout) e `AiChatViewModel.setShoppingContext` (troca de lista) — ambos os módulos (`:app`,
   `:feature:chat`) já dependem de `:core:ai`, onde `AiSession` é público.
2. **Corrigir B-07 isoladamente neutraliza B-08.** `AppSettings.aiProvider` tem default
   `"openrouter"` (`AppSettings.kt:10`; `SettingsRepositoryImpl.kt:64`) e nunca é `""`/`"auto"` —
   `updateAiSettings` sempre grava um provider concreto (`SettingsRepositoryImpl.kt:94-95`). Depois
   de separar `vendor`/`provider`, **todo** modelo em `AiModel.freeModels` passa a gravar
   `provider = "openrouter"`, então `AiAgentFactoryImpl.selectAgent` sempre cairia no branch
   `"openrouter" -> openRouter ?: gemini ?: NoAiProviderAvailableAgent`
   (`AiAgentFactoryImpl.kt:84-94`) — um Elvis que só cobre *feature flag desligada*, não falha de
   transporte. O branch `else` (que hoje, por acidente do bug B-07, é o único a envolver
   `FallbackAiAgent`) deixaria de ser alcançado por qualquer usuário real. `selectAgent` precisa
   ser reescrito para envolver em `FallbackAiAgent` sempre que os dois provedores estiverem
   disponíveis, independentemente do provider ser explícito ou não.
3. **`AiErrorMessages.kt` fica sem chamador.** Hoje ela só existe porque `GeminiAiAgent.kt:120` e
   `OpenRouterAiAgent.kt:141` capturam a exceção e **emitem** o texto amigável como se fosse a
   resposta do modelo — exatamente o que impede o fallback (achado 2 do backlog, B-08). Ao corrigir
   isso (agentes voltam a propagar a exceção), não há mais chamador para
   `aiErrorMessageFor`. A exceção passa a subir por `FallbackAiAgent` (se houver um secundário) e,
   se também falhar, por `CartAssistantUseCase` até `AiChatViewModel`, que hoje a descarta
   silenciosamente (`AiChatViewModel.kt:79-82`: `catch (e: Exception) { ...isLoading = false }`,
   sem mensagem). Essa tela é o único lugar que já tem `strings.xml` pt-BR/en-US para o chat
   (`feature/chat/src/main/res/values/strings.xml`, `values-pt-rBR/strings.xml`) — é o lugar certo
   (Clean Architecture: texto de usuário pertence a `presentation/`) para mostrar o erro,
   substituindo a conversão feita hoje na camada `data/service`.

### Diagrama de componentes

```
:core:domain
  └── model/AiModel.kt
      ├── vendor: String       [novo — label de UI, ex. "NVIDIA"]
      ├── provider: String     [novo — "openrouter" | "gemini", roteamento]
      └── isFree: Boolean      [inalterado]

:core:data
  └── repository/SettingsRepositoryImpl.kt
      └── updateAiSettings(): usa AiModel.provider (não mais .provider como vendor)
      └── getSettings(): migra AI_PROVIDER legado (vendor salvo) -> provider técnico, idempotente

:feature:settings/presentation
  ├── screen/AiSettingsScreen.kt:109      -> troca aiModel.provider por aiModel.vendor na label
  └── viewmodel/SettingsViewModel.kt:205  -> sem mudança de contrato, só nome do campo lido

:feature:ai-agent/data/service
  ├── AiAgentFactoryImpl.kt
  │   └── selectAgent(): sempre FallbackAiAgent(primary, secondary) quando os dois existem,
  │       inclusive com provider explícito (corrige achado 2 acima)
  ├── GeminiAiAgent.kt
  │   ├── MAX_TOOL_ROUNDS = 5 (companion object)
  │   ├── executeFunctionCall(): Result.fold em vez de getOrNull() ?: "Sucesso"
  │   ├── catch topo de sendMessage: relança (loga + crashReporter, depois `throw e`),
  │   │   CancellationException relançada antes de tudo (já correto hoje, mantém)
  │   └── corte de histórico via ConversationHistory corrigido (turn-boundary)
  ├── OpenRouterAiAgent.kt
  │   ├── mesmo que acima
  │   └── CancellationException relançada em sendMessage E executeToolCall (hoje só Gemini faz isso)
  ├── FallbackAiAgent.kt            [sem mudança — já correto, ver FallbackAiAgentTest.kt]
  ├── ConversationHistory.kt
  │   └── trimToRecentHistory(): corta no primeiro índice com role == "user" >= corte por contagem
  └── AiErrorMessages.kt / AiErrorMessagesTest.kt   [removidos]

:core:ai
  └── contract/AiSession.kt
      └── fun clearHistory()   [novo, public — ver nota de implementação abaixo]

:feature:chat/presentation/viewmodel
  └── AiChatViewModel.kt
      ├── injeta AiSession (já público em :core:ai, módulo já depende dele)
      ├── setShoppingContext(shoppingId): session.clearHistory() quando shoppingId muda de fato
      └── sendMessage(): catch do fluxo completo agora anexa ChatMessage(ASSISTANT, texto de erro)
          usando novas strings em feature/chat/src/main/res/values{,-pt-rBR}/strings.xml
          (IOException -> mensagem de conectividade; outros -> mensagem genérica)

:app
  └── MainViewModel.kt
      ├── injeta AiSession
      └── signOutWatchOnSignOut(): ao detectar !signedIn, também session.clearHistory()
```

> **Nota de implementação (registrada após o fato, por correção do code-reviewer — AJUSTES
> NECESSARIOS, achado bloqueante #2):** a versão original deste plano previa `session.history.clear()`
> direto em `AiChatViewModel`/`MainViewModel`. Isso não compila: `AiSession.history` é
> `MutableList<Content>`, e `Content` é do SDK do Gemini (`com.google.ai.client.generativeai`),
> que `core:ai` só declara como `implementation` (`core/ai/build.gradle.kts:27`) — não chega ao
> classpath de `:feature:chat`/`:app`, que nunca precisaram desse SDK. A implementação adicionou
> `AiSession.clearHistory()` (`core/ai/.../AiSession.kt:12-19`) exatamente para isso: um método
> público que não expõe o tipo `Content`, implementado em `AiAgentSession.clearHistory()`
> (`feature/ai-agent/.../AiAgentSession.kt:17-19`). Esta seção e a §4 abaixo foram atualizadas
> para refletir o contrato real.

### Injeção/Hilt

- `AiSession` já é `@Binds @Singleton` em `AiAgentModule.kt:25-27` (instalado em
  `SingletonComponent`), então é injetável em `:app` e `:feature:chat` sem novo módulo — ambos já
  declaram `implementation(project(":core:ai"))` (`app/build.gradle.kts:117`,
  `feature/chat/build.gradle.kts:15`).
- Nenhum novo módulo Hilt necessário.

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Justificativa / Notas |
|---|---|---|---|
| `AiModel.vendor: String` | Novo campo | `public` (`core:domain`, já é `data class` pública) | Consumido por `AiSettingsScreen` e `SettingsViewModel` (outro módulo) |
| `AiModel.provider: String` | Passa a valer sempre `"openrouter"` (todo `freeModels` é hospedado via OpenRouter hoje) | `public` | Consumido por `SettingsRepositoryImpl` (outro módulo) para roteamento |
| `AiAgentFactoryImpl.selectAgent` | Assinatura igual, comportamento interno corrigido | `internal` (já é) | Sem impacto de contrato público |
| `GeminiAiAgent.sendMessage` / `OpenRouterAiAgent.sendMessage` | Comportamento: propaga exceção de transporte em vez de emitir texto | `internal` (classes já são `internal`) | `AiAgent.sendMessage(): Flow<String>` (contrato em `:core:ai`) não muda de assinatura, só a semântica de "quando falha" |
| `ConversationHistory.trimToRecentHistory` | Assinatura igual, algoritmo corrigido | `internal` (já é) | — |
| `AiErrorMessages.kt` | Removido | — | Sem chamador após a correção (ver §3) |
| `AiSession.clearHistory()` | Novo método | `public` (`core:ai`, interface já pública entre `:core:ai` e `:feature:ai-agent`) | Registrado após a implementação (ver nota em §3): `history` expõe `Content` (SDK do Gemini, só `implementation` em `:core:ai`), inacessível de `:feature:chat`/`:app`; `clearHistory()` limpa sem expor o tipo |
| `AiChatViewModel` | Novo parâmetro de construtor `AiSession` | `internal`/classe de ViewModel (Hilt resolve) | Necessário para limpar histórico por `shoppingId` |
| `MainViewModel` | Novo parâmetro de construtor `AiSession` | `internal` | Necessário para limpar histórico no logout |
| Strings `ai_chat_error_generic`, `ai_chat_error_network` | Novas em `feature/chat` `strings.xml` (`values` = en-US, `values-pt-rBR`) | recurso Android, não é contrato Kotlin | Seguindo convenção já usada no módulo (`ai_chat_title`, etc.) |

Nenhuma mudança é **breaking**: `AiModel.vendor` é aditivo (o campo antigo `provider` continua
existindo, só muda o que ele contém); nenhuma função pública de outro módulo muda assinatura.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | `AiModel.provider` guarda o fabricante exibido na UI; `updateAiSettings` deriva um "provider" de roteamento a partir disso, quase sempre incorreto | `AiModel.vendor` = label de UI; `AiModel.provider` = `"openrouter"` para todo `freeModels` atual; `updateAiSettings` grava `AiModel.provider` direto | `AiModel.kt:3-23`; `SettingsRepositoryImpl.kt:94-95`; `AiSettingsScreen.kt:109` |
| 2 | `AiAgentFactoryImpl.selectAgent` só envolve `FallbackAiAgent` quando `aiProvider` não é `"gemini"`/`"openrouter"` (hoje, por acidente do bug 1) | `selectAgent` envolve `FallbackAiAgent(primary, secondary)` sempre que os dois provedores existem, inclusive com provider explícito | `AiAgentFactoryImpl.kt:84-94` |
| 3 | `GeminiAiAgent`/`OpenRouterAiAgent` capturam erro de transporte e **emitem** texto amigável — `FallbackAiAgent.catch` nunca dispara | Agentes relançam a exceção (depois de logar/`crashReporter`); só o ponto mais externo (`AiChatViewModel`) converte em texto para o usuário | `GeminiAiAgent.kt:110-121`; `OpenRouterAiAgent.kt:132-143`; `FallbackAiAgent.kt:21-25` |
| 4 | Falha de tool call (`Result.failure`) é reportada ao modelo como `"Sucesso"`; sucesso `Unit` como `"kotlin.Unit"` | `Result.failure` -> `"Erro: <mensagem>"`; `Result.success(Unit)` -> `"OK"`; outro sucesso -> `toString()` do valor | `GeminiAiAgent.kt:138-142`; `OpenRouterAiAgent.kt:180-181` |
| 5 | Loop de tool calls sem limite | `MAX_TOOL_ROUNDS = 5`; ao atingir, emite resposta final de fallback e encerra o turno | `GeminiAiAgent.kt:92-100`; `OpenRouterAiAgent.kt:104-122` |
| 6 | `OpenRouterAiAgent` captura `CancellationException` como `Exception` comum, em `sendMessage` e `executeToolCall` | Relançada antes de qualquer log/crashReporter, nos dois pontos (`GeminiAiAgent` já faz isso em `sendMessage`, mas não documentava `executeFunctionCall` — revisar também lá) | `OpenRouterAiAgent.kt:132,143,182`; `GeminiAiAgent.kt:110-111,143-144` (já ok) |
| 7 | Corte de histórico por contagem fixa pode abrir em `role != "user"` ou separar `functionCall`/`functionResponse` | Corte avança até o primeiro `role == "user"` a partir do índice de corte | `ConversationHistory.kt:14-19` |
| 8 | `AiSession` é `@Singleton`, histórico nunca limpo; `AiAgentOrchestrator.clearHistory()` existe mas não tem chamador (código morto) | Limpo em `MainViewModel` ao detectar logout e em `AiChatViewModel.setShoppingContext` ao trocar `shoppingId` | `AiAgentModule.kt:25-27`; `AiAgentOrchestrator.kt:36-38` (sem chamadores, grep confirmado); `MainViewModel.kt:50-59`; `AiChatViewModel.kt:40-48` |
| 9 *(novo, não numerado no backlog — efeito colateral positivo)* | Quando os dois provedores estão desligados por feature flag, `NoAiProviderAvailableAgent` lança e `AiChatViewModel` hoje só esconde o spinner, sem feedback | Mesma mensagem genérica de erro (regra 3) também cobre este caso, já que a exceção sobe pelo mesmo caminho | `NoAiProviderAvailableAgent.kt:13-15`; `AiChatViewModel.kt:67-83` |

## 6. Critérios de Aceitação

- **`vendor` vs `provider`:** todo item de `AiModel.freeModels` tem `vendor` (label atual) e
  `provider = "openrouter"`. `AiSettingsScreen.kt:109` exibe `vendor`.
- **Migração:** valor salvo de `AI_PROVIDER` (ex. `"nvidia"`, `"google"`) é migrado para o
  `provider` técnico do modelo correspondente em `AiModel.freeModels` na leitura de
  `getSettings()`; idempotente (reler duas vezes dá o mesmo resultado); se o modelo salvo não for
  encontrado, usa `"openrouter"` (mesmo default de hoje).
- **Fallback:** com os dois provedores habilitados, uma falha de transporte no primário (explícito
  ou automático) resulta em resposta do secundário, não em mensagem de erro — exceto se o primário
  já executou uma tool nesta rodada (evitar efeito colateral duplicado, regra já proposta no plano
  original e mantida).
- **Tool feedback:** `Result.failure(Exception("not found"))` -> contém `"Erro: not found"`.
  `Result.success(Unit)` -> `"OK"`. `Result.success(123)` -> contém `"123"`.
- **MAX_TOOL_ROUNDS:** com 6 chamadas de tool em sequência, o agente para na rodada 5 e emite a
  mensagem de fallback, sem nunca enviar uma 6ª requisição.
- **CancellationException:** cancelar a coroutine durante `sendMessage` ou durante a execução de
  uma tool, em qualquer agente, relança `CancellationException` sem logar no Crashlytics como erro
  não-fatal.
- **Histórico — privacidade:** logout limpa `AiSession.history` (verificável via
  `MainViewModel` + fake `AuthService`). Trocar de lista no chat (`setShoppingContext` com um
  `shoppingId` diferente do atual) também limpa.
- **Corte de histórico:** após o corte, `history.first().role == "user"` sempre, mesmo com
  `functionCall`/`functionResponse` no meio da janela.
- **Erro visível ao usuário:** se o `FallbackAiAgent` (ou agente único) falhar por completo, o chat
  mostra uma bolha do assistente com a mensagem de erro (rede ou genérica) em vez de só esconder o
  spinner.

## 7. Plano de Testes

**Unitários (novos ou alterados):**
- `AiModelTest` (novo): cada item de `freeModels` tem `provider == "openrouter"`; `vendor` não é
  vazio.
- `SettingsRepositoryImplTest`: migração de `AI_PROVIDER` legado (`"nvidia"`, `"google"`, valor
  desconhecido) -> provider técnico correto; idempotência.
- `AiAgentFactoryImplTest` (atualizar): os testes de `131,141,151,161,171,181,275` assumem que
  `create()` retorna o tipo de agente "nu" (`GeminiAiAgent`, `OpenRouterAiAgent`,
  `NoAiProviderAvailableAgent`) — isso muda, pois agora o retorno é sempre envolto em
  `FallbackAiAgent` quando os dois provedores existem. Reescrever essas asserções para verificar
  comportamento (ex.: stub de rede) em vez de tipo concreto, ou expor um hook de teste. O teste da
  linha 275 (`"create keeps BuildConfig models in auto mode..."`) descreve o bug B-07 e deve ser
  removido/invertido.
- `GeminiAiAgentGatingTest` / `OpenRouterAiAgentGatingTest` (estender): `executeFunctionCall`/`executeToolCall`
  com `Result.failure` -> `"Erro: ..."`; com `Result.success(Unit)` -> `"OK"`.
- `GeminiAiAgentTest` / `OpenRouterAiAgentTest` (novo, nível `sendMessage`): transporte lança ->
  `CancellationException` relançada sem log; outra exceção -> relançada após
  `crashReporter.recordException`. Loop de tool call com 6 iterações -> para na 5ª.
- `FallbackAiAgentTest`: já cobre o caso correto — manter, sem alteração necessária.
- `ConversationHistoryTest` (estender): histórico com `functionCall`/`model`/`tool` nas posições
  que cairiam no corte ingênuo -> corte real começa no primeiro `role == "user"` seguinte, nunca
  no meio.
- `AiChatViewModelTest` (estender): `assistantUseCase` lança após fallback esgotado -> UI state
  ganha uma `ChatMessage(sender = ASSISTANT)` com o texto de erro esperado; `setShoppingContext`
  com `shoppingId` novo -> `AiSession.history` limpo (mock/fake).
- `MainViewModelTest` (estender): transição `signedIn -> null` em `authState` -> `AiSession.history`
  limpo (mock/fake).
- Remover `AiErrorMessagesTest.kt` (classe removida).

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Testes existentes de `AiAgentFactoryImplTest` (linhas 131-181, 275) quebram ao mudar `selectAgent` | alta | baixo (esperado, é o objetivo da mudança) | Reescrever junto com a implementação, não depois |
| Mudar o catch de `Gemini`/`OpenRouterAiAgent` de "emitir texto" para "relançar" muda o que `AiChatViewModel` precisa fazer — se a UI não for atualizada junto, o usuário passa a ver o chat "travado" sem nenhuma mensagem | média | alto (regressão de UX) | Implementar a mudança de `AiChatViewModel` (§3) **no mesmo PR**, não depois |
| Cortar histórico no primeiro `role == "user"` pode, em casos extremos (muitas tool calls seguidas sem resposta final), devolver uma janela maior que `maxEntries` | baixa | baixo | Aceitável — histórico levemente maior é preferível a request malformado; documentar no teste |
| Migração de `AI_PROVIDER` muda o modelo efetivo de quem já usa o app | média | baixo | Migração idempotente; nenhum evento de analytics novo é requerido por este SDD (fora de escopo) |
| `FallbackAiAgent` sempre envolvendo o provider explícito pode mudar o modelo que responde quando o usuário escolheu um provider específico (ele pode acabar recebendo resposta do outro provider/modelo em caso de falha) | baixa | médio | É o comportamento desejado (resiliência > fidelidade estrita ao modelo escolhido durante uma falha de transporte); comunicar na mensagem de erro removida — não há mudança de texto do modelo que precise de aviso explícito, pois já é assim hoje para o caminho "auto" |

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma bloqueante. Decisões não-triviais tomadas nesta revisão (não bloqueantes, mas registradas
para revisão do maintainer):

- [ ] Confirmar que a mensagem de "limite de tentativas de tool call atingido" pode ficar como
  literal pt-BR no agente (mesmo padrão/dívida de `SystemPrompts.kt`, rastreada por F-07) em vez de
  ganhar `strings.xml` próprio em `:feature:ai-agent` (módulo hoje sem `res/`). Risco se a resposta
  for "não": aumenta o escopo desta tarefa com um novo módulo de recursos Android.
- [ ] Confirmar que não religar `AiAgentOrchestrator` ao fluxo real (ele é código morto hoje) está
  correto para este SDD — a alternativa seria apagá-lo nesta mesma tarefa; manter fora de escopo
  evita um diff maior que o necessário para a confiabilidade do agente.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (verificado em `AiModel.kt`, `AppSettings.kt`,
  `SettingsRepositoryImpl.kt`, `AiAgentFactoryImpl.kt`, `GeminiAiAgent.kt`, `OpenRouterAiAgent.kt`,
  `FallbackAiAgent.kt`, `ConversationHistory.kt`, `AiAgentModule.kt`, `AiAgentOrchestrator.kt`,
  `CartAssistantUseCase.kt`, `AiChatViewModel.kt`, `MainViewModel.kt`, `AiSettingsScreen.kt`).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas/diagramas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Nenhuma mudança quebra contrato público (todas aditivas).
- [x] Nenhum novo endpoint/remote-config/cache introduzido.
- [x] Perguntas em aberto exauridas (nenhuma bloqueante; duas decisões de escopo documentadas para
  revisão, não bloqueiam o início da implementação).
