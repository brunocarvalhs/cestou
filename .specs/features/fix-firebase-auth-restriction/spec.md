# Fix FirebaseAuth restricted to administrators only Specification

## Problem Statement

The Wear OS application crashes on startup with `com.google.firebase.auth.FirebaseAuthException: This operation is restricted to administrators only` when attempting to perform anonymous authentication. This is triggered by `getOrThrow()` in `FirebaseAnonymousAuthentication.getOrCreateUserId()` during the initial data loading in `ShoppingListViewModel`.

## Goals

- [ ] Prevent application crash on Firebase Auth failure.
- [ ] Implement a graceful guest mode fallback for Wear OS.
- [ ] Provide clear error logging for Firebase configuration issues.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Automatic Firebase Console configuration | I don't have access to the user's Firebase Console. |
| Full Login Flow on Wear OS | The app is intended to use anonymous auth or sync from phone; full UI login is a separate feature. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Cause of error | Firebase Console setting "Enable account creation" is disabled OR Wear app SHA-1 is missing. | Common cause for this specific error message in Firebase Auth. | N |
| Fallback behavior | Use a fixed "guest" user ID if auth fails. | Allows the UI to load and show a specific "offline" or "guest" state instead of crashing. | N |
| Error reporting | Log the error and show a Snackbar/Toast in the UI. | Standard UX for non-fatal initialization failures. | N |

**Open questions:** None - all resolved via assumptions above.

---

## User Stories

### P1: Crash Prevention & Graceful Fallback ⭐ MVP

**User Story**: As a user, I want the app to start even if authentication fails so that I don't experience a crash and can understand what's wrong.

**Why P1**: The current crash makes the app unusable.

**Acceptance Criteria**:

1. IF `signInAnonymously()` fails THEN the system SHALL NOT throw an exception from `getOrCreateUserId()`.
2. IF authentication fails THEN `getOrCreateUserId()` SHALL return a `guest` AuthenticatedUser.
3. WHEN `ShoppingListViewModel` fails to fetch data due to auth error THEN it SHALL update the `uiState.error` with a descriptive message.
4. The system SHALL log the specific Firebase exception message and code to Timber.

**Independent Test**: Can demo by disabling internet or using a misconfigured `google-services.json` and verifying the app doesn't crash and shows an error message.

---

## edge Cases

- IF Firebase returns a network error THEN system SHALL handle it the same as the restriction error (fallback to guest).
- IF `auth.currentUser` is already non-null but invalid THEN system SHALL attempt re-auth and handle failure.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| AUTH-01 | P1: Crash Prevention | Design | Pending |
| AUTH-02 | P1: Guest Fallback | Design | Pending |
| AUTH-03 | P1: Error UI | Design | Pending |

**Coverage:** 3 total, 0 mapped to tasks, 3 unmapped ⚠️

---

## Success Criteria

- [ ] App starts without crash on the provided emulator/device.
- [ ] Logcat shows descriptive Timber error instead of stack trace for the restriction error.
- [ ] Shopping list shows an error state instead of a blank screen or crash.
