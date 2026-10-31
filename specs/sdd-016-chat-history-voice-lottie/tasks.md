# Task: Histórico de Conversas com IA, Entrada de Voz Nativa (Speech-to-Text) e Animação Lottie de Pensamento

- **Slug:** chat-history-voice-lottie
- **Autor do plano:** Code-Planner (SDD) + Code-Implementer
- **Data:** 2026-03-30
- **Status:** ready (Implemented & Verified)
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- Evolução do chat com IA (`AiChatScreen`) para suportar gerenciamento de múltiplas sessões de conversa e histórico persistido via `ChatHistoryRepository`.
- Integração da tela de histórico (`AiHistoryScreen`) acessível pelo ícone de histórico no topo do chat, permitindo alternar entre conversas anteriores, excluir sessões ou iniciar novos chats (com retorno automático para o chat ativo).
- Suporte a entrada de voz nativa utilizando o intent do sistema Android (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) via `ActivityResultContracts.StartActivityForResult()`, garantindo compatibilidade total e confiabilidade em dispositivos físicos (com permissão `RECORD_AUDIO` e configuração `<queries>` no `AndroidManifest.xml`).
- Integração da animação Lottie `ai_thinking_loader.json` para o estado de carregamento/pensamento da IA (`TypingBubble`).

## 2. Escopo Implementado
- **Inclui:**
  - Adição da animação Lottie `ai_thinking_loader.json` em `feature/chat/src/main/res/raw/`.
  - Dependência `implementation(libs.lottie.compose)` em `feature/chat/build.gradle.kts`.
  - Entidade de domínio `ChatSession` e DTO `ChatSessionModel` / `ChatSessionsIndexModel`.
  - Atualização de `ChatHistoryRepository` e `ChatHistoryRepositoryImpl` para gerenciamento e indexação de múltiplas sessões de chat.
  - Implementação de `AiHistoryScreen.kt` e `AiHistoryViewModel.kt` para listagem, seleção e exclusão de conversas.
  - Conexão da rota `AiHistory` no `ChatGraph.kt` com navegação baseada em `sessionId` e `popUpTo(AiHistory) { inclusive = true }`.
  - Configuração do `AndroidManifest.xml` em `:app` com a permissão `RECORD_AUDIO` e `<queries>` para serviços de reconhecimento de voz.
  - Atualização de `AiChatScreen.kt` e `AiChatViewModel.kt` para uso do Reconhecimento de Voz Nativo do Android (`RecognizerIntent`).
  - Testes unitários cobrindo repositório e ViewModels (`ChatHistoryRepositoryImplTest`, `AiChatViewModelTest`, `AiHistoryViewModelTest`, `AiChatViewModelReliabilityTest`).

## 3. Impacto Arquitetural
- Módulos afetados: `:feature:chat`, `:core:navigation`, `:app`.
- Padrões aplicados: Clean Architecture + MVI, Nav3, Hilt DI, StorageService (DataStore).

## 4. Plano de Testes e Validação
- **Testes Unitários:** `./gradlew :feature:chat:testDebug` (33 testes executados com sucesso).
- **Lint Android:** `./gradlew :feature:chat:lintDebug` (Aprovado sem erros).
- **Detekt:** `./gradlew :feature:chat:detekt` (Aprovado sem violações).

## 5. Checklist de Conformidade
- [x] Código implementado conforme o plano SDD.
- [x] Testes unitários adicionados e passando.
- [x] Verificações de lint e detekt sem erros.
- [x] Funcionalidade de voz testada em dispositivo físico com intent nativo.
