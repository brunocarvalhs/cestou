# Task: Exibir anúncios no plano FREE (carrinho, histórico e IA) com intersticial no uso repetido de IA

- **Slug:** anuncios-free-carrinho-historico-ia
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-13
- **Status:** ready (decisões aprovadas pelo owner em 2026-11-13; ver seção 9)
- **Versão-alvo:** a definir pelo processo de release (última versão dev registrada: 2.5.1, commit `2e947b58`). Bump e CHANGELOG ficam fora dos PRs de feature (AGENTS.md §6, itens 5–6).
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive (novo módulo `core:ads`, novas flags de remote-config; nenhum contrato existente quebra)

## 1. Contexto e Motivação
- Pedido: monetizar o plano FREE com anúncios sem atrapalhar o uso direto do app.
  - Banner na tela de carrinho e no histórico.
  - Quando o usuário FREE usa a IA mais de uma vez, exibir anúncio de tela inteira (intersticial).
- Não existe SDK de anúncios no projeto. A busca por `AdView`, `InterstitialAd`, `MobileAds` e `BannerAd` em `*.kt` não retornou nada, e `gradle/libs.versions.toml` não tem dependência de anúncios.
- **Correção do que estava em `architecture/02-regras-de-negocio.md` §3.2:** o carrinho não é bloqueado para FREE. O gate PRO do carrinho só cobre compartilhar a lista (`feature/cart/.../navigation/CartGraph.kt:80-91`; `CartSubscriptionGateViewModel.kt:20`, "sharing is a Pro feature"). Isso significa que o banner no carrinho alcança o FREE sem mudança de regra.
- Mesmo ajuste para listas de compras: o gate PRO em `ShoppingSubscriptionGateViewModel.kt:19` cobre só o convite por QR code.
- **Conflito com a regra de IA:** FREE tem **uma** mensagem gratuita. Depois dela, o envio fica bloqueado (`feature/chat/.../ChatAiGateViewModel.kt:37-41`; teste `ChatAiGateViewModelTest.kt:57`). Ou seja, o FREE nunca chega à segunda mensagem enviada. O gatilho "mais de 1 vez" precisa ser definido em relação a esse bloqueio.
- **Histórico:** existem duas telas com esse nome.
  - `feature/shopping/.../presentation/screen/HistoryScreen.kt` (listas finalizadas, rota em `ShoppingGraph.kt:106`).
  - `feature/chat/.../presentation/screen/AiHistoryScreen.kt` (histórico de conversas com a IA).

## 2. Escopo
- **Inclui (se aprovado):**
  - Módulo `core:ads` com SDK de anúncios, consentimento de privacidade e composable de banner.
  - Banner no rodapé da tela de carrinho (`CartScreen.kt`) e na tela de histórico escolhida (seção 9).
  - Intersticial de tela inteira no gatilho de uso de IA definido na seção 9.
  - Visibilidade só para FREE, controlada por `AdsPolicy`.
  - Flags de remote-config para liga/desliga por formato.
  - Eventos de Analytics para exibição, clique e falha.
  - Testes unitários da política de exibição e dos contadores de frequência.
- **Exclui:**
  - Anúncios em Wear OS (`:wear`).
  - Anúncios em login, onboarding e paywall.
  - Anúncios em telas de edição de produto ou de lista (fora do pedido).
  - Mediação de múltiplas redes.
  - Login social (assunto separado).

## 3. Impacto Arquitetural

- Módulos afetados: `core:ads` (novo), `feature:cart`, `feature:shopping` (histórico), `feature:chat` (contador de uso e intersticial), `core:remote-config`, `core:domain` (leitura de `SubscriptionStatus`).
- Padrão: Clean Architecture + MVI. Features consomem só a API pública de `core:ads`, nunca o SDK.
- Novo módulo precisa entrar em `CORE_LAYERS` (`app/src/test/.../architecture/ArchitectureTest.kt`).

```
feature:cart (CartScreen)      feature:shopping (HistoryScreen)     feature:chat (AI)
   AdBanner(CART)                 AdBanner(HISTORY)                    AiUsageCounter ──┐
        │                              │                                                 │
        ▼                              ▼                                                 ▼
core:ads  presentation ──► AdsPolicy (domain) ◄── SubscriptionRepository (core:domain)  InterstitialGate
        │                       ▲                                                        │
        │                       └── FeatureFlagService (core:remote-config)              │
        ▼                                                                                ▼
  data: AdsSdkWrapper (banner + interstitial)  ◄──────────── show(Interstitial) ◄────────┘
        + ConsentGateway (UMP)
```

- Carregamento e exibição:
  - O banner usa o estado `Loaded | Loading | Failed | Hidden` como `StateFlow` imutável.
  - O intersticial é pré-carregado depois que o consentimento é concedido, para não atrasar a resposta da IA.
  - Um único objeto do SDK por processo (`@Singleton`).

## 4. Contratos e Interfaces

### 4.A `core:ads` (public, justificado)
- `AdPlacement` e `AdBanner` são usados por `feature:cart` e `feature:shopping`, então são `public` (AGENTS.md §6). O restante fica `internal`.
- `InterstitialGate` e `AiUsageCounter` são usados pelo `feature:chat`. Como o contador é de IA, ficam em `core:ads` apenas se for o ponto de exibição. Decisão a confirmar (seção 9).

```
// public (contrato entre módulos)
enum class AdPlacement { CART, HISTORY }
@Composable fun AdBanner(placement: AdPlacement, modifier: Modifier = Modifier)
interface InterstitialGate { suspend fun maybeShowAfterAiTurn(): InterstitialResult }

// internal
interface AdsPolicy { fun canShow(placement: AdPlacement): Flow<Boolean> }
interface ConsentGateway { suspend fun requestConsentIfNeeded(activity: Activity): ConsentResult }
interface AiTurnCounter { suspend fun incrementAndGet(): Int }   // persistência a decidir (ver 4.C)
```

- Breaking: nenhum.
- Consumidores: `feature:cart`, `feature:shopping`, `feature:chat`.

### 4.B Remote-config (novas flags)

| Chave | Default | Motivo |
|---|---|---|
| `ads_enabled` | `false` | Kill switch global. Ligar só após consentimento e unidades validadas. |
| `ads_banner_enabled` | `false` | Liga o banner (carrinho e histórico). |
| `ads_interstitial_enabled` | `false` | Liga o intersticial de IA. |
| `ads_ai_interstitial_threshold` | `2` | Número de turnos de IA a partir do qual o intersticial aparece. Valor depende da resposta da seção 9. |

- Constantes em `RemoteConfigKeys.kt` e defaults em `RemoteConfigDefaults.kt`, no mesmo padrão de `AUTH_*_ENABLED` (`RemoteConfigDefaults.kt:34-40`).

### 4.C Persistência do contador de turnos de IA
- Opções:
  - **DataStore (Preferences):** guarda um inteiro simples. Sobrevive a reinício, sem Firestore. Indicado.
  - **Firestore:** sincroniza entre dispositivos, mas adiciona I/O de rede e regra de segurança. Não recomendado.
  - **Memória:** zera ao reiniciar o app. Não cumpre a regra "mais de 1 vez" entre sessões.
- Recomendação: DataStore, leitura via `Flow` e escrita fora da thread principal.

### 4.D Analytics (nomes a confirmar com `core:analytics`)
- `ad_impression` (com `placement`), `ad_click`, `ad_load_failed`, `ai_interstitial_shown`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Carrinho é acessível a FREE; só o compartilhamento exige PRO. | Banner aparece no carrinho para FREE. Nenhuma mudança de acesso. | `CartGraph.kt:80-91`; `CartSubscriptionGateViewModel.kt:20` |
| 2 | Convite de lista por QR code é PRO. | Sem mudança. Fora do escopo de anúncio. | `ShoppingSubscriptionGateViewModel.kt:19` |
| 3 | FREE tem uma mensagem de IA; a segunda é bloqueada. | Regra de bloqueio mantida. O intersticial entra no gatilho definido na seção 9. | `ChatAiGateViewModel.kt:37-41`; `ChatAiGateViewModelTest.kt:57` |
| 4 | PRO envia IA sem limite. | `AdsPolicy` retorna `false` para PRO em todos os placements. | `ChatAiGateViewModel.kt:41`; `core/domain/.../model/SubscriptionStatus.kt:12` |
| 5 | Histórico de listas finalizadas. | Banner no `HistoryScreen`, se for a tela escolhida. | `ShoppingGraph.kt:106`; `HistoryScreen.kt` |
| 6 | Histórico de conversas com IA. | Banner no `AiHistoryScreen`, se for a tela escolhida. | `AiHistoryScreen.kt` |
| 7 | Sem consentimento de privacidade, não há requisição de anúncio personalizado. | `ConsentGateway` roda antes do primeiro carregamento. | Lacuna: não há código de consentimento. Seção 9. |
| 8 | `architecture/02` §3.2 diz "Carrinho gated por PRO". | O código mostra que o gate é só para compartilhar. **Atualizar a documentação** (AGENTS.md §3). | `CartGraph.kt:80-91` |

## 6. Critérios de Aceitação

- **Performance:**
  - Banner carrega fora da thread principal e não bloqueia a composição. Estado com `remember` e `StateFlow`.
  - Intersticial pré-carregado. Nunca carregar durante o envio da mensagem de IA, para não atrasar a resposta.
  - Contador de turnos lido uma vez por sessão e escrito de forma assíncrona (DataStore).
  - Nenhum I/O de rede de anúncio em tela PRO.
  - Sem recomposição extra no `CartScreen` e no `HistoryScreen` ao carregar o banner: o slot tem altura fixa reservada.
- **Feature toggles:**

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `ads_enabled` | false | remote-config | nenhum anúncio | anúncios para FREE, após consentimento |
  | `ads_banner_enabled` | false | remote-config | sem banner | banner em carrinho e histórico |
  | `ads_interstitial_enabled` | false | remote-config | sem intersticial | intersticial no gatilho de IA |
  | `ads_ai_interstitial_threshold` | 2 | remote-config | — | intersticial a partir do N-ésimo turno |

- **Observabilidade:** eventos da seção 4.D. Crashlytics registra falha de inicialização do SDK, sem dados pessoais nem conteúdo de conversa.
- **Compatibilidade:** minSdk 28. Sem efeito no Wear OS. Sem migração de Firestore; o contador novo vive no DataStore local.

## 7. Plano de Testes

- **Unitários:**
  - `AdsPolicy`: PRO → `false`; FREE com flag ON → `true`; FREE com `ads_enabled` OFF → `false`; consentimento negado → `false`.
  - Contador de turnos: incrementa uma vez por turno; sobrevive a reinício (teste com DataStore fake); threshold lido da flag.
  - Intersticial: não aparece antes do threshold; aparece no threshold; não aparece para PRO; não aparece se o carregamento falhou.
  - Falha de carregamento de anúncio não lança exceção nem bloqueia a tela.
  - Mock do SDK de anúncios, do `FeatureFlagService` e do `SubscriptionRepository`.
- **Instrumentados / UI:**
  - `testTag` no banner de `CartScreen` e `HistoryScreen`. Não checar texto (AGENTS.md §5).
  - PRO não encontra o `testTag` do banner.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`. `core:ads` precisa de testes para não derrubar a cobertura de 85%.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Intersticial após resposta de IA atrapalhar o uso e levar à desinstalação. | alta | alto | Threshold configurável por flag; começar com `ads_interstitial_enabled` OFF; medir antes de ligar. |
| Intersticial bater na política de anúncios (exibir logo após ação do usuário). | média | alto | Exibir só em transição natural (fim da resposta, não durante o envio); revisar política antes do release. |
| Anúncios derrubarem a ativação (memória: 79,9% saem antes do primeiro produto). | alta | alto | Banner só em telas pós-ativação (carrinho e histórico). Medir `ad_impression` por usuário. |
| Falta de consentimento LGPD/GDPR. | média | alto | `ConsentGateway` antes de qualquer carregamento. |
| Contador de IA zerar ao reinstalar, permitindo burlar a regra. | média | baixo | Aceito para intersticial (não é regra de cobrança). Se virar regra de cobrança, mover para Firestore com regra de segurança. |
| Base de usuários pequena (memória: 2 contas reais), o que torna a medição pouco estatística. | alta | médio | Kill switch e observação de eventos antes de decidir formato. |
| Pré-carregamento do intersticial consumir dados ou bateria. | baixa | baixo | Carregar só após consentimento e só quando FREE usa IA. |

## 9. Decisões (respondidas pelo owner)

- [x] **Intersticial e o bloqueio de IA:** opção **(c)**. O intersticial aparece ao final da 1ª resposta de IA do FREE, antes do bloqueio. Nenhuma mensagem extra é liberada. A regra de bloqueio em `ChatAiGateViewModel.kt:37-41` não muda.
- [x] **Histórico:** `HistoryScreen` (listas finalizadas, `ShoppingGraph.kt:106`). **Assumido** por não ter sido respondido explicitamente. `AiHistoryScreen` fica de fora.
- [x] **Frequência:** o intersticial aparece **uma vez** ao final da 1ª resposta de IA do FREE (threshold = 1 turno concluído). **Assumido**: o pedido original falava em "mais de 1 vez"; com a opção (c), o anúncio aparece logo depois do 1º uso. Ajustável pela flag `ads_ai_interstitial_threshold`.
- [x] **Telas:** só carrinho, histórico e IA. Nenhuma outra.
- [x] **Rede de anúncios:** Google AdMob. Em debug usar os IDs de teste oficiais; IDs reais de produção ficam fora do repositório.
- [x] **Consentimento:** UMP (Google User Messaging Platform) para LGPD/GDPR.
- [ ] **Público-alvo (aberto, não bloqueia):** assumido que o app não é direcionado a menores de 13 anos. Se for, anúncios precisam de configuração "child-directed" e a implementação deve ser revisada.
- [x] **Documentação:** corrigir `architecture/02-regras-de-negocio.md` §3.1–3.2 para refletir que o gate PRO é só de compartilhamento e convite.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [x] Nenhum código de produção escrito (apenas pseudocódigo e assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` em "Impacto público" (`core:ads` marcado `public` com justificativa).
- [x] Novo endpoint/remote-config: toggle definido, observabilidade planejada.
- [x] Novo cache: armazenamento justificado (DataStore para o contador; Firestore descartado).
- [x] Perguntas em aberto registradas; bloqueantes na seção 9.

## 11. Desvios da implementação (registrados após o build)
- **Acesso ao SDK só pelo `:app`:** `feature:cart`, `feature:shopping` e `feature:chat` não dependem de `:core:ads`. Os módulos de feature são compilados no `:wear`, então o SDK não pode entrar por eles. As features recebem `LocalAdBanner` e `LocalAiInterstitial` (`core:ui/contract`), e o `MainActivity` fornece as implementações. Sem provedor, nada aparece.
- **Exceção de detekt:** os dois `CompositionLocal` estão no baseline de `core:ui` (`CompositionLocalAllowlist:AdsLocals.kt$...`), com a mesma justificativa de `LocalCurrency`.
- **Sem `AdBannerViewModel`:** a regra `ViewModel -> .presentation.viewmodel` (`ArchitectureTest`) não aceita ViewModel em `core:*`. O banner lê `AdsPolicy` e `AdsSdk` por um `@EntryPoint` do Hilt (`service/AdsEntryPoint.kt`).
- **Sem contador de turnos no DataStore (seção 4.C):** o trial já grava "mensagem gratuita usada" (`AiTrialRepository`). O intersticial dispara na transição desse valor (`ChatAiGateViewModel.aiInterstitialRequests`), então um contador novo seria redundante. Por isso `ads_ai_interstitial_threshold` **não existe**.
- **IDs de unidade no Remote Config:** `ads_banner_unit_id` e `ads_interstitial_unit_id` (default: IDs de teste do AdMob) lidos por `service/AdUnitIds.kt`. Trocar para produção é ação no console. O **App ID** continua em `core/ads/src/main/res/values/ads.xml`, porque o SDK o lê na inicialização, antes do Remote Config.
- **Analytics não implementado:** os eventos da seção 4.D (`ad_impression`, `ad_click`...) ficaram de fora desta entrega.
- **Consequência do pedido "mais de 1 vez":** com a opção (c) e o trial de uma mensagem, o intersticial aparece **uma única vez por usuário FREE**, na primeira resposta. Não existe segundo uso gratuito para disparar um novo anúncio.
- **Público-alvo (seção 9):** assumido não direcionado a menores de 13 anos. Continua em aberto e precisa de confirmação antes do release.
- **Antes do release:** trocar os IDs de teste (remote config e `ads.xml`) pelos de produção.
