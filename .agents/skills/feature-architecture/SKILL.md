---
name: feature-architecture
description: Instruções para criar ou refatorar features seguindo a arquitetura modular do projeto Cestou (Clean Arch + MVI + Nav3 + Visibility Internal).
---

# Feature Architecture Skill

Use esta skill para estruturar novas features ou refatorar as existentes no projeto Cestou. O objetivo é manter a modularidade, o desacoplamento e o máximo de encapsulamento possível.

## 1. Visibilidade e Encapsulamento
> [!IMPORTANT]
> A regra de ouro é **INTERNALIZAR TUDO**. Apenas o que for estritamente necessário para outros módulos (ou para o `main` app interagir com a feature) deve ser `public`.
> - Classes, funções e propriedades dentro da feature devem usar o modificador `internal` por padrão.
> - O ponto de entrada da navegação da feature deve ser a principal interface pública.

## 2. Estrutura de Diretórios Obrigatória

Para cada `{feature}`, a estrutura deve seguir rigorosamente:

- **`data/`**
  - `repository/`: Implementações `internal` das interfaces de domínio.
  - `mapper/`: Extensões de mapeamento entre modelos de dados e entidades.
  - `model/`: DTOs ou entidades de banco de dados (ex: `ShoppingRoomEntity`).
  - `services/`: Interfaces de API (Retrofit) ou DAOs (Room).
- **`di/`**
  - Módulos Hilt `internal`.
- **`domain/`**
  - `entity/`: Entidades de negócio puro.
  - `repository/`: Interfaces que definem o contrato de dados.
  - `usecase/`: Casos de uso específicos (Single Responsibility).
- **`presentation/`**
  - `state/`: Data class `internal` para o estado da tela (`UiState`).
  - `intent/`: Data class `internal` de lambdas para ações do usuário.
  - `screen/`: Composables `internal`.
  - `viewmodel/`: `internal` ViewModels que expõem estado e intent.
  - `components/`: Componentes Compose específicos da feature.
- **`navigation/`**
  - Definições de rotas (`NavKey`).
  - Grafo da feature (`NavGraphBuilder.featureGraph`).

## 6. ViewModels Granulares (Um por Destino)
> [!IMPORTANT]
> Todo destino registrado no `NavGraphBuilder` (através de `composable`, `bottomSheet` ou `dialog`) deve ter seu próprio conjunto de `ViewModel`, `Intent` e `UiState`.
> - Evite reaproveitar o mesmo ViewModel em múltiplos destinos para garantir a independência de estado.
> - O ciclo de vida do ViewModel deve ser atrelado ao `BackStackEntry` do destino específico.

## 7. Padrão MVI com Data Class Intent

Em vez de um `onEvent` com `when`, utilize uma `data class` de lambdas no `ViewModel`.

### ViewModel Exemplo
```kotlin
@HiltViewModel
internal class MyFeatureViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState = _uiState.asStateFlow()

    // Intent mapeada diretamente para funções privadas do ViewModel
    val intent = MyIntent(
        onRefresh = { fetchItems() },
        onDelete = { id -> deleteItem(id) }
    )

    private fun fetchItems() { /* ... */ }
    private fun deleteItem(id: String) { /* ... */ }
}
```

### Intent Exemplo
```kotlin
internal data class MyIntent(
    val onRefresh: () -> Unit = {},
    val onDelete: (String) -> Unit = {}
)
```

## 4. Integração com IA (AgentActionUseCase)

Todo Usecase que representa uma ação que a IA pode executar deve:
1. Estender `AgentActionUseCase<T>`.
2. Ser anotado com `@AiAgentAction(id = "...", description = "...")`.
3. Ser registrado no módulo Hilt da feature (`AgentModule`) usando `@IntoSet`.

## 5. Navegação Modular (Navigation 3)

- Use `NavKey` (@Serializable) para rotas.
- O grafo da feature deve ser uma extensão de `NavGraphBuilder`.
- **Bottom Sheets e Dialogs**: Devem ser registrados no grafo usando `bottomSheet<Route>` ou `dialog<Route>` em vez de estados booleanos manuais na tela, garantindo que o `Navigator` controle o fluxo.

```kotlin
fun NavGraphBuilder.myFeatureGraph(navigator: Navigator) {
    composable<MyRoute> {
        val viewModel: MyViewModel = hiltViewModel()
        MyScreen(state = viewModel.uiState.collectAsState().value, intent = viewModel.intent)
    }
    
    bottomSheet<MyModalRoute> {
        MyModalScreen(...)
    }
}
```
