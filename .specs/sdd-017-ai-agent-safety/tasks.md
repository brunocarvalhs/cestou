# Task: AI Agent Safety & Chat Polish

- **Slug:** ai-agent-safety
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-01
- **Status:** ready
- **Versão-alvo:** 2.3.2 (patch de segurança/estabilidade)
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

Pedido do usuário: gerar o plano do `sdd-017-ai-agent-safety` priorizando **B-02** e **B-10**, ambos reclassificados como **P0** em `.specs/sdd-023-auditoria-logica-producao/tasks.md` §2.1 — qualquer colaborador de uma lista (até um membro sem papel de edição) pode, só pedindo no chat, apagar a lista inteira ou zerar `users`/`roles` de outro dono, porque nenhum dos dois use cases expostos como tool de IA valida o papel (`role`) de quem está pedindo.

Este plano também cobre os demais itens já atribuídos ao SDD-017 no roadmap (`.specs/tasks.txt`, `.specs/sdd-001-auditoria-geral/tasks.md` §3): **B-05**, **B-06**, **C-02**, **C-03**, **B-09**, **B-15** — todos revalidados linha a linha nesta sessão de planejamento, com evidência atual de `HEAD` (branch `fix/crashlytics-top-crashes`).

**Achado adicional desta investigação (corrige o diagnóstico de causa-raiz de `sdd-023`):** `sdd-023-auditoria-logica-producao/tasks.md` §2.1 apontava a ausência de `userId` no metadata da tool (`AiChatUiState.toMetadata()`, linhas 19-22) como bloqueio estrutural para corrigir B-02/B-10. Investigação mais profunda mostra que:
- `AiSession.userId` (`core/ai/.../contract/AiSession.kt:10`) existe, mas é preenchido **uma única vez**, no momento em que o Hilt resolve o `@Singleton` `AgentSession` (`AgentModule.kt:32-38`: `authService.currentUser?.id` lido na criação do singleton) — não é um valor observado live, então confiar nele para uma checagem de permissão seria uma segunda classe de bug (usuário stale após troca de sessão).
- O caminho correto já tem precedente direto no mesmo pacote: `ShoppingCreateUseCase` (`feature/shopping/.../domain/usecase/ShoppingCreateUseCase.kt:35,43`) já injeta `AuthService` diretamente no use case e lê `authService.getOrCreateUserId()`/`currentUser` **no momento da execução**, não via `AiSession`/metadata. A correção de B-02/B-10 segue exatamente esse padrão já estabelecido — nenhuma mudança é necessária em `core:ai` (`AiSession`, `AgentContext`, `AgentAction`).

Rastreabilidade:
- `feature/shopping/.../domain/usecase/ShoppingUpdateUseCase.kt:1-61`
- `feature/shopping/.../domain/usecase/ShoppingMultiDeleteUseCase.kt:1-39`
- `feature/shopping/.../domain/usecase/ShoppingCreateUseCase.kt:32-72` (padrão de referência)
- `feature/shopping/.../domain/exception/OwnershipRequiredException.kt:1-17` (exceção já existente, hoje só usada no caminho não-IA)
- `feature/shopping/.../presentation/viewmodel/ShoppingListViewModel.kt:96-109,168-191` (checagem OWNER já existente no caminho de UI, usada como referência de estilo)
- `feature/chat/.../presentation/viewmodel/AiChatViewModel.kt:146-189`
- `feature/chat/.../presentation/screen/AiChatScreen.kt:251-305`
- `feature/ai-agent/.../data/service/GeminiAgent.kt:115`, `OpenRouterAgent.kt:117-120`
- `feature/products/.../domain/usecase/PriceComparisonUseCase.kt:1-43`
- `feature/products/.../di/ProductAgentModule.kt:42-44`
- `feature/ai-agent/.../data/service/AgentFactoryImpl.kt:35-42,62-66`
- `feature/settings/.../presentation/screen/AiSettingsScreen.kt:70-77,166-180`
- `.specs/sdd-001-auditoria-geral/tasks.md` §2.1-2.2 (B-02, B-05, B-06, B-09, B-10, C-02, C-03)
- `.specs/sdd-023-auditoria-logica-producao/tasks.md` §2.1-2.2 (B-02, B-10 reclassificado P0, B-15)
- `architecture/05-decisoes-arquiteturais.md` AD-010 (chokepoint de gating Pro já usado como modelo de dispatch)

---

## 2. Escopo

**Inclui:**
- **B-02 (P0):** bloquear alteração de `users`/`roles` via `update_shopping_list` por quem não é OWNER da lista.
- **B-10 (P0):** bloquear `delete_multi_shopping_list` por quem não é OWNER da lista.
- **C-03 (P2, mesmo arquivo de B-02):** aceitar `status` enviado pelo LLM independente de caixa (`"finished"` == `"FINISHED"`).
- **B-05 (P1):** só marcar o crédito de mensagem grátis como usado depois de receber ao menos uma resposta do assistente, nunca antes do envio nem em erro de rede.
- **B-06 (P1):** impedir múltiplos envios concorrentes a partir do botão de enviar/microfone e de `sendMessage()`.
- **C-02 (P1):** garantir que `isLoading` sempre retorna a `false`, mesmo se o Flow do assistente completar sem emitir nenhuma resposta.
- **B-09 (P2):** remover a tool `compare_prices` (`PriceComparisonUseCase`) do registry — dados fictícios apresentados como reais.
- **B-15 (P2):** impedir que a tela de Configurações de IA salve/saia com tier BYOK selecionado e chave em branco, para que `AgentFactoryImpl` nunca mais caia silenciosamente na chave compartilhada em nome do usuário.
- Correção de visibilidade (`internal`) de `ShoppingMultiDeleteUseCase`, hoje `public` sem nenhum consumidor fora do módulo (desvio de AGENTS.md §5 encontrado de forma incidental ao tocar no arquivo).
- Testes unitários/instrumentados para todos os itens acima.

**Exclui:**
- Publicação de `firestore.rules` (D-01, `sdd-001`) — a correção aqui é só na camada de aplicação; sem as regras publicadas, um cliente modificado ainda poderia repetir a escrita direto no Firestore. Fica registrado como risco residual (seção 8), não como bloqueio deste SDD.
- Qualquer mudança em `AiSession`, `AgentContext` ou no contrato `AgentAction` de `core:ai` — não são necessárias (ver nota de causa-raiz na seção 1).
- B-03, B-04, B-07, B-08, B-11, B-12, B-13, B-14, B-16, B-17, C-04, D-01, D-02 — atribuídos a SDD-018/019/020 por `.specs/sdd-023-auditoria-logica-producao/tasks.md` §3.
- Implementação real de comparação de preços (fica para um SDD futuro, se houver integração com serviço real de cotação).
- Mudança no mecanismo de fallback BYOK→chave compartilhada dentro de `AgentFactoryImpl` em si — a correção atua na origem (impedir salvar/entrar nesse estado pela UI), não no fallback (ver "Perguntas em Aberto" para a alternativa considerada e descartada).

---

## 3. Impacto Arquitetural

- **Módulos e camadas afetados:**
  - `feature/shopping` — `domain/usecase/ShoppingUpdateUseCase.kt`, `domain/usecase/ShoppingMultiDeleteUseCase.kt` (+ testes em `src/test`).
  - `feature/chat` — `presentation/viewmodel/AiChatViewModel.kt`, `presentation/screen/AiChatScreen.kt` (+ testes).
  - `feature/products` — remoção de `domain/usecase/PriceComparisonUseCase.kt`, `di/ProductAgentModule.kt` (binding), `src/test/.../PriceComparisonUseCaseTest.kt` (removido).
  - `feature/settings` — `presentation/screen/AiSettingsScreen.kt` (validação local, sem tocar ViewModel/UseCase — ver justificativa na seção 9).
- **Padrão aplicado:** Clean Architecture + MVI, sem novas camadas. Os dois use cases de `feature/shopping` passam a injetar `AuthService` (`core:domain`), exatamente como `ShoppingCreateUseCase` já faz — nenhum novo tipo de dependência entre módulos.
- **Novos arquivos:** nenhum. Todas as mudanças são em arquivos existentes; `PriceComparisonUseCase.kt` e seu teste são **removidos**, não criados.
- **Injeção via Hilt:** `ShoppingUpdateUseCase`/`ShoppingMultiDeleteUseCase` continuam `@Singleton @Inject constructor`, só ganham um parâmetro (`AuthService`) já disponível no grafo de `feature/shopping` (usado por `ShoppingCreateUseCase` no mesmo módulo). A remoção do `@Binds @IntoSet` de `PriceComparisonUseCase` em `ProductAgentModule.kt:42-44` tira a classe do `Set<AgentActionUseCase<*>>` injetado em `CestouApplication.kt:16`, que é o que hoje força sua auto-instanciação e autorregistro (`AgentActionUseCase.init { AgentRegistry.register(this) }`, `core/ai/.../base/AgentActionUseCase.kt:33-38`) — sem o binding, a tool nunca é construída e desaparece de `AgentRegistry.getAll()`, que é a lista usada por `GeminiAgent`/`OpenRouterAgent` para declarar `FunctionDeclaration`s ao modelo.

### Diagrama ASCII — B-02/B-10 (chokepoint de permissão)

```
┌─────────────────────────────────────────────────────────────────────┐
│ LLM decide chamar update_shopping_list / delete_multi_shopping_list  │
└───────────────────────────────────┬───────────────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│ GeminiAgent.executeFunctionCall / OpenRouterAgent.executeToolCall│
│  (chokepoint de dispatch, AD-010 — só gating de Pro hoje)            │
└───────────────────────────────────┬───────────────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│ ShoppingUpdateUseCase.execute() / ShoppingMultiDeleteUseCase.invoke()│
│  1. repository.getById(id)                                          │
│  2. NOVO: val callerId = authService.currentUser?.id   (live read)  │
│  3. NOVO: if (shopping.roles[callerId] != OWNER) →                  │
│           Result.failure(OwnershipRequiredException(...))           │
│  4. (OK) repository.updateFields(...) / repository.delete(...)      │
└─────────────────────────────────────────────────────────────────────┘
```

### Esboço (assinatura + lógica ilustrativa, não é código final)

```kotlin
// feature/shopping/.../domain/usecase/ShoppingUpdateUseCase.kt
internal class ShoppingUpdateUseCase @Inject constructor(
    private val repository: ShoppingRepository,
    private val authService: AuthService           // NOVO parâmetro
) : AgentActionUseCase<Unit>() {

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val id = arguments.getString("shopping_id") ?: throw Exception("Shopping ID is required")
        val shopping = repository.getById(id) ?: throw Exception("Shopping list not found")

        val fields = buildMap<String, Any?> {
            arguments.getString("title")?.let { put("title", it) }
            arguments.getString("description")?.let { put("description", it) }
            arguments.getDouble("price")?.let { put("price", it) }
            arguments.getString("status")
                // C-03: tolerante a caixa — entries.firstOrNull(ignoreCase) em vez de valueOf
                ?.let { s -> Shopping.Status.entries.firstOrNull { it.name.equals(s, ignoreCase = true) } }
                ?.let { put("status", it.name) }

            if (arguments.containsKey("users") || arguments.containsKey("roles")) {
                // B-02: só o OWNER pode alterar posse/papéis
                val callerId = authService.currentUser?.id
                if (shopping.roles[callerId] != User.Role.OWNER.name) {
                    return Result.failure(OwnershipRequiredException("update_shopping_list.users_roles"))
                }
                arguments.getList("users")?.let { put("users", it) }
                arguments.getMap("roles")?.let { put("roles", it) }
            }
        }
        return invoke(id, fields)
    }
}
```

```kotlin
// feature/shopping/.../domain/usecase/ShoppingMultiDeleteUseCase.kt
internal class ShoppingMultiDeleteUseCase @Inject constructor(   // visibilidade corrigida: public → internal
    private val repository: ShoppingRepository,
    private val authService: AuthService           // NOVO parâmetro
) : AgentActionUseCase<Unit>() {

    suspend operator fun invoke(id: String): Result<Unit> = runCatching {
        val shopping = repository.getById(id) ?: throw Exception("Shopping not found")
        // B-10: só o OWNER pode excluir a lista
        val callerId = authService.currentUser?.id
        if (shopping.roles[callerId] != User.Role.OWNER.name) {
            throw OwnershipRequiredException("delete_multi_shopping_list")
        }
        repository.delete(shopping)
    }
}
```

`OwnershipRequiredException` já existe (`feature/shopping/.../domain/exception/OwnershipRequiredException.kt`) e sua mensagem já é adequada como feedback para o LLM: `Result.failure` chega em `Result<*>.toToolFeedback()` (`feature/ai-agent/.../ToolFeedback.kt:12-15`), que converte para `"Erro: Ação '...' requer papel de owner na lista"` — texto devolvido ao modelo, não exibido como string de UI (`strings.xml` não se aplica aqui).

### Esboço — B-05/B-06/C-02 (`AiChatViewModel.sendMessage()`)

```kotlin
private fun sendMessage() {
    val text = _uiState.value.input
    if (text.isBlank() || _uiState.value.isLoading) return   // B-06: guarda contra chamada concorrente

    analyticsTracker.trackEvent(AnalyticsEvents.AI_CHAT_MESSAGE_SENT)
    // B-05: markFreeMessageUsed() REMOVIDO daqui — só dispara depois de uma resposta recebida

    val userMessage = ChatMessage(text = text, sender = ChatMessage.Sender.USER)
    _uiState.update { it.copy(messages = it.messages + userMessage, input = "", isLoading = true) }
    persistHistory()

    viewModelScope.launch {
        var receivedAny = false
        try {
            assistantUseCase(text, _uiState.value)
                .onCompletion { _uiState.update { it.copy(isLoading = false) } }  // C-02: nunca trava
                .collect { response ->
                    receivedAny = true
                    val assistantMessage = ChatMessage(text = response, sender = ChatMessage.Sender.ASSISTANT)
                    _uiState.update { it.copy(messages = it.messages + assistantMessage, isLoading = false) }
                    persistHistory()
                }
            if (receivedAny) aiTrialRepository.markFreeMessageUsed()  // B-05: só após sucesso real
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // ... tratamento de erro existente (linhas 177-186), sem markFreeMessageUsed() ...
        }
    }
}
```

`AiChatScreen.kt:280,296` — adicionar `!state.isLoading` à condição `enabled` dos dois `FilledIconButton` (microfone e enviar), como segunda camada de defesa (a guarda em `sendMessage()` já cobre o caso raiz, mas o botão habilitado durante `isLoading` é uma UX enganosa por si só).

---

## 4. Contratos e Interfaces

| Elemento | Antes | Depois | `public`/`internal` | Justificativa |
|---|---|---|---|---|
| `ShoppingUpdateUseCase` (construtor) | `(repository: ShoppingRepository)` | `(repository: ShoppingRepository, authService: AuthService)` | `internal` (sem mudança) | Classe já era `internal`; só consumida via Hilt dentro do módulo. Teste em `src/test` precisa ser atualizado para o novo construtor (não é contrato público entre módulos). |
| `ShoppingMultiDeleteUseCase` (construtor + classe) | `class ... (repository: ShoppingRepository)` (`public`) | `internal class ... (repository: ShoppingRepository, authService: AuthService)` | `public` → `internal` | Confirmado via busca (`grep -rln "ShoppingMultiDeleteUseCase"`) que não há nenhum consumidor fora de `feature/shopping` — `public` era desvio de AGENTS.md §5, não um contrato intencional. |
| `PriceComparisonUseCase` | existe, registrado | **removido** (classe, binding em `ProductAgentModule.kt:42-44`, teste) | — | Tool nunca teve dado real; `requiresPro`/`id` eram só metadados internos do registry, não contrato consumido por outro módulo. |
| `AiChatUiState`, `AgentContext`, `AiSession`, `AgentAction` | inalterados | inalterados | — | Nenhuma mudança necessária (ver nota de causa-raiz, seção 1). |

Nenhum novo endpoint, coleção Firestore, evento de Analytics ou remote-config é introduzido. Nenhum `NavKey` é alterado.

---

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Qualquer membro pode alterar `users`/`roles` de uma lista via tool de IA. | Só o OWNER (`shopping.roles[callerId] == OWNER`) pode alterar `users`/`roles`; demais recebem `OwnershipRequiredException`. | `ShoppingUpdateUseCase.kt:55-56` |
| 2 | Qualquer membro pode excluir a lista inteira via tool de IA. | Só o OWNER pode excluir; demais recebem `OwnershipRequiredException`. | `ShoppingMultiDeleteUseCase.kt:26-29` |
| 3 | `status` enviado pela IA precisa casar exatamente com o enum (`valueOf`). | `status` é aceito independente de caixa (`"finished"` == `"FINISHED"`). | `ShoppingUpdateUseCase.kt:52-54` |
| 4 | Crédito de mensagem grátis é debitado antes do envio ter sucesso. | Crédito só é debitado depois de receber ao menos uma resposta do assistente. | `AiChatViewModel.kt:151` |
| 5 | Botão de enviar/microfone e `sendMessage()` não bloqueiam envio concorrente. | Guarda em `sendMessage()` (early return se `isLoading`) + botões desabilitados durante `isLoading`. | `AiChatViewModel.kt:146-148`; `AiChatScreen.kt:280,296` |
| 6 | Flow do assistente pode completar sem emitir nada, deixando `isLoading=true` para sempre. | `.onCompletion` garante `isLoading=false` em qualquer desfecho do Flow. | `AiChatViewModel.kt:163-174` |
| 7 | Tool `compare_prices` retorna preços fictícios hardcoded como se fossem reais. | Tool removida do registry; IA não tem mais essa opção disponível. | `PriceComparisonUseCase.kt:28-35` |
| 8 | Tela de Configurações de IA permite sair/salvar com tier BYOK e chave em branco. | Salvar/voltar é bloqueado nesse estado; usuário vê erro inline até preencher a chave ou trocar de tier. | `AiSettingsScreen.kt:70-77,166-180` |

---

## 6. Critérios de Aceitação

- **Performance:** nenhuma allocação ou I/O adicional relevante — a checagem de permissão é um lookup O(1) em `Map` já carregado (`shopping.roles`), sem nova leitura de rede (`repository.getById` já era chamado antes da mudança). Nenhuma nova coroutine/dispatcher introduzido.
- **Feature toggles:** nenhum necessário — comportamento correto não deve ser opcional; ver risco residual de dados de `roles` legados na seção 8.
- **Observabilidade:** nenhum evento de Analytics novo é proposto. Decisão deliberada: `AgentActionUseCase`s vivem em `domain/usecase/` (domínio puro, AGENTS.md §2) e `AnalyticsTracker` é infraestrutura transversal (`core:analytics`) — nenhum outro use case de IA hoje injeta `AnalyticsTracker`, e abrir essa exceção aqui violaria a separação de camadas só para instrumentar uma negação esperada de regra de negócio (`BusinessRuleException`, por definição "não necessariamente um bug a investigar" — `core/domain/.../exception/BusinessRuleException.kt:6-8`). Exceções genuínas continuam cobertas pelo `crashReporter.recordException` já existente no catch do chokepoint de dispatch (`GeminiAgent.kt:172-179`).
- **Compatibilidade:** `minSdk 28` inalterado. `:wear` não é afetado — `ShoppingUpdateUseCase`/`ShoppingMultiDeleteUseCase` pertencem a `feature:shopping` (não compilado no `:wear`); `AiChatViewModel` é compilado no `:wear` (comentário em `AgentModule.kt`... na verdade em `ChatAiGateViewModel.kt:9-11`), mas as mudanças propostas usam apenas tipos já presentes no `:wear` (`AiTrialRepository`, `Flow.onCompletion`), sem novas dependências de módulo.

---

## 7. Plano de Testes

- **`ShoppingUpdateUseCaseTest.kt`** (atualizar construtor em todos os testes existentes para `ShoppingUpdateUseCase(repository, authService)`):
  - Novo: `execute` nega alteração de `users`/`roles` quando `authService.currentUser?.id` não é o OWNER em `shopping.roles` → `Result.isFailure`, `repository.updateFields` **nunca** chamado (`coVerify(exactly = 0)`).
  - Novo: `execute` permite alteração de `users`/`roles` quando o chamador é OWNER.
  - Novo: `execute` aceita `status` em minúsculo (`"finished"`) e grava `"FINISH"`.
  - Existentes: continuam válidos, só ajustando a instanciação do use case.
- **`ShoppingMultiDeleteUseCaseTest.kt`** (mesmo ajuste de construtor):
  - Novo: `invoke`/`execute` negam exclusão quando o chamador não é OWNER → `Result.isFailure`, `repository.delete` nunca chamado.
  - Novo: `invoke`/`execute` permitem exclusão quando o chamador é OWNER.
- **`AiChatViewModelTest.kt` / `AiChatViewModelReliabilityTest.kt`:**
  - Novo (B-05): `markFreeMessageUsed()` não é chamado quando o `Flow` do assistente lança `IOException` (mock de `CartAssistantUseCase` retornando `flow { throw IOException() }`).
  - Novo (B-05): `markFreeMessageUsed()` é chamado exatamente uma vez após receber uma resposta com sucesso.
  - Novo (B-06): segunda chamada a `sendMessage()` enquanto `isLoading == true` não dispara uma segunda invocação de `assistantUseCase` (`coVerify(exactly = 1)`).
  - Novo (C-02): `Flow` que completa sem emitir nenhum valor (`flow { }`) resulta em `uiState.value.isLoading == false` ao final.
- **`AiChatScreenTest.kt`** (instrumentado, `testTag` existente da tela): adicionar caso cobrindo botão de enviar/microfone desabilitado quando `state.isLoading == true`.
- **`PriceComparisonUseCaseTest.kt`:** removido junto com a classe. `ProductAgentModuleTest` (se existir) ou verificação manual: `AgentRegistry.find("compare_prices")` deve retornar `null` após a remoção do binding.
- **Novo teste instrumentado `AiSettingsScreenTest.kt`** (`feature/settings/src/androidTest`, seletor por `testTag` — ex.: `ai_settings_byok_key_field`, `ai_settings_byok_key_error`): selecionar tier BYOK, deixar a chave em branco, tocar em "Salvar" → tela permanece aberta e o erro inline é exibido; preencher a chave → "Salvar" navega de volta normalmente.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout` — obrigatório antes do PR, incluindo verificação de que a cobertura de `ShoppingUpdateUseCase`/`ShoppingMultiDeleteUseCase` (lógica nova) atinge o mínimo de 80% exigido.

---

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Dados legados de `roles` corrompidos ou incompletos (ex.: lista criada antes de algum fix anterior, sem entrada OWNER) passam a negar a própria alteração/exclusão a um usuário que deveria ser dono. | Baixa | Médio (usuário legítimo bloqueado de gerenciar a própria lista) | `ShoppingListViewModel` já aplica a mesma checagem (`roles[userId] == OWNER`) no caminho de UI hoje — se o dado estiver corrompido, o usuário já está bloqueado lá também; não é uma regressão nova, só estende a mesma regra ao caminho de IA. Monitorar `OwnershipRequiredException` via Crashlytics não-fatal se a taxa de negação for anormalmente alta após o release. |
| Sem `firestore.rules` publicadas (D-01), a correção de B-02/B-10 só impede o caminho pelo app — um cliente modificado ainda escreve direto no Firestore. | Alta (já é um risco aberto hoje) | Alto | Fora de escopo deste SDD; registrado como dependência para D-01, já rastreado em `sdd-001`. |
| Remover `PriceComparisonUseCase` tira uma funcionalidade (ainda que fictícia) que o usuário podia perceber como "funcionando". | Baixa | Baixo (dado era fictício; manter é o risco real — usuário tomando decisão de compra com preço inventado) | Nenhuma — é o resultado desejado. Se houver demanda de produto por comparação de preços real, abrir um SDD novo com integração real. |
| Validação de BYOK só no Composable (não no ViewModel/UseCase) pode ser contornada por um caminho de entrada futuro que não passe por `AiSettingsScreen`. | Baixa (hoje só existe esse caminho) | Baixo | Documentado explicitamente na seção 9; se um segundo ponto de entrada para `AiConfigTier`/`customApiKey` for criado no futuro, a mesma validação precisa ser replicada ou centralizada em `UpdateAiSettingsUseCase`. |

---

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma bloqueante. Duas decisões de design foram tomadas durante o planejamento (não bloqueantes, registradas para rastreabilidade):

- [x] **Onde validar B-15 (BYOK em branco)?** Decidido: validação local no Composable `AiSettingsScreen`, não no `AiSettingsViewModel`/`UpdateAiSettingsUseCase`. Motivo: tanto o botão "Salvar" (linha 166-180) quanto o botão de voltar na topbar (linha 70-77) chamam `intent.onUpdateAiSettings(...)` seguido **imediatamente** de `intent.onBack()`/navegação, de forma incondicional e sem aguardar confirmação do ViewModel (que roda em `viewModelScope.launch`, assíncrono). Colocar a validação só no ViewModel exigiria introduzir um canal de evento "pode navegar" (ex.: `SharedFlow` de confirmação) para sincronizar a navegação com o resultado assíncrono — escopo desproporcional para impedir um campo de texto em branco. A validação local (`tier == CUSTOM_BYOK && customApiKey.isBlank()`) é síncrona, resolve o cenário de falha documentado por completo, e é consistente com o fato de já existir estado local (`remember { mutableStateOf(...) }`) para os três campos editáveis na mesma tela.
- [x] **Corrigir também o fallback silencioso dentro de `AgentFactoryImpl`?** Decidido: não neste SDD. Com a validação da UI em vigor, o estado "tier BYOK + chave em branco" não é mais alcançável pelo fluxo normal do app; adicionar uma segunda defesa dentro da factory (ex.: log/crash report se esse estado for alcançado) é um nice-to-have de defesa-em-profundidade, não uma correção do bug documentado — pode entrar em um SDD de hardening futuro se o item for priorizado.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `architecture/05-decisoes-arquiteturais.md` AD-010, quando aplicável).
- [x] Nenhum código de produção escrito nesta tarefa (apenas esboços ilustrativos, claramente rotulados).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança de visibilidade de `ShoppingMultiDeleteUseCase` (`public`→`internal`) registrada na seção 4; confirmado sem consumidores externos antes de propor.
- [x] Nenhum novo endpoint/remote-config — não aplicável.
- [x] Nenhum novo cache — não aplicável.
- [x] Perguntas em aberto exauridas (nenhuma bloqueante; duas decisões de design documentadas).
