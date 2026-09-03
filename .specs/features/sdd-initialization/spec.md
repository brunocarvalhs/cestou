# SDD Infrastructure & Project Mapping Specification

## Problem Statement

The "How Much" project lacks a formal specification and decision tracking system. This makes it difficult to ensure architectural consistency and precise verification as the project grows. We need to initialize the Spec-Driven Development (SDD) infrastructure and document the existing core features.

## Goals

- [ ] Initialize the `.specs/` directory structure.
- [ ] Create a central `STATE.md` for project decisions and handoff.
- [ ] Reverse-engineer and document core features (Shopping, Products, Settings, AI) into formal specs.
- [ ] Establish a `LESSONS.md` to capture and apply project-local knowledge.
- [ ] Update project documentation (READMEs) and `CHANGELOG.md` with recent architectural and CI changes.

## Out of Scope

| Feature                  | Reason                                      |
| ------------------------ | ------------------------------------------- |
| New feature implementation | The focus is purely on documenting existing state. |
| Automated test generation | This spec covers documentation; tests come during feature execution. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Core features breakdown | Shopping, Products, Settings, AI | These match the current module/feature structure. | [y] |
| Documentation level | EARS notation for ACs | To maintain consistency with the SDD skill. | [y] |
| Changelog content | Based on git history since 2025-10-21 | To catch up with the significant CI and refactoring work. | [y] |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: SDD Infrastructure Initialization ⭐ MVP

**User Story**: As a developer, I want a central place to track decisions and project state so that I can maintain context across sessions.

**Why P1**: Foundation for all other SDD activities.

**Acceptance Criteria**:

1. The `.specs/STATE.md` SHALL exist and contain "Decisions" and "Handoff" sections.
2. The `.specs/LESSONS.md` SHALL exist.
3. The `.specs/features/sdd-initialization/spec.md` SHALL be confirmed.

**Independent Test**: Verify the presence of files and their required sections.

---

### P2: Core Feature Reverse Engineering

**User Story**: As a developer, I want formal specs for existing features so that I can verify them and build upon them with precision.

**Why P2**: Enables the "Execute" phase for future changes to these features.

**Acceptance Criteria**:

1. A `spec.md` SHALL be created for `shopping`, `products`, `settings`, and `ai` features.
2. Each reverse-engineered spec SHALL use EARS notation for its Acceptance Criteria.
3. Each reverse-engineered spec SHALL include a "Requirement Traceability" table.

**Independent Test**: Verify each `spec.md` against the EARS pattern and requirement ID structure.

---

### P3: Documentation & Changelog Update

**User Story**: As a project maintainer, I want the documentation and changelog to reflect the current state of the project so that contributors can understand recent changes.

**Why P3**: Ensures the repository's metadata matches the actual codebase and CI/CD infrastructure.

**Acceptance Criteria**:

1. The `CHANGELOG.md` SHALL be updated with a new version entry covering changes since 2025-10-21.
2. The `CHANGELOG.md` SHALL follow the existing format (standard changelog or the project's specific style).
3. The root `README.md` SHALL be verified and updated if the architecture or technology stack has changed.

**Independent Test**: Verify the new entry in `CHANGELOG.md` and links in `README.md`.

---

## Edge Cases

- IF a feature implementation is found to be inconsistent with itself THEN the spec SHALL document the "intended" behavior as an assumption.

---

## Requirement Traceability

| Requirement ID | Story                       | Phase  | Status  |
| -------------- | --------------------------- | ------ | ------- |
| SDD-01         | P1: SDD Infra Init          | Design | Pending |
| SDD-02         | P2: Core Rev. Engineering   | Design | Pending |
| SDD-03         | P3: Doc & Changelog Update  | Design | Pending |

**ID format:** `SDD-[NUMBER]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 2 total, 0 mapped to tasks, 2 unmapped ⚠️

---

## Success Criteria

- [ ] The `.specs` directory reflects the current state of the project.
- [ ] All core features have at least one P1 story documented.
- [ ] `STATE.md` contains the initial architectural decisions found in the README.
