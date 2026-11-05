# Task: Restaurar gate de cobertura (layout) e estabilizar testes instrumentados de settings

- **Slug:** sdd-024-cobertura-testes-ai-settings
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** ready
- **Versão-alvo:** 2.2.2 (patch — refactor + testes, sem mudança de comportamento visível)
- **Tipo de mudança:** refactor
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

O PR #242 foi mesclado em `develop` com o pipeline de cobertura falhando:
`Rule violated: lines covered percentage is 78.525200, but expected minimum is 80`.

Reproduzi o relatório localmente (`./gradlew koverXmlReportLayout`) e confirmei que o
número **78,53%** é exatamente o da variante **`layout`** do Kover
(`buildSrc/src/main/kotlin/howmuch.kover.gradle.kts:147-154`), não da cobertura geral
(`koverVerify` geral está em 85,87%). A variante `layout` só inclui
`*.presentation.screen.*` e `*.presentation.components.*`
(`howmuch.kover.gradle.kts:23-29`) e só é alimentada pelos testes JVM (`testDebug` /
Robolectric) — **testes `androidTest` (instrumentados) não contam**
(comentário em `howmuch.kover.gradle.kts:146`).

A causa raiz: `AiSettingsScreen.kt` ganhou lógica nova (`trySave`, estado
`byokKeyError`, branch `isError` em `Tier3Content`) no commit `642ae3e7`, e a única
cobertura adicionada foi `feature/settings/src/androidTest/.../AiSettingsScreenTest.kt`
— que não é contado pela variante `layout`. Medido localmente:
`AiSettingsScreenKt` está em 64,0% (67 linhas não cobertas de 186) e o singleton de
lambdas padrão `ComposableSingletons$AiSettingsScreenKt` em 50,0% (14/28 não cobertas).

Além disso, o job `:feature:settings:connectedDebugAndroidTest` falhou no CI com sinais
de instabilidade de emulador (`Unable to connect to adb daemon`,
`Failed to find ColorBuffer`), ~10s após "Running tests on devices" — tempo insuficiente
para os 3 testes de `AiSettingsScreenTest`. Isso é uma falha de infraestrutura do runner,
não do teste em si, mas o teste também está no lugar errado para o propósito dele (ver
§2 e §9).

Pedido do usuário: elevar a cobertura de `develop` ao mínimo exigido criando testes
novos, e quebrar as classes complexas envolvidas em unidades isoladas e testáveis
aplicando SOLID e Clean Code.

## 2. Escopo

**Inclui:**
- Extrair a lógica de validação/salvamento de `AiSettingsScreen.kt`
  (`trySave`, `byokKeyError`) para uma função pura testável em JVM puro (sem
  Robolectric), isolada de `Composable`.
- Extrair `Tier2Content`, `Tier3Content` e `TierCard` de
  `AiSettingsScreen.kt:192-343` para arquivos próprios em `presentation/components/`
  (hoje só existe `SettingsHeader.kt` lá — `AiSettingsScreen.kt` concentra 4
  composables privados no mesmo arquivo).
- Criar testes **Robolectric (`src/test`)** para `AiSettingsScreen.kt` cobrindo os
  mesmos 3 cenários hoje só cobertos por `androidTest`
  (`feature/settings/src/androidTest/.../AiSettingsScreenTest.kt:44-78`), para contar
  na variante `layout`.
- Criar testes unitários para a função de validação extraída (casos: tier BYOK + chave
  vazia, tier BYOK + chave presente, tier != BYOK).
- Adicionar testes Robolectric para telas/componentes de `layout` hoje em 0% que não
  dependem de hardware (câmera/scanner), para abrir margem de segurança acima de 80% e
  não voltar a quebrar no próximo PR: `AiHistoryScreenKt` (0%, 78 linhas,
  `feature/chat/src/main/.../screen/AiHistoryScreenKt`) e `CustomMfaContentKt` /
  `CustomReauthContentKt` (0%, 77 e 39 linhas,
  `feature/auth/src/main/.../components/auth/`).
- Investigar e propor mitigação para a instabilidade do emulador no job
  `connectedDebugAndroidTest` de `:feature:settings` (ver §9 — não bloqueante para este
  plano, pois é infraestrutura de CI, não código do app).
- Manter o teste `androidTest` existente (ele valida o comportamento real de Compose UI
  end-to-end em dispositivo/emulador; não é redundante, só não conta para o gate).

**Exclui:**
- Qualquer mudança de comportamento visível ao usuário (strings, UX, fluxo).
- Refatorar componentes de câmera/scanner (`CameraCaptureViewKt`, `CameraPreviewKt`,
  `QrCodeScannerKt`, `BarcodeAnalyzer`, `PriceTagScannerCanvasKt` — todos em 0% na
  variante `layout`): dependem de hardware real, mesma categoria dos já excluídos em
  `howmuch.kover.gradle.kts:124-129`. Ver pergunta em aberto não bloqueante (§9).
- Mudar o valor do `minBound(80)` em `howmuch.kover.gradle.kts:141,151`.
- Qualquer outro módulo fora de `feature:settings`, `feature:chat`, `feature:auth`
  (as adições fora de `feature:settings` são apenas para margem de segurança, não
  obrigatórias para o gate específico do PR #242).

## 3. Impacto Arquitetural

- Módulo: `feature:settings` (principal), `feature:chat`, `feature:auth` (margem de
  segurança).
- Camada: `presentation/` (screen + components), sem tocar `domain/`, `data/`,
  `navigation/` ou `di/`.
- Padrão aplicado: Clean Code (extração de função pura / SRP), mantendo MVI (nenhuma
  mudança em `AiSettingsIntent` ou `AiSettingsUiState`).
- Novos arquivos:
  - `feature/settings/src/main/java/br/com/brunocarvalhs/howmuch/feature/settings/presentation/components/aisettings/AiTierCard.kt`
    (move `TierCard` — renomeado para evitar colisão com nomes genéricos de outros
    arquivos de `components/`)
  - `.../presentation/components/aisettings/AiTier2Content.kt` (move `Tier2Content`)
  - `.../presentation/components/aisettings/AiTier3Content.kt` (move `Tier3Content`)
  - `.../presentation/screen/AiSettingsFormValidator.kt` (nova função pura
    `internal fun validateByokSave(tier, customApiKey): Boolean` — ou classe
    equivalente; ver §4)
  - `feature/settings/src/test/java/.../presentation/screen/AiSettingsFormValidatorTest.kt`
  - `feature/settings/src/test/java/.../presentation/screen/AiSettingsScreenTest.kt`
    (Robolectric, espelha os 3 cenários do `androidTest` homônimo)
  - `feature/chat/src/test/java/.../presentation/screen/AiHistoryScreenTest.kt`
  - `feature/auth/src/test/java/.../presentation/components/auth/CustomMfaContentTest.kt`
  - `feature/auth/src/test/java/.../presentation/components/auth/CustomReauthContentTest.kt`
- Diagrama (fluxo da validação extraída):

```
AiSettingsScreen (Composable)
   └─ trySave(onSaved) ──calls──▶ validateByokSave(tier, apiKey): Boolean
                                      (pura, sem Compose, testável em JVM puro)
         │ false (inválido)            │ true (válido)
         ▼                             ▼
   byokKeyError = true          intent.onUpdateAiSettings(...); onSaved()
```

- Sem novas dependências Hilt (a função extraída é `internal`, chamada direto pelo
  composable — não é um `UseCase` de domínio porque não há I/O nem regra de negócio de
  persistência, só validação de formulário local, que já é o padrão usado em outras
  telas de `presentation/screen/*` deste módulo).

## 4. Contratos e Interfaces

Todas as assinaturas abaixo são **`internal`** — nenhuma cruza módulo, então nenhuma
precisa ser `public`.

```kotlin
// AiSettingsFormValidator.kt — função pura, sem efeito colateral, sem Compose
internal fun validateByokSave(
    tier: AiConfigTier,
    customApiKey: String
): Boolean // true = pode salvar; false = bloquear (tier BYOK + chave em branco)

// AiTierCard.kt
@Composable
internal fun AiTierCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isProRequired: Boolean = false,
    content: @Composable (() -> Unit)? = null
)

// AiTier2Content.kt
@Composable
internal fun AiTier2Content(
    selectedModel: String,
    availableModels: List<AiModel>,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
)

// AiTier3Content.kt
@Composable
internal fun AiTier3Content(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
)
```

Nenhum modelo de domínio, repositório, `NavKey`, endpoint, coleção Firestore ou evento
de Analytics é criado ou alterado. Nenhum **BREAKING**.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Salvar com `tier == CUSTOM_BYOK` e chave em branco bloqueia o save e mostra erro | Idêntico — lógica só extraída para função pura, mesmo resultado | `AiSettingsScreen.kt:71-78` |
| 2 | Editar a chave BYOK depois do erro limpa `byokKeyError` | Idêntico, inalterado (fica no composable, é estado de UI) | `AiSettingsScreen.kt:154-157` |
| 3 | `koverVerifyLayout` exige ≥80% de linhas cobertas em `presentation.screen.*` / `presentation.components.*` | Sem mudança na regra; cobertura passa a atingir o mínimo | `howmuch.kover.gradle.kts:145-154` |

Nenhuma regra de negócio muda de comportamento — este plano é puramente estrutural
(extração) e de testes.

## 6. Critérios de Aceitação

- Performance: `validateByokSave` é O(1), sem alocação além da comparação de enum/
  string já existente; nenhuma mudança de dispatcher/coroutine (função é síncrona, como
  já era inline).
- Sem recomposição adicional: mover `Tier2Content`/`Tier3Content`/`TierCard` para
  arquivos separados não altera a árvore de composição nem os `remember`/`key` já
  existentes.
- Sem feature toggle (mudança estrutural, não comportamental).
- Observabilidade: nenhum evento de Analytics novo (fora de escopo).
- Compatibilidade: nenhuma mudança de minSdk/Wear OS/migração de dados.
- `./gradlew koverXmlReportLayout` local deve reportar ≥80% na variante `layout` após
  as mudanças (verificar antes de abrir PR, não só confiar no CI).
- `./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout` devem passar
  localmente antes do commit.

## 7. Plano de Testes

- **Unitários (JVM puro, sem Robolectric):**
  - `AiSettingsFormValidatorTest`: `tier=CUSTOM_BYOK` + chave vazia → `false`;
    `tier=CUSTOM_BYOK` + chave com espaços → `false` (usa `isBlank()`);
    `tier=CUSTOM_BYOK` + chave não vazia → `true`; `tier=DEFAULT`/`SPECIFIC` + chave
    vazia → `true` (regra só se aplica a BYOK).
- **Robolectric (`src/test`, contam para `koverVerifyLayout`):**
  - `AiSettingsScreenTest` (settings): replicar os 3 cenários de
    `feature/settings/src/androidTest/.../AiSettingsScreenTest.kt:44-78`
    (`savingWithBlankByokKeyShowsErrorAndDoesNotSave`,
    `fillingByokKeyAfterErrorAllowsSaving`,
    `savingWithNonBlankByokKeySavesImmediately`) usando `createComposeRule()` padrão
    Robolectric já usado em outros testes do módulo (ver
    `feature/settings/src/test/.../screen/SettingsSubScreensTest.kt` como referência de
    padrão existente).
  - `AiHistoryScreenTest` (chat): smoke test de renderização com lista vazia e com
    itens, cobrindo os branches principais do composable (0% atual).
  - `CustomMfaContentTest` / `CustomReauthContentTest` (auth): smoke test de
    renderização nos estados principais (loading/erro/sucesso, conforme o composable
    exigir).
- **Instrumentados / UI:** manter `AiSettingsScreenTest` (`androidTest`) como está —
  ele cobre o comportamento real em dispositivo, mas não substitui o Robolectric para
  fins de gate.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`,
  `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout` — todos obrigatórios
  antes do PR.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Robolectric não renderizar `ExposedDropdownMenuBox`/`Slider` de Material3 sem erro de ambiente | média | médio (bloqueia teste novo) | Seguir exatamente o setup de `SettingsSubScreensTest.kt` / `LegalContentScreenTest.kt`, já usados no mesmo módulo com Material3 |
| Extração dos composables privados introduzir import circular ou quebrar `@PreviewCestouScreens` existentes (`AiSettingsScreen.kt:345-372`) | baixa | baixo | Previews ficam em `AiSettingsScreen.kt`; apenas os composables movidos são importados de volta |
| Cobertura ainda abaixo de 80% mesmo após os testes de `feature:settings` (margem apertada) | média | alto (gate falha de novo) | Testes extras em `feature:chat`/`feature:auth` (§2) como margem de segurança — medir localmente antes do PR |
| Instabilidade do emulador no CI persistir mesmo com testes corrigidos | média | médio (gate de UI continua vermelho) | Fora do escopo de código; registrar como ação de infraestrutura separada (ver §9) — não bloqueia este plano, que resolve a causa de cobertura |

## 9. Perguntas em Aberto (não bloqueantes)

- [ ] A instabilidade do `connectedDebugAndroidTest` (adb daemon / ColorBuffer) é
  infraestrutura do runner de CI (`reactivecircus/android-emulator-runner`), fora do
  código do app — recomendo abrir uma ação separada (ex.: aumentar timeout de boot do
  emulador, trocar perfil de imagem do sistema, ou rodar com `-no-window -gpu swiftshader_indirect`)
  em `.github/workflows/` e `.github/PIPELINE.md`, não neste plano de cobertura.
- [ ] `CameraCaptureViewKt`, `CameraPreviewKt`, `QrCodeScannerKt`, `BarcodeAnalyzer`,
  `PriceTagScannerCanvasKt` estão em 0% na variante `layout` e dependem de hardware de
  câmera — candidatos a entrar na lista de exclusão de `howmuch.kover.gradle.kts:93-129`
  (mesma categoria dos wrappers já excluídos). Decisão de maintainer: excluir
  explicitamente ou aceitar como dívida de cobertura? Não bloqueia este plano porque não
  são tocados pela mudança do PR #242.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir — não há seção
  de arquitetura específica para `feature:settings`/telas de IA, lacuna registrada
  acima via citação direta ao código).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `breaking` em "Impacto público" — N/A, tudo
  `internal`.
- [x] Novo endpoint/remote-config: N/A, nenhum criado.
- [x] Novo cache: N/A, nenhum criado.
- [x] Perguntas em aberto exauridas — nenhuma é bloqueante; status = `ready`.
