# Arquitetura de IA - Cestou Assistant

Este documento descreve o funcionamento do sistema de Agentes de IA e serve como guia para novas implementações.

## 🏗️ Estrutura do Agente

O sistema é baseado no padrão **Agentic Workflow**, onde a IA não apenas responde, mas pode interagir com o app através de ferramentas (Tools).

### 1. AiAgent (O Cérebro)
Interface principal para comunicação. A implementação atual utiliza o `OpenRouterAiAgent` que suporta **Tool Calling**.
- **Input**: Prompt + `AgentContext` (Estado atual da tela).
- **Processo**: Envia para o LLM -> Recebe resposta ou Chamada de Ferramenta -> Executa Ferramenta -> Devolve resultado para o LLM -> Retorna resposta final.

### 2. AgentContext (O Olho)
Interface que qualquer Estado (State) ou DTO pode implementar para fornecer dados contextuais à IA sem que ela precise "adivinhar".
- Exemplo: `ShoppingListState` implementa `AgentContext` para passar o ID da lista aberta.

### 3. AgentAction (As Mãos)
São as "ferramentas" que o agente pode usar. No Cestou, usamos UseCases como ações.
- Para criar uma nova ação:
  1. Crie um UseCase estendendo `AgentActionUseCase`.
  2. Use `@AiAgentAction` para descrever o que a ação faz.
  3. Use `@AiAgentParameter` para definir o que a IA deve fornecer.

## 🚀 Como adicionar uma nova Feature à IA

1. **Defina a Ação**: Se a IA precisa "Excluir um produto", crie o `DeleteProductUseCase`.
2. **Anote o UseCase**:
   ```kotlin
   @AiAgentAction(id = "delete_product", description = "Remove um produto da lista")
   @AiAgentParameter(name = "productId", type = "string", description = "ID único do produto")
   class DeleteProductUseCase : AgentActionUseCase<Unit>() { ... }
   ```
3. **Registre no DI**: Certifique-se de que o UseCase seja injetado no `AgentRegistry` via Hilt.
4. **Contexto**: Se a ação depende de onde o usuário está, implemente `AgentContext` no ViewModel/State correspondente.

## 🛠️ Diretrizes de Prompting
- O Agente deve ser conciso.
- Sempre use o `AgentSession` (Locale) para responder no idioma correto.
- Nunca invente IDs; se um ID não está no contexto, o agente deve perguntar ou usar a ação de busca.
