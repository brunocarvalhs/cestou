# Fix FirebaseAuth restricted to administrators only Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: Skip (Straightforward resilience fix)
**Status**: Draft

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: none - strong defaults applied.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Service | unit | All branches; 1:1 to spec ACs; all listed edge cases | `core/auth/src/test/java/**/*.kt` | `./gradlew :core:auth:test` |
| Repository | unit | Error handling paths | `feature/shopping/src/test/java/**/*.kt` | `./gradlew :feature:shopping:test` |
| ViewModel | unit | Error state propagation | `feature/shopping/src/test/java/**/*.kt` | `./gradlew :feature:shopping:test` |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | After tasks with unit tests only | `./gradlew test` |
| Full | After tasks with integration tests | `./gradlew connectedAndroidTest` |
| Build | After phase completion | `./gradlew assembleDebug` |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Authentication Resilience

Handle Firebase failures gracefully in the auth service.

```
T1 → T2
```

### Phase 2: Data & UI Resilience

Propagate errors from repository to the UI.

```
T2 → T3 → T4
```

---

## Task Breakdown

### T1: Handle signInAnonymously failure

**What**: Wrap `signInAnonymously` in a try-catch to avoid throwing, ensuring it returns a `Result.failure`.
**Where**: `core/auth/src/main/java/br/com/brunocarvalhs/howmuch/core/auth/FirebaseAnonymousAuthentication.kt`
**Depends on**: None
**Requirement**: AUTH-01

**Done when**:
- [ ] `signInAnonymously` returns `Result.failure` on Firebase error instead of crashing.
- [ ] Log message includes the exception details.
- [ ] Unit tests pass: `./gradlew :core:auth:test`

**Tests**: unit
**Gate**: quick

---

### T2: Implement Guest Fallback in getOrCreateUserId

**What**: Update `getOrCreateUserId` to return a guest user if `signInAnonymously` fails.
**Where**: `core/auth/src/main/java/br/com/brunocarvalhs/howmuch/core/auth/FirebaseAnonymousAuthentication.kt`
**Depends on**: T1
**Requirement**: AUTH-02

**Done when**:
- [ ] `getOrCreateUserId` returns a user with ID "guest" on auth failure.
- [ ] Unit tests in `FirebaseAnonymousAuthenticationTest` cover the fallback scenario.

**Tests**: unit
**Gate**: quick

---

### T3: Handle Auth Errors in ShoppingRepository

**What**: Update `observeAll` and `getAll` to handle potential exceptions from auth service.
**Where**: `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/app/data/repository/ShoppingRepositoryImpl.kt`
**Depends on**: T2
**Requirement**: AUTH-01, AUTH-02

**Done when**:
- [ ] `observeAll` flow doesn't crash if `getOrCreateUserId` throws (though it shouldn't after T2, it's good to be safe).
- [ ] Repository returns empty list or propagates failure correctly.

**Tests**: unit
**Gate**: quick

---

### T4: Update ShoppingListViewModel to show Error UI

**What**: Catch repository/auth failures in `observeData` and update `uiState.error`.
**Where**: `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/app/presentation/viewmodel/ShoppingListViewModel.kt`
**Depends on**: T3
**Requirement**: AUTH-03

**Done when**:
- [ ] `uiState.error` is set when data loading fails due to auth.
- [ ] Unit tests in `ShoppingListViewModelTest` cover error state propagation.

**Tests**: unit
**Gate**: quick

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Handle signInAnonymously failure | 1 function | ✅ Granular |
| T2: Guest Fallback | 1 function | ✅ Granular |
| T3: Repository Error Handling | 2 methods | ✅ Granular |
| T4: ViewModel Error UI | 1 viewmodel | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | None | ✅ Match |
| T2 | T1 | T1 -> T2 | ✅ Match |
| T3 | T2 | T2 -> T3 | ✅ Match |
| T4 | T3 | T3 -> T4 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Service | unit | unit | ✅ OK |
| T2 | Service | unit | unit | ✅ OK |
| T3 | Repository | unit | unit | ✅ OK |
| T4 | ViewModel | unit | unit | ✅ OK |
