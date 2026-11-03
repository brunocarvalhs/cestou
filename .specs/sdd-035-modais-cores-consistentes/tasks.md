# Task: Unificar o fundo do drag handle com o conteúdo dos modais (bottom sheets)

- **Slug:** modais-cores-consistentes
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.2.2 (patch, fix visual)
- **Tipo de mudança:** fix
- **Impacto público (módulo):** additive (novas funções públicas em `core:ui`, ver §4)

## 1. Contexto e Motivação
- Pedido do usuário: "os modais estão com cores diferentes", esclarecido em seguida: "me refiro a cor do Drag handle, o fundo dele está diferente".
- O drag handle é desenhado pelo `ModalBottomSheet` sobre o **container do sheet**. O conteúdo logo abaixo pinta outro fundo por cima. A faixa do handle fica numa cor e o corpo em outra, e o corte aparece como uma "tarja" no topo.
- Causa raiz (1): o `ColorScheme` não define tokens `surfaceContainer*` (`core/theme/.../Theme.kt:13-57`). O container padrão do `ModalBottomSheet` é `surfaceContainerLow`, o baseline roxo-acinzentado do M3 (light `#F7F2FA`, dark `#1D1B20`).
- Causa raiz (2): conteúdos com `Scaffold` próprio. O `containerColor` padrão do `Scaffold` é `colorScheme.background` (light `#F9FAFB`, dark `#030712`, `Theme.kt:36,56`), diferente do container do sheet.
  - `feature/products/.../ProductScreen.kt:58`: `Scaffold` dentro de `ModalBottomSheet` (`ProductsGraph.kt:25`).
  - `feature/chat/.../AiChatScreen.kt:178`: `Scaffold` dentro de `ModalBottomSheet` (`:162`); só o `TopAppBar` é transparente (`:182`).
- Sheets sem `Scaffold` (Notificações, Tema, Dados, Editar lista, Entrar na lista, Cart, Feedback, Compartilhar) têm fundo uniforme, mas usam o roxo do baseline. Os que passam `containerColor = surface` (`QrCodeBottomSheet.kt:69`, `ShoppingScreen.kt:185`, `WelcomeScreen.kt:162,235`) ficam brancos. O resultado é um sheet com três fundos entre telas, além da tarja interna do Chat e do Products.
- Intencional, fora de escopo: Scanner com `Color.Black` e handle branco (`ShoppingGraph.kt:186-189`).
- Nota: o botão em `WelcomeScreen.kt:355` usa `surfaceContainerLow` e será afetado pela mudança de tokens (ver §8).

## 2. Escopo
- **Inclui:**
  - Definir os tokens `surfaceContainer*` nos dois `ColorScheme` de `core:theme`, com a paleta da marca.
  - Criar `CestouModalBottomSheet` e `CestouModalDefaults` em `core:ui` com uma única cor de container.
  - Fazer os conteúdos com `Scaffold` (Chat, Products) usarem `containerColor = Color.Transparent`. Assim o handle e o corpo compartilham o fundo do sheet, sem tarja.
  - Migrar todos os `ModalBottomSheet` de `feature/**` e `core/ui/**` para o wrapper. Remover overrides locais de `containerColor = surface`.
  - Alinhar o `AlertDialog` (`DataSettingsScreen.kt:120`) e o `CategoryPickerDialog` (`CategoryPickerDialog.kt:65`) à mesma cor.
- **Exclui:**
  - Scanner de câmera (fundo preto intencional).
  - Redesenho de conteúdo e espaçamento; comportamento de gestos do chat.
  - Módulo `:wear` (tema próprio, `wear/.../theme/Theme.kt:34`).
  - Dynamic color (desligado, `Theme.kt:67`).

## 3. Impacto Arquitetural
- Módulos: `:core:theme` (tokens), `:core:ui` (wrapper), `:feature:settings|shopping|products|cart|feedback|chat|auth` (apenas `presentation`/`navigation`).
- Padrão: design system central em `core:*` consumido pelas features, sem dependência feature→feature (AGENTS.md §2).
- Novos arquivos:
  - `core/ui/src/main/java/br/com/brunocarvalhs/howmuch/core/ui/components/CestouModalBottomSheet.kt`
  - `core/ui/src/test/.../components/ModalDefaultsTest.kt`
  - `core/theme/src/test/.../ThemeSurfaceTokensTest.kt` (o módulo ainda não tem `src/test`)

```
 core:theme  Theme.kt ── surfaceContainer* (light/dark)
      │
      ▼
 core:ui     CestouModalBottomSheet ─► ModalBottomSheet(containerColor = CestouModalDefaults.containerColor)
             CestouModalDefaults.containerColor == token único
      │
      ▼
 feature:*   sheet ──[handle]── mesma cor ──[conteúdo Scaffold transparente]── mesma cor
```
- DI: nenhuma (apenas Composables).

## 4. Contratos e Interfaces
Assinaturas (sem implementação):

```kotlin
// core:ui, public: consumido por 7 módulos de feature (não pode ser internal)
@Composable
public fun CestouModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = CestouModalDefaults.containerColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetGesturesEnabled: Boolean = true,
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable ColumnScope.() -> Unit,
)

// core:ui, public
public object CestouModalDefaults {
    public val containerColor: Color @Composable get()       // colorScheme.surfaceContainerLow (token único)
    public val dialogContainerColor: Color @Composable get()
    public val sheetShape: Shape                              // topo 24dp, unifica o shape
}
```
- Sem `NavKey`, repositório, Firestore, remote-config ou endpoint novo. Sem breaking change.
- Analytics: nenhum evento novo.
- Q1 (não bloqueante): não criar wrapper de `AlertDialog` (só 1 uso); usar `dialogContainerColor` direto.

## 5. Regras de Negócio Aplicáveis
Não há regra de negócio; apenas regras visuais.

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Handle desenhado em `surfaceContainerLow` roxo; corpo do Chat/Products em `background` | Corpo transparente: handle e corpo exibem a mesma cor do sheet | `AiChatScreen.kt:178`, `ProductScreen.kt:58` |
| 2 | Sheets sem `containerColor` usam o roxo do baseline | Todos usam o token único da marca | `Theme.kt:13-57`; lista em §1 |
| 3 | Sheets com `containerColor = surface` ficam brancos | Mesmo token dos demais (sem override local) | `QrCodeBottomSheet.kt:69`, `ShoppingScreen.kt:185`, `WelcomeScreen.kt:162,235` |
| 4 | `shape` varia (28dp padrão M3 vs 24dp explícito) | `sheetShape` 24dp única | `ShareOptionsBottomSheet.kt:40`, `QrCodeBottomSheet.kt:68` |
| 5 | Scanner `Color.Black` | Mantido | `ShoppingGraph.kt:188` |
| 6 | `AlertDialog` e `CategoryPickerDialog` usam cores diferentes | Mesmo `dialogContainerColor` | `DataSettingsScreen.kt:120`, `CategoryPickerDialog.kt:65` |

Decisão de cor (recomendação): `surfaceContainerLow = surface` (branco / `#111827`), igual às telas, e os demais `surfaceContainer*` em neutros cinza já presentes na paleta (`#F3F4F6`/`#E5E7EB` no claro, `#1F2937`/`#374151` no escuro). Se o maintainer preferir o cinza leve do M3, só o valor do token muda.

## 6. Critérios de Aceitação
- Em todos os sheets, o fundo do handle e o do corpo são idênticos (sem tarja) em light e dark. Chat e Products são os casos de verificação principal.
- Nenhum tom roxo em sheets ou dialogs.
- Nenhum `ModalBottomSheet(` direto em `feature/**` e `core/ui/**`, exceto dentro do wrapper e o Scanner (que passa `Color.Black` pelo wrapper).
- Chat mantém `sheetGesturesEnabled`, `confirmValueChange`, `contentWindowInsets` e o handle condicional (`AiChatScreen.kt:145-170`), inalterados.
- Snackbar e `topBar` do `ProductScreen` continuam legíveis sobre o fundo do sheet.
- Performance: o default lê `MaterialTheme.colorScheme` (sem alocação extra por recomposição); `sheetShape` é constante; sem I/O ou coroutine nova.
- Feature toggles: não aplicável (mudança visual pura).
- Observabilidade: sem mudanças.
- Compatibilidade: minSdk 28; tokens `surfaceContainer*` já usados em `WelcomeScreen.kt:355`; Wear não afetado; sem migração de dados.
- Sem strings novas; sem `@Suppress`.
- `CHANGELOG.md` com a entrada de fix (AGENTS.md §3, §6.5).

## 7. Plano de Testes
- **Unitários:**
  - `ThemeSurfaceTokensTest`: os dois schemes definem `surfaceContainerLow/High` diferentes do baseline (`#F7F2FA`, `#ECE6F0`, `#1D1B20`, `#2B2930`). Expor os schemes como `internal` para teste.
  - `ModalDefaultsTest`: `sheetShape` com raio 24dp no topo.
- **Instrumentados / UI:** em `core:ui`, abrir `CestouModalBottomSheet` com conteúdo em `Scaffold(containerColor = Transparent)` sob `CestouTheme(light|dark)` e verificar por `testTag` que o conteúdo aparece. A comparação de pixels do handle vs corpo (screenshot) fica como Q2, não bloqueante: o projeto não tem screenshot testing.
- **Validação manual (Maestro com `testTag`):** Chat, Products (adicionar produto), Cart (4 sheets), Editar lista, Entrar na lista, Notificações, Tema, Dados, Compartilhar, Feedback e Welcome, em light e dark.
- **Comandos:** `./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Definir `surfaceContainer*` altera outros componentes que os usam por padrão (NavigationBar, pickers, `WelcomeScreen.kt:355`) | média | visual fora do escopo | revisar essas telas; manter valores próximos aos atuais onde houver override |
| `Scaffold` transparente expõe o fundo do sheet atrás de snackbar ou conteúdo sobreposto | baixa | legibilidade | validar o snackbar do `ProductScreen` e o `bottomBar` do chat em light e dark |
| Chat tem gesto, inset e altura animada especiais (`AiChatScreen.kt:140-175`) | média | regressão de UX | o wrapper repassa todos os parâmetros; teste manual do chat |
| `core:theme` sem `src/test` | média | esforço extra | adicionar `testImplementation` pelo convention plugin usado nos outros `core:*` |
| Cobertura Kover 85% | baixa | falha de pipeline | testes dos defaults; Composables seguem as exclusões atuais (`buildSrc/.../howmuch.kover.gradle.kts`), confirmar |
| Branch atual tem alterações em andamento (`ChatGraph.kt`) | média | conflito | implementar em branch própria a partir de `develop` |

## 9. Perguntas em Aberto (bloqueantes)
Nenhuma bloqueante. Não bloqueantes (com a recomendação adotada por padrão):
- [ ] Q1: aceitar a recomendação de não criar wrapper de `AlertDialog` (§4)?
- [ ] Q2: sheets brancos (= `surface`, recomendado) ou cinza leve do M3? Afeta só o valor do token (§5).

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (`architecture/` não cobre tema/UI; lacuna: não há documento de design system).
- [x] Nenhum código de produção escrito (apenas assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Símbolos públicos novos em `core:ui` refletidos como `additive` e justificados (consumo cross-módulo).
- [x] Sem novo endpoint/remote-config: toggle não aplicável.
- [x] Sem novo cache.
- [x] Perguntas bloqueantes exauridas.
