# Task: Converter Tela de Chat em ModalBottomSheet com Histórico e Entrada de Voz

- **Slug:** chat-bottom-sheet-voice
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2025-05-17
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- O usuário solicitou converter a tela de chat (`AiChatScreen`) em um `ModalBottomSheet` acessível via navegação em diálogo/bottom sheet (`chatGraph`), melhorando a experiência contextual sem sair da tela atual (carrinho ou lista de compras).
- O layout deve conter:
  1. Ícone de fechar à direita.
  2. Ícone de histórico à esquerda (abrindo lista de conversas anteriores).
  3. Texto de boas-vindas: "Como Cestou pode lhe ajudar?".
  4. Input de mensagem com botão dinâmico: ícone de enviar (`Send`) quando houver texto, ou ícone de microfone (`Mic` / voice input) quando vazio, permitindo gravação/transcrição de áudio para texto para a IA processar.
- Referência: [ChatGraph.kt](file:///Users/bruno/Developers/How-Much/feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/navigation/ChatGraph.kt#L21-L35), [AiChatScreen.kt](file:///Users/bruno/Developers/How-Much/feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/presentation/screen/AiChatScreen.kt#L107-L170).

## 2. Escopo
- **Inclui:**
  - Atualizar `chatGraph` em `:feature:chat` para exibir `AiChatScreen` dentro de um `ModalBottomSheet` (ou estratégia de overlay/dialog do Nav3).
  - Redesenhar o cabeçalho e os controles de topo de `AiChatScreen`: ícone de histórico (esquerda), título/boas-vindas, ícone de fechar (direita).
  - Implementar lista de histórico de conversas anteriores (acessível pelo botão de histórico).
  - Implementar input de mensagem com botão dinâmico (troca automática entre Send e Microfone).
  - Integrar suporte a gravação/transcrição de áudio (Speech-to-Text via `RecognizerIntent` ou callback de áudio).
  - Atualizar testes unitários e de UI (`AiChatScreenTest`, `AiChatViewModelTest`).
- **Exclui:**
  - Alterações no backend do OpenRouter ou modelos Gemini no `core:ai`.

## 3. Impacto Arquitetural
- Módulo e camada afetados: `:feature:chat` (`presentation/screen`, `presentation/viewmodel`, `presentation/intent`, `navigation`), `:core:navigation`.
- Padrão aplicado: Clean Architecture + MVI, Nav3 BottomSheet/Dialog strategy ou `ModalBottomSheet` compose nativo.
- Novos arquivos:
  - `feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/presentation/components/ChatHistoryBottomSheet.kt`
  - `feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/presentation/components/VoiceInputHelper.kt` (ou wrapper para SpeechRecognizer)
- Diagrama ASCII:
  ```
  CartScreen / ShoppingScreen
       │ (navigate(AiChat))
       ▼
  ChatGraph (ModalBottomSheet)
       │
       ├─► AiChatScreen (BottomSheet content)
       │    ├─ TopBar: [Histórico] ... [Como Cestou pode lhe ajudar?] ... [Fechar]
       │    ├─ Message List
       │    └─ InputBar: [TextField] [Send / Mic (Voice)]
       └─► ChatHistoryBottomSheet (Conversas anteriores)
  ```

## 4. Contratos e Interfaces
- Novos intents em `AiChatIntent`:
  - `val onToggleHistory: () -> Unit`
  - `val onSelectHistoryItem: (String) -> Unit`
  - `val onStartVoiceInput: () -> Unit`
- Novos campos em `AiChatUiState`:
  - `val showHistory: Boolean = false`
  - `val previousConversations: List<ChatSessionSummary> = emptyList()`
  - `val isListening: Boolean = false`
- Visibilidade: `internal` para componentes de apresentação, rotas públicas inalteradas em `:core:navigation`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Tela de chat ocupava a tela inteira (`composable<AiChat>`) | Chat abre como `ModalBottomSheet` com alinhamento de diálogo/overlay | [ChatGraph.kt](file:///Users/bruno/Developers/How-Much/feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/navigation/ChatGraph.kt#L21) |
| 2 | Botão de envio fixo com ícone `Send` | Botão dinâmico: `Send` se `text.isNotBlank()`, `Mic` se vazio | [AiChatScreen.kt](file:///Users/bruno/Developers/How-Much/feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/presentation/screen/AiChatScreen.kt) |
| 3 | Sem acesso rápido a histórico na barra superior | Ícone de histórico à esquerda abre lista de conversas anteriores | [AiChatScreen.kt](file:///Users/bruno/Developers/How-Much/feature/chat/src/main/java/br/com/brunocarvalhs/howmuch/feature/chat/presentation/screen/AiChatScreen.kt) |

## 6. Critérios de Aceitação
- **Performance:** Recomposições otimizadas no input e na lista de mensagens usando `remember` e chaves estáveis.
- **Observabilidade:** Analytics events para abertura de histórico, uso de voz e envio de mensagem.
- **Compatibilidade:** Suporte total a minSdk 28 com tratamento de permissão de áudio (`Manifest.permission.RECORD_AUDIO`) e fallback gracioso se o SpeechRecognizer não estiver disponível no dispositivo.

## 7. Plano de Testes
- **Unitários:**
  - `AiChatViewModelTest`: testar transição de estado ao acionar histórico, gravação de voz e envio condicional.
- **Instrumentados / UI:**
  - `AiChatScreenTest`: verificar presença do ModalBottomSheet, botões de fechar, histórico, texto de boas-vindas e alternância entre Mic e Send.
- **Comandos de Qualidade:**
  - `./gradlew :feature:chat:testDebug`
  - `./gradlew lintDebug`
  - `./gradlew detekt`

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Falta de permissão de microfone em runtime | Média | Média | Solicitar permissão com `ActivityResultContracts.RequestPermission` e exibir mensagem amigável caso negada. |
| Altura do BottomSheet cortando o teclado | Alta | Média | Utilizar `ModalBottomSheet` com `skipPartiallyExpanded = true` e `Modifier.imePadding()`. |

## 9. Perguntas em Aberto (bloqueantes)
- [ ] Nenhuma pergunta bloqueante. A transcrição de voz usará `RecognizerIntent` nativo do Android com fallback simulado para testes.

## 10. Checklist de Conformidade
- [ ] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [ ] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [ ] Seção 6 preenchida de forma substantiva.
- [ ] Mudança em símbolo público reflete `breaking` em "Impacto público".
- [ ] Novo endpoint/remote-config: toggle definido, observabilidade planejada.
- [ ] Novo cache: armazenamento justificado.
- [ ] Perguntas em aberto exauridas.
