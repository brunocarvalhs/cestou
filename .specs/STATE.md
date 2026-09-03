# STATE

## Decisions

### AD-001
- **Decision**: Multi-module Clean Architecture.
- **Reason**: Separation of concerns, scalability, and independent feature development.
- **Trade-off**: Increased boilerplate for module configuration.
- **Scope**: Entire project.
- **Date**: 2026-08-20
- **Status**: active

### AD-002
- **Decision**: Jetpack Compose for all UI components.
- **Reason**: Modern, declarative UI framework with better productivity and Material 3 support.
- **Trade-off**: Requires modern Android tooling and differs from legacy XML patterns.
- **Scope**: :app, :feature modules.
- **Date**: 2026-08-20
- **Status**: active

### AD-003
- **Decision**: Hilt for Dependency Injection.
- **Reason**: Standard Android DI solution, simplifies boilerplate for ViewModel and Repository injection.
- **Trade-off**: Compile-time overhead and less flexibility than raw Dagger.
- **Scope**: Entire project.
- **Date**: 2026-08-20
- **Status**: active

### AD-004
- **Decision**: MVI (Model-View-Intent) presentation pattern with Data Class Intent.
- **Reason**: Decouples UI from business logic using a unidirectional data flow. Data class intents (lambdas) in the ViewModel simplify communication and state management in Compose.
- **Trade-off**: Requires boilerplate for State and Intent classes for simple screens.
- **Scope**: :app, :feature modules.
- **Date**: 2026-08-24
- **Status**: active

### AD-005
- **Decision**: Modular Feature Anatomy with Internal Visibility.
- **Reason**: Enforce strict isolation and standardization. Features follow a flat structure: `data/`, `di/`, `domain/`, `presentation/`, and `navigation/`. All implementation details MUST be `internal`.
- **Trade-off**: Requires careful management of public API surface (usually just the Navigation entry point).
- **Scope**: All feature modules.
- **Date**: 2026-08-24
- **Status**: active

### AD-006
- **Decision**: Navigation 3 with NavKey (Kotlinx Serialization).
- **Reason**: Type-safe navigation, multi-backstack support, and better integration with Compose and ViewModels.
- **Trade-off**: Requires serialization boilerplate for all routes.
- **Scope**: Entire project.
- **Date**: 2026-08-24
- **Status**: active

### AD-007
- **Decision**: AI Integration via AgentActionUseCase.
- **Reason**: Expose app workflows to the internal AI agent in a structured way, enabling voice commands and system shortcuts.
- **Trade-off**: Requires registering use cases in Hilt modules with specific annotations.
- **Scope**: Core and Feature modules providing user-executable actions.
- **Date**: 2026-08-24
- **Status**: active

## Handoff

- **Feature**: .specs/features/sdd-initialization
- **Phase / Task**: Execution / Completed
- **Completed**: AD-005 documented, Core and Feature modules refactored, FeatureInitializers implemented, Reverse specs created.
- **In-progress**: Final verification
- **Next step**: Run full build and tests
- **Blockers**: none
- **Uncommitted files**: All files modified during refactor
- **Branch**: main
