# Task: Meta de gasto mensal sincronizada na conta e linha de orçamento na lista

- **Slug:** meta-gasto-mensal
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.3.0 (minor: capacidade nova e visível ao usuário)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive (`MonthlyGoalRepository`, modelos de progresso e calculadora em `core:domain`; ver §4)

## 1. Contexto e Motivação
- Pedido do usuário: meta de gasto mensal nas configurações e orçamento por lista, com o app mostrando quanto da lista e quanto da meta mensal já foi gasto, para organizar a compra.
- Decisões do usuário (respostas às perguntas do plano):
  - **Q0:** o valor da lista é o **preço total** (todos os itens). A lista pode ou não ter orçamento. A visualização é uma **linha** (progresso) com a porcentagem ou o valor restante, pensada como uma experiência agradável.
  - **Q1:** gasto do mês = compras finalizadas do mês, mais o total da lista aberta como projeção.
  - **Q2:** avisos só dentro do app, sem notificação push.
  - **Q3:** meta **sincronizada na conta**, porque há listas compartilhadas e todos devem ver o valor estipulado. Interpretação adotada (confirmar em Q6): a meta mensal é da conta de cada usuário e vai para o Firestore; o orçamento de cada lista já é compartilhado com os membros por estar no documento da lista.
- **O orçamento por lista já existe**: `Shopping.budget` (`core/domain/.../Shopping.kt:18`), editável em `EditShoppingContent.kt:65-66,133-145`, exibido na Home (`ShoppingItem.kt:114`), usado no Wear (`ShoppingDetailUiState.kt:20`) e citado no prompt da IA (`SystemPrompts.kt:18`). Falta compará-lo com o total na tela da lista.
- O total da lista é calculado só na UI (`CartScreen.kt:109-121`) e mostrado na barra inferior (`CartBottomBar.kt:62`). `Shopping.price` nasce `0.0` (`ShoppingCreateUseCase.kt:50`) e só recebe valor ao finalizar (`FinishPurchaseViewModel.kt:68`).
- O mês de uma compra segue o histórico: `finishedAt ?: createdAt` no fuso do `Clock` (`GroupHistoryByMonthUseCase.kt:21-37`).
- **Privacidade (AD-009):** `users/{uid}` é legível por qualquer usuário logado (`firestore.rules`, bloco `/users/{userId}`); a meta é dado financeiro e não pode ficar nele. Vai para a subcoleção privada `users/{uid}/preferences/budget`, no padrão de `common-products` (`firestore.rules:248-260`).
- `ShoppingSummaryCard` (com `totalBudget`) só é usado em preview e teste (`ShoppingComponentsTest.kt:128`): código morto.

## 2. Escopo
- **Inclui:**
  - Campo "Meta mensal de gastos" em Configurações > Compras, opcional, em moeda, salvo na conta.
  - Cartão de meta mensal na Home: gasto do mês, meta e percentual.
  - Linha de orçamento na tela da lista (abaixo do cabeçalho): barra de progresso e legenda com porcentagem e valor restante, só quando a lista tem orçamento; linha da meta do mês quando há meta.
  - Avisos in-app ao cruzar 80% e 100% (lista e mês), uma vez por faixa e por abertura da lista.
  - Kill switch remoto, analytics sem valores monetários, textos pt-BR/en/es, `testTag`s, regras do Firestore, flows Maestro, documentação.
- **Exclui:**
  - Notificação push (Q2), meta por categoria, histórico de metas.
  - Painel de "gasto dos itens comprados": o valor da lista é o total (Q0).
  - Destaque de estouro nos itens da Home, Wear e prompt da IA.
  - Conversão de valores ao trocar a moeda.
  - Meta compartilhada entre membros de uma lista (ver Q6).

## 3. Impacto Arquitetural
- Módulos e camadas:
  - `:core:domain`: contrato `MonthlyGoalRepository`, modelo e calculadora pura.
  - `:core:data`: implementação sobre `NetworkService` (Firestore).
  - `:core:ui`: componente da linha de orçamento.
  - `:core:remote-config` e `:core:analytics`: constantes.
  - `:feature:settings`, `:feature:shopping`, `:feature:cart`: `presentation` e, em settings, `domain/usecase`.
- Features não dependem entre si (AGENTS.md §2): cálculo em `core:domain`, visual em `core:ui`, repositório em `core:domain`/`core:data`.
- Novos arquivos:
  - `core/domain/.../repository/MonthlyGoalRepository.kt`
  - `core/domain/.../budget/BudgetProgress.kt`
  - `core/domain/.../budget/MonthlySpendingCalculator.kt`
  - `core/data/.../repository/MonthlyGoalRepositoryImpl.kt`, `core/data/.../model/BudgetPreferencesModel.kt`
  - `core/ui/.../components/CestouBudgetProgress.kt`
  - `feature/shopping/.../components/shopping/MonthlyGoalCard.kt`
  - testes correspondentes (§7)

```
 Settings > Compras ──setGoal──► MonthlyGoalRepositoryImpl ──POST──► Firestore users/{uid}/preferences/budget
                                        ▲ observeGoal(): Flow<Double?> (cache offline do Firestore)
                  ┌─────────────────────┴───────────────────────┐
                  ▼                                             ▼
        ShoppingListViewModel                              CartViewModel
        observeAll() + goal                                getAll() 1x + goal + produtos
        MonthlySpendingCalculator                         BudgetProgress(lista) + BudgetProgress(mês)
                  ▼                                             ▼
          MonthlyGoalCard (Home)                  CestouBudgetProgress sob o cabeçalho + aviso 80%/100%
```
- DI: `MonthlyGoalRepository` ligado por `@Binds` no módulo Hilt de `core:data`; `MonthlySpendingCalculator` recebe o `Clock` já provido.

## 4. Contratos e Interfaces
Assinaturas (sem implementação):

```kotlin
// core:domain, public: usado por settings, shopping, cart e core:data
interface MonthlyGoalRepository {
    /** Meta do usuário logado; `null` sem meta ou sem sessão. Emite do cache offline. */
    fun observeGoal(): Flow<Double?>
    /** `null` ou valor <= 0 remove a meta. */
    suspend fun setGoal(goal: Double?): Result<Unit>
}

enum class BudgetLevel { NONE, OK, WARNING, EXCEEDED }

data class BudgetProgress(val limit: Double?, val spent: Double) {
    val ratio: Double?       // spent / limit; null sem limite válido (> 0)
    val level: BudgetLevel   // WARNING >= 0.8, EXCEEDED >= 1.0
    val remaining: Double?   // limit - spent; negativo quando estourou
}

class MonthlySpendingCalculator @Inject constructor(clock: Clock) {
    /** Soma `price` das listas FINISH do mês de [reference], pela regra do histórico. */
    fun finishedSpent(lists: List<Shopping>, reference: Instant, excludeId: String? = null): Double
}
```
- `public` justificado: três features e `core:data` os consomem. Sem alterar `AppSettings` nem `SettingsRepository` (a meta não é uma preferência local).
- Firestore:
  - documento `users/{uid}/preferences/budget` com `{ id: "budget", monthlyGoal: Double? }`;
  - `UserRepositoryImpl.deleteProfile` passa a apagar também a subcoleção `preferences` (`core/data/.../UserRepositoryImpl.kt`), para "apagar conta" não deixar dado para trás;
  - `firestore.rules`: bloco `match /preferences/{docId}` só para o dono, `create` com `keys().hasOnly(['id','monthlyGoal'])`. O arquivo é proposta não publicada (AD-009): publicar é decisão do mantenedor.
- Remote-config: `FeatureFlagKeys.BUDGET_ALERTS_ENABLED = "feat_budget_alerts_enabled"` (default true), como `FEEDBACK_ENABLED`.
- Analytics (constantes): `monthly_goal_set` (`has_goal`, `goal_bucket`), `budget_alert_shown` (`scope`: list|month, `level`: warning|exceeded). Nunca o valor em moeda.
- Intent novo em `ShoppingSettingsIntent`: `onUpdateMonthlyGoal: (Double?) -> Unit`; estado novo `monthlyGoal: Double?`.
- Sem `NavKey` novo. Sem breaking: tudo aditivo.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Orçamento da lista é opcional e só informativo | Gera a linha de progresso e avisos; `budget <= 0` é ignorado (como no Wear) | `Shopping.kt:18`, `ShoppingDetailViewModel.kt:114` |
| 2 | Total da lista = soma de todos os itens (só na UI) | É o "gasto" da lista (Q0); alimenta a linha e a projeção do mês | `CartScreen.kt:109-121` |
| 3 | Lista finalizada guarda o total em `price` | Gasto do mês = soma de `price` das listas FINISH do mês | `FinishPurchaseViewModel.kt:68` |
| 4 | Mês da compra = `finishedAt ?: createdAt`, fuso do `Clock` | Mesma regra para a meta | `GroupHistoryByMonthUseCase.kt:21-37` |
| 5 | Não existe meta mensal | Meta opcional por conta, em `users/{uid}/preferences/budget`; vazio ou 0 remove | novo |
| 6 | Avisos de orçamento não existem | 80% = aviso, 100% = estourou; uma vez por faixa e por abertura da lista; kill switch remoto | novo |
| 7 | Lista aberta não entra no gasto do mês | Na tela da lista, mês projetado = finalizadas do mês (sem a lista atual) + total da lista (Q1, opção A) | novo |
| 8 | `users/{uid}` é legível por qualquer logado | A meta fica em subcoleção privada, nunca no documento público | `firestore.rules` (AD-009) |

## 6. Critérios de Aceitação
- **Linha de orçamento (lista):** com `budget`, aparece uma barra fina abaixo do cabeçalho, preenchida pelo total sobre o orçamento, com legenda "N% do orçamento · R$ X restantes"; ao estourar, "Estourou R$ X"; sem `budget`, não aparece. Nível por cor e por texto (não só cor).
- **Meta do mês:** com meta, uma segunda linha na mesma área mostra o impacto projetado ("Meta do mês: N%"); sem meta, não aparece.
- **Home:** com meta, cartão com gasto do mês, meta e percentual; sem meta ou flag OFF, nada aparece.
- **Avisos:** ao cruzar 80% e depois 100% (lista ou mês), um aviso único; adicionar itens na mesma faixa não repete; reabrir a lista reinicia a contagem.
- **Sincronização:** a meta definida em um aparelho aparece em outro com a mesma conta; funciona offline (cache) e sincroniza ao voltar a rede; escrever não bloqueia a UI.
- **Privacidade:** a meta nunca é gravada em `users/{uid}`; "apagar conta" remove `preferences`.
- Performance: cálculo O(n) leve (soma de preços) direto na coleta, sem troca de thread, com `distinctUntilChanged`; a Home reaproveita o `observeAll()` existente; o detalhe da lista usa um `getAll()` único ao abrir, sem novo listener (as finalizadas não mudam durante a edição de uma lista aberta); a linha só recompõe quando total ou orçamento mudam.
- Feature toggle:

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `feat_budget_alerts_enabled` | true | remote-config | sem cartão, linha e avisos; a meta continua salva | comportamento completo |

- Observabilidade: eventos por constantes (o `ArchitectureTest` proíbe literais) e callbacks do intent rastreados no ViewModel.
- Compatibilidade: minSdk 28; documento ausente lê como sem meta; Wear não é afetado; textos em pt-BR, en e es; sem `@Suppress`; `CHANGELOG.md` e `architecture/04` atualizados.

## 7. Plano de Testes
- **Unitários:**
  - `BudgetProgress`: faixas em 79,9%, 80%, 99,9% e 100%, limite nulo ou 0, restante negativo.
  - `MonthlySpendingCalculator`: virada de mês e fuso, lista sem `finishedAt`, `excludeId`, só listas FINISH.
  - `MonthlyGoalRepositoryImpl` (com `NetworkService` mockado): observa o documento, mapeia ausente para `null`, grava com POST e id `budget`, remove com `null` e 0, erro de rede vira `Result.failure`, sem sessão não grava.
  - `UserRepositoryImpl.deleteProfile`: apaga também `preferences`.
  - `ShoppingSettingsViewModel`: salva a meta, evento `monthly_goal_set` sem valor, valor inválido ignorado.
  - `ShoppingListViewModel` e `CartViewModel`: estado do cartão e da linha, flag OFF, sem meta, aviso uma única vez por faixa.
- **UI (Robolectric/Compose, `testTag`):** `settings_monthly_goal_field`, `home_monthly_goal_card`, `cart_budget_progress`, `cart_budget_remaining`, `cart_month_progress`.
- **Maestro:** `09_settings_flow` (campo da meta) e `05/06` (orçamento e linha), só por `testTag`. Aviso: validação no aparelho depende de a conta de teste e do aparelho estarem disponíveis.
- **Comandos:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Gravar a meta no documento público `users/{uid}` expõe dado financeiro | alta se feito sem cuidado | privacidade | subcoleção privada `preferences` + regra do dono + teste de que o payload de `users` não muda |
| As regras do Firestore ainda não estão publicadas (AD-009): a regra nova só vale quando forem | média | falsa sensação de privacidade | registrar no PR; publicar é decisão do mantenedor |
| `NetworkService.make` aguarda o servidor; sem rede a gravação pode demorar | média | UI parada | gravar fora da main e sem esperar o resultado para atualizar a tela (o listener já reflete a escrita local) |
| Mês virando com o app aberto deixa o cartão desatualizado | média | percentual do mês anterior | recalcular ao voltar ao primeiro plano; teste de relógio |
| Lista antiga finalizada sem `finishedAt` cai no mês de criação | média | mês trocado | mesma regra do histórico; sem migração |
| `getAll()` no detalhe pesa em contas com muitas listas | baixa | abertura mais lenta | chamada única fora da main; fallback sem projeção do mês |
| Listas compartilhadas somam o total inteiro | média | número inflado | Q4 (não bloqueante): contar todas as listas do usuário |
| Aviso intrusivo | média | incômodo | uma vez por faixa, kill switch, meta opcional |

## 8.1 Notas de implementação
- O `CartViewModel` já mantinha `repository.observeAll()` aberto (`allShoppings`); a projeção do mês reaproveita esse listener, então o detalhe da lista não faz leitura nova nem `getAll()`.
- O aviso só dispara depois de produtos, outras listas e meta terem chegado, e a primeira leitura completa fixa a linha de base (abrir uma lista já em 90% não avisa). A faixa guardada nunca desce: entrar e sair de 80% não repete o aviso.
- Lista já finalizada entra no mês pelo `price` pago e só se foi finalizada neste mês; lista aberta entra pelo total dos itens.
- `NetworkService.make` só retorna depois do servidor confirmar; a tela de Configurações grava fora da main e o listener já reflete a escrita local.
- Validação no aparelho dos flows Maestro `05` e `12_budget_goal_flow` pendente (aparelho desconectado durante a implementação).

## 9. Perguntas em Aberto (bloqueantes)
Nenhuma bloqueante. Decisões registradas: Q0 preço total e linha de progresso; Q1 opção A; Q2 só in-app; Q3 sincronizada na conta.
- [ ] Q4 (não bloqueante): listas compartilhadas contam inteiras no gasto do mês? Recomendação adotada: sim.
- [ ] Q5 (não bloqueante): faixas de 80% e 100% fixas? Recomendação adotada: sim.
- [ ] Q6 (não bloqueante): a meta mensal é por conta (cada usuário a sua), com o orçamento da lista compartilhado entre os membros, como entendi, ou os membros de uma lista devem dividir uma mesma meta? Recomendação adotada: por conta.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (`architecture/` não cobre orçamento nem metas; lacuna registrada).
- [x] Nenhum código de produção escrito (apenas assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Símbolos públicos novos refletidos como `additive`.
- [x] Novo remote-config com toggle e observabilidade planejados.
- [x] Sem novo cache próprio; armazenamento justificado (Firestore com cache offline, por ser sincronizado).
- [x] Perguntas bloqueantes exauridas.
