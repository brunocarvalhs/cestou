# 02 — Regras de negócio

> Fonte de verdade: o código citado em cada regra. Regras sem citação não devem ser usadas em planos SDD. Onde o código e este documento divergirem, corrija este documento.

## 1. Listas de compras (Shopping)

| # | Regra | Origem |
|---|---|---|
| 1.1 | Uma lista nova nasce com status `NEW`, sem preço (`price = 0.0`), com o criador como `OWNER` e um `shortCode` de 6 caracteres (`A–Z`, `0–9`). | `feature/shopping/domain/usecase/ShoppingCreateUseCase.kt`; `core/domain/.../model/Shopping.kt` |
| 1.2 | Título e descrição vazios são preenchidos com textos padrão (`texts.newListTitle()`, `texts.newListDescription()`). | `ShoppingCreateUseCase.kt` |
| 1.3 | Status possíveis: `NEW`, `IN_PROGRESS`, `FINISH`. | `core/domain/.../model/Shopping.kt` |
| 1.4 | Reabrir uma lista finalizada move o status para `IN_PROGRESS`. | `feature/shopping/domain/usecase/ShoppingReopenUseCase.kt` |
| 1.5 | Duplicar cria uma lista nova com novo `id`, título com sufixo de cópia, status `NEW`, sem favorito, e copia os produtos com novo `id` e `isPurchased = false`. | `ShoppingDuplicateUseCase.kt` |
| 1.6 | Entrar em uma lista (`join`) aceita um código curto (até 8 caracteres, convertido para maiúsculas, resolvido pelo `shortCode`) ou o `id` completo. Após entrar, os demais membros recebem notificação do tipo `list_joined`. | `feature/shopping/domain/usecase/ShoppingJoinUseCase.kt` |
| 1.7 | A atualização feita pelo agente de IA é **parcial**: só os campos presentes nos argumentos são alterados; os demais mantêm o valor atual. | `ShoppingUpdateUseCase.kt` |
| 1.8 | Papéis: `OWNER` e `EDITOR`. | `core/domain/.../model/User.kt` |
| 1.9 | Somente membros (`users` contém o `uid`) leem a lista. | `firestore.rules`, bloco `match /shopping/{shoppingId}` |

**Estado atual (1.6 e 1.9):** a entrada por código resolve primeiro pelo índice `shortCodes/{code}` (`ShoppingRepositoryImpl.kt`, `resolveShortCode`). Só se o índice não existir, usa uma consulta legada por `shortCode` em `shopping`, que os comentários do código e de `firestore.rules` indicam que será negada quando as regras da decisão AD-009 (`05-decisoes-arquiteturais.md`) forem publicadas. Listas criadas antes do índice dependem desse fallback. Ao planejar mudanças nessa área, confirme se o fallback ainda é necessário.

## 2. Produtos

| # | Regra | Origem |
|---|---|---|
| 2.1 | Total do produto = `price × quantity`; sem preço, o total é zero. | `core/domain/.../model/Product.kt` |
| 2.2 | Categoria padrão: `"Outros"`. | `Product.kt` |
| 2.3 | Cada produto guarda histórico de atividades (`ADDED`, `EDITED`, `PURCHASED`). Quem adicionou, editou por último e comprou são derivados do histórico. | `Product.kt`, `ProductActivity.kt` |
| 2.4 | Limpar comprados remove os produtos marcados como comprados da lista. | `core/domain/.../usecase/ShoppingClearPurchasedUseCase.kt` e `feature/products/.../ShoppingClearPurchasedUseCaseImpl.kt` |
| 2.5 | Verificação de duplicidade antes de salvar. (A tool de IA `compare_prices`/`PriceComparisonUseCase.kt` foi removida em `sdd-017`: retornava preços fictícios hardcoded apresentados como reais — ver `specs/sdd-017-ai-agent-safety/tasks.md`, B-09.) | `feature/products/domain/usecase/ProductDuplicateCheckUseCase.kt` |
| 2.6 | Ordenação de produtos. | `core/domain/.../usecase/SortProductsUseCase.kt` |

## 3. Assinatura (FREE e PRO)

Entitlement reduzido a dois estados, sem dados de compra (`core/domain/.../model/SubscriptionStatus.kt`). Os detalhes da compra ficam em `core:billing`.

| # | Recurso | Regra | Origem |
|---|---|---|---|
| 3.1 | Listas de compras (feature shopping) | Gate PRO aplicado na navegação; o `isPro` vem do `SubscriptionRepository`. | `feature/shopping/presentation/viewmodel/ShoppingSubscriptionGateViewModel.kt`; `feature/shopping/navigation/ShoppingGraph.kt` |
| 3.2 | Carrinho | Mesmo padrão de gate PRO. | `feature/cart/presentation/viewmodel/CartSubscriptionGateViewModel.kt`; `feature/cart/navigation/CartGraph.kt` |
| 3.3 | Chat com IA | PRO: mensagens ilimitadas. FREE: **uma** mensagem gratuita, controlada por `AiTrialRepository`. Enviar fica bloqueado quando não é PRO e a mensagem gratuita já foi usada. | `feature/chat/.../ChatAiGateViewModel.kt` |
| 3.4 | Ações de IA marcadas com `requiresPro` | Se a ação exige PRO e o usuário não é PRO, a ação não é executada. | `feature/ai_agent/data/service/GeminiAgent.kt` (~linha 134); `OpenRouterAgent.kt` (~linha 176) |
| 3.5 | Tela de assinatura | Mostra o estado PRO e o botão de assinar quando é FREE. | `feature/subscription/presentation/screen/PaywallScreen.kt`; `feature/profile/presentation/screen/ProfileScreen.kt` |

## 4. Conta, perfil e configurações

| # | Regra | Origem |
|---|---|---|
| 4.1 | Nome de pessoa: mínimo de 2 letras; máximo de 30 letras por palavra, 3 palavras e 60 caracteres no total; rejeita sequências de teclado. | `core/domain/.../util/PersonNameValidator.kt` |
| 4.2 | Excluir conta e excluir todos os dados são use cases separados. | `feature/settings/domain/usecase/DeleteAccountUseCase.kt`, `DeleteAllDataUseCase.kt` |
| 4.3 | Login por provedor é controlado por flag remota. Google e e-mail vêm ligados por padrão, para que o revisor da loja sempre consiga entrar. Os demais provedores vêm desligados. | `core/remote-config/.../model/RemoteConfigKeys.kt` (`FeatureFlagKeys`) |
| 4.4 | Pareamento com o relógio é controlado pela flag `wear_login_enabled` (padrão ligado). | `RemoteConfigKeys.kt` |

## 5. Lembretes

| # | Regra | Origem |
|---|---|---|
| 5.1 | Lembrete de lista usa horário padrão 18:00 (hora máxima 23). | `feature/settings/data/worker/ShoppingReminderScheduler.kt` |

## 6. Pendências

- Esta lista cobre os casos de uso principais, mas não é exaustiva. Regras de cache offline, sincronização com o relógio e cálculo de orçamento (`budget`) ainda não foram documentadas aqui. Adicione-as com citação ao código antes de planejar mudanças nessas áreas.
