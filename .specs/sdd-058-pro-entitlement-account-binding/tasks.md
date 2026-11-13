# Task: Vincular entitlement PRO à conta Cestou (falso positivo de assinatura)

- **Slug:** pro-entitlement-account-binding
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-10
- **Status:** ready
- **Versão-alvo:** a definir após decisão de produto (provável minor, pois altera comportamento visível)
- **Tipo de mudança:** fix
- **Impacto público (módulo):** additive (se optar por `setObfuscatedAccountId`) ou breaking (se optar por verificação server-side com nova coleção/contrato)

## 1. Contexto e Motivação
- **Bug relatado:** o usuário tem uma conta Google com assinatura PRO no aparelho. Ao logar no Cestou com *outra* conta (sem assinatura), o app mostra PRO. Falso positivo.
- **Causa raiz (verificada no código):** o status PRO é derivado **somente** do estado de compras do Google Play no aparelho, que é global por conta Google do Play, não por usuário Cestou/Firebase.
  - `core/billing/.../PlayBillingSubscriptionRepository.kt:107-133`: `refresh()` chama `queryPurchasesAsync(SUBS)` sem nenhuma referência ao uid Firebase.
  - `core/billing/.../PlayBillingSubscriptionRepository.kt:208-216`: `handlePurchases` grava `PRO` com base em `purchases.toSubscriptionStatus()`.
  - `core/billing/.../PurchaseMapper.kt:16-21`: `PRO` se houver qualquer compra `PURCHASED`, sem checar conta.
  - `core/billing/.../PlayBillingSubscriptionRepository.kt:157-188`: `BillingFlowParams` não chama `setObfuscatedAccountId`; a compra também não fica atrelada a conta Cestou. Verificado por busca: não há `ObfuscatedAccountId` nem `accountIdentifiers` em `core`, `feature` ou `app`.
  - `core/billing/.../BillingStatusCache.kt:11-22`: o cache é `SharedPreferences` do aparelho (`billing_preferences`), sem chave por usuário. Ele é carregado na inicialização (`PlayBillingSubscriptionRepository.kt:62`).
  - `core/auth/.../FirebaseAuthService.kt:113-130`: `signOut()` não toca em billing nem no cache. Trocar de conta não recalcula o entitlement.
- **Consequência:** todos os gates leem o mesmo `observeStatus()` e liberam recursos PRO para a conta errada: `feature/shopping/.../ShoppingSubscriptionGateViewModel.kt:26-27`, `feature/cart/.../CartSubscriptionGateViewModel.kt:27-28`, `feature/chat/.../ChatAiGateViewModel.kt:38-41`, `feature/ai-agent/.../BaseAgent.kt:154-155`, `feature/profile/.../ProfileSubscriptionViewModel.kt:24-25`. Também propaga para o Wear (`WearSubscriptionSync`/`syncSubscription(isPro)`, `core/data/.../WearableSyncServiceImpl.kt:31-33`) e para a propriedade de analytics `UserPropertiesSync.kt:64,139`.
- **Contrato atual (AD-010):** `architecture/05-decisoes-arquiteturais.md:196-212` diz que não há "sign-in-based auto-upgrade" e que o status só muda ao reler o cache do Play. Esse texto está correto, mas o cache do Play é global ao aparelho, o que a AD não considerou explicitamente. A AD precisa ser revisada.
- **Referência do pedido:** relato do usuário na sessão de `/code-planner` (não há entrada em `.specs/tasks.txt` para esta tarefa).

## 2. Escopo
- **Inclui:**
  - Definir a regra de entitlement "PRO pertence a qual conta" (decisão bloqueante, seção 9).
  - Implementar a regra escolhida sem regredir assinantes legítimos.
  - Garantir que trocar de conta Cestou (sign-out/sign-in) reavalie o entitlement e limpe o cache do conta anterior.
  - Propagar o status corrigido para Wear e analytics (já são consumidores do mesmo `observeStatus()`).
  - Atualizar `AD-010` em `architecture/05-decisoes-arquiteturais.md` e registrar a decisão nova.
  - Compras sem vínculo (legado) ficam FREE. Reembolso, comunicação e migração de assinantes ficam fora deste plano.
- **Exclui:**
  - Mudanças de UI do paywall (preço, fluxo de compra) ou de SKU no Play Console.
  - Migração de assinantes atuais além do que for definido na seção 9.
  - Alteração no `CHANGELOG.md` e em `versionName` (regra do release, ver `AGENTS.md` §6 e memória `feedback_never_touch_changelog_version`). Ficam para o processo de release.

## 3. Impacto Arquitetural
- **Módulos/camadas afetados:**
  - `core:domain`: `SubscriptionRepository` (`core/domain/.../repository/SubscriptionRepository.kt:13-15`) e `SubscriptionStatus` (`core/domain/.../model/SubscriptionStatus.kt:10-13`). Só mudam se a opção B (server-side) exigir um estado extra, como `PRO_UNVERIFIED`. Caso contrário, ficam inalterados.
  - `core:billing`: `PlayBillingSubscriptionRepository`, `PurchaseMapper`, `BillingStatusCache` (chave por uid ou invalidação no sign-out).
  - `core:auth`: `FirebaseAuthService` precisa expor o uid atual de forma síncrona ou via `Flow` para o billing (hoje `authState`, usado em `app/.../MainViewModel.kt:80-92`).
  - `app`: `MainViewModel` (reavaliar entitlement no sign-in/sign-out) e `UserPropertiesSync`.
  - Opção B adiciona: `functions/` ou backend equivalente (fora do repositório Android atual; a existência de backend precisa ser confirmada, ver seção 9).
- **Padrão:** Clean Architecture + MVI. Os gates continuam dependendo só de `SubscriptionRepository`. A regra de vínculo fica dentro de `core:billing`.
- **Novos arquivos (provisórios):**
  - `core/billing/src/main/java/br/com/brunocarvalhs/howmuch/core/billing/AccountEntitlementPolicy.kt` (decide se uma `Purchase` vale para o uid atual).
  - Testes em `core/billing/src/test/java/br/com/brunocarvalhs/howmuch/core/billing/`.

```
  Firebase Auth (uid) ──────────────┐
                                    ▼
  BillingLifecycleObserver ──► PlayBillingSubscriptionRepository ──► SubscriptionRepository.observeStatus()
                                    │                                        │
  queryPurchasesAsync(SUBS) ────────┘                                        ├─► Gates (shopping, cart, chat, ai-agent, profile)
       (Play, por conta Google do aparelho)                                  ├─► WearableSyncService (isPro)
                                                                             └─► UserPropertiesSync (analytics)

  Hoje:   PURCHASED (qualquer conta do aparelho) ──► PRO
  Depois: PURCHASED && vínculo(uid atual) ──► PRO   (regra a definir, seção 9)
```

- **Injeção:** Hilt já fornece `PlayBillingSubscriptionRepository` como singleton (`core/billing/.../di/BillingModule.kt`). O uid deve chegar por interface do `core:domain` ou `core:auth` (não instanciar Firebase dentro do billing).

## 4. Contratos e Interfaces
- **Não há mudança obrigatória em `SubscriptionRepository.observeStatus()`.** Os consumidores continuam recebendo `Flow<SubscriptionStatus>`.
- **Possível novo contrato (depende da opção, seção 9):**
  - Opção A: nenhum contrato público novo. Apenas `BillingFlowParams.Builder.setObfuscatedAccountId(uid)` na compra (`core:billing`, `internal`).
  - Opção B: `core/domain` ganharia uma leitura de entitlement por uid, por exemplo `fun observeStatus(uid: String)`, ou o `SubscriptionStatus` passaria a ser calculado no backend e lido via Firestore. Se for `public` entre módulos, justificar conforme AGENTS.md §6 e marcar **BREAKING** se alterar a assinatura existente.
- **Visibilidade:** `internal` por padrão. Qualquer símbolo novo em `core/domain` precisa de justificativa (AGENTS.md §6, item 2).
- **Firestore (só opção B):** nova coleção de entitlement por uid (ex.: `users/{uid}/entitlement`), com regras de segurança que impeçam o cliente de escrever PRO. Isso é contrato público e exige registro em AGENTS.md §6 e `architecture/04-integracoes-externas.md`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | PRO = existe compra `PURCHASED` no Play do aparelho, sem checar conta Cestou. | PRO só se a compra estiver vinculada ao uid atual (opção A ou B). | `PurchaseMapper.kt:16-21`; `PlayBillingSubscriptionRepository.kt:107-133` |
| 2 | Compra não grava vínculo com conta Cestou. | Compra nova grava o uid via `setObfuscatedAccountId` (opção A) ou o backend registra a compra por uid (opção B). | `PlayBillingSubscriptionRepository.kt:157-188` |
| 3 | Cache PRO em `SharedPreferences` do aparelho, carregado no `init`. | Cache passa a ser chaveado por uid ou é invalidado no sign-out/sign-in. | `BillingStatusCache.kt:11-22`; `PlayBillingSubscriptionRepository.kt:62` |
| 4 | `signOut()` não mexe em billing. | Sign-out e troca de conta disparam `refresh()` com o novo uid e zeram o PRO vinculado ao anterior. | `FirebaseAuthService.kt:113-130`; `MainViewModel.kt:80-92` |
| 5 | Sign-out envia `isPro=false` ao Wear, mas o telefone continua PRO. | Wear recebe o mesmo status recalculado para a conta atual. | `MainViewModel.kt:86-89`; `WearableSyncServiceImpl.kt:31-33` |
| 6 | Analytics recebe PRO com base no status global. | Propriedade de usuário reflete o status da conta atual. | `UserPropertiesSync.kt:64,139` |
| 7 | AD-010 diz que não há auto-upgrade por sign-in. | AD-010 é revisada para registrar que o vínculo é por conta Cestou. | `architecture/05-decisoes-arquiteturais.md:196-212` |

## 6. Critérios de Aceitação
- **Performance:**
  - Nenhuma chamada extra de `queryPurchasesAsync` por frame. `refresh()` continua no `BillingLifecycleObserver` com throttle de 3 s (`BillingLifecycleObserver.kt:7-19`).
  - A verificação de vínculo (opção A) é local, sem I/O. Na opção B, o backend faz a checagem; o cliente só lê o documento Firestore, com cache offline do Firestore.
  - `_status` continua `MutableStateFlow`; nada de coleção nova em memória.
  - Sem I/O duplicado: uid e compras são lidos uma vez por `refresh`.
- **Feature toggles:** nenhum por padrão. Se a rollout for gradual, usar remote-config (`core:remote-config`) com toggle `billing_account_binding` (default `false`). Estados a definir na seção 9.
- **Observabilidade:**
  - Evento Analytics `entitlement_account_mismatch` (sem uid nem token, por AGENTS.md §5) quando uma compra existe mas não está vinculada à conta atual.
  - Não logar `purchaseToken` nem uid em Timber/Crashlytics (AGENTS.md §5).
- **Compatibilidade:**
  - minSdk 28; Billing Library `9.1.0` (`gradle/libs.versions.toml:88`), que expõe `Purchase.getAccountIdentifiers()` e `setObfuscatedAccountId`. Confirmar no código-fonte da versão antes de implementar.
  - Assinantes atuais: as compras feitas antes desta mudança não têm `obfuscatedAccountId`. Sem tratamento, elas perderiam PRO. Ver seção 9.
  - Wear: continua recebendo `isPro` pelo DataItem (`WearablePaths.kt:25`).

## 7. Plano de Testes
- **Unitários (`core/billing`):**
  - `PurchaseMapperTest`: compra `PURCHASED` com uid correto → PRO; uid diferente → FREE; sem `obfuscatedAccountId` → conforme política da seção 9; `PENDING` → FREE.
  - `PlayBillingSubscriptionRepositoryTest`: o teste existente `init refreshes and a purchased subscription becomes Pro` (`PlayBillingSubscriptionRepositoryTest.kt:113`) precisa ser ajustado para a nova regra. Adicionar: troca de uid zera PRO; falha de `queryPurchasesAsync` não derruba PRO (já existe em `:241`, manter).
  - `BillingStatusCacheTest`: cache de um uid não vaza para outro.
  - Cenários negativos: rede indisponível, toggle OFF (se houver), cache miss, Play retorna lista vazia (`:265` já cobre), compra sem vínculo.
- **Unitários (`app`):** `MainViewModel` reavalia entitlement no sign-in/sign-out.
- **Unitários (`core:auth`):** `FirebaseAuthService` expõe uid corretamente após sign-out.
- **Instrumentados / UI:** opcional. Se a tela de perfil mudar, seletores por `testTag`, sem assert de texto (AGENTS.md §5).
- **Validação manual (device):** dois logins no mesmo aparelho (conta A com assinatura no Play, conta B sem); confirmar que B aparece FREE e que A continua PRO.
- **Vínculo por uid:**
  - Compra via `launchBillingFlow` com `setObfuscatedAccountId(uid)`: PRO para o uid comprador.
  - Mesma conta após reinstalação, com a compra ainda no Play: PRO.
  - Outra conta no mesmo aparelho: FREE.
  - Compra sem `obfuscatedAccountId` (legado): FREE em qualquer conta.
  - Conta sem assinatura: FREE, sem exceção.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Assinantes atuais (sem `obfuscatedAccountId`) perdem PRO ao aplicar regra estrita | alta | alto | Política de transição (seção 9, Q2) + toggle remote-config + comunicação |
| Opção A não resolve compras antigas; o falso positivo do próprio usuário pode continuar | alta | médio | Decidir entre A com transição e B (Q3) |
| Opção B exige backend e Play Developer API (service account), fora do app | média | alto | Confirmar se existe backend (Q4) antes de estimar |
| Fraude em aparelho comprometido (cliente forjando uid) na opção A | baixa | médio | Aceitar como limitação ou migrar para B |
| Regressão em gates e Wear sem cobertura | média | médio | Testes da seção 7; revisão de maintainer (AGENTS.md §6) |
| Usuário com duas contas Cestou no mesmo aparelho com uma compra só | média | baixo | Política explícita (Q1) |
| Assinantes legados ficam FREE até reassinar, sem comunicação | alta | alto | Fora deste plano (decisão do owner); registrar no release |
| Versões antigas continuam mostrando PRO para qualquer conta enquanto a compra legada existir | certa | médio | Fora deste plano; ver risco acima |

## 9. Perguntas em Aberto (bloqueantes)
### Respostas recebidas (2026-11-10)
- **Q1 — RESOLVIDA:** o PRO é vinculado à **conta Cestou logada**. Conta sem assinatura é conta normal (FREE), sem exceção. Não é a conta Google do Play do aparelho.
- **Q3 — RESOLVIDA:** opção **A (cliente)**, porque não há backend (Q4).
- **Q4 — RESOLVIDA:** não há backend. Não existe `functions/` nem seção `functions` em `firebase.json` (que só tem `firestore` e `hosting`). A opção B fica descartada neste escopo.
- **Identificador do vínculo:** recomendação de usar o **uid do Firebase** em vez do e-mail. Motivos: e-mail pode mudar, é dado pessoal e o uid é estável. Pendente de confirmação do usuário (ver nota em Q2).

### Abertas
- [x] **Q2 — REVISADA (2026-11-10): opção (b).** Compras sem vínculo (anteriores à mudança) continuam dando PRO a qualquer conta logada no aparelho, enquanto estiverem ativas. Compras novas entram vinculadas via `setObfuscatedAccountId`, e compra vinculada a outra conta não dá PRO. Risco aceito pelo owner: o falso positivo original volta para esse grupo residual. A opção (a) original previa aviso no paywall, que não foi implementado. Sheet de vínculo (claim) foi considerado e fica como evolução futura. Opção (a) original, para referência: Opções consideradas: compras feitas **antes** desta versão não têm `obfuscatedAccountId`, então não há como saber de qual conta Cestou são. Opções:
  - (a) Ninguém recebe PRO por essas compras até repetir a compra (ou trocar de plano) na conta certa. Atende "nada de falso positivo", mas os assinantes atuais perdem o acesso.
  - (b) Essas compras valem para qualquer conta (grandfathering). Mantém os assinantes atuais, mas um falso positivo como o relatado continua possível para compras antigas.
  - (c) Primeira conta Cestou que logar no aparelho reclama a compra (claim). Atende os assinantes, mas repete o problema se a outra conta logar primeiro.
  - Recomendação: **(a)** com mensagem clara no paywall, pois é a única opção que cumpre "nada de falso positivo". Se a base de assinantes atuais for pequena, (a) é aceitável. Confirmar com o usuário.
- [x] **Q5 — RESOLVIDA: uid do Firebase** (não e-mail). Com e-mail, a mesma pessoa com dois provedores (Google e e-mail/senha) tem o mesmo e-mail mas uids diferentes, então o e-mail evita perda de acesso nesse caso, ao custo de depender de e-mail mutável e pessoal.

### Verificação de API (billing 9.1.0)
- `BillingFlowParams$Builder.setObfuscatedAccountId` e `Purchase.getAccountIdentifiers()` existem em `com/android/billingclient/api` no `classes.jar` da 9.1.0 (Gradle cache). O app não sabe o e-mail da conta Google do Play: a comparação é entre o valor gravado na compra e o uid/e-mail atual da conta Cestou.
- [ ] **Q5 (não bloqueante):** `AD-010` deve ser reescrita no mesmo PR ou em um PR de documentação separado? Recomendação: mesmo SDD, para não deixar a AD incorreta.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` e `architecture/05-decisoes-arquiteturais.md:196-212`.
- [x] Nenhum código de produção escrito (apenas pseudocódigo, nomes provisórios e diagramas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Impacto público marcado como `additive` ou `breaking` conforme a opção; definido na seção 9.
- [ ] Novo endpoint/remote-config: toggle e observabilidade definidos (depende de Q3 e Q4).
- [x] Novo cache: não há cache novo; `BillingStatusCache` é ajustado (justificativa: `SharedPreferences` do aparelho é suficiente para o status, a chave precisa incluir o uid).
- [x] Perguntas em aberto registradas; status `blocked`.

---

## Próximo passo

Plano em `ready`. Próximo passo: `code-implementer` com este arquivo, quando você autorizar a implementação. Fora do escopo do plano: CHANGELOG e `versionName`, que ficam para o release.
