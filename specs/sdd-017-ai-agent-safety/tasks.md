# Task: Remover tools simuladas, proteger roles/status contra manipulação pelo LLM e corrigir consumo de trial no chat

- **Slug:** sdd-017-ai-agent-safety
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-31
- **Status:** ready
- **Versão-alvo:** 2.3.0 (minor — novos invariantes de segurança, sem breaking change em contrato público)
- **Tipo de mudança:** fix + refactor
- **Impacto público (módulo):** additive (novos parâmetros `@AiAgentParameter` removidos de `ShoppingUpdateUseCase`; o `ShoppingRepository.updateFields` continua público inalterado)

---

## 1. Contexto e Motivação

O backlog de auditoria (`specs/tasks.txt`, item 2) identificou três classes de problemas de segurança no subsistema de IA do Cestou:

1. **Tool simulada em produção** — `PriceComparisonUseCase` (`feature/products/src/main/java/.../PriceComparisonUseCase.kt:19`) retorna dados hardcoded de estabelecimentos fictícios como se fossem dados reais. O LLM os apresenta ao usuário como comparação de preços genuína, induzindo decisões de compra baseadas em informação falsa.

2. **Manipulação de roles/status pelo LLM** — `ShoppingUpdateUseCase` (`feature/shopping/src/main/java/.../ShoppingUpdateUseCase.kt:44-50`) aceita `users` e `roles` (mapa de permissões) como argumentos livres do LLM, que pode escrever `"OWNER"` para qualquer userId recebido no contexto. Aceita também `status` como string arbitrária (apenas `valueOf` valida, que lança exceção em vez de recusar silenciosamente). Um prompt adversarial do tipo "mude meu role para OWNER na lista X" ou "mude o status para DELETED" seria executado.

3. **Trial consumido antes da resposta** — `AiChatViewModel.sendMessage()` (`feature/chat/src/main/java/.../AiChatViewModel.kt:66`) chama `aiTrialRepository.markFreeMessageUsed()` no momento do envio da mensagem do usuário, **antes** de a resposta chegar. Se o provider falhar (timeout, rede, modelo indisponível), o trial é queimado sem que o usuário tenha recebido nenhum valor.

Rastreabilidade: `specs/tasks.txt` (item 2), `AiChatViewModel.kt:66`, `PriceComparisonUseCase.kt:27-36`, `ShoppingUpdateUseCase.kt:44-50`.

---

## 2. Escopo

**Inclui:**
- Remover `PriceComparisonUseCase` como `AgentAction` registrada (desregistrar do `AgentRegistry`/remover de `ProductAgentModule`) ou eliminar completamente a tool, pois não há backend real de comparação de preços.
- Remover os parâmetros `users` e `roles` do contrato de tool de `ShoppingUpdateUseCase`, impedindo o LLM de alterar permissões de membros.
- Restringir `status` em `ShoppingUpdateUseCase` a uma allowlist de transições seguras (ex.: apenas `IN_PROGRESS` via `ShoppingReopenUseCase` já dedicada — avaliar remoção completa do campo `status` da tool de update).
- Mover `markFreeMessageUsed()` para depois do sucesso da resposta do agente em `AiChatViewModel`.
- Testes unitários para cada fix.

**Exclui:**
- Implementar comparação de preços real (feature futura, sem escopo definido).
- Alterar o fluxo de assinatura/billing (`core:billing`, `feature:subscription`).
- Modificar `ShoppingRepository.updateFields` (contrato público inalterado).
- Alterar `ChatAiGateViewModel` ou a lógica de gate de acesso ao chat.
- Gestão de membros (join/sair) — coberta pelo sdd-019.

---

## 3. Impacto Arquitetural

### Módulos e camadas afetados

| Módulo | Camada | Arquivo |
|---|---|---|
| `:feature:products` | `domain/usecase` | `PriceComparisonUseCase.kt` |
| `:feature:products` | `di` | `ProductAgentModule.kt` |
| `:feature:shopping` | `domain/usecase` | `ShoppingUpdateUseCase.kt` |
| `:feature:chat` | `presentation/viewmodel` | `AiChatViewModel.kt` |
| `:feature:products` (test) | `domain/usecase` | `PriceComparisonUseCaseTest.kt` (novo) |
| `:feature:shopping` (test) | `domain/usecase` | `ShoppingUpdateUseCaseSecurityTest.kt` (novo) |
| `:feature:chat` (test) | `presentation/viewmodel` | `AiChatViewModelTrialTest.kt` (novo) |

### Padrões aplicados
- Clean Architecture + MVI: a lógica de segurança fica na camada `domain/usecase` (sem vazar para `presentation`).
- Allowlist de transições de estado no use case, não no repositório.
- Trial consumption: side-effect movido para o `onSuccess` do `Flow`, mantendo o `viewModelScope` atual.

### Diagrama ASCII — fluxo corrigido do trial

```
AiChatViewModel.sendMessage()
    │
    ├─► emit userMessage ao estado (UI atualiza)
    │
    ├─► assistantUseCase(prompt, context).collect { response →
    │       emit assistantMessage ao estado
    │       aiTrialRepository.markFreeMessageUsed()  ← MOVIDO PARA CÁ
    │       persistHistory()
    │   }
    │
    └─► catch(Exception) →
            emit errorMessage
            // trial NÃO consumido em caso de erro
```

### Diagrama ASCII — ShoppingUpdateUseCase allowlist

```
ShoppingUpdateUseCase.execute()
    │
    ├─► extrair "title", "description", "price"    ← permitidos
    ├─► extrair "status" → apenas se IN_PROGRESS ou DONE  ← allowlist
    ├─► NÃO extrair "users"                         ← removido
    └─► NÃO extrair "roles"                         ← removido
```

### Injeção via Hilt
- `PriceComparisonUseCase`: remover `@Binds` em `ProductAgentModule`. Se a classe não for usada por nenhum outro ponto, pode ser deletada; caso contrário, manter a classe mas remover o binding do `@InstallIn(SingletonComponent)`.
- Nenhum novo módulo Hilt necessário.

---

## 4. Contratos e Interfaces

### A. `PriceComparisonUseCase` — `internal` → removida do registry

```kotlin
// ANTES (feature/products/.../ProductAgentModule.kt)
@Binds
@IntoSet
abstract fun bindPriceComparison(useCase: PriceComparisonUseCase): AgentActionUseCase<*>

// DEPOIS: linha removida.
// A classe PriceComparisonUseCase em si pode ser deletada ou mantida
// internamente SEM anotação @AiAgentAction (decisão: deletar, pois não
// tem função sem o binding de agent).
```

> Impacto público: **nenhum** — `PriceComparisonUseCase` é `internal`-by-convention; o `AgentRegistry` é `object` interno ao módulo. Nenhum consumidor externo.

### B. `ShoppingUpdateUseCase.execute()` — parâmetros proibidos

```kotlin
// ANTES: o buildMap aceitava "users", "roles" e "status" livre
// DEPOIS:
private val ALLOWED_STATUS_TRANSITIONS = setOf(
    Shopping.Status.IN_PROGRESS,
    Shopping.Status.DONE
)

override suspend fun execute(...): Result<Unit> {
    val id = arguments.getString("shopping_id") ?: throw Exception("Shopping ID is required")
    repository.getById(id) ?: throw Exception("Shopping list not found")
    val fields = buildMap<String, Any?> {
        arguments.getString("title")?.let { put("title", it) }
        arguments.getString("description")?.let { put("description", it) }
        arguments.getDouble("price")?.let { put("price", it) }
        arguments.getString("status")
            ?.let { raw ->
                runCatching { Shopping.Status.valueOf(raw) }.getOrNull()
            }
            ?.takeIf { it in ALLOWED_STATUS_TRANSITIONS }
            ?.let { put("status", it.name) }
        // "users" e "roles" removidos: o LLM não pode alterar permissões de membros
    }
    return invoke(id, fields)
}
```

> Impacto público: `internal` — sem breaking change externo. A tool `update_shopping_list` no registry perde silenciosamente os parâmetros `users`/`roles`, pois eles eram aceitos dinamicamente via `buildMap`, não como `@AiAgentParameter` declarados — logo **o contrato de tool exposto ao LLM não muda** (já não havia parâmetros `users`/`roles` na anotação `@AiAgentParameter`).

### C. `AiChatViewModel.sendMessage()` — trial consumption

```kotlin
// ANTES
viewModelScope.launch { aiTrialRepository.markFreeMessageUsed() }  // L66, disparo imediato

// DEPOIS: removida a linha acima do bloco de lançamento.
// Dentro do collect:
assistantUseCase(text, _uiState.value).collect { response ->
    val assistantMessage = ...
    _uiState.update { ... }
    viewModelScope.launch { aiTrialRepository.markFreeMessageUsed() }  // consumido após resposta
    persistHistory()
}
```

> Impacto público: **nenhum** — comportamento interno de `AiChatViewModel` (`internal` via Hilt).
> `markFreeMessageUsed()` continua usando `NonCancellable` em `AiTrialRepositoryImpl`, então a proteção contra cancelamento do scope permanece válida (`core/data/.../AiTrialRepositoryImpl.kt:28`).

---

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | `PriceComparisonUseCase` retorna preços simulados de 3 estabelecimentos fictícios | Tool removida do registry; LLM não pode mais invocar `compare_prices` | `PriceComparisonUseCase.kt:27-36` |
| 2 | `ShoppingUpdateUseCase` aceita `users` e `roles` livres do LLM | Campos removidos do `buildMap`; tentativa de alterar membros/roles via IA resulta em no-op silencioso | `ShoppingUpdateUseCase.kt:44-50` |
| 3 | `ShoppingUpdateUseCase` aceita qualquer string como `status` (apenas `valueOf` valida) | Apenas `IN_PROGRESS` e `DONE` são aceitos; outros valores são ignorados silenciosamente | `ShoppingUpdateUseCase.kt:47-49` |
| 4 | `markFreeMessageUsed()` é chamado antes da resposta do agente | `markFreeMessageUsed()` só é chamado após ao menos um chunk de resposta ser coletado com sucesso | `AiChatViewModel.kt:66` |
| 5 | Trial Free: 1 mensagem grátis antes de bloquear (regra atual de produto) | Regra mantida; apenas o *momento* do consumo muda (após resposta bem-sucedida) | `ChatAiGateViewModel.kt:37`, `AiTrialRepositoryImpl.kt:23` |

---

## 6. Critérios de Aceitação

### Fix 1 — PriceComparisonUseCase removida

- [ ] `AgentRegistry.getAll()` não contém `compare_prices` após inicialização do grafo Hilt.
- [ ] Um prompt "compare preços de arroz" não aciona nenhuma tool call; o LLM responde dentro do escopo textual.
- [ ] `ProductAgentModule` não tem `@Binds` para `PriceComparisonUseCase`.

### Fix 2 — ShoppingUpdateUseCase sem roles/users/status livre

- [ ] Um `execute()` com `arguments = mapOf("shopping_id" to "X", "roles" to mapOf("uid" to "OWNER"))` não escreve nada em `roles` no Firestore.
- [ ] Um `execute()` com `arguments = mapOf("shopping_id" to "X", "users" to listOf("uid2"))` não escreve nada em `users`.
- [ ] `status = "DELETED"` (ou qualquer valor fora da allowlist) é ignorado (nenhuma chamada a `updateFields` com essa chave).
- [ ] `status = "IN_PROGRESS"` e `status = "DONE"` são aceitos e escritos.

### Fix 3 — Trial consumido após resposta

- [ ] Quando o agente retorna erro (exception propagada), `markFreeMessageUsed()` **não** é chamado.
- [ ] Quando o agente retorna ao menos um chunk, `markFreeMessageUsed()` é chamado exatamente uma vez.
- [ ] O comportamento de gate (`ChatAiGateViewModel.isSendEnabled`) permanece inalterado — o teste existente `ChatAiGateViewModelTest` não quebra.

### Performance / Observabilidade

- Nenhum dispatch extra de coroutine no caminho feliz.
- `markFreeMessageUsed()` continua dentro de `NonCancellable` (garantido pela impl em `AiTrialRepositoryImpl`).
- Nenhum evento de Analytics novo necessário (fix é de segurança, não feature).

### Compatibilidade

- minSdk 28, targetSdk 37 — nenhuma API nova.
- Wear OS: `AiTrialRepository` é usado no `:wear` via `core:domain`; a interface não muda.

---

## 7. Plano de Testes

### Unitários

#### `PriceComparisonUseCaseTest` (novo — ou arquivo de remoção se a classe for deletada)
- Se a classe for **deletada**: verificar que o arquivo não existe mais (lint/compilação).
- Se mantida sem binding: teste de regressão confirmando que `AgentRegistry.find("compare_prices")` retorna `null` após inicialização do módulo Hilt em testes.

#### `ShoppingUpdateUseCaseSecurityTest` (novo — `:feature:shopping/test`)

```
given: execute com "roles" = mapOf("uid" to "OWNER")
when: invocado
then: ShoppingRepository.updateFields NÃO recebe a chave "roles"

given: execute com "users" = listOf("uid2")
when: invocado
then: ShoppingRepository.updateFields NÃO recebe a chave "users"

given: execute com "status" = "DELETED"
when: invocado
then: ShoppingRepository.updateFields NÃO recebe a chave "status"

given: execute com "status" = "IN_PROGRESS"
when: invocado
then: ShoppingRepository.updateFields RECEBE status = "IN_PROGRESS"

given: execute com "status" = "DONE"
when: invocado
then: ShoppingRepository.updateFields RECEBE status = "DONE"

given: execute com "title" = "Nova lista" (sem campos proibidos)
when: invocado
then: ShoppingRepository.updateFields RECEBE apenas title = "Nova lista"
```

Mocks: `ShoppingRepository` (verificação de argumentos via `mockk verify`).

#### `AiChatViewModelTrialTest` (novo — `:feature:chat/test`)

```
given: assistantUseCase lança exception
when: sendMessage invocado
then: aiTrialRepository.markFreeMessageUsed NÃO é chamado

given: assistantUseCase retorna Flow com 1 item
when: sendMessage invocado
then: aiTrialRepository.markFreeMessageUsed é chamado exatamente 1 vez

given: assistantUseCase retorna Flow vazio (sem emit)
when: sendMessage invocado
then: aiTrialRepository.markFreeMessageUsed NÃO é chamado (sem resposta = sem consumo)
```

Mocks: `CartAssistantUseCase`, `AiTrialRepository`, `ChatHistoryRepository`, `AnalyticsTracker`, `AiSession`.

> **Nota sobre "Flow vazio":** a discussão sobre se um Flow vazio deve ou não consumir o trial é uma decisão de produto; o comportamento sugerido acima (não consumir) é conservador e pode ser alterado na revisão.

### Instrumentados / UI
- Nenhum teste instrumentado novo: os fixes são de lógica pura (use cases e ViewModel), já cobertos por unitários.
- Os testes existentes `AiChatViewModelTest` e `AiChatViewModelReliabilityTest` devem passar sem alteração (verificar após a mudança).

### Lint / Detekt / Cobertura
```
./gradlew lintDebug
./gradlew detekt
./gradlew testDebug
./gradlew koverVerify koverVerifyLayout
```

---

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Usuários que dependiam de `compare_prices` (prompt habitual) deixam de ver resposta de ferramenta | baixa | baixo — a tool nunca retornou dados reais; o LLM passa a responder textualmente sobre preços | System prompt já instrui o LLM a lidar com limitações dentro do escopo; nenhuma degradação real |
| `markFreeMessageUsed()` chamado múltiplas vezes se o Flow emitir múltiplos items | média | baixo — `save(KEY_FREE_MESSAGE_USED, true)` é idempotente (sobrescreve `true` com `true`) | Garantido por `AiTrialRepositoryImpl.kt:29` (escreve `true` incondicionalmente); mesmo assim, adicionar flag local `trialConsumed` no ViewModel para chamar apenas na primeira emissão |
| `ShoppingUpdateUseCase` interna: `invoke(id, fields)` chamado com `fields` vazio se o LLM passar só campos proibidos | baixa | baixo — `updateFields` com mapa vazio é no-op no Firestore (merge parcial com zero campos) | Adicionar early-return se `fields.isEmpty()` para evitar round-trip desnecessário ao Firestore |
| Testes existentes de `AiChatViewModel` mockam `aiTrialRepository` e podem falhar se a assinatura do mock mudar | baixa | baixo | Revisar `AiChatViewModelTest.kt` e `AiChatViewModelReliabilityTest.kt` após a mudança; ajustar `verify` se necessário |

---

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma questão bloqueante identificada. As decisões abaixo são deliberadas e podem ser revisadas pelo maintainer na revisão do plano, mas não impedem o início da implementação:

- [ ] **Decisão de produto — `compare_prices`:** deletar a classe `PriceComparisonUseCase` completamente ou apenas remover o binding do Hilt (mantendo a classe para eventual reutilização futura com backend real)? **Sugestão do plano: deletar** — sem backend real, a classe só gera confusão futura.
- [ ] **Flow vazio = trial consumido?** Se o provider retornar um `Flow` que completa sem nenhum `emit` (ex.: modelo retorna resposta vazia), o trial deve ser consumido? **Sugestão do plano: não consumir** (conservador, mas pode ser revertido).
- [ ] **`status = "DONE"` deve ser permitido via IA?** Finalizar uma lista via LLM pode ser intencional (e.g. "marque minha lista como concluída"). A allowlist acima inclui `DONE`. Se o produto decidir que apenas `IN_PROGRESS` é seguro, remover `DONE` da allowlist.

> Nenhum item acima é bloqueante: a implementação pode avançar com as sugestões do plano; os itens são pontos de revisão.

---

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `breaking` em "Impacto público" — **não há breaking change**.
- [x] Novo endpoint/remote-config: não aplicável.
- [x] Novo cache: não aplicável.
- [x] Perguntas em aberto exauridas (nenhuma bloqueante).
