# LESSONS

This file is automatically managed by the SDD skill. It captures project-local knowledge and verified patterns to prevent repeating past failures.

---
<!-- lessons-start -->
- **L-001**: Always use `internal` visibility for `data` and `presentation` implementation details in feature modules to enforce modularity and avoid leaks between features.
- **L-002**: Domain layer must be pure Kotlin and free of Android/Firebase dependencies to ensure maximum testability and stability.
- **L-003**: Use `FeatureInitializer` for navigation entry points to decouple `:app` from internal feature implementations.
- **L-004**: Prefer MVI with lambda-based `Intent` data classes to avoid large `when` blocks in ViewModels and improve readability.
- **L-005**: Always annotate AI-exposed UseCases with `@AiAgentAction` and register them in the feature's Hilt `AgentModule` using `@IntoSet`.
<!-- lessons-end -->
