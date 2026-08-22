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
- **Decision**: MVVM/MVI presentation pattern.
- **Reason**: Decouples UI from business logic; facilitates state management in Compose.
- **Trade-off**: Complexity in managing ViewState objects for very simple screens.
- **Scope**: :app, :feature presentation layers.
- **Date**: 2026-08-20
- **Status**: active

### AD-005
- **Decision**: Domain-Centric Multi-module Anatomy.
- **Reason**: Enforce strict isolation of business logic and standardization of feature module structures as per `architecture-and-layers` spec.
- **Trade-off**: Requires significant refactoring of existing modules to follow the new directory and package taxonomy.
- **Scope**: All feature modules and core modules.
- **Date**: 2026-08-21
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
