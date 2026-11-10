# Task: Migrar a sincronização com o Wear OS de pareamento por código para Bluetooth

- **Slug:** wear-bluetooth-sync
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-09
- **Status:** blocked
- **Versão-alvo:** a definir após decisão (minor se for feature nova; major se alterar `WearableSyncService` ou o protocolo de pareamento de forma incompatível)
- **Tipo de mudança:** a definir (provavelmente `feature` ou `refactor`)
- **Impacto público (módulo):** a definir. `WearableSyncService` e `WearablePaths` (`:core:common`) são contratos entre `:app`, `:feature:auth` e `:wear`; qualquer troca de assinatura é `breaking`.

> **Bloqueado.** O pedido original ("migrar de código para bluetooth com a sincronização com o relógio Wear OS") admite pelo menos três leituras com escopos e riscos muito diferentes. A implementação não pode começar até as perguntas da seção 9 serem respondidas.

## 1. Contexto e Motivação

- Hoje o pareamento do relógio é feito por um **código de 6 dígitos** exibido no relógio e digitado no celular. O relógio gera o código e o guarda só em memória (`PairingCodeHolder`); o celular o usa para achar o nó e verificá-lo por mensagem.
- A troca de dados do pareamento e do sign-out trafega pela **Wearable Data Layer** (Google Play services). O app não escolhe o canal físico; o transporte (Bluetooth ou Wi-Fi, conforme o caso) é gerenciado pelo Google Play services. Não há nenhum código Bluetooth próprio no repositório: nenhuma permissão `BLUETOOTH_*` em `AndroidManifest.xml` e nenhuma API `android.bluetooth` em `*.kt`.
- Pedido do usuário: "quero migrar de código para bluetooth com a sincronização com o relógio wear os" (`/code-planner`, sem `.specs/tasks.txt`).

Rastreabilidade (arquivo:linha):
- Geração e armazenamento do código: `core/data/src/main/java/br/com/brunocarvalhs/howmuch/core/data/wearable/WearableSyncServiceImpl.kt:78-85` (`updatePairingCode`), `core/common/src/main/java/br/com/brunocarvalhs/howmuch/core/common/wearable/PairingCodeHolder.kt:14`.
- Busca do nó por código (capability + verificação por mensagem com timeout de 5 s): `WearableSyncServiceImpl.kt:88-121` e `:129-157`.
- Envio do par de auth ao relógio: `WearableSyncServiceImpl.kt:159-175` (`sendAuthPairToNode`), chamado por `feature/auth/.../viewmodel/LinkWearViewModel.kt:66` e `:74`.
- Verificação no relógio: `wear/src/main/java/br/com/brunocarvalhs/howmuch/wear/presentation/service/WearAuthListenerService.kt:51-60`.
- Sign-out propagado ao relógio: `app/src/main/java/br/com/brunocarvalhs/howmuch/MainViewModel.kt:80` → `WearableSyncServiceImpl.kt:177-192` (`notifySignOut`).
- Caminhos de mensagem compartilhados: `core/common/.../wearable/WearablePaths.kt:10-17`.
- Arquitetura (parcial): `architecture/04-integracoes-externas.md:74-77` (pareamento de 6 dígitos) e `architecture/05-decisoes-arquiteturais.md:87` (AD-009).

### Atualização (2026-11-09): motivação real

- Reclamação recorrente de clientes: **token errado no relógio**. A causa está na lógica do app, não no canal. O Data Layer já usa Bluetooth (ou Wi-Fi) por baixo; trocar o canal não corrige um token ou um estado errado.
- Hipóteses verificadas no código (a confirmar com logs/Crashlytics antes de implementar):
  1. **Falha silenciosa no relógio.** `FirebaseAuthService.signInWithGoogleIdToken` (`core/auth/.../FirebaseAuthService.kt:177-191`) só registra o erro. O celular envia o par e retorna sucesso sem ack (`LinkWearViewModel.kt:74-76`). O usuário vê o relógio parado, sem mensagem.
  2. **Conta diferente.** Se o uid do Firebase no relógio não bate com o `expectedUserId`, o relógio faz `auth.signOut()` (`FirebaseAuthService.kt:179-182`). O celular só compara o e-mail sem diferença de caixa (`LinkWearViewModel.kt:17`), então pode aceitar um token que o relógio rejeita.
  3. **Lista de outra conta.** `WearListStore` (`feature/shopping/.../data/local/WearListStore.kt`) só é limpo no sign-out (`WearAuthListenerService.kt:46-49`). Um novo pareamento com outra conta pode mostrar no tile e em `LastListViewModel` a última lista da conta anterior.
  4. **Código em memória.** `PairingCodeHolder` (`core/common/.../PairingCodeHolder.kt:14-16`) fica só em memória. Se o processo do `:wear` morrer, o código some e a verificação falha com "Dispositivo não encontrado" (`LinkWearViewModel.kt:66`).
  5. **Token do Google obtido sem filtro de conta explícito.** `GoogleIdTokenProvider` usa `setFilterByAuthorizedAccounts(true)` e `setAutoSelectEnabled(true)` (`feature/auth/.../firebaseui/GoogleIdTokenProvider.kt:22-24`), então o token pode vir de uma conta que não é a do app.
- **Leitura sobre Bluetooth no primeiro acesso:** faz sentido como escopo da opção A, limitado ao pareamento inicial (sem código digitado). Não resolve o token errado sozinho: o token continuaria sendo gerado e enviado pelo mesmo caminho. Para o problema reportado, a prioridade é tornar a falha visível (ack do relógio), validar a conta antes do envio e limpar o estado do relógio em um novo pareamento. Isso vale com ou sem Bluetooth.

## 2. Escopo

Escopo **a confirmar** (ver seção 9). Enquanto isso, o que não muda em nenhuma opção:

- **Inclui (candidato):** trocar o mecanismo de descoberta/confirmação do relógio, hoje por código de 6 dígitos, por um mecanismo Bluetooth; manter o envio do token Google do celular ao relógio, que é o que autentica o relógio no Firebase (AD-009).
- **Inclui (candidato, só se a opção C for escolhida):** mover a sincronização de dados das listas do celular para o relógio por conexão Bluetooth.
- **Exclui (proposto):** trocar o provedor de autenticação; mudar as regras do Firestore (`firestore.rules`, AD-009); alterar o fluxo de Google Sign-In do celular.

## 3. Impacto Arquitetural

- **Módulos afetados:** `:core:common` (contrato `WearableSyncService`, `WearablePaths`, `PairingCodeHolder`), `:core:data` (`WearableSyncServiceImpl`, `PlatformModule`), `:feature:auth` (`LinkWearViewModel`, `PairingViewModel`, telas `PairingCodeWearScreen` e `LinkPhoneWearScreen`), `:wear` (`WearAuthListenerService`, `AndroidManifest.xml`), `:app` (`MainViewModel` para sign-out), `:core:navigation` (`PairingCode` e `QrCode` em `MobileRoutes.kt:43-46`).
- **Camada:** `data` (infraestrutura do canal) e `presentation` (fluxo de pareamento). A `domain` não deve conhecer Bluetooth; se houver caso de uso novo, a interface fica em `:core:common` e a implementação em `:core:data`, como hoje.
- **Padrão:** Hilt (`PlatformModule.kt:29` faz o binding), MVI nas ViewModels de pareamento.
- **Novos arquivos (estimativa, depende da opção):** `core/data/.../wearable/bluetooth/*` (transporte), ajustes em `core/common/.../wearable/`, novas permissões em `core/data` ou `feature/auth` no manifest do app.

Diagrama do fluxo atual (ASCII):

```
Celular (feature:auth)                         Relógio (:wear)
  LinkWearViewModel                              PairingViewModel (gera código 6 dígitos)
    │ findNodeByPairingCode(code)                   │ PairingCodeHolder (memória)
    │   CapabilityClient ─────────────────────────► │ WEAR_CAPABILITY
    │   MessageClient /pairing/verify(code) ──────► WearAuthListenerService: confirma se código bate
    │ ◄──────────────────────── /pairing/confirm ───┘
    │ sendAuthPairToNode(nodeId, uid, idToken)
    │   MessageClient /auth/pair ─────────────────► WearAuthListenerService: signInWithGoogleIdToken
    │ notifySignOut() (/auth/sign-out) ───────────► signOut + wearListStore.clear()
Transporte físico: Data Layer (Google Play services; Bluetooth/Wi-Fi gerenciados pela plataforma)
```

### Fatos encontrados que afetam qualquer opção

1. **As listas não passam pelo celular.** `syncShoppingLists` e `syncAuthStatus` (`WearableSyncServiceImpl.kt:37-75`) estão implementados, mas **não têm nenhum chamador de produção** (a busca em `app`, `feature`, `core` e `wear` só encontra a interface, a implementação e testes). O relógio lê as listas diretamente por `ShoppingGetAllUseCase` e as telas em `feature/shopping/.../presentation/wear/viewmodel/` (`LastListViewModel`, `ShoppingDetailViewModel`), após o próprio login no Firebase. Ou seja, hoje a "sincronização" é Firestore pela rede do relógio, não Bluetooth. Confirmar se `syncShoppingLists` é código morto.
2. **`WEAR_LOGIN_ENABLED` não é lida em nenhum lugar.** A constante está em `core/remote-config/.../model/RemoteConfigKeys.kt:62`, e `architecture/02-regras-de-negocio.md:51` diz que o pareamento é controlado por ela. Nenhuma linha de código usa `WEAR_LOGIN_ENABLED`, então o kill switch não tem efeito hoje (o código prevalece sobre a doc).
3. **Documentação de arquitetura desatualizada.** `architecture/05-decisoes-arquiteturais.md:172-176` diz que o pareamento "só copia o uid para o DataStore (`updateUserId`)". O código atual envia o `idToken` e o relógio faz `signInWithGoogleIdToken` (`WearAuthListenerService.kt:34-44`). Não usar a AD-009 como fonte para o desenho sem revalidar.
4. **Pareamento gated por Pro.** `CHANGELOG.md:416` ("gate ... Wear pairing behind Pro"). Não encontrei a checagem no código nesta análise; confirmar onde ela acontece antes de desenhar o gate da nova opção.
5. **Standalone.** `wear/src/main/AndroidManifest.xml` declara `com.google.android.wearable.standalone = true` e o relógio consulta o Firestore sozinho. Um pareamento só por Bluetooth com o celular pode quebrar esse uso sem celular por perto.

### Opções de leitura (a escolher na seção 9)

| Opção | O que muda | Impacto principal |
|---|---|---|
| **A. Pareamento por Bluetooth no lugar do código** | Celular descobre o relógio por Bluetooth (BLE ou scan próximo) e confirma o pareamento na tela, sem digitar código. Token continua indo pelo canal escolhido. | Novas permissões `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` (runtime). Remove `PairingCode`/`PairingCodeHolder`. Quebra relógios já pareados? A verificar. |
| **B. Remover o código, mantendo a Data Layer** | Descoberta automática por capability (`CapabilityClient`) com confirmação no celular, sem código. Bluetooth continua só como transporte da plataforma. | Menor risco; não há código Bluetooth próprio. Não atende literalmente "migrar para bluetooth". |
| **C. Sincronizar dados por conexão Bluetooth própria** | Celular envia as listas ao relógio por socket/GATT Bluetooth (funciona offline), e o relógio deixa de depender do Firestore para ler as listas. | Maior escopo. Exige decidir sobre standalone (item 5), conflito entre Firestore e cache local e a regra de segurança do dado financeiro. |

## 4. Contratos e Interfaces

Nenhuma assinatura é definida até a decisão. Pontos de contrato que qualquer opção toca:

- `WearableSyncService` (`core/common/.../wearable/WearableSyncService.kt`), `public` por ser contrato entre `:core:data`, `:feature:auth`, `:app`. **Se mudar a assinatura de `findNodeByPairingCode(code)` ou `updatePairingCode(code)`, é breaking** e exige: lista de consumidores (`LinkWearViewModel`, `PairingViewModel`, `MainViewModel`), testes (`LinkWearViewModelTest`, `PairingViewModelTest`), entrada no `CHANGELOG.md` e bump de versão (AGENTS.md §6).
- `WearablePaths` (`core/common/.../wearable/WearablePaths.kt`): constantes compartilhadas com `:wear`. Trocar `PAIRING_VERIFY` e `PAIRING_CONFIRM` exige atualizar o relógio no mesmo release; um relógio com versão antiga deixa de parear. **Breaking entre os dois apps.**
- `PairingCodeHolder` (`:core:common`), `public`, usado por `WearAuthListenerService`. Opções A e B o removem.
- Rotas `PairingCode` e `QrCode` (`core/navigation/.../mobile/MobileRoutes.kt:43-46`). Remover `PairingCode` é breaking para o grafo de `feature:auth`; confirmar se há deep link.
- Permissões novas no manifest (opção A ou C): `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` e, se houver anúncio, `BLUETOOTH_ADVERTISE`, todas com justificativa de uso na tela.
- Eventos de Analytics: a definir após a decisão; nome canônico em `core/analytics/.../model/AnalyticsEvents.kt`.
- Remote Config: decidir se `WEAR_LOGIN_ENABLED` passa a ser de fato checado (item 2 da seção 3) e se a nova opção precisa de toggle próprio.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Pareamento usa código de 6 dígitos, gerado no relógio, com verificação por mensagem. | A definir (opções A/B/C). | `WearableSyncServiceImpl.kt:78-157`, `PairingViewModel.kt:30-33` |
| 2 | Login do relógio é pelo token Google, nunca pelo uid copiado. | Mantido em todas as opções (AD-009). | `WearAuthListenerService.kt:34-44`, `LinkWearViewModel.kt:9-10` |
| 3 | O celular só pareia se a conta Google do relógio for a mesma do app. | Mantido. | `LinkWearViewModel.kt:17-18` |
| 4 | Sign-out do celular derruba a sessão do relógio e limpa `wearListStore`. | Mantido. | `MainViewModel.kt:70-85`, `WearAuthListenerService.kt:46-49` |
| 5 | Pareamento só ocorre com `wear_login_enabled` ligada (segundo a doc). | Hoje não é verificado no código; definir se a nova opção respeita a flag. | `RemoteConfigKeys.kt:62`, `architecture/02-regras-de-negocio.md:51` |
| 6 | Relógio é standalone e lê as listas pelo Firestore. | A definir (opção C muda isso). | `wear/src/main/AndroidManifest.xml`, `ShoppingDetailViewModel.kt` |

## 6. Critérios de Aceitação

- **Performance:** o scan Bluetooth deve parar por timeout e ser cancelado ao sair da tela (sem scan contínuo). Nenhum scan em `Dispatchers.Main`; callbacks de BLE em dispatcher de IO. Sem alocação por callback de scan em loop. Se houver transferência de listas (opção C), enviar payload incremental, não a lista inteira a cada alteração, e não repetir I/O já feito.
- **Feature toggles:**

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `wear_login_enabled` | true | remote-config | Pareamento oculto (fluxo atual, a confirmar) | Pareamento disponível |
  | (novo, se necessário) `wear_bluetooth_pairing_enabled` | a definir | remote-config | Fluxo por código ou Data Layer | Fluxo Bluetooth |

- **Observabilidade:** eventos de Analytics e erros (Crashlytics) com nome canônico, sem dados pessoais nem token. Registrar falha de permissão Bluetooth negada e timeout de descoberta.
- **Compatibilidade:** minSdk 28 no app e 30 no `:wear` (`wear/build.gradle.kts`). Permissões de Bluetooth em API 31+ exigem fluxo de runtime; verificar comportamento em API 28-30. Migração: relógios já pareados (sessões Firebase existentes) não podem perder login sem aviso. Definir se o pareamento antigo continua aceito durante a transição.

## 7. Plano de Testes

- **Unitários:** `WearablePaths` (encode/decode, `core/common/src/test/.../WearablePathsTest.kt`), `PairingCodeHolder` se mantido, `LinkWearViewModel` e `PairingViewModel` com mocks de `WearableSyncService`. Cenários: negativo (scan sem resultado, permissão negada, toggle OFF, relógio não encontrado, conta Google diferente).
- **Instrumentados / UI:** telas de pareamento com `testTag` (não asserts de texto, AGENTS.md §5). Teste em dispositivo real com Bluetooth e um relógio físico; emulador não cobre BLE.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`. Atenção: `core/data/config/detekt/baseline.xml:5,19` já tem entradas de baseline para `WearableSyncServiceImpl`; não estender o baseline sem justificativa (AGENTS.md §5).

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Quebra de relógios já pareados (protocolo muda entre app e `:wear`) | alta | alto | Release coordenado dos dois apps; manter caminho antigo durante transição (toggle) |
| Permissões Bluetooth negadas pelo usuário | média | médio | Fallback para o caminho atual; mensagem clara em `strings.xml` (pt-BR e en-US) |
| BLE instável em alguns modelos de relógio/celular | média | alto | Timeout, retry limitado, testar em hardware real |
| Opção C deixa o relógio sem dados sem celular, quebrando o standalone | média | alto | Decisão de produto antes da implementação (pergunta 4) |
| Regras do Firestore (AD-009) bloqueiam o relógio se o login mudar | baixa (se o token continuar igual) | alto | Não alterar o fluxo de token; revisar AD-009 antes de qualquer troca |
| Documentação de arquitetura desatualizada leva a desenho errado | alta | médio | Usar o código como fonte; corrigir `architecture/04` §6 e `05` AD-009 junto com a mudança |

Dependência externa: APIs Bluetooth do Android (`android.bluetooth`) e Wearable Data Layer (Google Play services, `libs.play.services.wearable` em `gradle/libs.versions.toml:200`).

## 9. Perguntas em Aberto (bloqueantes)

- [ ] **Qual é o objetivo exato?** Escolha entre:
  - **A.** Trocar só o pareamento por código por um pareamento Bluetooth (sem digitar código).
  - **B.** Remover o código e manter a Data Layer (descoberta automática); não há código Bluetooth próprio.
  - **C.** Sincronizar as listas do celular para o relógio por Bluetooth próprio, além do pareamento.
- [ ] **Relógio standalone:** ele deve continuar lendo as listas pelo Firestore quando o celular não está por perto? (Se a resposta for "não", a opção C muda a regra 6 da seção 5.)
- [ ] **`syncShoppingLists` e `syncAuthStatus`** (`WearableSyncServiceImpl.kt:37-75`) não têm chamadores. Podem ser removidos, ou fazem parte da intenção (ex.: a opção C seria a forma de ligá-los)?
- [ ] **Relógios já pareados:** devem continuar funcionando sem novo pareamento após a atualização? Se sim, o protocolo antigo precisa coexistir.
- [ ] **Gate de Pro:** o pareamento Bluetooth continua restrito a assinantes Pro (`CHANGELOG.md:416`)? Onde está essa checagem no código?
- [ ] **`wear_login_enabled`:** a doc diz que controla o pareamento, mas nenhum código a lê. Deve ser ligada ao fluxo novo (corrigir o código) ou a doc está errada?
- [ ] **Escopo de release:** a mudança entra como minor (feature) ou faz parte de um release maior, e o `:wear` é publicado junto com o app?

Itens não bloqueantes (registrar, não impedem o plano):
- Confirmar a lista de sinais de `WearAuthListenerService` contra o intent-filter do manifest (`/auth` e `/pairing` apenas; `/auth/sign-out` cai no prefixo `/auth`).
- Atualizar `architecture/04-integracoes-externas.md` §6 e `architecture/05-decisoes-arquiteturais.md` AD-009 após a decisão.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [x] Nenhum código de produção escrito (apenas diagramas e tabelas).
- [ ] Seção 6 preenchida de forma substantiva (parcial: depende da opção escolhida).
- [x] Mudança em símbolo público reflete `breaking` em "Impacto público" (marcado como a definir, com os pontos de breaking listados na seção 4).
- [ ] Novo endpoint/remote-config: toggle definido (depende da opção).
- [ ] Novo cache: armazenamento justificado (só aplicável à opção C).
- [ ] Perguntas em aberto exauridas (7 bloqueantes pendentes).
