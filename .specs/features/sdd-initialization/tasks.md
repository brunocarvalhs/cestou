# SDD Infrastructure & Project Mapping Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

---

**Design**: `.specs/features/sdd-initialization/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from codebase sampling. Guidelines found: none - strong defaults applied.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Domain / Use Case | unit | 1:1 to spec ACs | `feature/*/src/test/**/usecase/*Test.kt` | `./gradlew test` |
| Presentation / ViewModel | unit | Logic coverage | `feature/*/src/test/**/viewmodel/*Test.kt` | `./gradlew test` |
| Data / Mapper | unit | Mapping logic | `feature/*/src/test/**/mapper/*Test.kt` | `./gradlew test` |
| Documentation | none | - (build gate only) | - | - |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After doc tasks | none |
| Build | After completion | `./gradlew lint` |

---

## Execution Plan

### Phase 1: SDD Infrastructure & Core Features

```
T1a → T1b → T2 → T3 → T4 → T5
```

### Phase 2: Documentation Refresh

```
T6 → T7
```

---

## Task Breakdown

### T1a: Finalize STATE.md

**What**: Initialize `STATE.md` with current project context.
**Where**: `.specs/STATE.md`
**Depends on**: None
**Requirement**: SDD-01

**Tools**:
- MCP: `filesystem`

**Done when**:
- [ ] `STATE.md` contains the 4 initial decisions (AD-001 to AD-004).
- [ ] `spec.md` for `sdd-initialization` is fully marked.

**Tests**: none
**Gate**: Build

---

### T1b: Initialize LESSONS.md

**What**: Initialize `LESSONS.md` with managed header.
**Where**: `.specs/LESSONS.md`
**Depends on**: T1a
**Requirement**: SDD-01

**Tools**:
- MCP: `filesystem`

**Done when**:
- [ ] `LESSONS.md` contains the managed header.

**Tests**: none
**Gate**: Build

---

### T2: Reverse Engineer Shopping Feature

**What**: Create `spec.md` for the Shopping feature.
**Where**: `.specs/features/shopping/spec.md`
**Depends on**: T1b
**Requirement**: SDD-02

**Tools**:
- MCP: `filesystem`

**Done when**:
- [ ] `spec.md` documents Create, Observe, Join, Share, and Budget use cases.
- [ ] EARS notation used for all ACs.

**Tests**: none
**Gate**: Build

---

### T3: Reverse Engineer Products Feature

**What**: Create `spec.md` for the Products feature.
**Where**: `.specs/features/products/spec.md`
**Depends on**: T2
**Requirement**: SDD-02

**Tools**:
- MCP: `filesystem`

**Done when**:
- [ ] `spec.md` documents Scan, Search, Suggestions, and AI Analysis.
- [ ] EARS notation used for all ACs.

**Tests**: none
**Gate**: Build

---

### T4: Reverse Engineer Settings Feature

**What**: Create `spec.md` for the Settings feature.
**Where**: `.specs/features/settings/spec.md`
**Depends on**: T3
**Requirement**: SDD-02

**Tools**:
- MCP: `filesystem`

**Done when**:
- [ ] `spec.md` documents AI, Theme, Data, and Notification settings.
- [ ] EARS notation used for all ACs.

**Tests**: none
**Gate**: Build

---

### T5: Reverse Engineer AI Feature

**What**: Create `spec.md` for the AI feature.
**Where**: `.specs/features/ai/spec.md`
**Depends on**: T4
**Requirement**: SDD-02

**Tools**:
- MCP: `filesystem`

**Done when**:
- [ ] `spec.md` documents Assistant and Message Processing.
- [ ] EARS notation used for all ACs.

**Tests**: none
**Gate**: Build

---

### T6: Update CHANGELOG.md

**What**: Add new version entry with changes since 2025-10-21.
**Where**: `CHANGELOG.md`
**Depends on**: T5
**Requirement**: SDD-03

**Tools**:
- MCP: `filesystem`

**Done when**:
- [ ] New section for version (e.g., 1.4.0) exists.
- [ ] Key CI/CD and refactoring changes from git log are included.

**Tests**: none
**Gate**: Build

---

### T7: Verify and Update README.md

**What**: Ensure root README matches current architecture.
**Where**: `README.md`
**Depends on**: T6
**Requirement**: SDD-03

**Tools**:
- MCP: `filesystem`

**Done when**:
- [ ] Architecture diagram/description matches current module structure.
- [ ] Technology list is up to date.

**Tests**: none
**Gate**: Build

---

## Phase Execution Map

Visual representation of task ordering:

```
Phase 1 → Phase 2

Phase 1:  T1a ------→ T1b ------→ T2 ------→ T3 ------→ T4 ------→ T5
Phase 2:  T5 ------→ T6 ------→ T7
```

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1a: Finalize STATE.md | 1 file | ✅ Granular |
| T1b: Initialize LESSONS.md | 1 file | ✅ Granular |
| T2: Rev. Eng. Shopping | 1 file | ✅ Granular |
| T3: Rev. Eng. Products | 1 file | ✅ Granular |
| T4: Rev. Eng. Settings | 1 file | ✅ Granular |
| T5: Rev. Eng. AI | 1 file | ✅ Granular |
| T6: Update CHANGELOG.md | 1 file | ✅ Granular |
| T7: Update README.md | 1 file | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1a | None | None | ✅ Match |
| T1b | T1a | T1a -> T1b | ✅ Match |
| T2 | T1b | T1b -> T2 | ✅ Match |
| T3 | T2 | T2 -> T3 | ✅ Match |
| T4 | T3 | T3 -> T4 | ✅ Match |
| T5 | T4 | T4 -> T5 | ✅ Match |
| T6 | T5 | T5 -> T6 | ✅ Match |
| T7 | T6 | T6 -> T7 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1a | Documentation | none | none | ✅ OK |
| T1b | Documentation | none | none | ✅ OK |
| T2 | Documentation | none | none | ✅ OK |
| T3 | Documentation | none | none | ✅ OK |
| T4 | Documentation | none | none | ✅ OK |
| T5 | Documentation | none | none | ✅ OK |
| T6 | Documentation | none | none | ✅ OK |
| T7 | Documentation | none | none | ✅ OK |
