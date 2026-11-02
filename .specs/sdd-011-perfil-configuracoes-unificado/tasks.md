# Task: Unificar Configurações dentro da tela de Perfil (layout accordion)

- **Slug:** sdd-011-perfil-configuracoes-unificado
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.3.0 (minor — nova funcionalidade visível ao usuário; a quebra de contrato é de superfície *interna* entre módulos do mesmo app — `feature:settings` ↔ `feature:profile` — não de uma API publicada/consumida por terceiros, então não força major)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** breaking

## 1. Contexto e Motivação

Hoje a aba "Perfil" (`feature/profile/.../screen/ProfileScreen.kt:51-136`) mostra apenas avatar/nome/e-mail, card de assinatura, um atalho para vincular o Wear OS e "Sair". As configurações vivem em uma tela cheia separada, aberta pelo ícone de engrenagem no topo do Perfil (`ProfileScreen.kt:67-72` → `ProfileIntent.onNavigate` → `ProfileViewModel.kt:43` → NavKey `Settings`, `feature/settings/navigation/Settings.kt:7`), renderizada como uma `LazyColumn` plana com todas as seções sempre expandidas (`SettingsScreen.kt:46-66`, dados construídos em `SettingsViewModel.buildSections():119-129`).

O usuário pediu para fundir Configurações dentro do Perfil, com um layout inspirado nas 3 capturas de tela do app iFood anexadas ao pedido: cabeçalho com foto do usuário no topo e, abaixo, uma lista única e rolável onde cada categoria expande/colapsa ao toque (accordion), em vez de navegar para uma tela nova.

Rastreabilidade: pedido direto do usuário via `/code-planner` (anexos de referência visual); código-fonte citado acima.

## 2. Escopo

**Inclui:**
- Fundir o conteúdo de `SettingsScreen` dentro de `ProfileScreen`, como lista única abaixo do cabeçalho do usuário.
- Comportamento accordion **não-exclusivo**: cabeçalho de cada `SettingSection` expande/colapsa ao toque, de forma independente das demais — múltiplas seções podem ficar abertas ao mesmo tempo, já que o accordion serve para categorizar visualmente, não para forçar escolha única. Seções começam colapsadas ao abrir a tela.
- Itens-folha de uma seção expandida continuam navegando para as telas internas já existentes de `feature:settings` (`ThemeSettings`, `LanguageSettings`, `CurrencySettings`, `AiSettings`, `ShoppingSettings`, `DataSettings`, `NotificationSettings`, `SupportContact`, `SupportBugReport`, `SupportFeedback`, `AppRate`, `TermsOfUse`, `PrivacyPolicy`, `OpenSourceLicenses`, `ReleaseNotes`) — nenhuma dessas telas é alterada.
- Remover o ícone de engrenagem do topo do Perfil e a rota pública `Settings` como ponto de entrada separado, já que seu conteúdo passa a viver inline (**BREAKING**, ver Seção 4).
- Manter "Vincular dispositivo Wear OS" e "Sair" como itens fixos (fora do accordion), nas mesmas posições relativas de hoje.
- Atualizar testes unitários e de UI afetados; `strings.xml` (`values` default/en, `values-pt-rBR`, `values-es` — os três locais já versionados no repo) para os novos `contentDescription`/semantics de expandir/colapsar.

**Exclui:**
- Qualquer elemento específico do iFood sem equivalente no domínio do Cestou (Clube, Pagamentos, Comer Fora, Avaliações e Contribuições, selo de verificação, banners promocionais). As imagens são referência apenas de **layout** (cabeçalho com foto + lista categorizada expansível), não de escopo de produto.
- Mudanças em `domain`/`data` de `feature:settings` (use cases, `SettingsRepository`, DTOs) — regras de negócio de cada configuração não mudam.
- Mudanças nas telas internas de configuração (Theme, Language, Currency, AI, Shopping, Data, Notification, telas legais, etc.) — permanecem como estão.
- `feature/profile/.../wear/screen/ProfileWearScreen.kt` (Wear OS) — fora de escopo.
- Fluxo de assinatura/paywall (`ProfileSubscriptionViewModel`, `SubscriptionStatusCard`) — inalterado, apenas reposicionado na mesma tela.

## 3. Impacto Arquitetural

- **Módulos/camadas:** `feature:profile` (presentation) e `feature:settings` (presentation + navigation). `feature:profile` já depende de `feature:settings` via Gradle (`feature/profile/build.gradle.kts:14`), dependência pré-existente a esta tarefa — ver nota em "Regras de Negócio" sobre o desvio de AGENTS.md §2 ("features não dependem umas das outras diretamente").
- **Padrão:** Clean Architecture + MVI mantido; nenhum ViewModel passa a injetar outro ViewModel — `SettingsViewModel` é obtido via `hiltViewModel()` dentro do `composable<Profile>` de `ProfileGraph.kt`, exatamente como já ocorre hoje com `ProfileSubscriptionViewModel` (`ProfileGraph.kt:23`).

**Novos arquivos:**
- `feature/profile/src/main/java/br/com/brunocarvalhs/howmuch/feature/profile/presentation/components/ProfileSettingsSection.kt` — composable privado ao módulo profile, container visual (card + espaçamento) que embrulha `SettingsSectionsList` no layout do Perfil.

**Arquivos alterados:**
- `feature/profile/.../screen/ProfileScreen.kt` — insere a lista de seções entre o grupo "Vincular dispositivo" e "Sair"; remove o `IconButton` de engrenagem do `CenterAlignedTopAppBar` (linhas 66-73).
- `feature/profile/.../intent/ProfileIntent.kt` — remove `onNavigate: (Any) -> Unit`.
- `feature/profile/.../viewmodel/ProfileViewModel.kt` — remove o import e uso de `feature.settings.navigation.Settings` (linhas 16, 43).
- `feature/profile/navigation/ProfileGraph.kt` — instancia `SettingsViewModel` via `hiltViewModel()` dentro do `composable<Profile>`, igual ao padrão da linha 23.
- `feature/settings/.../screen/SettingsScreen.kt` — extrai o corpo da `LazyColumn` (linhas 46-66) para um composable público reutilizável `SettingsSectionsList`, com suporte a accordion e `testTag`s; `SettingsScreen` (tela cheia, hoje alcançada pela rota removida) passa a delegar para esse composable **ou é removida** se nada mais a utilizar (confirmar durante a implementação com busca de usos).
- `feature/settings/.../state/SettingsUiState.kt`, `.../state/SettingItem.kt`-equivalente (`SettingSection`) — ver Seção 4.
- `feature/settings/.../intent/SettingsIntent.kt` — adiciona `onToggleSection`.
- `feature/settings/.../viewmodel/SettingsViewModel.kt` — adiciona lógica de expand/collapse e `id` estável por seção (ver Seção 4 e Pergunta em Aberto #2); emite o novo evento de analytics.
- `feature/settings/navigation/Settings.kt` — remove `data object Settings : NavKey` (**BREAKING**).
- `feature/settings/navigation/SettingsGraph.kt` — remove o bloco `composable<Settings> { ... }` (linhas 39-49, mantendo o restante de `generalSettings`).
- `core/analytics/.../model/AnalyticsEvents.kt` — novo evento (Seção 4g).

**Diagrama ASCII (fluxo após a mudança):**

```
Bottom Nav "Perfil"
        │
        ▼
composable<Profile>  (feature:profile/navigation/ProfileGraph.kt)
        │
        ├── hiltViewModel<ProfileViewModel>()             → ProfileUiState (user, isLoading)
        ├── hiltViewModel<ProfileSubscriptionViewModel>()  → SubscriptionStatus
        └── hiltViewModel<SettingsViewModel>()             → SettingsUiState (sections, expandedSectionIds)
                        │
                        ▼
                ProfileScreen(profileState, subscriptionStatus, settingsState, profileIntent, settingsIntent)
                        │
            ┌───────────┼───────────────────────┐
            ▼           ▼                       ▼
      UserInfoHeader  SubscriptionCard   SettingsSectionsList (feature:settings, público)
      (avatar+nome)   (CestouCard)         │  accordion não-exclusivo (N seções abertas)
      Wear OS / Sair  (fixos, fora do      │  item-folha tap → onNavigate(item.route)
      (fixos)          accordion)          ▼
                               ThemeSettings / AiSettings / ... (inalterados)
```

**Injeção Hilt:** sem módulos novos. `SettingsViewModel` já é `@HiltViewModel`; passa a ser resolvido também no grafo do Perfil, do mesmo jeito que hoje é resolvido no grafo de Settings — sem duplicar coleta de `SettingsRepository.getSettings()` porque cada ponto de composição tem sua própria instância de ViewModel escopada à entrada do NavGraph (comportamento padrão do Hilt+Nav3, igual ao já usado para `ProfileSubscriptionViewModel`).

## 4. Contratos e Interfaces

**(a) `feature/settings/navigation/Settings.kt` — remoção (BREAKING)**
```kotlin
// REMOVIDO. Único consumidor identificado: ProfileViewModel.intent.onNavigate
// (feature/profile/.../ProfileViewModel.kt:43). Confirmar com busca antes de remover:
// grep -rn "settings\.navigation\.Settings\b" --include=*.kt .
// data object Settings : NavKey
```

**(b) Mudança de visibilidade `internal` → `public` em `feature:settings`**

`internal` no Kotlin é por *módulo Gradle*; como `feature:profile` é um módulo diferente de `feature:settings`, os tipos abaixo precisam ficar `public` para que `ProfileScreen`/`ProfileGraph` os referenciem entre módulos (hoje só `Settings`, o NavKey, era público nesse pacote). Esta é a justificativa exigida por AGENTS.md §6.2: sem isso o build de `feature:profile` não compila.

```kotlin
// feature/settings/presentation/viewmodel/SettingsViewModel.kt
@HiltViewModel
class SettingsViewModel @Inject constructor(...) : ViewModel()   // era internal

// feature/settings/presentation/state/SettingsUiState.kt
data class SettingsUiState(
    val sections: List<SettingSection> = emptyList(),
    val expandedSectionIds: Set<String> = emptySet()   // novo campo — ver (c); accordion
                                                         // não-exclusivo, múltiplas seções podem
                                                         // ficar abertas ao mesmo tempo
)                                                  // era internal

data class SettingSection(
    val id: String,                               // novo campo — ver (c)
    val title: UiText,
    val items: List<SettingItem>
)                                                  // era internal

data class SettingItem(
    val title: UiText,
    val subtitle: UiText? = null,
    val icon: ImageVector,
    val route: Any? = null
)                                                  // era internal (sem mudança de forma)

// feature/settings/presentation/intent/SettingsIntent.kt
data class SettingsIntent(
    val onNavigate: (Any) -> Unit = {},
    val onBack: () -> Unit = {},
    val onSendEmail: (String) -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    val onUpdateLanguage: (String) -> Unit = {},
    val onUpdateCurrency: (String) -> Unit = {},
    val onToggleSection: (sectionId: String) -> Unit = {}   // novo — alterna a presença do id
                                                               // em expandedSectionIds (add/remove)
)                                                              // era internal

// feature/settings/presentation/screen/SettingsScreen.kt
@Composable
fun SettingsSectionsList(                         // novo, extraído de SettingsScreen.kt:46-66
    sections: List<SettingSection>,
    expandedSectionIds: Set<String>,
    intent: SettingsIntent,
    modifier: Modifier = Modifier
)                                                   // público desde a criação
```

**(c) `SettingSection.id` — identificador estável.** `title` é `UiText` (pode ser `StringResource`), inadequado como chave do estado de expansão (muda por locale). Confirmado com o usuário: IDs fixos e estáveis por seção, atribuídos em `SettingsViewModel.buildSections()` (`SettingsViewModel.kt:119-129`): `"general"`, `"ai"`, `"shopping"`, `"data"`, `"notifications"`, `"support"`, `"about"`. É mudança de forma de um tipo já usado em testes existentes (`SettingsScreenTest.kt`, `SettingsScreenUiTest.kt`, `SettingsViewModelTest.kt`) — atualizar esses testes como parte da implementação.

**(d) `feature/profile/presentation/intent/ProfileIntent.kt`**
```kotlin
internal data class ProfileIntent(
    // onNavigate removido — não há mais navegação para uma tela "Settings" cheia
    val onSignOut: () -> Unit = {},
    val onLinkWearDevice: () -> Unit = {},
    val onLinkMobileDevice: () -> Unit = {},
    val onManageSubscription: () -> Unit = {}
)
```

**(e) `feature/profile/presentation/screen/ProfileScreen.kt`** (assinatura permanece `internal` — consumida só por `ProfileGraph.kt` no mesmo módulo)
```kotlin
@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    intent: ProfileIntent,
    settingsState: SettingsUiState,   // novo, público em feature:settings
    settingsIntent: SettingsIntent,   // novo, público em feature:settings
    subscriptionStatus: SubscriptionStatus = SubscriptionStatus.FREE
)
```

**(f) Novo evento de Analytics** (`core/analytics/.../model/AnalyticsEvents.kt`, próximo a `SETTINGS_LANGUAGE_CHANGED`/`PROFILE_SIGN_OUT`):
```kotlin
const val SETTINGS_SECTION_TOGGLED = "settings_section_toggled"
// params: AnalyticsParams.SECTION_ID (String), AnalyticsParams.EXPANDED (Boolean)
```

Nenhum novo endpoint, coleção Firestore ou chave de remote-config.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | O ícone de engrenagem no topo do Perfil navega para uma tela cheia "Settings" com lista plana de seções. | Ícone removido; as seções de configuração aparecem inline, abaixo do cabeçalho do usuário, dentro da própria tela Perfil. | `ProfileScreen.kt:66-73` |
| 2 | `SettingsScreen` renderiza todas as seções sempre expandidas, em `LazyColumn` plana. | Cada seção começa colapsada; toque no cabeçalho expande/colapsa de forma independente das demais — múltiplas seções podem ficar abertas ao mesmo tempo (accordion não-exclusivo, confirmado com o usuário: "o accordion é para categorizar"). | `SettingsScreen.kt:46-66` |
| 3 | A rota `Settings` (NavKey público) é o único ponto de entrada da tela de configurações. | Rota removida; entrada sempre via aba "Perfil". Subtelas (`ThemeSettings` etc.) continuam navegáveis pelo `Navigator` a partir dos itens-folha. | `Settings.kt:7`, `SettingsGraph.kt:39-49` |
| 4 | "Vincular dispositivo Wear OS" e "Sair" são `ProfileMenuGroup`s fixos, fora de qualquer seção de configurações. | Inalterado — permanecem fixos, fora do accordion, nas mesmas posições relativas. | `ProfileScreen.kt:108-133` |
| 5 | `buildSections()` constrói as seções observando `SettingsRepository.getSettings()`, dentro de `SettingsViewModel`. | Inalterado — mesma função, mesmo ViewModel; passa a ser instanciado também a partir do grafo de navegação do Perfil, sem duplicar lógica. | `SettingsViewModel.kt:113-129` |
| 6 (desvio pré-existente, não introduzido por esta tarefa) | `feature:profile` já depende de `feature:settings` via Gradle, contrariando "features não dependem umas das outras diretamente" (AGENTS.md §2). | Esta tarefa **aprofunda** esse acoplamento (de 1 NavKey para ViewModel + estado + composable). Não é regressão nova, mas amplia uma exceção já existente — registrar em `architecture/05-decisoes-arquiteturais.md` como decisão explícita. | `feature/profile/build.gradle.kts:14` |

## 6. Critérios de Aceitação

- **Performance:** o estado de expansão (`expandedSectionIds: Set<String>`) usa `id: String` como chave (não `UiText`), evitando recomposição ampla a cada toggle — `onToggleSection` produz um novo `Set` apenas com a entrada alterada (`+`/`-`), nunca reconstrói as demais seções; usar `key(section.id)` nos itens de `LazyColumn`/`Column` para animação estável por seção independente. `SettingsViewModel` mantém uma única coleta de `settingsRepository.getSettings()` (sem duplicar `Flow` ao ser resolvido em dois pontos de composição, pois cada composição tem sua própria instância escopada ao NavGraph). Sem alocação de novas listas a cada recomposição — `buildSections()` só roda quando `AppSettings` emite.
- **Toggles:** nenhuma feature flag necessária — mudança é 100% de apresentação, sem condicional de remote-config.
- **Observabilidade:** evento `settings_section_toggled` disparado a cada expand/collapse, com `section_id` e `expanded`; `trackScreenView("profile", ...)` e `trackScreenView("settings", ...)` continuam disparando uma vez cada (nenhuma duplicação por reuso do `SettingsViewModel`).
- **Compatibilidade:** minSdk 28 sem impacto (Compose puro); Wear OS fora de escopo e não afetado; nenhuma migração de dado local/Firestore.
- **Acessibilidade:** cada cabeçalho de seção expõe estado expandido/colapsado via `Modifier.semantics { stateDescription = ... }` ou o papel semântico padrão do Compose para elementos expansíveis, para leitores de tela — preferir essa abordagem a strings novas dedicadas.

## 7. Plano de Testes

**Unitários:**
- `feature/settings/.../viewmodel/SettingsViewModelTest.kt` — novo caso: `onToggleSection(id)` alterna a presença do `id` em `expandedSectionIds` (adiciona se ausente, remove se presente); expandir uma seção **não** colapsa as demais já abertas (accordion não-exclusivo); evento de analytics disparado com os parâmetros corretos; `SettingSection.id` estável e único por seção construída em `buildSections()`.
- `feature/profile/.../viewmodel/ProfileViewModelTest.kt` — remover os asserts relacionados a `onNavigate`/`Settings`; nenhum caso novo de estado (responsabilidade do `ProfileViewModel` não muda).
- Reaproveitar os fakes de `SettingsRepository` já existentes em `SettingsViewModelTest.kt`.

**UI (Robolectric, JVM — seguindo o padrão de `ProfileScreenTest.kt`/`SettingsScreenTest.kt`):**
- Novo `SettingsSectionsListTest` (em `feature:settings`): seção colapsada não expõe os `testTag`s dos itens-filho; tocar o cabeçalho expande (itens aparecem); tocar outro cabeçalho expande essa seção sem colapsar a anterior — ambas permanecem abertas simultaneamente; tocar item-folha com `route` não nulo invoca `onNavigate(route)`; item sem `route` (ex.: "Versão") não invoca nada.
- `ProfileScreenTest.kt` — remover o teste `tapping the settings icon triggers onNavigate` (ícone removido); novo teste garantindo que as seções de `settingsState` aparecem na tela e que tocar um item-folha delega para `settingsIntent.onNavigate`.
- Asserções **novas** desta tarefa devem usar `testTag` (AGENTS.md §5), não `onNodeWithText`/`onNodeWithContentDescription` — os testes antigos que usam texto não precisam ser migrados nesta tarefa (fora de escopo), mas nada novo deve reintroduzir esse padrão.

**Instrumentado (`./gradlew :feature:profile:connectedDebugAndroidTest`, conforme tabela de comandos do AGENTS.md):**
- Fluxo: abrir aba "Perfil" → tocar uma categoria → verificar expansão → tocar item-folha → verificar navegação para a subtela correspondente → voltar → verificar que a seção permanece expandida (estado de `SettingsViewModel` preservado pelo NavGraph, para manter a experiência fluida conforme confirmado com o usuário).

**Lint/Detekt/Cobertura:** `./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout` — `koverVerifyLayout` é especialmente relevante aqui porque dois módulos (`feature:settings`, `feature:profile`) mudam superfície pública/estrutura de pacotes.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Tornar `SettingsViewModel`/`SettingsUiState`/`SettingSection`/`SettingItem`/`SettingsIntent` públicos aumenta a superfície exposta de `feature:settings`, facilitando acoplamento futuro indevido de outros módulos | média | médio (dívida arquitetural) | Documentar como decisão explícita em `architecture/05-decisoes-arquiteturais.md` (novo AD-0xx); revisão obrigatória de maintainer (AGENTS.md §6.7) |
| Remoção da rota pública `Settings` quebra algum consumidor não identificado pela busca estática (deep link externo, notificação push, atalho salvo) | baixa | alto (crash de navegação) | Buscar todas as referências antes de remover (`grep -rn "settings\.navigation\.Settings\b"`); checar `core:remote-config` e payloads de notificação por deep links hardcoded |
| Composição de duas fontes de estado (`ProfileUiState` + `SettingsUiState`) na mesma tela pode gerar mais recomposição que a tela atual (já otimizada com `ProfileSubscriptionViewModel` isolado) | baixa | baixo | Perfilar com Compose Compiler metrics / Layout Inspector antes de abrir o PR |
| Testes existentes usam `onNodeWithText`/`onNodeWithContentDescription`, não `testTag`, divergindo de AGENTS.md §5 — esta tarefa toca esses arquivos | alta | baixo | Migrar apenas as asserções novas/alteradas para `testTag`; não é escopo migrar o arquivo inteiro |

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma pendência bloqueante. As 3 perguntas da versão anterior deste plano foram respondidas diretamente pelo usuário em 2026-10-27:

1. **Modelo de accordion:** não-exclusivo — múltiplas seções podem ficar expandidas simultaneamente ("o accordion é para categorizar", não para forçar escolha única). Refletido em 4(b)/(c) (`expandedSectionIds: Set<String>`), 5, 6 e 7.
2. **`SettingSection.id`:** aprovado — ids fixos (`"general"`, `"ai"`, `"shopping"`, `"data"`, `"notifications"`, `"support"`, `"about"`) adicionados em `SettingsViewModel.buildSections()`; testes existentes que usam `SettingSection` serão atualizados como parte da implementação.
3. **Estado do accordion ao voltar de uma subtela:** confirmado que deve permanecer expandida — "a ideia é manter, uma experiência boa".

`status: ready` — a implementação pode começar a partir deste plano.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existe cobertura).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `breaking` em "Impacto público" (remoção de `Settings` NavKey + tipos que passam de `internal` para `public`).
- [ ] Novo endpoint/remote-config: N/A (nenhum criado nesta tarefa).
- [ ] Novo cache: N/A (nenhum criado nesta tarefa).
- [x] Perguntas em aberto exauridas.
