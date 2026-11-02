# Task: Criar barra de busca na tela principal com ícone de IA e acesso global ao Chat de IA

- **Slug:** sdd-015-search-bar-ai-chat
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2025-02-23
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação
- A tela principal de listas (`ShoppingScreen`) atualmente não exibe uma barra de busca visual para o usuário pesquisar suas listas de compras diretamente, embora o `ShoppingListViewModel` já possua lógica de filtragem via `searchQuery` e `applyFilters()`.
- O usuário precisa de um ponto de entrada direto e acessível de IA na barra de busca (sufixo com ícone de IA) na tela principal e de acesso ao Chat de IA a partir de qualquer parte principal do aplicativo através da barra de navegação inferior (`CestouBottomNavigation`).
- Referência ao pedido: solicitação direta do usuário em chat ("crie um search bar na tela principal, onde os usuários podem pesquisar suas listas, e quero que o suffix seja um icone de IA, a ideia e criar um chat de IA acessivel em qualquer parte do app").
- Rastreabilidade:
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/screen/ShoppingScreen.kt:70-136`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/intent/ShoppingListIntent.kt:5-23`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/state/ShoppingListUiState.kt:12`
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/viewmodel/ShoppingListViewModel.kt:68-71,235-256`
  - `core/ui/src/main/java/br/com/brunocarvalhs/howmuch/core/ui/components/CestouBottomNavigation.kt:31-61`
  - `app/src/main/java/br/com/brunocarvalhs/howmuch/MainActivity.kt:164`

## 2. Escopo
- **Inclui:**
  - Criação do componente Composable `ShoppingSearchBar` em `:feature:shopping` com campo de texto para busca, botão de limpar busca (quando houver texto) e ícone sufixo de IA (`Icons.Default.AutoAwesome`).
  - Integração do `ShoppingSearchBar` no topo do conteúdo da `ShoppingScreen` (acima do grid de listas).
  - Adição da ação `onOpenAiChat: () -> Unit` no `ShoppingListIntent` e seu tratamento no `ShoppingListViewModel` para navegar até `AiChat(shoppingId = "")`.
  - Inclusão do item `BottomNavItem.AiChat` no `CestouBottomNavigation` (`:core:ui`), utilizando o ícone de IA (`Icons.Filled.AutoAwesome` / `Icons.Outlined.AutoAwesome`) e a string existente `nav_ai_chat`, tornando o Chat de IA acessível no menu inferior global.
  - Atualização e adição de testes unitários em `:feature:shopping` e `:core:ui`.
- **Exclui:**
  - Alterações no comportamento interno de geração de resposta da IA em `AiChatViewModel` ou `CartAssistantUseCase`.
  - Mudanças na interface ou navegação do aplicativo Wear OS (`:wear`).

## 3. Impacto Arquitetural
- **Módulos e camadas afetados:**
  - `:feature:shopping`: `presentation/components/shopping/` (novo componente), `presentation/intent/` (novo callback de intent), `presentation/screen/` (integração da UI) e `presentation/viewmodel/` (manipulação do evento de navegação para IA).
  - `:core:ui`: `components/CestouBottomNavigation.kt` (adicionar `BottomNavItem.AiChat` na barra de navegação).
  - `:app`: `MainActivity.kt` (já possui `AiChat(shoppingId = "")` na lista `rootRoutes`, sem alterações estruturais necessárias além de confirmar alinhamento).
- **Padrão aplicado:** Clean Architecture + MVI em `:feature:shopping`, desacoplado via contratos de navegabilidade do `Navigator` em `:core:navigation`.
- **Novos arquivos:**
  - `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/presentation/components/shopping/ShoppingSearchBar.kt`
- **Diagrama ASCII do fluxo:**

```
[ Usuário na ShoppingScreen ]
            │
            ├──── Digita texto ───────► intent.onQueryChange(text) ───► ShoppingListViewModel.applyFilters() ───► ShoppingListUiState.filteredList
            │
            ├──── Clica no ícone IA ──► intent.onOpenAiChat() ────────► Navigator.navigate(AiChat(shoppingId = "")) ──► AiChatScreen
            │
[ CestouBottomNavigation (Barra Inferior) ]
            │
            └──── Clica em "Chat IA" ─► onNavigate(AiChat("")) ────────► Navigator.navigate(AiChat(shoppingId = "")) ──► AiChatScreen
```

## 4. Contratos e Interfaces

### 1. `ShoppingListIntent` (`feature/shopping/.../presentation/intent/ShoppingListIntent.kt`)
```kotlin
internal data class ShoppingListIntent(
    val onFetchAll: () -> Unit = {},
    val onCreate: () -> Unit = {},
    val onOpen: (String) -> Unit = {},
    val onFilter: (ShoppingFilter) -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onOpenAiChat: () -> Unit = {}, // NOVO
    val onToggleFavorite: (Shopping) -> Unit = {},
    val onDuplicate: (Shopping) -> Unit = {},
    val onShare: (Shopping) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onEdit: (Shopping) -> Unit = {},
    val onUpdate: (Shopping) -> Unit = {},
    val onReopen: (Shopping) -> Unit = {},
    val onShowJoinDialog: () -> Unit = {},
    val onMove: (Int, Int) -> Unit = { _, _ -> },
    val onShowCreateSheet: (Boolean) -> Unit = {},
    val onCreateConfirmed: (String, String, String) -> Unit = { _, _, _ -> }
)
```
- **Visibilidade:** `internal` (restrito ao módulo `:feature:shopping`).

### 2. `BottomNavItem` (`core/ui/.../components/CestouBottomNavigation.kt`)
```kotlin
sealed class BottomNavItem(
    val route: Any,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Lists : BottomNavItem(
        route = ShoppingList,
        titleRes = R.string.nav_lists,
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart
    )

    object AiChat : BottomNavItem( // NOVO
        route = br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat(shoppingId = ""),
        titleRes = R.string.nav_ai_chat,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    )

    object ProfileItem : BottomNavItem(
        Profile,
        R.string.nav_profile,
        Icons.Default.AccountCircle,
        Icons.Outlined.AccountCircle
    )
}
```
- **Visibilidade:** `public` (extensão aditiva de classe selada pública em `:core:ui`).
- **Justificativa do `public`:** `BottomNavItem` é consumido pela `MainActivity` no módulo `:app`.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | A filtragem por query em `ShoppingListViewModel` existe via `applyFilters()`, mas não há campo de busca na UI de `ShoppingScreen`. | `ShoppingScreen` exibe `ShoppingSearchBar`. Alterações no campo acionam `intent.onQueryChange`, filtrando dinamicamente por título e descrição. | `feature/shopping/.../presentation/viewmodel/ShoppingListViewModel.kt:235-256` |
| 2 | O Chat de IA só pode ser aberto a partir de uma lista individual no carrinho (`CartScreen`). | O Chat de IA pode ser aberto diretamente a partir do botão sufixo com ícone de IA na barra de busca da `ShoppingScreen` e da barra de navegação inferior em qualquer aba principal. | `feature/cart/.../presentation/screen/CartScreen.kt:309`, `core/ui/.../components/CestouBottomNavigation.kt:58-61` |
| 3 | A rota `AiChat` recebe `shoppingId`. Quando vinda da tela inicial ou bottom bar, `shoppingId` é `""`. | `AiChatViewModel.setShoppingContext("")` carrega/salva o histórico geral sem associar a uma lista de compras específica. | `feature/chat/.../presentation/viewmodel/AiChatViewModel.kt:51-61` |

## 6. Critérios de Aceitação
- **Performance:**
  - O campo de busca opera sobre `StateFlow<ShoppingListUiState>` e não gera alocações excessivas ou recomposições desnecessárias do grid de listas.
  - As listas filtradas continuam encapsuladas em `StableList` para otimização de recomposição no Compose.
- **UI / UX:**
  - O `ShoppingSearchBar` utiliza estilo alinhado ao Design System Material 3 (bordas arredondadas, suporte a tema claro/escuro, ícone de busca à esquerda, botão de limpar busca e ícone sufixo de IA à direita).
  - O botão de IA possui `contentDescription` acessível via `R.string.content_description_ai_icon`.
  - A barra de navegação inferior (`CestouBottomNavigation`) passa a exibir 3 abas: "Listas", "IA Chat" e "Perfil".
- **Observabilidade:**
  - A navegação para o Chat de IA registra a visualização de tela padrão (`ai_chat`) em `AiChatViewModel`.
- **Compatibilidade:**
  - Totalmente compatível com `minSdk 28`, Compose Material 3 e Navigation 3.

## 7. Plano de Testes
- **Unitários:**
  - `ShoppingListViewModelTest`:
    - Validar que a invocação de `intent.onOpenAiChat` navega para `AiChat(shoppingId = "")` via `Navigator`.
    - Validar que `intent.onQueryChange("termo")` atualiza `searchQuery` no `uiState` e filtra a lista corretamente.
  - `CestouBottomNavigationTest` (ou teste de componente equivalente em `:core:ui`):
    - Validar que a lista de `BottomNavItem` contém os 3 itens (`Lists`, `AiChat`, `ProfileItem`).
- **UI / Previews:**
  - Adicionar `@Preview` para `ShoppingSearchBar` com estado vazio e preenchido.
  - Atualizar previews de `ShoppingScreen` para incluir visualmente a barra de busca.
- **Comandos de verificação:**
  - `./gradlew lintDebug`
  - `./gradlew detekt`
  - `./gradlew testDebug`
  - `./gradlew koverVerify koverVerifyLayout`

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Aumento da altura do topo da `ShoppingScreen` empurrar itens para fora da tela inicial | Baixa | Baixo | Integrar o `ShoppingSearchBar` no topo do layout com padding adequado (16.dp), garantindo scroll suave do grid. |
| Incompatibilidade visual na barra de navegação inferior ao adicionar o 3º item | Baixa | Baixo | Testar com diferentes tamanhos de tela e verificar renderização do `CestouBottomNavigation`. |

## 9. Perguntas em Aberto (bloqueantes)
*Nenhuma pergunta em aberto bloqueante.*

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` em "Impacto público".
- [x] Novo endpoint/remote-config: N/A.
- [x] Novo cache: N/A.
- [x] Perguntas em aberto exauridas.
