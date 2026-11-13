# Task: Paywall com múltiplos planos (mensal, semestral, anual) e force update via Play In-App Updates

- **Slug:** paywall-multi-planos-force-update
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-13
- **Status:** ready
- **Versão-alvo:** gerada automaticamente pelo `.github/scripts/version.sh` no release. Este plano não define nem altera `versionName` nem `versionCode`; a comparação do force update usa o `versionCode` que o próprio script gera.
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive (`core:billing`: novo parâmetro/lista de planos; `feature:subscription`: estados e intents novos; `core:remote-config`: nova chave)

> Nota de divergência: o contexto informado diz versão 2.2.1 / versionCode 14, mas `app/build.gradle.kts:49-50` declara `versionCode = 20` e `versionName = "2.5.1"`. Este plano usa o código como fonte (AGENTS.md §12 e §3). Confirmar antes do release.

## 1. Contexto e Motivação

- A tela de paywall só comporta um produto. Hoje o botão de assinatura usa o Pro mensal e o preço vem de um único `productId` (`core/billing/.../BillingConstants.kt:13`, `PlayBillingSubscriptionRepository.kt:139` e `:158`). Serão lançados os planos **semestral** e **anual**.
- Pedido 2: forçar atualização do app quando a versão instalada estiver abaixo do mínimo exigido, usando a biblioteca de In-App Updates do Google.
- Pedido do usuário: "quero que o layout tenha suporte" a múltiplos planos e "incluir um force update usando lib da google" (`/code-planner`).
- Não há hoje nenhuma verificação de versão mínima nem dependência de In-App Updates: `gradle/libs.versions.toml` só traz `play-review-ktx` (linha 211); o `AppVersionProvider` existe em `core:remote-config` apenas para versionamento de flags (`service/FirebaseRemoteConfigService.kt:38-46`).

## 2. Escopo

**Inclui**
- Paywall que lista os planos disponíveis, cada um com preço formatado vindo do Play (`queryProductDetails`), e permite selecionar um antes de assinar.
- `purchase` recebendo o plano escolhido (`productId` + `offerToken` do base plan correto).
- Remote config com a versão mínima exigida (`app_min_version_code`) e a lista de planos exibidos (ver Perguntas).
- Force update: ao iniciar e ao voltar ao foreground, comparar `BuildConfig.VERSION_CODE` com o mínimo remoto; se abaixo, iniciar atualização **IMMEDIATE** via `AppUpdateManager`.
- Retomada da atualização imediata se o usuário sair do fluxo do Play no meio (`DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS`).
- Strings em pt-BR e en-US (e es, já existente nos módulos).

**Exclui**
- Desconto/"economize X%" entre planos (exige regra de negócio própria; fica para outro SDD).
- Trial/oferta promocional (o cálculo de preço fica restrito à fase recorrente, ver seção 3).
- Alterar a regra de entitlement PRO: ela já é agnóstica ao produto (`PurchaseMapper.kt:22-28`) e permanece assim.
- Atualização flexível (FLEXIBLE). Só IMMEDIATE, por ser force update.
- Mudança em CHANGELOG e `versionName` (processo de release).

## 3. Impacto Arquitetural

### 3.1 Billing (`core:billing`, camada `data`/infraestrutura)
- `BillingConstants` passa a listar os planos conhecidos (ex.: `PRO_MONTHLY`, `PRO_SEMIANNUAL`, `PRO_ANNUAL`), em vez de um único produto.
- `PlayBillingSubscriptionRepository`:
  - `queryFormattedPrice(productId)` (`:139`) passa a ser `queryPlans(productIds)`, que consulta todos os produtos em uma única chamada de `queryProductDetails` e retorna um `List<PlanOffer>`.
  - Corrigir a escolha de fase de preço: hoje usa `pricingPhaseList.firstOrNull()` (`:143`). Se houver trial, mostra o preço do trial. Usar a última fase, que é a recorrente.
  - Corrigir a escolha de offer: hoje usa `subscriptionOfferDetails.firstOrNull()` (`:171`). Com vários base plans, a oferta deve ser a do base plan selecionado (`basePlanId`).
  - `purchase(activity, productId)` (`:156-160`) passa a receber `productId` e `basePlanId` opcionais, mantendo o padrão atual para não quebrar chamadores.
- Pseudocódigo do mapeamento de oferta:

```
planOffer(productDetails, basePlanId):
  offer = subscriptionOfferDetails.firstOrNull { it.basePlanId == basePlanId && it.offerId == null }
  phase = offer.pricingPhases.last()          // fase recorrente, nunca trial
  return PlanOffer(productId, basePlanId, offer.offerToken, phase.formattedPrice, phase.billingPeriod)
```

### 3.2 Paywall (`feature:subscription`, camada `presentation`, MVI)
- `PaywallUiState` ganha `plans: ImmutableList<PlanUi>` e `selectedPlanId: String?`. Coleção imutável (AGENTS.md §5).
- `PaywallIntent` ganha `onPlanSelected: (String) -> Unit`. `onSubscribeClick` passa a receber o plano selecionado.
- `PaywallViewModel` (`presentation/viewmodel/PaywallViewModel.kt`) substitui `loadPrice()` por `loadPlans()` e seleciona o plano padrão (ver Perguntas).
- `PaywallScreen` (`presentation/screen/PaywallScreen.kt:109`) troca o botão único por uma lista de cards selecionáveis e mantém um `CestouButton` para assinar o plano selecionado. Cada card tem `testTag("paywall_plan_<id>")`.
- Fluxo:

```
PaywallScreen ──onPlanSelected(id)──▶ PaywallViewModel ──update──▶ uiState.selectedPlanId
PaywallScreen ──onSubscribeClick(activity)──▶ subscribe(activity, selectedPlanId)
                                              └─▶ billingRepository.purchase(activity, productId, basePlanId)
```

### 3.3 Force update (novo componente)
- Localização: `app/` (orquestração) + contrato em `core:remote-config`. Motivo: `AppUpdateManager` precisa da `Activity` para `startUpdateFlowForResult`, então o disparo fica no `MainActivity`, enquanto a decisão (versão mínima) fica em `MainViewModel`.
- Novos arquivos (propostos):
  - `app/src/main/java/br/com/brunocarvalhs/howmuch/update/ForceUpdateController.kt`: `internal class`, envolve `AppUpdateManager` (`com.google.android.play:app-update-ktx`).
  - `app/src/main/java/br/com/brunocarvalhs/howmuch/update/ForceUpdateState.kt`: `sealed interface` (`Idle`, `Required`, `Blocked`).
  - `app/src/test/java/br/com/brunocarvalhs/howmuch/update/ForceUpdateControllerTest.kt`.
- Fluxo:

```
onStart / onResume
   │
   ▼
ForceUpdateController.check(minVersionCode)
   ├─ installed >= min ─────────────▶ Idle (segue o app)
   ├─ installed <  min
   │     ├─ updateAvailability == UPDATE_AVAILABLE && isImmediateUpdateAllowed
   │     │        └─▶ startUpdateFlowForResult(IMMEDIATE)  ─▶ Play cuida da tela
   │     ├─ updateAvailability == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
   │     │        └─▶ retomar o fluxo IMMEDIATE
   │     └─ Play sem atualização disponível (instalação fora do Play, ou versão ainda não publicada)
   │              └─▶ Blocked (tela de bloqueio com link para a Play Store)   ← depende da Pergunta 2
```

- Pseudocódigo de `check`:

```
check(minVersionCode):
  if BuildConfig.VERSION_CODE >= minVersionCode: return Idle
  info = appUpdateManager.appUpdateInfo.await()        // IO, fora da main thread
  return when {
    info.updateAvailability == IN_PROGRESS -> Required(resume = true)
    info.isImmediateUpdateAllowed           -> Required(resume = false)
    else                                    -> Blocked
  }
```

- Consumo do `MainViewModel` (`MainViewModel.kt`): novo `forceUpdate: StateFlow<ForceUpdateState>` alimentado pelo `ForceUpdateController` e pelo `RemoteVariableService.getLong(...)`. A `MainActivity` observa o estado no `CestouApp` e, quando `Required`, chama `startUpdateFlowForResult`.
- O `MainActivity` (`MainActivity.kt:105`, `onStart`) já chama `viewModel.onForeground()`. A checagem entra no mesmo ponto, sem novo ciclo de vida.
- DI: Hilt. `AppUpdateManager` é fornecido por um módulo `@InstallIn(SingletonComponent::class)` no `app/`. Nada é instanciado manualmente em Composables.

### 3.4 Remote config (`core:remote-config`)
- Nova constante em `RemoteConfigKeys.kt` (perto de `API_REQUEST_TIMEOUT_SECONDS`, linha ~297):
  - `APP_MIN_VERSION_CODE = "app_min_version_code"` (Long, default `0L`, ou seja, nenhum force update por padrão).
- Nova entrada em `RemoteConfigDefaults.kt` com o default `0L`.
- Leitura via `RemoteVariableService.getLong(key, default = 0L)` (contrato existente em `contract/RemoteVariableService.kt`). Não há mudança de contrato público.
- Para a lista de planos: ver Pergunta 1.

## 4. Contratos e Interfaces

| Símbolo | Módulo | Visibilidade | Mudança |
|---|---|---|---|
| `PlanOffer(productId, basePlanId, offerToken, formattedPrice, billingPeriod)` | `core:billing` | `public` (usado pelo `feature:subscription`) | novo |
| `PlayBillingSubscriptionRepository.queryPlans(productIds): List<PlanOffer>` | `core:billing` | `public` | substitui `queryFormattedPrice` (**mudança de assinatura**) |
| `PlayBillingSubscriptionRepository.purchase(activity, productId, basePlanId?)` | `core:billing` | `public` | parâmetro `basePlanId` adicionado com default (**aditivo**) |
| `BillingConstants.PRO_SEMIANNUAL`, `PRO_ANNUAL` | `core:billing` | `public` | novos |
| `PaywallUiState.plans`, `selectedPlanId` | `feature:subscription` | `public` (já é público) | campos novos com default |
| `PaywallIntent.onPlanSelected` | `feature:subscription` | `public` | novo com default |
| `ForceUpdateController`, `ForceUpdateState` | `app` | `internal` | novos |
| `RemoteConfigKeys.APP_MIN_VERSION_CODE` | `core:remote-config` | `public` (constante de contrato entre módulos) | novo |

**Justificativa de `public`:** `PlanOffer` e `queryPlans` precisam ser `public` porque `feature:subscription` consome o billing a partir de outro módulo (AGENTS.md §6). `ForceUpdateController` fica `internal` ao `app`.

- Consumidores de `queryFormattedPrice`: `PaywallViewModel` (`loadPrice`). Confirmar com busca antes de remover (`grep -rn queryFormattedPrice`).
- Sem mudança de Firestore, sem novo evento de Analytics obrigatório (ver seção 6, observabilidade).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Um único produto, `pro_monthly`, é o padrão de preço e de compra. | Lista de planos consultada em uma chamada; o plano selecionado é o usado na compra. | `core/billing/.../BillingConstants.kt:13`, `PlayBillingSubscriptionRepository.kt:139,158` |
| 2 | Preço exibido = primeira fase da primeira oferta. | Preço exibido = última fase (recorrente) da oferta do base plan escolhido. | `PlayBillingSubscriptionRepository.kt:142-143` |
| 3 | Oferta de compra = primeira oferta do produto. | Oferta de compra = oferta do `basePlanId` selecionado. | `PlayBillingSubscriptionRepository.kt:171` |
| 4 | Status PRO vale para qualquer compra PURCHASED vinculada à conta. | Sem mudança: o entitlement continua agnóstico ao produto. | `core/billing/.../PurchaseMapper.kt:22-28` |
| 5 | Paywall mostra "já é PRO" e não mostra o botão. | Sem mudança. | `feature/subscription/.../screen/PaywallScreen.kt` (bloco `status == PRO`) |
| 6 | Sem remote config de versão mínima; o app nunca força atualização. | Se `VERSION_CODE < app_min_version_code` (e > 0), força atualização IMMEDIATE. | novo; base em `core/remote-config/.../service/FirebaseRemoteConfigService.kt:71-75` |
| 7 | Versão comparada como texto para flags (`isAtLeastVersion`). | Force update compara **versionCode** (inteiro), não versionName. | `FirebaseRemoteConfigService.kt:48-53` (referência de padrão, não de comparação) |
| 8 | Gerenciar assinatura abre o Play com `sku=pro_monthly`. | Mantém o SKU atual; o deep link do Play aceita qualquer SKU ativo do usuário. Revisar só se o produto mensal for descontinuado. | `feature/subscription/.../viewmodel/ManageSubscriptionViewModel.kt:57` |

## 6. Critérios de Aceitação

- **Performance:**
  - `queryPlans` faz **uma** chamada de `queryProductDetails` com todos os produtos (não uma por plano).
  - A consulta roda em `Dispatchers.IO` (já é o escopo do repositório, `PlayBillingSubscriptionRepository.kt:73`).
  - `ForceUpdateController.check` roda fora da main thread. Sem I/O duplicado: uma checagem por `onStart`, não por recomposição.
  - Listas de planos no `UiState` são imutáveis; cards não recompõem quando outro plano é selecionado (estado por `key = planId`).
- **Feature toggles:**

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `app_min_version_code` | `0` | remote-config (Long) | sem force update | força IMMEDIATE se `VERSION_CODE` < valor |
  | `paywall_multi_plans_enabled` | `false` | remote-config (flag) | só o mensal (comportamento atual) | lista completa de planos |

  O toggle `paywall_multi_plans_enabled` é **único e controla todos os planos de uma vez** (não há toggle por plano). OFF mostra só o mensal (comportamento atual); ON mostra mensal, semestral e anual. Permite lançar o código e ativar os planos sem nova versão, caso os produtos ainda não estejam ativos no Play Console.

- **Observabilidade:**
  - Evento de Analytics `PAYWALL_PLAN_SELECTED` com `plan_id` (novo, nome canônico em `AnalyticsEvents`).
  - Evento `FORCE_UPDATE_REQUIRED` com `min_version_code` e `installed_version_code` (novo).
  - Crashlytics: falhas de `AppUpdateManager` registradas como não fatais, sem dados pessoais.
- **Compatibilidade:**
  - minSdk 28 (`gradle/libs.versions.toml`): `app-update-ktx` suporta. Confirmar a versão no Gradle antes de implementar.
  - Wear OS: sem mudança. Force update não se aplica ao `wear/` (ele não é distribuído pela Play como o app do telefone). Confirmar (Pergunta 2).
  - Dados locais: `BillingStatusCache` (`core/billing`) não muda de formato.

## 7. Plano de Testes

**Unitários**
- `PlayBillingSubscriptionRepositoryTest` (já existe em `core/billing/src/test/`):
  - `queryPlans` retorna os 3 planos; um produto ausente é ignorado sem derrubar os demais.
  - Preço usa a fase recorrente quando há trial (cenário negativo).
  - `purchase` escolhe o `offerToken` do `basePlanId` certo; sem `basePlanId`, mantém o comportamento atual.
  - Falha de conexão retorna `unavailableResult`.
- `PaywallViewModelTest` (já existe): seleção de plano atualiza `selectedPlanId`; `subscribe` usa o plano selecionado; `PAYWALL_PLAN_SELECTED` disparado uma vez por troca.
- `ForceUpdateControllerTest` (novo), com `AppUpdateManager` fake:
  - `VERSION_CODE >= min` → `Idle`.
  - `VERSION_CODE < min` + `isImmediateUpdateAllowed` → `Required`.
  - `IN_PROGRESS` → `Required(resume = true)`.
  - Sem atualização disponível → `Blocked`.
  - `min = 0` → nunca consulta o Play.
- `MainViewModelTest` (já existe em `app/src/test`): o estado `forceUpdate` reage ao valor remoto.

**Instrumentados / UI**
- `PaywallScreenTest` (já existe): cards por `testTag("paywall_plan_<id>")`, botão de assinatura reflete o plano selecionado. Sem asserts de texto (AGENTS.md §5).
- Teste manual de force update com **track de teste interno** da Play (o In-App Updates só funciona para build instalada pela Play).

**Lint/Detekt/Cobertura**
- `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout` (mínimo de 85%).
- Sem `@Suppress`.

**Strings**
- Novas chaves em `values/`, `values-pt-rBR/` e `values-es/` (nomes de planos, rótulos de cards, mensagem e botão do bloqueio de force update).

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Play Console sem os produtos semestral/anual ativos → lista vazia ou erro na compra | média | alto | Toggle `paywall_multi_plans_enabled` (default OFF), mostrar só planos encontrados, manter o mensal como fallback |
| Usuário instalou fora da Play (APK, Firebase App Distribution) → In-App Update indisponível | alta | alto | Caminho `Blocked` com link para a Play Store (depende da Pergunta 2) |
| Force update sem versão publicada na Play → usuário bloqueado sem atualização possível | média | alto | Só exigir mínimo quando a versão estiver publicada; ou bloqueio sem loop (Pergunta 2) |
| Usuário sai do fluxo IMMEDIATE e fica preso | média | médio | Retomar com `DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS` em `onResume` |
| Preço exibido com trial errado | baixa | alto | Usar a última fase de preço e testar o cenário (seção 7) |
| Mudança de assinatura de `queryFormattedPrice` quebra chamador oculto | baixa | médio | `grep` antes da remoção; manter wrapper temporário se necessário |
| Versão divergente (2.2.1 no contexto vs 2.5.1 no gradle) | — | baixo | Não afeta a implementação: `versionName` é gerado automaticamente pelo `version.sh` no release |

## 9. Perguntas em Aberto (bloqueantes)

1. **Estrutura dos planos no Play Console (respondida).** Semestral e anual serão **base plans do mesmo produto `pro_monthly`**, com `basePlanId` `monthly`, `semiannual` e `annual`. Um único SKU mantém o `ManageSubscription` (`:57`) e o entitlement já existente sem mudança. Decisão do maintainer (Bruno), registrada em 2026-11-13.
2. **Force update sem atualização disponível no Play (respondida).** Quando a versão mínima for maior que a instalada e o Play não oferecer atualização (instalação fora da Play, testers do Firebase App Distribution, ou versão não publicada), o app **bloqueia** com tela de "atualize o app" e link para a Play Store, e registra o evento `FORCE_UPDATE_BLOCKED`. Regra operacional: `app_min_version_code` só é aumentado depois que a versão nova estiver publicada na Play (inclusive no track interno), para evitar bloqueio sem caminho de atualização.
3. **Toggle de planos (respondida).** Um único toggle `paywall_multi_plans_enabled`, default OFF, controla todos os planos juntos. Sem toggle individual por plano.
4. **Plano padrão pré-selecionado (não bloqueante, default assumido).** Assumo o **anual** pré-selecionado, por ser o de maior valor para o usuário. Confirmar (alternativa: mensal, para não surpreender quem já conhece o paywall).

Perguntas 1 e 2 respondidas (base plans do mesmo `pro_monthly`; bloquear com link para a Play Store). Nenhuma pergunta bloqueante restante; `status: ready`.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha`.
- [x] Nenhum código de produção escrito (apenas pseudocódigo, assinaturas e diagramas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público marcada como **aditiva** e **mudança de assinatura** (`queryFormattedPrice` → `queryPlans`).
- [x] Novo endpoint/remote-config: toggle definido e observabilidade planejada.
- [x] Novo cache: nenhum. `BillingStatusCache` inalterado; checagem de versão não é cacheada.
- [x] Perguntas em aberto exauridas (2 bloqueantes, 2 com default assumido).
