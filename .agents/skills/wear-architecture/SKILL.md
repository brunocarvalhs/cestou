---
name: wear-architecture
description: Expert guidance for modularizing and implementing Wear OS features in the How-Much (Cestou) project. Follows Clean Architecture, MVI, and strict internal visibility.
---

# Wear OS Architecture Skill

Use this skill to create, refactor, or modularize features for the Wear OS version of the project. It extends the project's core architecture principles to the wearable form factor.

## 1. Core Principles

- **Modular Features**: Every feature should be its own module (e.g., `:feature:shopping-wear`).
- **Internal Visibility**: Enforce encapsulation by using `internal` for everything except the navigation entry point.
- **Clean Architecture**: Standard `domain`, `data`, and `presentation` layers.
- **MVI with Intent**: Use `UiState` and `Intent` (data class of lambdas) for predictable state management.
- **Material 3 for Wear**: Strictly follow `wear-compose-m3` guidelines (AppScaffold, ScreenScaffold, TransformingLazyColumn).

## 2. Standard Directory Structure

Features in Wear OS must follow this layout:

- **`domain/`**: Pure logic.
  - `entity/`, `repository/`, `usecase/`.
- **`data/`**: Implementation details.
  - `repository/`, `model/`, `mapper/`.
- **`presentation/`**: UI Layer.
  - `state/`: `internal data class FeatureUiState`.
  - `intent/`: `internal data class FeatureIntent(val onAction: () -> Unit = {})`.
  - `viewmodel/`: `internal class FeatureViewModel`.
  - `screen/`: Wear Compose screens.
  - `components/`: Wear-specific reusable items.
- **`navigation/`**: Navigation 3 integration.
  - `NavKey` definitions and `NavGraphBuilder` extensions.
- **`di/`**: Internal Hilt modules.

## 3. Best Practices for Wear OS

### Scaffold Usage
Always use `AppScaffold` as the root and `ScreenScaffold` for each screen. Ensure `scrollState` is correctly passed to `ScreenScaffold` to support scroll indicators and the time text.

### Scaling & Morphing
Use `TransformingLazyColumn` for all list-based screens. Leverage `transformedHeight` and `SurfaceTransformation` to ensure proper Material 3 morphing behavior as defined in the `wear-compose-m3` skill.

### Shared Logic
Reuse domain entities and repositories from the core modules whenever possible to ensure consistency between the mobile and wearable apps.
