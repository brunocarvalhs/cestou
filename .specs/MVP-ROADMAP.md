# MVP Roadmap — Cestou (How-Much)

Status: Phases 0–2 mostly done — see gap list for what's still open
Owner: bruno
Last updated: 2026-09-09

Note: the project runs on Firebase's free Spark plan (no billing account) — Cloud Functions
require the Blaze plan even for free-tier usage, so nothing in this plan should depend on them.
Any "server-side" behavior has to be either client-side (Firestore writes from the app, gated by
security rules) or skipped.

## Why this document exists

The core shopping/products/settings/AI features are already built and marked "Verified" in
`.specs/features/*/spec.md`. What's actually blocking an MVP release is not new core
functionality — it's a short list of launch-readiness and compliance gaps, plus landing a very
large branch. This doc lists those gaps and breaks the work into features a user (or Google Play)
would notice, so each one can be picked up, spec'd with the `tlc-spec-driven` skill if needed, and
shipped independently.

## Current state snapshot

- **G1 is resolved.** `feat/new-layout` merged into `develop` via PR #30 — the entire multi-module
  Clean Architecture rewrite (AD-001–AD-007), Wear OS support, and the AI agent are now on
  `develop`. The CI pipeline was also rebuilt around Git Flow with staged checks (PR #47).
  `develop` is the live integration branch; feature branches PR into it, and `develop` → `master`
  is a decision the repo owner makes directly.
- Google Sign-In, product scanning/AI, list sharing, and settings are functionally real (verified
  by reading the code, not just the specs) — they are not on this list.
- A 2026-09-09 audit (this session) reviewed ViewModels, repositories, and Compose screens for
  common bug patterns (coroutine leaks, unguarded `!!`, listener cleanup); see "Bug audit" at the
  end of this doc for findings.

## Gap list

| # | Gap | Why it matters | Est. effort |
|---|---|---|---|
| ~~G1~~ | ~~`feat/new-layout` → `develop` not merged~~ | Done — PR #30. `develop` now carries the full multi-module rewrite. | M–L |
| ~~G2~~ | ~~No in-app account + data deletion~~ | Done — PR #18 | S–M |
| ~~G3~~ | ~~Privacy Policy / Terms only in-app, no hosted URL~~ | Pages drafted — PR #21. Hosting decision + wiring the URL still needs you. | S |
| ~~G4~~ | ~~Store listing assets missing~~ | Descriptions drafted (en/pt-BR/es) — PR #22. Screenshots/feature graphic still need a device. | S |
| ~~G5~~ | ~~Collaboration notifications have no writer~~ | Done, client-side (no Cloud Functions) — PR #20. Firestore rules need a manual check, noted in the PR. | M |
| ~~G6~~ | ~~`lint-rules/` module has uncommitted deleted files~~ | Done — orphaned index state from an abandoned attempt, unstaged. | S |
| ~~G7~~ | ~~Dead `signInWithGoogle`/`signInWithApple`~~ | Done — PR #19 | S |
| ~~G8~~ | ~~Apple Sign-In never offered~~ | Removed the unreachable UI branch rather than implementing it — PR #19 | S |
| G9 | `ShoppingRepositoryImpl.updatePositions` is a no-op (`ShoppingRepositoryImpl.kt:133-139`) — reordering lists by drag updates local state optimistically (`ShoppingListViewModel.kt:269`) but never writes to Firestore, so the order silently reverts on next sync | Feature is already exposed in the UI and looks like it works; found during a 2026-09-04 Tech Lead audit | **Fix pushed, PR #67 open against `develop`, not yet merged/reviewed** |
| G10 | Cross-feature module coupling: `cart`/`shopping`/`chat`/`ai-agent`/`profile` import `feature.settings` directly, `cart` imports `feature.chat`/`feature.products`, `products` imports `feature.chat`, `shopping` imports `feature.products` | Violates AD-005 (feature modules should only expose a `navigation` entry point); makes each feature module's real dependency graph wider than documented, raising the risk of accidental coupling as the app grows | M — needs a design pass (extract shared contracts to `core/*`), not a quick fix |

Every item marked done above shipped as its own branch + PR (none merged without review): shared
`StorageService` for `core/auth` (#14), shopping-reminder push notifications (#15), Maestro
regression suite (#16), this doc (#17), account & data deletion (#18), dead social-auth cleanup
(#19), collaboration notifications (#20), hosted legal pages (#21), store listing descriptions
(#22), `feat/new-layout` → `develop` merge (#30), CI rebuild around Git Flow (#47),
`updatePositions` fix (#67, open).

**Still genuinely open, none of them fixable from this environment:**
- **F0.3** — first real execution (2026-09-09, Samsung SM-A146M, Android 15, wireless adb) found
  two suite-authoring bugs, both fixed on `test/maestro-e2e-coverage`: (1) `home_flow.yaml`'s
  `launchApp: clearState: true` logs the device out of Google Sign-In instead of landing on an
  authenticated empty home — split into a separate `onboarding_flow.yaml` (clearState allowed,
  excluded from `test_suite.yaml`) and a `home_flow.yaml` that assumes an existing session; (2) all
  8 flows asserted hardcoded strings against the device — first "fixed" to hardcoded pt-BR (still
  fragile: would've broken again on a CI emulator defaulting to en-US), then corrected to a
  `Modifier.testTag`-based selector strategy so flows don't depend on device locale either way
  (~25 Compose files touched, scoped to only what the flows touch; see `.maestro/README.md`
  "Language / locale"). `onboarding_flow.yaml` ran and passed in full, both before and after the
  testTag rework. The rest of the suite is blocked on a human logging back into the device (this
  session's own test run cleared the session) — see `.specs/STATE.md` handoff for the exact ask.
  F2.2 (Google Sign-In QA) still needs a manual pass; Maestro can't drive the Google account picker.
- Screenshots/feature graphic (part of G4) need a device to capture.
- The privacy/terms pages (G3) need a hosting decision before the in-app links can point anywhere.
- Firestore security rules for the new `notifications` writes (G5) live outside this repo and need
  a manual check in the Firebase Console.
- **G9** needs your review/merge of PR #67 — don't duplicate the fix.
- **G10** needs a design decision (which shared contracts move to `core/*`) before it's worth
  spec'ing as its own initiative; flagged here so it doesn't silently grow.

## Plan — features broken by user value

### Phase 0 — Unblock (infra, no direct user value, but nothing ships without it)

- ~~**F0.1 — Merge `feat/new-layout` into `develop`.**~~ Done — PR #30.
- ~~**F0.2 — Resolve `lint-rules/` uncommitted state (G6).**~~ Done.
- **F0.3 — Run the Maestro suite (`maestro test .maestro/test_suite.yaml`) on a real device**,
  fix whatever selector drift shows up now that it's finally executable.

### Phase 1 — Play Store launch blockers (must-have)

- ~~**F1.1 — Account & data deletion (G2).**~~ Done — PR #18. `DeleteAccountUseCase` leaves/deletes
  the user's shopping lists, deletes their profile, clears local settings, then deletes the
  Firebase account, in that order. No re-auth flow for `FirebaseAuthRecentLoginRequiredException`
  — surfaces as a generic error if Firebase demands a recent login.
- ~~**F1.2 — Hosted Privacy Policy + Terms (G3).**~~ Pages drafted — PR #21
  (`docs/legal/privacy.html`, `terms.html`). Not linked from the app yet: needs a hosting decision
  (`cestou.app` vs. GitHub Pages) before wiring the URL into `CustomMethodPickerTerms`, Settings,
  and Play Console.
- ~~**F1.3 — Store listing assets (G4).**~~ Descriptions done for en/pt-BR/es — PR #22
  (`fastlane/metadata/android/`). Screenshots and the feature graphic still need a device.

### Phase 2 — Deliver on what the app already promises

- ~~**F2.1 — Wire real-time collaboration notifications (G5).**~~ Done, client-side — PR #20.
  `ShoppingJoinUseCase` and `FinishPurchaseViewModel` each write a `notifications` document per
  other member instead of relying on a Cloud Function (Spark plan has none). Only covers changes
  made while some device has the app open — not a true push notification. **Needs a manual check**
  that Firestore security rules allow a user to create a notification addressed to someone else.
- **F2.2 — Device QA pass on Google Sign-In.** Still open. Config is verified correct
  (`serverClientId` matches `google-services.json`); what's unverified is the live flow (SHA-1
  fingerprints registered, OAuth consent screen published). Needs an actual device run.
- ~~**F2.3 — Resolve the Apple Sign-In question (G7, G8).**~~ Done — PR #19. Removed the dead
  `signInWithGoogle`/`signInWithApple` methods and the unreachable Apple UI branch rather than
  implementing Apple for real, since this is Android-only.
- **F2.4 — Fix `updatePositions` no-op (G9).** Fix pushed — PR #67 (`fix/shopping-update-positions`),
  open against `develop`, awaiting review/merge.

### Phase 3 — Post-launch polish (not MVP blockers)

- F3.1 — Biometric app-lock (pattern from FriendsSecrets' `core/biometric`).
- F3.2 — Extend shared `StorageService` adoption to `feature/settings` (deferred earlier this
  session — needs combining ~10 preference keys into one reactive contract without losing
  atomicity).
- F3.3 — Replace the placeholder system notification icon with a branded monochrome asset.
- ~~F3.4 — Fix `AiChatScreen`'s settings icon (`contentDescription = null`) for
  accessibility/testability.~~ Done this session — icon now uses
  `ai_chat_settings_content_description` (en/es/pt-BR), and `chat_flow.yaml` asserts it's reachable.
- F3.5 — Address G10 (cross-feature module coupling) once a design pass decides which shared
  contracts move to `core/*`.

## Suggested order

`feat/new-layout` is merged; `develop` is now the live integration branch. What's left needs you
specifically:

1. Review and merge PR #67 (G9 fix) — small, low-risk, already implemented and tested.
2. Once you have a device: run the Maestro suite (`maestro test .maestro/test_suite.yaml`, F0.3,
   now includes `account_data_flow.yaml`), do the Google Sign-In QA pass (F2.2), and capture the
   screenshots/feature graphic (rest of G4).
3. Pick a host for the legal pages (G3) and wire the URL in.
4. Check the Firestore rules for the new notification writes (G5) in the Firebase Console.
5. When ready, decide on `develop` → `master` for the actual store release — that merge is yours to
   make, same as G1 was.
6. Schedule a design pass for G10 (cross-feature coupling) before it grows further — not a launch
   blocker, but the longer it's left the more feature modules will depend on it.
7. Prioritize **G15** (hardcoded Gemini API key, no remote-rotation path) among the new bug-audit
   items — it's the only one with a security angle, the rest (G12–G14, G16) are correctness/UX bugs
   without a compromise scenario.

## Bug audit — 2026-09-09

A pass over ViewModels, repository implementations, and Compose screens for common bug patterns
(coroutine scope leaks, unguarded `!!`, missing Firestore listener cleanup, `LaunchedEffect`/
`remember` key mistakes). Findings beyond what was already tracked (G9, F3.4):

| # | Gap | File | Fix status |
|---|---|---|---|
| ~~G11~~ | ~~Camera analyzer thread leak~~ — `CameraPreview`'s single-thread `Executor` was created via `remember` but never shut down; every scanner screen visit (open → back → reopen) leaked a background thread | `feature/products/.../components/scanner/CameraPreview.kt:33` | **Fixed this session** — added `DisposableEffect` to shut down the executor |
| G12 | `CartViewModel.observeData()` leaks duplicate Flow collectors — its settings `collect{}` calls `observeProducts()`, which launches a *new* `viewModelScope` collector each time instead of using `flatMapLatest`; every DataStore settings write anywhere in the app (theme, language, AI prefs) adds one more permanent product collector, each re-running `sortProductsUseCase`/`resolveMemberProfiles` | `feature/cart/.../viewmodel/CartViewModel.kt:98-127` | Open — needs its own PR, moderate risk (touches the cart's core observe loop) |
| G13 | QR-code list join has no scan debounce — `BarcodeAnalyzer` fires `onBarcodeScanned` on every analyzed camera frame with no throttle/one-shot guard, and `ScannerViewModel.onTokenScanned` has no "already processing" flag; holding a code in frame re-triggers `ShoppingJoinUseCase`, which loops a notification write per other member on every duplicate join | `feature/products/.../scanner/BarcodeAnalyzer.kt:22`, `feature/shopping/.../viewmodel/ScannerViewModel.kt:28-35`, `ShoppingJoinUseCase.kt:33-38` | Open — needs its own PR; also spams other members with duplicate push notifications |
| G14 | `ProfileViewModel.observeProfile()` subscribes to the Firestore profile but discards the emitted value, always rebuilding state from cached `authService.currentUser` instead — a Firestore-only profile edit never reaches the UI, and the listener runs forever for no effect | `feature/profile/.../viewmodel/ProfileViewModel.kt:47-54` | Open — needs its own PR; needs care to confirm `UserProfile` vs. `authService.currentUser` field parity before merging them |
| G15 | Gemini API key is compiled into the APK (`BuildConfig.GEMINI_API_KEY`) in three places, and the remote-config key meant for server-side rotation (`RemoteVariableKeys.GEMINI_API_KEY`) is never actually read — so a compromised/abused key can't be revoked without a new release | `feature/products/.../ProductRepositoryImpl.kt:44-47`, `RecipeRepositoryImpl.kt:29-32`, `feature/ai-agent/.../GeminiAiAgent.kt:29-30`, `core/remote-config/.../RemoteConfigKeys.kt:19` | Open — security-relevant; needs its own PR wiring the repositories to read from Remote Config with the compiled key as fallback |
| G16 | `ProductSearchViewModel.search()` has no debounce or request cancellation — every keystroke past 3 chars launches a fresh, untracked coroutine; a slower earlier response can arrive after a faster later one and overwrite `_uiState` with stale results for a query the user no longer typed | `feature/products/.../viewmodel/ProductSearchViewModel.kt:55-90` | Open — needs its own PR (debounce + cancel-previous-job pattern) |

G12–G16 are documented here rather than fixed in this branch on purpose: none of them can be
exercised on a device in this environment, and each touches a different feature's core behavior
(cart observation, QR join, profile sync, AI cost/security, search) — bundling behavioral fixes
like these into one PR is exactly the risk the "Process" section below exists to avoid. G11 was
fixed here because it's a pure resource-cleanup change with zero behavior change.

## Process

Each item above ships as its own branch off `develop`, with its own PR into `develop` — never
committed or merged directly. That keeps every change independently reviewable and revertable.
`develop` → `master` is a separate, deliberate step the repo owner takes. PR numbers are noted next
to each completed item as they land.
