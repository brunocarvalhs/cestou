# Task: Card de aprovação para ações irreversíveis da IA e execução em segundo plano com notificação

- **Slug:** card-aprovacao-acoes-ia
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0 (mesmo ciclo do gate do sdd-032, ainda não lançado)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive em relação à última versão lançada (2.2.1): `AiSession` troca os membros de confirmação por turno (criados e ainda não lançados) por um pedido de aprovação; `AgentAction` ganha um membro com default

## 1. Contexto e Motivação
- Pedido do usuário: "caso a IA precise modificar algo, deve pedir permissão para casos extremos, aí um card perguntando se recusa ou aprova".
- Pedido adicional do usuário: "a IA deve executar em segundo plano, se o app fechar ou estiver em outra tela, e criar uma notificação local que informa que foi executado o que foi pedido, e ao abrir a notificação abre o chat".
- Decisões do usuário: o card vale **só para ações irreversíveis** (as que já têm `requiresConfirmation`: apagar lista, apagar várias listas e limpar comprados), e **Aprovar executa a ação na hora**, sem depender de o modelo repetir a chamada.
- Hoje o gate é por texto: a primeira chamada não executa e devolve `CONFIRMATION_REQUIRED_MESSAGE` (`BaseAgent.kt:63-68`); a IA pergunta no chat e só uma chamada igual em turno posterior executa (`isApprovedByUser`, `BaseAgent.kt:110-115`, e `AgentSession.consumeConfirmation`). Pontos fracos: qualquer mensagem do usuário arma a execução (até um "não") e depende de a IA obedecer.
- O dispatch passa por um único ponto (`BaseAgent.executeAction`, `BaseAgent.kt:51`), que já aplica o gate Pro, a telemetria e o tratamento de erro. O loop de tool-calling roda no turno desacoplado (`ChatTurnTracker`, sdd-035), então um `await` ali não segura nenhuma requisição HTTP aberta: o loop executa a ação entre duas chamadas ao provedor.
- Decisões do usuário sobre a notificação: o texto é a **mensagem da própria IA**; só envia se as notificações estiverem permitidas no app; e o app **pede a permissão de notificações ao ser aberto**.
- Hoje o app nunca pede `POST_NOTIFICATIONS` (só há pedido de câmera, em `QrCodeScanner.kt:43`); o worker de lembretes apenas ignora o envio quando falta a permissão (`ShoppingReminderWorker.kt:75-78`). O interruptor "Ativar notificações" das configurações já existe (`AppSettings.notificationsEnabled`).
- Hoje o turno da IA já roda fora da tela, no `@ChatBackgroundScope` (`ChatModule.kt`, sdd-035): sobrevive a fechar o chat e a trocar de tela. **Não** sobrevive ao app ser encerrado: é uma coroutine do processo, e o Android encerra processos em segundo plano. Não existe serviço em primeiro plano nem `ProcessLifecycleOwner` no projeto; existe canal de notificação e a permissão `POST_NOTIFICATIONS` (`ShoppingReminderWorker.kt:63-94`, `AndroidManifest.xml:11`).
- Deep link atual: só convite de lista, por `ACTION_VIEW` + `data` (`MainViewModel.onDeepLink`, `MainViewModel.kt:181`), guardado em `SavedStateHandle` e consumido uma vez.
- Rastreabilidade: `AgentAction.kt:23` (`requiresConfirmation`), `AiSession.kt` (`requestConfirmation`/`consumeConfirmation`/`beginTurn`), `AgentSession.kt:64-75`, `AiChatViewModel.kt` (coleta de `aiSession.activeAction`), `ShoppingDeleteUseCase.kt:26`, `ShoppingMultiDeleteUseCase.kt:30`, `ShoppingClearPurchasedUseCaseImpl.kt:28`.

## 2. Escopo
- **Inclui:**
  - Card no chat, dentro da lista de mensagens, com título, resumo do que será afetado e os botões Recusar e Aprovar.
  - A ação espera a decisão dentro do dispatch: aprovada executa pelo mesmo chokepoint; recusada devolve ao modelo que nada foi feito.
  - Recusa automática por tempo esgotado e ao fechar o chat com um pedido pendente.
  - Resumo legível por ação (nome da lista), sem expor ids.
  - **Execução em segundo plano:** um serviço em primeiro plano (`AiTurnService`, tipo `shortService`) mantém o processo vivo enquanto houver turno da IA em andamento, com uma notificação contínua "A IA está trabalhando no seu pedido". É iniciado quando o turno começa (o app está em primeiro plano) e para quando não há turno pendente.
  - **Permissão de notificações:** ao abrir o app (primeira exibição da Home depois do login, uma vez por instalação) o app pede `POST_NOTIFICATIONS` no Android 13+. Isso também passa a habilitar os lembretes de compras, hoje mudos para quem nunca concedeu.
  - **Notificação de resultado:** ao terminar, se o usuário não está olhando o chat (chat fechado ou app em segundo plano), uma notificação local mostra a mensagem da própria IA (o que foi executado) e, ao tocar, abre o chat naquela conversa. Só é enviada se o interruptor de notificações do app estiver ligado, a permissão concedida e o canal não bloqueado.
  - **Aprovação em segundo plano:** com o app fora de vista, uma notificação "A IA precisa da sua aprovação" abre o chat, onde está o card.
  - Prompt do sistema atualizado (não pedir confirmação por texto), analytics sem dados pessoais, strings pt/en/es, `testTag`s, testes.
- **Exclui:**
  - Ações não irreversíveis (criar, editar), que seguem sem card.
  - Aprovar "para sempre" ou lembrar a escolha.
  - Botões de Aprovar e Recusar dentro da notificação (a decisão acontece no card, com o contexto à vista) e persistência do card no histórico.
  - Sobreviver a o usuário forçar a parada do app ou o sistema matar o processo (melhor esforço; ver riscos).
  - Validação por Maestro do fluxo completo (depende de a IA decidir apagar algo).

## 3. Impacto Arquitetural
- Módulos e camadas: `:core:ai` (contrato), `:feature:ai-agent` (`data/service`), `:feature:chat` (`presentation`), `:feature:shopping` e `:feature:products` (use cases de ação), `:core:analytics` (constantes). Features não dependem entre si; o canal é o `AiSession` singleton de `core:ai`.
- Novos arquivos:
  - `core/ai/.../model/ApprovalRequest.kt`
  - `feature/chat/.../presentation/component/ApprovalCard.kt`
  - `feature/chat/.../presentation/helper/ApprovalTexts.kt` (título por ação)
  - testes correspondentes (§7)

```
 modelo pede apagar ──► BaseAgent.executeAction
                          │ requiresConfirmation
                          ▼
                 session.awaitApproval(request) ◄── suspende (timeout 2 min)
                          │ publica pendingApproval (StateFlow)
                          ▼
   AiChatViewModel coleta ──► state.pendingApproval ──► ApprovalCard [Recusar] [Aprovar]
                                                            │ onApprove / onReject
                          ┌───────── session.resolveApproval(bool) ◄─┘
                          ▼
              aprovado: action.execute (mesmo chokepoint, Pro, telemetria, erro)
              recusado/tempo/fechou: "o usuário recusou; nada foi executado" ao modelo
```
- Sem DI nova para o card: o ViewModel já recebe `AiSession`; o `ChatGraph` já chama `onClose` ao sair do dialog.
- Segundo plano e notificação (todos em `:feature:chat`, `internal`):
  - `feature/chat/.../data/service/AiTurnService.kt` (serviço em primeiro plano, declarado no manifest do módulo)
  - `feature/chat/.../data/notification/AiNotifier.kt` (canal `ai_actions`, notificação contínua, de resultado e de aprovação)
  - `feature/chat/.../data/service/ChatVisibilityTracker.kt` (singleton: o chat está visível e o app em primeiro plano?)
  - `ChatTurnTracker.inFlight` passa a ser observado por um `AiTurnServiceController` que liga e desliga o serviço
  - o `ChatGraph` marca a visibilidade; o `MainViewModel`/`MainActivity` leem o extra do chat no `Intent` e navegam para `AiChat(sessionId)`, como já fazem com o convite

```
 turno começa ─► ChatTurnTracker.begin ─► AiTurnServiceController liga AiTurnService (notificação contínua)
 turno termina ─► ChatTurnTracker.end  ─► serviço para
        │ resultado: texto da última resposta da IA
        ▼
 ChatVisibilityTracker.isUserWatching?  ── sim ─► nada (a resposta já está na tela)
        └── não ─► AiNotifier.postResult(sessionId, texto)  ──toque──► MainActivity(extra sessionId)
                                                                        └► Navigator → AiChat(sessionId) (histórico já gravado)
```

## 4. Contratos e Interfaces
Assinaturas (sem implementação):

```kotlin
// core:ai, public: consumido por feature:ai-agent (quem pede) e feature:chat (quem decide)
data class ApprovalRequest(
    val actionId: String,
    val summary: String?     // ex.: título da lista; dado do usuário, sem tradução
)

interface AiSession {
    /** Pedido de aprovação aberto, ou `null`. */
    val pendingApproval: StateFlow<ApprovalRequest?>

    /**
     * Publica [request] e suspende até [resolveApproval] ou o fim de [timeoutMillis].
     * `true` só se o usuário aprovou; recusa, tempo esgotado e cancelamento devolvem `false`
     * e sempre limpam o pedido.
     */
    suspend fun awaitApproval(request: ApprovalRequest, timeoutMillis: Long = APPROVAL_TIMEOUT_MS): Boolean

    fun resolveApproval(approved: Boolean)
}

interface AgentAction<R> {
    /** Resumo do que a ação vai afetar, mostrado no card; `null` usa só o título genérico. */
    suspend fun confirmationSummary(arguments: Map<String, Any?>): String? = null
}
```
```kotlin
// feature:chat, internal
internal interface ChatVisibilityTracker {
    /** Verdadeiro com o chat composto e o app em primeiro plano (RESUMED). */
    val isUserWatching: StateFlow<Boolean>
    fun setChatVisible(visible: Boolean)
}
internal class AiNotifier {
    fun postRunning()                                   // contínua, do serviço
    fun postResult(sessionId: String, summary: String)  // toque abre o chat
    fun postApprovalNeeded(sessionId: String)           // toque abre o chat com o card
}
```
- Removidos do `AiSession`: `beginTurn`, `requestConfirmation`, `consumeConfirmation` e o relógio de turnos em `AgentSession` (criados no mesmo ciclo, nunca lançados; ver CHANGELOG `[Unreleased]`).
- `public` justificado: três módulos consomem. Os use cases de apagar listas implementam `confirmationSummary` (consulta o título pelo repositório).
- `AiChatUiState.pendingApproval: ApprovalRequest?`; `AiChatIntent.onApprove` e `onReject`, ambos rastreados no ViewModel.
- Analytics (constantes): `ai_tool_confirmation_requested` (já existe, passa a sair quando o card aparece) e `ai_tool_confirmation_resolved` com `action_id` e `decision` (`approved`, `rejected`, `timeout`, `dismissed`). Nunca o resumo nem argumentos.
- Remote-config: reaproveita os toggles existentes; sem flag nova.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Ação irreversível só roda na chamada seguinte, depois de qualquer mensagem do usuário | Roda só depois de Aprovar no card | `BaseAgent.kt:110-115` |
| 2 | A IA pergunta a confirmação por texto | A IA chama a ação e espera; o card faz a pergunta | `BaseAgent.kt:139` |
| 3 | O modelo recebe `NÃO EXECUTADO` e precisa repetir a chamada | Aprovar executa pelo mesmo chokepoint e devolve o resultado real; Recusar devolve que o usuário recusou | `BaseAgent.kt:63-68` |
| 4 | Gate Pro vem antes da confirmação | Mantido: sem Pro nem se abre o card | `BaseAgent.kt:57-62` |
| 5 | O turno da IA sobrevive ao fechamento do chat | Fechar o chat com card aberto recusa o pedido (`dismissed`), para o turno não ficar preso | `ChatGraph.kt:43-45` |
| 6 | Não existe limite de espera | 2 minutos sem resposta recusam (`timeout`) | novo |
| 7 | Logout cancela os turnos pendentes | O cancelamento do turno também limpa o pedido aberto | `MainViewModel.kt` |
| 8 | Prompt orienta confirmar por texto | Prompt orienta chamar a ação e aguardar o card | `SystemPrompts.kt` |
| 9 | O turno vive só enquanto o processo vive | Enquanto houver turno em andamento, o serviço em primeiro plano mantém o processo; ao terminar, o serviço para | `ChatModule.kt` (`@ChatBackgroundScope`) |
| 10 | O resultado só aparece dentro do chat | Com o usuário fora do chat, uma notificação local informa que o pedido foi executado; tocar abre a conversa | `MainViewModel.kt:181` (padrão de deep link) |
| 11 | A resposta já está gravada no histórico ao fim do turno | A notificação abre `AiChat(sessionId)`, que recarrega o histórico gravado | `AiChatViewModel` (`loadSession`) |
| 12 | O app não pede a permissão de notificações | Pede ao abrir (Home depois do login, uma vez por instalação); negada, nada é enviado e o turno segue normal | `QrCodeScanner.kt:43` (único pedido atual: câmera) |
| 13 | A notificação pode aparecer na tela bloqueada | O texto fica privado na tela bloqueada (versão pública genérica); sem notificar se o usuário está olhando o chat | novo |

## 6. Critérios de Aceitação
- Ao pedir "apague a lista X", aparece um card com o título da ação, o nome da lista e os botões Recusar e Aprovar; nada é apagado antes de Aprovar.
- Aprovar apaga na hora, o card some e a IA responde com o resultado real; Recusar fecha o card e a IA diz que não fez nada.
- Sem resposta em 2 minutos o pedido é recusado e o chat volta ao normal; fechar o chat com card aberto recusa o pedido e libera o turno (nenhum chat novo fica esperando).
- Só um card por vez; ações seguidas na mesma resposta abrem um card por ação.
- Botão Aprovar de ação destrutiva usa a cor de erro; alvos de toque de 48 dp; leitor de tela anuncia título e resumo.
- Performance: sem polling (`StateFlow` + `Deferred`), sem I/O além da consulta do título, sem coroutine extra além da já existente no turno.
- **Segundo plano:** com o chat fechado ou o app em outra tela, o pedido continua e termina; a notificação de resultado chega e, tocada, abre o chat na conversa com a resposta. Com o app encerrado pelo usuário (deslizar em recentes) o serviço mantém o turno em melhor esforço.
- **Notificação:** só aparece se o usuário não está olhando o chat **e** as notificações estão permitidas no app (interruptor ligado, `POST_NOTIFICATIONS` concedida, canal `ai_actions` não bloqueado); o texto é a mensagem da IA, truncada; sem permissão não notifica e o turno segue normalmente; a notificação contínua do serviço some quando o turno acaba.
- **Permissão:** o pedido aparece uma vez por instalação, na primeira Home depois do login, no Android 13+; abaixo disso nada é pedido; negar não bloqueia nenhuma função.
- Observabilidade: eventos por constantes, sem argumentos nem nomes; callbacks do intent rastreados.
- Compatibilidade: minSdk 28; sem migração de dados; textos em pt-BR, en e es.

## 7. Plano de Testes
- **Unitários:**
  - `AgentSession`: `awaitApproval` devolve `true` ao aprovar, `false` ao recusar, `false` no timeout (relógio virtual) e `false` no cancelamento; sempre limpa o pedido; um pedido por vez.
  - `BaseAgent`/gating (`GeminiAgentGatingTest` e similares): aprovada executa e conta o tool-call; recusada, timeout e cancelamento não executam e devolvem a mensagem de recusa; Pro bloqueia antes do card; ação sem confirmação não pede nada.
  - Use cases de apagar: `confirmationSummary` devolve o título da lista e `null` quando a lista não existe.
  - `AiChatViewModel`: coleta `pendingApproval` para o estado; `onApprove` e `onReject` resolvem o pedido e emitem o evento; `onClose` com pedido aberto recusa com `dismissed`.
  - `ApprovalTexts`: título por ação conhecida e fallback genérico.
  - `ChatVisibilityTracker`: visível só com chat composto e app em primeiro plano.
  - `AiNotifier`/decisão de notificar: notifica com chat fechado ou app em segundo plano; não notifica com o chat à vista, nem com o interruptor desligado, nem sem permissão; o texto é a mensagem da IA e a tela bloqueada mostra a versão genérica.
  - Pedido da permissão: feito uma vez por instalação, só no Android 13+, não repete depois de negar e respeita a concessão prévia.
  - Controlador do serviço: liga ao primeiro turno, desliga quando `inFlight` esvazia, não liga duas vezes.
  - Intent da notificação: extra do chat vira `AiChat(sessionId)` uma única vez (mesmo padrão do convite).
- **UI (Robolectric, `testTag`):** `ai_approval_card`, `ai_approval_approve`, `ai_approval_reject`; card some após decidir; resumo exibido.
- **Arquitetura:** `ArchitectureTest` (intents rastreados, constantes de analytics).
- **Comandos:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| O turno fica preso esperando e bloqueia novos chats (`awaitIdleTurns`) | média | chat travado em carregando | timeout de 2 min, recusa ao fechar o chat, limpeza no cancelamento do turno |
| Dois cards de turnos diferentes ao mesmo tempo | baixa | decisão aplicada ao pedido errado | um pedido por vez no `AiSession`; o segundo `awaitApproval` espera o primeiro ou recusa |
| O modelo ignora o novo prompt e pergunta por texto também | média | pergunta duplicada | texto do prompt direto; o card é a única autoridade, a pergunta extra não executa nada |
| Serviço em primeiro plano mal declarado quebra no Android 14+ (tipo e permissão exigidos) | média | crash ao iniciar o turno | tipo `shortService`, declarado no manifest; teste instrumentado no aparelho; iniciar sempre com o app em primeiro plano |
| O sistema ou o fabricante encerra o processo mesmo com o serviço | média | resposta perdida; a conversa fica só com a mensagem do usuário | melhor esforço, registrado como limite; a mensagem já está gravada ao iniciar o turno |
| Notificação com conteúdo sensível na tela bloqueada | média | vazamento de nomes de lista | `VISIBILITY_PRIVATE` com versão pública genérica |
| Notificar com o chat à vista é ruído | média | incômodo | só notifica quando o usuário não está olhando (`ChatVisibilityTracker`) |
| `POST_NOTIFICATIONS` negada | média | sem aviso de conclusão | o turno continua; a resposta está no chat; o interruptor de notificações nas configurações segue como caminho para tentar de novo |
| Pedir a permissão ao abrir pode ser invasivo | média | desistência | pedido uma vez por instalação e só depois do login, nunca na tela de boas-vindas |
| Quem aprova não vê o que será apagado | média | aprovação às cegas | `confirmationSummary` com o título; teste de lista inexistente |
| Troca de contrato em `AiSession` quebra testes e fakes | média | testes vermelhos | atualizar `AgentSession` e os testes no mesmo commit; não há outro implementador |
| Fechar o chat recusa uma ação que o usuário ainda queria | baixa | repetir o pedido | comportamento explícito no §5; a IA informa que foi recusada |

## 8.1 Notas de implementação
- `awaitApproval` devolve `ApprovalDecision` (`APPROVED`, `REJECTED`, `TIMEOUT`, `DISMISSED`) em vez de `Boolean`, para a telemetria distinguir recusa, tempo esgotado e saída do chat.
- Serviço, notificações e visibilidade ficaram atrás de uma interface pública `AiTurnBackground` (`domain/service`), com implementação interna; o ViewModel recebe só ela. O `ChatTurnTracker` não precisou mudar.
- A visibilidade do chat usa o ciclo de vida do processo (`lifecycle-process`) somado ao chat composto; fechar o chat chama `dismissPendingApproval` (recusa), mas a rotação, que recompõe o dialog, não recusa.
- `clear_purchased_products` usa o título genérico no card (sem resumo): o use case não conhece o nome da lista e o texto do resumo viria sem tradução.
- O pedido de permissão guarda "já pedi" em `SharedPreferences` e roda na primeira Home depois do login e do nome.
- Não validado no aparelho: o serviço em primeiro plano com o app fechado e a notificação, que dependem de o modelo de IA decidir apagar algo e de testar com o app em segundo plano.

## 9. Perguntas em Aberto (bloqueantes)
Nenhuma bloqueante. Decisões registradas: só ações irreversíveis; Aprovar executa na hora.
- [ ] Q1 (não bloqueante): tempo limite de 2 minutos? Recomendação adotada: sim.
- [ ] Q2 (não bloqueante): fechar o chat com o card aberto recusa o pedido? Recomendação adotada: sim, para não deixar o turno preso. Observação: com a notificação, sair do chat para outra tela do app não fecha o chat da IA e não recusa; só fechar o chat recusa.
- [x] Q3 (resolvida pelo usuário): a notificação mostra a mensagem da IA (truncada; versão genérica na tela bloqueada).
- [x] Q4 (resolvida pelo usuário): só envia se as notificações estiverem permitidas no app, e o app pede a permissão ao ser aberto.
- [ ] Q5 (não bloqueante): o pedido ao abrir acontece uma vez por instalação, na primeira Home depois do login. Se você preferir toda vez que o app abrir sem a permissão (o Android para de mostrar após duas recusas), diga.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (`architecture/04` já documenta o gate atual e será atualizado).
- [x] Nenhum código de produção escrito (apenas assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Símbolos públicos novos refletidos como `additive` (membros ainda não lançados são substituídos, não removidos de uma versão publicada).
- [x] Sem endpoint nem remote-config novo; observabilidade planejada.
- [x] Sem novo cache.
- [x] Perguntas bloqueantes exauridas.
