# LESSONS

This file is automatically managed by the SDD skill. It captures project-local knowledge and verified patterns to prevent repeating past failures.

---
<!-- lessons-start -->
- **L-001**: Always use `internal` visibility for `data` and `presentation` implementation details in feature modules to enforce modularity and avoid leaks between features.
- **L-002**: Domain layer must be pure Kotlin and free of Android/Firebase dependencies to ensure maximum testability and stability.
- **L-003**: Use `FeatureInitializer` for navigation entry points to decouple `:app` from internal feature implementations.
<!-- lessons-end -->
