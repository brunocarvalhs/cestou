# Task: Auditar o projeto e montar o backlog de crashes, bugs de lógica e funcionalidades faltantes

- **Slug:** auditoria-geral
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** draft
- **Versão-alvo:** 2.2.2 (ondas 0–3, correções) e 2.3.0 (onda 4, funcionalidades)
- **Tipo de mudança:** fix (backlog com itens de feature e refactor)
- **Impacto público (módulo):** breaking, só no item B-01 (`NetworkService` em `core:domain`). Os demais itens são `none` ou `additive` (ver cada item).

> Este documento é um **plano de backlog**, não um plano de implementação único. Cada item (C-xx, B-xx, F-xx, D-xx) deve virar um SDD próprio (`specs/sdd-0NN-<slug>/tasks.md`) com `status: ready` antes de ir para o `code-implementer`. Os IDs sugeridos estão na seção 2.5.

## 1. Contexto e Motivação

- Pedido: "quero um SDD completo do projeto e uma lista de tasks com o que falta de funcionalidade, bugs de lógica e possíveis crashes" (`specs/tasks.txt`, entrada `sdd-001-auditoria-geral`).
- Método: leitura estática dos repositórios, use cases, ViewModels, agentes de IA, billing, Wear e `firestore.rules`, além de buscas por padrões de risco (`!!`, `valueOf`, `toInt()`, `[0]`, `first()` em Flow infinito, `catch (Exception)`). Nenhum teste foi rodado e nada foi reproduzido em dispositivo.
- Achado estrutural: `NetworkServiceImpl.make()` engole toda exceção e retorna `null` (`core/data/.../network/NetworkServiceImpl.kt:71-77`). Os repositórios envolvem essa chamada em `runCatching` esperando falhas que nunca chegam. Por isso, gravações que falham parecem ter dado certo. Esse comportamento está por trás de vários bugs abaixo, e o próprio código já o registra em `ShoppingRepositoryImpl.kt:147`.
- Rastreabilidade arquitetural: `architecture/01` a `05`. Divergências entre a documentação e o código estão nos itens D-xx.

**Legenda de severidade:** **P0**: perda de dados, segurança ou indisponibilidade geral. **P1**: o fluxo principal quebra ou o resultado fica errado sem aviso. **P2**: comportamento incorreto com contorno possível. **P3**: risco baixo, dívida técnica ou latente.
**Confiança:** **CONFIRMADO**: o caminho do defeito foi lido no código de ponta a ponta. **PLAUSÍVEL**: depende de condição externa (Crashlytics, provedor, timing) e precisa ser confirmado antes de corrigir.

## 2. Escopo

- **Inclui:** o backlog abaixo (2.1–2.4), a ordem de execução (2.5) e, para cada item, evidência (`arquivo:linha`), cenário de falha e correção proposta.
- **Exclui:** qualquer alteração de código de produção, de `firestore.rules` ou de configuração do Firebase. Também ficam de fora o deploy das regras e a escolha de novas dependências, que serão decididos nos SDDs filhos.

Caminhos abreviados: `FS` = `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping`, `FP` = `feature/products/.../feature/products`, `FC` = `feature/cart/.../feature/cart`, `FA` = `feature/ai-agent/.../feature/ai_agent`, `FH` = `feature/chat/.../feature/chat`, `FT` = `feature/settings/.../feature/settings`, `CD` = `core/data/.../core/data`.

### 2.1 Possíveis crashes (C)

| ID | Sev. | Conf. | Defeito | Evidência | Cenário de falha | Correção proposta |
|---|---|---|---|---|---|---|
| C-01 | P1 | PLAUSÍVEL | `Navigator` é singleton e `MainActivity` usa `launchMode` padrão. Um link de convite abre uma **segunda** instância da Activity, que faz `bind`. Quando ela fecha, o `unbind` por identidade zera o navigator e a primeira instância fica sem NavController. | `core/navigation/.../Navigator.kt:23`; `app/.../MainActivity.kt:100,108,186`; `app/src/main/AndroidManifest.xml` (sem `launchMode`, intent-filter `/join` na linha 82) | App aberto → usuário toca o link `https://how-much-2a72e.web.app/join/...` em outro app → volta → toca numa lista → `IllegalStateException: Navigator not bound to a NavController`. O mesmo erro já aparece no Crashlytics (comentário em `MainActivity.kt`). | `launchMode="singleTask"` e tratar `onNewIntent` (ver B-30). Fazer `Navigator.navigate/goBack` ignorar e registrar em log quando não houver vínculo, em vez de lançar exceção. No médio prazo, escopo de Activity (`@ActivityRetainedScoped`) em vez de `@Singleton`. |
| C-02 | P2 | CONFIRMADO | Parse inseguro do horário do lembrete na UI. | `FT/presentation/screen/NotificationSettingsScreen.kt:97-98` (`timeParts[0].toInt()`, `timeParts[1]`) | `reminderTime` vazio, legado ou corrompido no DataStore → `NumberFormatException`/`IndexOutOfBoundsException` ao abrir o seletor de horário, e a composição quebra. | Reaproveitar o parse seguro que já existe em `FT/data/worker/ShoppingReminderScheduler.kt:48-53` (extrair para `core:common`). |
| C-03 | P2 | CONFIRMADO | `getOrCreateUserId()` lança `IllegalStateException` sem sessão (`core/auth/.../FirebaseAuthService.kt:109`) e é chamado dentro de `viewModelScope.launch` sem tratamento. | `FC/presentation/viewmodel/EditItemViewModel.kt:34`; `ConfirmItemViewModel.kt:34`; `FP/domain/usecase/ProductSaveUseCase.kt:53` (chamado em `ProductFormViewModel.kt:105` e `ProductPhotoViewModel.kt`) | Sessão revogada ou logout em outro fluxo com a sheet aberta, ou restauração de processo com sessão expirada → exceção não tratada → crash. | Use cases retornam `Result` (envolver em `runCatching`) e os VMs tratam `onFailure`. Avaliar um `CoroutineExceptionHandler` padrão nos VMs. |
| C-04 | P3 | CONFIRMADO | `valueOf` sem proteção ao ler o histórico do chat. | `FH/data/mapper/ChatMessageMapper.kt:10` | Histórico persistido com `sender` desconhecido (versão antiga ou corrupção) → `IllegalArgumentException` ao abrir o chat. | `runCatching { valueOf }` e descartar a mensagem inválida. |
| C-05 | P3 | CONFIRMADO (latente) | `value!!` e `whereIn` sem validação no cliente Firestore. | `CD/network/FirestoreClient.kt:143,146,149` | Filtro com valor `null`, ou lista vazia / com mais de 30 itens → `IllegalArgumentException`. Hoje nenhum chamador faz isso, mas nada impede. | Validar a entrada e devolver erro tipado. |
| C-06 | P3 | CONFIRMADO (latente) | `LocalViewModelStoreOwner.current!!`. | `FP/presentation/screen/ProductScreen.kt:53` | Composição fora de um owner (preview ou teste) → NPE. | `checkNotNull` com mensagem, ou receber o owner por parâmetro. |

### 2.2 Bugs de lógica (B)

| ID | Sev. | Conf. | Defeito | Evidência | Cenário de falha | Correção proposta |
|---|---|---|---|---|---|---|
| B-01 | **P0** | CONFIRMADO | `make()` engole exceções e retorna `null`. Todo `runCatching` nos repositórios retorna sucesso. | `CD/network/NetworkServiceImpl.kt:71-77`; ex.: `FP/data/repository/ProductRepositoryImpl.kt:90-99,120-128,135-144`; `FS/data/repository/ShoppingRepositoryImpl.kt:94-115,147` | Gravação negada (offline expirado, regras AD-009 publicadas, `NOT_FOUND`) → a UI mostra sucesso e o dado se perde sem aviso. Quando as regras do D-01 forem publicadas, toda negação vai passar despercebida. | **BREAKING em `core:domain`**: `make()` passa a lançar `NetworkException` (ou retornar `Result<T?>`). Migrar todos os chamadores (`grep "networkService.make"`). Pré-requisito de F-01. |
| B-02 | **P0** | CONFIRMADO | Duplicar lista: (a) `collect` sobre o snapshot listener (Flow infinito) nunca termina; (b) usa `update` (PUT → `update()` do Firestore em documento inexistente → `NOT_FOUND`, engolido) em vez de `save`; (c) copia `users`, `roles`, `shortCode`, `budget`, `price` e `position` da lista original. | `FS/domain/usecase/ShoppingDuplicateUseCase.kt:18,29,31`; `firestore.rules:108-112` | Duplicar → a coroutine fica presa para sempre, `fetchAll()` nunca roda e a cópia fica **sem produtos**. Duplicar uma lista compartilhada → a cópia pertence ao dono original, inclui todos os membros e reaproveita o código de convite. | `first()` em vez de `collect`; `save` em vez de `update`; `users=[eu]`, `roles={eu: OWNER}`, `shortCode` novo, `price=0`, `budget` mantido (confirmar, P-02). Gravar o índice do código. |
| B-03 | P1 | CONFIRMADO | Finalizar compra sobrescreve `description` com o nome do estabelecimento e grava o documento inteiro a partir da cópia (desatualizada) que veio na rota. O resultado não é checado. | `FC/presentation/viewmodel/FinishPurchaseViewModel.kt:45,48` | A descrição escrita pelo usuário some, e alterações de outros membros (título, orçamento, favorito) são revertidas. A tela volta mesmo se a gravação falhar. | Atualização parcial (`status`, `price` e um campo de estabelecimento: P-03). Navegar só em caso de sucesso. |
| B-04 | P1 | CONFIRMADO | Escritas com o documento inteiro a partir de snapshots desatualizados (vence a última gravação). | `FS/presentation/viewmodel/EditShoppingViewModel.kt:34` (`initialShopping` da rota); `ShoppingListViewModel` (ToggleFavorite com o item da lista); `FS/data/repository/ShoppingRepositoryImpl.kt:106-115` | Dois membros editam a mesma lista → um apaga a mudança do outro. | Adicionar `ShoppingRepository.updateFields(id, Map)` (aditivo) e usar mapas parciais, como `updatePositions` e `leave` já fazem. |
| B-05 | P1 | CONFIRMADO | Não é possível **remover** o orçamento: o PUT descarta campos `null`. | `CD/network/FirestoreClient.kt:99` | O usuário apaga o orçamento → salva → o orçamento antigo continua lá. | Para campos anuláveis limpos, enviar `FieldOp.Delete` (já existe, `FirestoreClient.kt:103-104`). |
| B-06 | P1 | CONFIRMADO | A reordenação manual das listas não se mantém: a ordenação usa `createdAt` ou nome e ignora `position`. As posições também são calculadas só sobre a lista filtrada. | `FS/presentation/viewmodel/ShoppingListViewModel.kt:229-232,272` | O usuário arrasta uma lista → na próxima emissão do Firestore a ordem volta. Com filtro ativo, as posições colidem com as das listas ocultas. | Modo de ordenação `MANUAL` por `position` e cálculo sobre a lista completa. |
| B-07 | P1 | CONFIRMADO | O seletor de modelo de IA não tem efeito em 10 dos 11 modelos: o provider salvo é o fabricante (`nvidia`, `google`, `cohere`...), e a fábrica só aplica o modelo escolhido quando o provider é `openrouter` ou `gemini`. | `CD/repository/SettingsRepositoryImpl.kt:94-95`; `FA/data/service/AiAgentFactoryImpl.kt:54,61,87`; `core/domain/.../model/AiModel.kt` | O usuário escolhe "Nemotron 3 Ultra" → recebe o modelo padrão do `BuildConfig`, via `FallbackAiAgent`. `contains("gemini")` mandaria um id do OpenRouter para o SDK do Gemini. | Separar `vendor` de `provider` em `AiModel`: todos os `freeModels` usam o provider `openrouter`. Migrar o valor já salvo no DataStore. |
| B-08 | P1 | CONFIRMADO | `FallbackAiAgent` nunca usa o agente secundário: os agentes capturam toda exceção e emitem uma mensagem de erro amigável, então o `.catch` não dispara. | `FA/data/service/FallbackAiAgent.kt:22`; `GeminiAiAgent.kt:120`; `OpenRouterAiAgent.kt:141` | O OpenRouter cai → o usuário vê a mensagem de erro, embora o Gemini estivesse disponível. | Erros de transporte lançam `AiProviderUnavailableException`, e só o agente mais externo converte em texto. Fazer o fallback **só se nenhuma ferramenta já tiver sido executada**, para não repetir efeitos colaterais. |
| B-09 | P1 | CONFIRMADO | Falha de ferramenta é informada ao modelo como `"Sucesso"`. Um sucesso `Unit` vira `"kotlin.Unit"`. | `FA/data/service/GeminiAiAgent.kt:142`; `OpenRouterAiAgent.kt:181` | Adicionar produto falha → o modelo responde "Adicionei o leite!" sem ter adicionado. | Mapear `Result.failure` para `"Erro: <mensagem>"` e `Unit` para `"OK"`. |
| B-10 | P1 | CONFIRMADO | O loop de tool calls não tem limite. | `FA/data/service/GeminiAiAgent.kt:92`; `OpenRouterAiAgent.kt:104` | Um modelo gratuito fraco repete a chamada de ferramenta → loop sem fim, custo e chat travado. | `MAX_TOOL_ROUNDS` (ex.: 5) e resposta de fallback ao atingir o limite. |
| B-11 | P1 | CONFIRMADO | O histórico da IA nunca é limpo: `AiAgentSession` é singleton e `clearHistory()` não tem chamadores. | `FA/domain/orchestrator/AiAgentOrchestrator.kt:36` | O contexto da lista A vaza para a lista B. No mesmo aparelho, **depois do logout, o próximo usuário recebe o contexto da conversa do anterior** (privacidade). | Limpar no logout (mesmo ponto de `MainViewModel.signOutWatchOnSignOut`) e quando o `shoppingId` mudar, ou criar uma sessão por `shoppingId`. |
| B-12 | P1 | CONFIRMADO | O trial gratuito do chat é consumido antes da resposta, até para PRO e em caso de falha. O gate começa como `true`. | `FH/presentation/viewmodel/AiChatViewModel.kt:55`; `ChatAiGateViewModel.kt:35` | Falha de rede na primeira mensagem → trial perdido. Um FREE que já usou o trial consegue enviar antes de os flows carregarem. | Marcar como usado só depois de uma resposta bem-sucedida e se não for PRO. Valor inicial `false` (ou estado de carregamento). |
| B-13 | P1 | CONFIRMADO | O spinner do chat fica infinito quando o provedor não retorna texto (o Flow termina sem emitir). Erros não aparecem para o usuário, e é possível enviar de novo durante o carregamento. | `FH/presentation/viewmodel/AiChatViewModel.kt:69-83`; `FA/data/service/GeminiAiAgent.kt:102`; `OpenRouterAiAgent.kt` (bloco `response.choices.firstOrNull()?.message?.content`) | Resposta vazia ou só com tool calls → `isLoading` continua `true` para sempre. | `onCompletion { isLoading=false }`, mensagem de erro no estado e bloqueio de envio enquanto `isLoading`. |
| B-14 | P2 | CONFIRMADO | Entrar numa lista: um token com mais de 8 caracteres é tratado como id; o join acontece antes de validar; dá para entrar em lista finalizada; reentrar renotifica todos os membros. | `FS/domain/usecase/ShoppingJoinUseCase.kt:22,30,38` | O usuário abre o mesmo convite duas vezes (ou gira a tela, ver B-30) → os membros recebem `list_joined` repetido. | Se já for membro, não notificar. Definir se lista finalizada aceita entrada (P-08). |
| B-15 | P2 | CONFIRMADO | Excluir lista pela IA não verifica se o usuário é dono nem pede confirmação. `ShoppingMultiDeleteUseCase` é cópia exata do Delete (mesma descrição). | `FS/domain/usecase/ShoppingDeleteUseCase.kt:28`; `ShoppingMultiDeleteUseCase.kt:22` | Um EDITOR pede "apaga essa lista" no chat → a lista some para todos (as regras atuais não foram publicadas, ver D-01). Duas tools idênticas confundem o modelo. | Verificar o papel OWNER no use case, não só no VM. Remover `delete_multi_shopping_list` ou implementar exclusão em lote de verdade. |
| B-16 | P2 | CONFIRMADO | `compare_prices` devolve preços **fixos e simulados**, que a IA apresenta como reais. | `FP/domain/usecase/PriceComparisonUseCase.kt:28` | O usuário pergunta "onde o arroz está mais barato?" → recebe dados inventados. | Retirar a tool do registry até existir uma fonte real (P-05). |
| B-17 | P2 | CONFIRMADO | Mover produto apaga primeiro e salva depois. | `FP/domain/usecase/ProductsUseCaseImpl.kt:26` | Lista de destino finalizada (`checkIsLocked` falha) → o produto some das duas listas. | Salvar no destino, depois apagar na origem (ou `WriteBatch`). |
| B-18 | P2 | CONFIRMADO | O status `IN_PROGRESS` nunca é definido automaticamente, só ao reabrir. | `FS/domain/usecase/ShoppingReopenUseCase.kt:29` (único ponto); `ShoppingItem.kt:217-229` exibe o rótulo | A lista fica "Nova" até ser finalizada, e o rótulo "Em andamento" quase nunca aparece. | Mudar de `NEW` para `IN_PROGRESS` ao marcar o primeiro produto como comprado (regra nova: atualizar `architecture/02` §1). |
| B-19 | P2 | CONFIRMADO | Autoria da compra inconsistente: marcar como comprado quando o produto já tem preço não registra `PURCHASED`. | `FC/presentation/viewmodel/CartViewModel.kt:178,183` (compare com `ConfirmItemViewModel.kt`) | O histórico e o "comprado por" ficam vazios na maioria das compras. | `withActivity(PURCHASED, userId)` em todos os caminhos. Definir o registro ao desmarcar. |
| B-20 | P2 | CONFIRMADO | Os caminhos de IA em lote não registram autoria, não validam a entrada e ignoram falhas. `checkIsLocked` faz uma leitura da lista por produto (N+1). | `FP/domain/usecase/ProductListSaveUseCase.kt:31,53`; `CommonProductAddAllToShoppingUseCase.kt:31`; `RecipeAddToListUseCase.kt:57`; `ShoppingClearPurchasedUseCaseImpl.kt:31`; `FP/data/repository/ProductRepositoryImpl.kt:275` | `"leite,,pão"` cria um produto sem nome. Adicionar a cesta básica faz 16 leituras de `shopping/{id}`. | Filtrar nomes em branco, usar `withActivity(ADDED)`, verificar o lock uma vez por operação e agregar as falhas. |
| B-21 | P2 | CONFIRMADO | Um único documento malformado esconde **todas** as listas ou produtos: o `List<T>` é decodificado como um bloco só. | `CD/network/NetworkServiceImpl.kt:126-134` | Um produto com campo inválido → o carrinho aparece vazio. | Decodificar elemento a elemento, descartar e registrar os inválidos. |
| B-22 | P2 | CONFIRMADO | O snapshot listener fecha de vez no primeiro erro. O repositório emite lista vazia e o Flow termina. | `CD/network/FirestoreClient.kt:222,240`; `FS/data/repository/ShoppingRepositoryImpl.kt:49-53` | Erro transitório (permissão, rede, App Check) → "Nenhuma lista" até a tela ser recriada. | `retryWhen` com backoff e estado de erro exposto em vez de lista vazia. |
| B-23 | P2 | CONFIRMADO | A heurística de "decrypt" decodifica qualquer string que seja Base64URL válida e volte idêntica ao recodificar. `EXCLUDED_KEYS = setOf("")` faz todo campo passar por ela. | `CD/security/Encryptor.kt:109`; `CD/network/NetworkServiceImpl.kt:169` | Um valor `"QUJD"` gravado em texto é lido como `"ABC"`. A chance é baixa, mas a corrupção é silenciosa (nomes, códigos, ids). "Criptografia" aqui é só Base64. | Marcar valores legados com prefixo de versão, ou migrar e remover a decodificação. |
| B-24 | P2 | CONFIRMADO | Billing: `refresh` ignora o `responseCode`; `connect()` pode ficar suspenso para sempre; `refresh` só roda no paywall (o KDoc diz "on every app resume"); o entitlement é por aparelho, não por conta. | `core/billing/.../PlayBillingSubscriptionRepository.kt:45,102,163`; único chamador `feature/subscription/.../PaywallViewModel.kt:40` | Play indisponível → lista vazia → um usuário **PRO vira FREE**. Assinatura cancelada continua PRO até o app reiniciar. | Só atualizar o status com `OK`. Timeout e retry no `connect`. `refresh` em `ON_RESUME` (ProcessLifecycleOwner). Entitlement por conta: P-07. |
| B-25 | P1 | CONFIRMADO | Excluir a conta pode apagar a conta e deixar as listas: `getAll()` retorna lista vazia em caso de erro. Notificações destinadas ao usuário também ficam para trás (as regras negam delete). | `FT/domain/usecase/DeleteAccountUseCase.kt:27-36`; `FS/data/repository/ShoppingRepositoryImpl.kt:71-82`; `firestore.rules:358` | Rede instável na exclusão → o perfil e a conta são apagados, mas as listas e produtos com dados do usuário ficam órfãos (LGPD). | Falhar se a leitura falhar (depende de B-01). Limpar `notifications` do usuário (exige regra de delete pelo destinatário). |
| B-26 | P3 | CONFIRMADO | "Excluir todos os dados" só limpa o DataStore local e o cache. Os dados no Firestore continuam lá. | `FT/domain/usecase/DeleteAllDataUseCase.kt`; `CD/repository/SettingsRepositoryImpl.kt:139` | Se a UI prometer apagar os dados da conta, a promessa não é cumprida (P-04). | Renomear para "limpar dados locais" ou implementar a exclusão remota. |
| B-27 | P2 | CONFIRMADO | A IA pode sobrescrever `roles` e `status` com valores livres: `valueOf` lança exceção quando o status vem fora do enum. | `FS/domain/usecase/ShoppingUpdateUseCase.kt:55,57-58` | O LLM manda `status:"finished"` → exceção (vira texto de erro e não-fatal no Crashlytics). O LLM manda `roles:{}` → o dono perde OWNER. | Remover `users` e `roles` dos argumentos aceitos pela IA. Parse tolerante de status (`entries.firstOrNull { equals(ignoreCase) }`). |
| B-28 | P3 | CONFIRMADO | A cesta básica é recriada quando o usuário a esvazia de propósito. Os itens padrão são fixos em pt-BR. | `FP/data/repository/CommonProductRepositoryImpl.kt:52,76-93` | O usuário remove tudo → ao usar "adicionar cesta" os 16 itens voltam. | Flag "já semeado" por usuário. Nomes vindos de recursos (ver F-09). |
| B-29 | P3 | PLAUSÍVEL | O idioma inicial `"pt-BR"` é aplicado antes de o DataStore carregar. | `app/.../MainViewModel.kt:116`; `MainActivity.kt` (`LaunchedEffect(language)`) | Usuário em EN → `setApplicationLocales("pt-BR")` e depois `("en")` → recriação extra ou flash na abertura. | Valor inicial `null` e não aplicar enquanto não carregar. |
| B-30 | P3 | CONFIRMADO | O deep link de convite só é lido no intent de abertura (sem `onNewIntent`). O token se perde se o usuário não estiver logado e é reprocessado após recriação da Activity. | `app/.../MainActivity.kt:186-193` | Usuário deslogado abre o convite → login → o convite se perdeu. Girar a tela → `JoinList` é empilhado de novo (ver B-14). | Guardar o token pendente no VM/SavedState, consumir uma única vez, tratar `onNewIntent` (junto com C-01). |
| B-31 | P3 | CONFIRMADO | O `shortCode` é gerado sem checar colisão. A criação do índice falha sem aviso (B-01). | `FS/domain/usecase/ShoppingCreateUseCase.kt:54`; `FS/data/repository/ShoppingRepositoryImpl.kt:185-194` | Colisão (rara, 36⁶) → a regra nega o índice → o código não resolve ou aponta para a lista errada no fallback legado. | Tentar de novo com código novo quando o índice já existir. |
| B-32 | P3 | CONFIRMADO | Código morto com violação de exception transparency: `getSuggestions` e `getQuestionSuggestions` não têm chamadores e emitem dentro de `runCatching`. | `FP/data/repository/ProductRepositoryImpl.kt:62-84,180-211` | Se alguém passar a usá-los com `first()`, o `AbortFlowException` é capturado e o `emit` seguinte lança `IllegalStateException`. | Remover, ou reescrever com `catch {}` do Flow. |
| B-33 | P3 | CONFIRMADO | `OpenRouterAiAgent` captura `CancellationException` (`catch (e: Exception)` sem relançar). | `FA/data/service/OpenRouterAiAgent.kt:132,182` | Sair do chat no meio da resposta → não-fatal falso no Crashlytics e `emit` num Flow cancelado. | Relançar `CancellationException`, como `GeminiAiAgent.kt` já faz. |
| B-34 | P3 | PLAUSÍVEL | O corte do histórico do Gemini (20 entradas a partir do início) pode separar `functionCall` de `functionResponse`, ou começar com papel `model`. | `FA/data/service/ConversationHistory.kt:13-18`; `GeminiAiAgent.kt:107` | Conversa longa com ferramentas → erro 400 do Gemini → mensagem de erro genérica. | Cortar em fronteiras de turno do usuário. |

### 2.3 Funcionalidades faltantes (F)

| ID | Sev. | Lacuna | Evidência | Proposta |
|---|---|---|---|---|
| F-01 | P1 | Não há feedback de erro nem estado offline nas gravações (adicionar, editar, comprar, finalizar). | VMs só tratam `onSuccess`: `EditItemViewModel.kt`, `ConfirmItemViewModel.kt`, `CartViewModel.kt:170-200`; depende de B-01 | Snackbar/`UiText` de erro padronizado e indicador de conectividade. |
| F-02 | P1 | Não existe UI para sair da lista, remover membro ou transferir a propriedade. `leave()` existe, mas só a exclusão de conta usa. | `FS/data/repository/ShoppingRepositoryImpl.kt:148`; único chamador `DeleteAccountUseCase.kt:30`; regras (a) e (d) em `firestore.rules:145-177` | Ação "Sair da lista" para EDITOR, e "Remover membro" / "Tornar dono" para OWNER. |
| F-03 | P1 | Orçamento não aparece no carrinho: dá para editar, mas não há progresso nem alerta de estouro (só o tile do Wear usa o valor). | `grep budget feature/cart` sem resultado; `wear/.../LastListTileService.kt` | Barra de progresso total/orçamento no `CartDetailHeader` e aviso ao ultrapassar. |
| F-04 | P2 | Não há push: `list_joined` e `list_finished` só aparecem dentro do app. Notificações não podem ser apagadas nem paginadas. | Nenhuma referência a `FirebaseMessaging`; `CD/repository/NotificationRepositoryImpl.kt:9-22` (sem `orderBy`/`limit`); `firestore.rules:358` | FCM exige backend (P-07). Até lá: `limit` + `orderBy(timestamp)` e regra de delete pelo destinatário. |
| F-05 | P2 | Tocar no lembrete diário não faz nada, e o ícone é o do sistema. | `FT/data/worker/ShoppingReminderWorker.kt:83-84` (sem `setContentIntent`, TODO) | `PendingIntent` para `MainActivity` (lista padrão) e ícone monocromático. |
| F-06 | P2 | Não há desfazer para ações destrutivas (excluir produto ou lista, limpar comprados). | `CartViewModel.kt` `deleteProduct`/`clearPurchased` | Snackbar "Desfazer" com remoção adiada ou regravação. |
| F-07 | P2 | Textos visíveis gerados ou padrão estão fixos em pt-BR (viola `AGENTS.md` §5). | `FP/data/repository/ProductRepositoryImpl.kt:168,181,267` ("Produto Desconhecido", sugestões); `CommonProductRepositoryImpl.kt:76-93`; prompts em `SystemPrompts.kt` | Strings em `strings.xml` (pt-BR e en-US) e prompt com o idioma do usuário. |
| F-08 | P2 | Convite não é preservado durante o login (deep link adiado). | B-30 | Token pendente consumido depois da autenticação. |
| F-09 | P2 | Entitlement PRO não está ligado à conta nem verificado no servidor. | `PlayBillingSubscriptionRepository.kt` (só o cache local do Play) | Exige backend (Play Developer API). Ver P-07. |
| F-10 | P3 | Histórico do produto cresce sem limite (risco para o limite de 1 MB do documento e para o tamanho de argumentos de navegação, já que `EditItemRoute(product)` carrega o histórico). | `core/domain/.../model/Product.kt` (`history`); `CartViewModel.kt` (`EditItemRoute(product, ...)`) | Limitar a N eventos e passar só o `productId` nas rotas. |
| F-11 | P3 | O módulo `:wear` não tem testes. | `find wear/src/test wear/src/androidTest` → 0 arquivos | Testes de VM e do parse de `WearablePaths`. |

### 2.4 Infraestrutura, segurança e documentação (D)

| ID | Sev. | Item | Evidência | Proposta |
|---|---|---|---|---|
| D-01 | **P0** | `firestore.rules` **não está publicado**. Produção roda com a regra placeholder `allow read, write: if request.time < <data>`, que dá acesso total a qualquer pessoa com a config do projeto. **Quando a data expirar, o app inteiro para** (toda leitura e escrita é negada). | `firestore.rules:6-10`; AD-009 em `architecture/05` | Confirmar a data no console (P-01). Publicar as regras depois de B-01, da migração do índice `shortCodes` das listas legadas e do ajuste do Wear (AD-009). |
| D-02 | P2 | A documentação diz "Navigation 3 / `NavKey`", mas o app usa **Navigation Compose** (`rememberNavController`, `NavHost`, `toRoute`). | `AGENTS.md` §2 e §11; `architecture/01` §3; `app/.../MainActivity.kt:91,202`; VMs com `savedStateHandle.toRoute` | Corrigir a documentação ou abrir um SDD de migração (P-06). |
| D-03 | P3 | Há 13 `@Suppress` em código de produção, contra `AGENTS.md` §10 (ex.: `TooManyFunctions` em `ShoppingRepositoryImpl.kt:28`, `UNCHECKED_CAST` em `NetworkServiceImpl.kt:110,144,159`). | `grep -rn "@Suppress"` | Corrigir a causa caso a caso, ou justificar em PR. |
| D-04 | P3 | 7 comentários em `.kt` ainda citam `.specs/` (removido). | ex.: `FC/presentation/viewmodel/CartViewModel.kt` (comentário "G12 in .specs/MVP-ROADMAP.md") | Apontar para `architecture/05` (follow-up do sdd-000). |
| D-05 | P3 | `architecture/02` §6 lista como pendentes: orçamento, cache offline e sincronização do Wear. | `architecture/02-regras-de-negocio.md` §6 | Documentar com citação ao código quando B-05, B-18 e F-03 forem planejados. |

### 2.5 Ordem de execução e SDDs filhos sugeridos

| Onda | Objetivo | Itens | SDD sugerido | Versão |
|---|---|---|---|---|
| 0 | Pré-requisitos: sem isso, os outros consertos não ficam visíveis | D-01 (só verificação), B-01, F-01 | `sdd-002-network-error-contract` | 2.2.2 |
| 1 | Perda de dados e crashes | B-02, B-03, B-04, B-05, B-17, B-25, C-01, C-02, C-03 | `sdd-003-shopping-data-integrity`, `sdd-004-navigator-deeplink`, `sdd-005-crash-hardening` | 2.2.2 |
| 2 | IA: correção e privacidade | B-07, B-08, B-09, B-10, B-11, B-12, B-13, B-15, B-16, B-27, B-33, B-34 | `sdd-006-ai-agent-reliability`, `sdd-007-ai-chat-gate` | 2.2.2 |
| 3 | Regras de domínio e robustez | B-06, B-14, B-18, B-19, B-20, B-21, B-22, B-23, B-24, B-30, B-31 | `sdd-008-shopping-rules`, `sdd-009-firestore-decoding`, `sdd-010-billing-hardening` | 2.2.2 |
| 4 | Funcionalidades | F-02, F-03, F-05, F-06, F-07, F-08 | um SDD por feature | 2.3.0 |
| 5 | Dependem de backend ou são dívida técnica | F-04, F-09, F-10, F-11, B-26, B-28, B-29, B-32, C-04–C-06, D-02–D-05 | a definir | — |

## 3. Impacto Arquitetural

Mapa atual (verificado em `settings.gradle.kts`, `architecture/01` e no código lido):

```
                 ┌──────────────────────── :app (MainActivity, MainViewModel, Navigator bind) ───────────────┐
                 │  Navigation Compose NavHost  ◄── FeatureNavGraph (Set, Hilt multibinding)                 │
                 └───────┬──────────────┬──────────────┬──────────────┬──────────────┬─────────────────────┘
                         ▼              ▼              ▼              ▼              ▼
                :feature:shopping  :feature:cart  :feature:products :feature:chat  :feature:settings ...
                 (VM → UseCase)     (VM)           (UseCase impl.    (VM → CartAssistantUseCase)
                         │              │           de ProductsUseCase)        │
                         └──────┬───────┴──────────────┬────────────────────────┘
                                ▼                      ▼
                       :core:domain (Shopping, Product, ShoppingRepository, ProductsUseCase,
                                     NetworkService  ◄── contrato afetado por B-01)
                                ▼
                       :core:data  NetworkServiceImpl ─► FirestoreClient ─► Firestore
                                   (Encryptor/CompatibilityConverter no caminho de leitura: B-21, B-23)
                       :feature:ai-agent  AiAgentFactoryImpl ─► Fallback(OpenRouter, Gemini) ─► AgentRegistry
                                          (tools = use cases @AiAgentAction de shopping/products)
                       :core:billing  PlayBillingSubscriptionRepository ─► SubscriptionRepository (FREE/PRO)
                       :wear  (sessão Firebase própria; tráfego afetado pelo D-01/AD-009)
```

Pontos de mudança previstos (detalhados nos SDDs filhos):

- `core:domain/services/NetworkService.kt` (B-01): o contrato de `make` muda e todos os repositórios de `core:data` e `feature:*` são migrados.
- `core:domain/repository/ShoppingRepository.kt` (B-04, B-05): `updateFields(id, fields)` aditivo.
- `core:navigation/Navigator.kt` e `app/AndroidManifest.xml` (C-01, B-30).
- `feature:ai-agent/data/service/*` (B-07 a B-10, B-33, B-34) e `AiAgentSession` (B-11).
- `core:billing` (B-24).
- Sem novos módulos. Toda injeção continua via Hilt, sem service locator.

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Item | Consumidores |
|---|---|---|---|---|
| `NetworkService.make(...)` | **BREAKING**: lança `NetworkService.NetworkException` em falha, em vez de retornar `null` (ou passa a retornar `Result<T?>`, a decidir no sdd-002). | `public` (contrato entre `core:domain`, `core:data` e as features) | B-01 | `ShoppingRepositoryImpl`, `ProductRepositoryImpl`, `CommonProductRepositoryImpl`, `UserRepositoryImpl`, `NotificationRepositoryImpl`, `RecipeRepositoryImpl`, `deleteDocumentWithSubCollections` |
| `ShoppingRepository.updateFields(id: String, fields: Map<String, Any?>): Result<Unit>` | Aditivo | `public` (interface em `core:domain`, implementação em `feature:shopping`) | B-03, B-04, B-05 | VMs de shopping e cart |
| `AiModel` | Aditivo: separar `vendor` (exibição) de `provider` (`openrouter`/`gemini`). Migração do valor salvo. | `public` (`core:domain`) | B-07 | `SettingsRepositoryImpl`, `AiSettingsScreen`, `AiAgentFactoryImpl` |
| `AgentAction` / registry | Remove `compare_prices` e `delete_multi_shopping_list`. `update_shopping_list` deixa de aceitar `users`/`roles`. | contrato com o LLM (não é Kotlin público) | B-15, B-16, B-27 | prompts remotos (`ai_system_prompt`), se citarem as tools |
| `Shopping` | Possível campo novo `establishment: String?` | `public` (`core:domain`, Firestore) | B-03 (se P-03 = sim) | mapper `ShoppingModel`, Wear |

Novas coleções, eventos ou flags: nenhuma neste plano. Os SDDs filhos podem propor `feat_*` para F-03 e F-06.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Duplicar copia membros, papéis e código de convite (regra 1.5 de `architecture/02` não fala disso). | A cópia é privada do usuário que duplicou, com código novo e produtos copiados de fato. | `FS/domain/usecase/ShoppingDuplicateUseCase.kt:18-35` |
| 2 | Finalizar grava o estabelecimento em `description`. | A descrição é preservada e o estabelecimento vai para um campo próprio (P-03). | `FC/presentation/viewmodel/FinishPurchaseViewModel.kt:45` |
| 3 | Status: só `NEW` → `FINISH` e, ao reabrir, → `IN_PROGRESS`. | `NEW` → `IN_PROGRESS` no primeiro item comprado. | `ShoppingReopenUseCase.kt:29` |
| 4 | Só o OWNER exclui (verificado apenas no VM). | Só o OWNER exclui, verificado no use case (UI e IA). | `ShoppingListViewModel.kt` (Delete); `ShoppingDeleteUseCase.kt:28` |
| 5 | Trial FREE: uma mensagem, consumida ao enviar. | Consumida só depois de uma resposta bem-sucedida e só para FREE. | `AiChatViewModel.kt:55` (regra 3.3 de `architecture/02`) |
| 6 | O provider de IA é derivado do fabricante do modelo. | Provider explícito por modelo. O modelo escolhido sempre vale. | `SettingsRepositoryImpl.kt:94-95` |
| 7 | Mover produto: apagar e depois salvar. | Salvar e depois apagar (nunca perde o item). | `ProductsUseCaseImpl.kt:26` |
| 8 | Orçamento só pode ser alterado, nunca removido. | Pode ser removido. | `FirestoreClient.kt:99` |
| 9 | Ordem das listas: data de criação ou nome. | Novo modo manual por `position`. | `ShoppingListViewModel.kt:229-232` |

## 6. Critérios de Aceitação

Para cada SDD filho:
- **Rastreabilidade:** o SDD cita o ID deste backlog e cumpre a coluna "Correção proposta", ou justifica por que diverge.
- **Performance:** nada de `collect` sobre listener para obter um único valor (usar `first()`). B-20 elimina as N leituras de `checkIsLocked` por lote. Sem `getById` redundante após cada ação (`ShoppingListViewModel.kt:208` chama `fetchAll()` mesmo havendo listener em tempo real; remover). Coroutines em `Dispatchers.IO` só na camada de dados.
- **Erros:** depois de B-01, nenhuma gravação que falhou aparece como sucesso. A UI mostra o erro (F-01), sem literais (`strings.xml` pt-BR e en-US).
- **IA:** cada tool call devolve sucesso ou erro real ao modelo. Máximo de N rodadas. O histórico não atravessa contas nem listas.
- **Toggles sugeridos** (decisão nos SDDs filhos):

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `feat_budget_progress` | false | remote-config | carrinho igual ao atual | barra de orçamento e alerta (F-03) |
  | `feat_undo_destructive` | false | remote-config | exclusão imediata | snackbar "Desfazer" (F-06) |

- **Observabilidade:** não-fatais no Crashlytics para falhas de rede depois de B-01 (sem dado pessoal). Evento `ai_tool_failed` com `function`, sem argumentos do usuário. O D-01 é acompanhado por métricas de `PERMISSION_DENIED`.
- **Compatibilidade:** minSdk 28. Dados existentes no Firestore continuam legíveis (B-21 e B-23 não podem quebrar documentos legados). Valor de `AI_PROVIDER` já salvo é migrado (B-07). O Wear não pode quebrar antes de o D-01 ser resolvido.

## 7. Plano de Testes

- **Unitários (por item, JUnit + coroutines-test + fakes de `NetworkService`):**
  - B-01: o fake lança `NetworkException` → os repositórios propagam `Result.failure`.
  - B-02: duplicar termina (`runTest` sem timeout), chama `save` N vezes, e a cópia tem `users=[eu]` e `shortCode` novo.
  - B-05: limpar o orçamento envia `FieldOp.Delete`.
  - B-06: depois de mover, a ordem se mantém após uma nova emissão do Flow.
  - B-07: para cada `AiModel.freeModels`, a fábrica usa o id escolhido.
  - B-08: o primário lança `AiProviderUnavailableException` → o secundário responde. O primário executou uma tool → não há fallback.
  - B-09/B-10: `Result.failure` vira `"Erro: ..."`. O loop para em `MAX_TOOL_ROUNDS`.
  - B-11: o logout limpa `session.history`.
  - B-12/B-13: falha não consome o trial. O Flow vazio zera `isLoading`.
  - B-17: falha ao salvar no destino → nada é apagado na origem.
  - B-21: uma lista com 1 documento inválido retorna os válidos.
  - B-24: `BILLING_UNAVAILABLE` não altera o status.
  - B-25: `getAll` falha → a conta não é apagada.
  - C-02: `reminderTime` inválido → 18:00.
  - C-03: sem sessão → `Result.failure`, sem exceção.
- **Instrumentados / UI (seletores por `testTag`):** C-01 (abrir deep link com o app aberto e navegar depois de voltar), B-03 (descrição preservada), F-03 (barra de orçamento).
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout` (mínimo de 80%).

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| A regra placeholder do Firestore expira e derruba o app (D-01). | desconhecida (P-01) | total | Verificar a data hoje. Se estiver próxima, estender o placeholder como medida temporária e priorizar a onda 0. |
| B-01 expõe erros que hoje estão escondidos, e vários fluxos passam a mostrar falhas. | alta | médio | Entregar junto com F-01. Migrar por repositório com testes. Rollout gradual no App Distribution. |
| Publicar as regras (D-01) quebra o join legado e o Wear (AD-009). | alta | alto | Backfill do índice `shortCodes` e ajuste do Wear antes do deploy. Testar no emulador de regras. |
| Mudar o contrato de tools quebra o prompt remoto (`ai_system_prompt`). | média | médio | Revisar o prompt no Remote Config junto com a onda 2. |
| Migrar `AI_PROVIDER` (B-07) muda o modelo efetivo de quem já usa o app. | média | baixo | Migração idempotente e evento de analytics com o modelo efetivo. |
| Itens marcados PLAUSÍVEL não se confirmam. | média | baixo | Confirmar no Crashlytics (C-01) ou no provedor (B-34) antes de abrir o SDD filho. |

## 9. Perguntas em Aberto

Nenhuma bloqueia **este** documento, que é só o backlog. Cada uma bloqueia o SDD filho indicado, que deve nascer `blocked` se a pergunta continuar aberta.

- [ ] **P-01** (bloqueia a priorização do D-01): qual a data de expiração da regra placeholder no console do Firestore (`how-much-2a72e`)?
- [ ] **P-02** (B-02): a cópia de uma lista compartilhada deve ser privada (`users=[eu]`)? Mantém o orçamento? Recomendação: privada e com orçamento.
- [ ] **P-03** (B-03): criar `Shopping.establishment` (contrato público, `AGENTS.md` §6) ou descartar o estabelecimento?
- [ ] **P-04** (B-26): o texto de "Excluir todos os dados" promete apagar os dados da conta na nuvem?
- [ ] **P-05** (B-16): remover `compare_prices` ou existe uma fonte de preços real prevista?
- [ ] **P-06** (D-02): migrar para Navigation 3 ou corrigir `AGENTS.md`/`architecture` para Navigation Compose?
- [ ] **P-07** (F-04, F-09): há plano de sair do Spark para o Blaze (Cloud Functions) para push e verificação de compra no servidor?
- [ ] **P-08** (B-14): pode entrar numa lista finalizada?

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existe).
- [x] Nenhum código de produção escrito (só propostas e assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `breaking` em "Impacto público" (B-01).
- [x] Novo endpoint/remote-config: nenhum neste plano. Toggles sugeridos ficam para os SDDs filhos.
- [x] Novo cache: nenhum.
- [ ] Perguntas em aberto exauridas: P-01 a P-08 pendentes, cada uma ligada ao SDD filho que bloqueia.
