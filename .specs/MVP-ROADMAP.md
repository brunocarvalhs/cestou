# MVP Roadmap — Cestou (How-Much)

Status: Phases 0–2 mostly done (9 PRs, #14–#22) — see gap list for what's still open
Owner: bruno
Last updated: 2026-09-01

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

- Branch `feat/new-layout` is **215 commits ahead of `develop`** and contains the entire
  multi-module Clean Architecture rewrite (AD-001–AD-007), Wear OS support, the AI agent, and this
  session's fixes (Google Sign-In audit, Maestro regression suite, shared `StorageService`,
  shopping-reminder notifications). `develop` itself is stale relative to all of this.
- Because of that gap, **merging this branch is the single biggest risk item** before anything
  else ships — bigger than any individual feature below.
- Google Sign-In, product scanning/AI, list sharing, and settings are functionally real (verified
  by reading the code, not just the specs) — they are not on this list.

## Gap list

| # | Gap | Why it matters | Est. effort |
|---|---|---|---|
| G1 | `feat/new-layout` → `develop` not merged | Nothing else ships until this lands; 215 commits of architecture change is not a normal PR review | M–L (process, not code) — **open, needs your call** |
| ~~G2~~ | ~~No in-app account + data deletion~~ | Done — PR #18 | S–M |
| ~~G3~~ | ~~Privacy Policy / Terms only in-app, no hosted URL~~ | Pages drafted — PR #21. Hosting decision + wiring the URL still needs you. | S |
| ~~G4~~ | ~~Store listing assets missing~~ | Descriptions drafted (en/pt-BR/es) — PR #22. Screenshots/feature graphic still need a device. | S |
| ~~G5~~ | ~~Collaboration notifications have no writer~~ | Done, client-side (no Cloud Functions) — PR #20. Firestore rules need a manual check, noted in the PR. | M |
| ~~G6~~ | ~~`lint-rules/` module has uncommitted deleted files~~ | Done — orphaned index state from an abandoned attempt, unstaged. | S |
| ~~G7~~ | ~~Dead `signInWithGoogle`/`signInWithApple`~~ | Done — PR #19 | S |
| ~~G8~~ | ~~Apple Sign-In never offered~~ | Removed the unreachable UI branch rather than implementing it — PR #19 | S |

Every item below shipped as its own branch + PR against `feat/new-layout` this session (none
merged without review): shared `StorageService` for `core/auth` (#14), shopping-reminder push
notifications (#15), Maestro regression suite (#16), this doc (#17), account & data deletion
(#18), dead social-auth cleanup (#19), collaboration notifications (#20), hosted legal pages
(#21), store listing descriptions (#22).

**Still genuinely open, none of them fixable from this environment:**
- **G1** — the `develop` merge is your call, not something to automate.
- **F0.3 / F2.2** — no adb/emulator here, so the Maestro suite and the Google Sign-In flow have
  never actually run.
- Screenshots/feature graphic (part of G4) need a device to capture.
- The privacy/terms pages (G3) need a hosting decision before the in-app links can point anywhere.
- Firestore security rules for the new `notifications` writes (G5) live outside this repo and need
  a manual check in the Firebase Console.

## Plan — features broken by user value

### Phase 0 — Unblock (infra, no direct user value, but nothing ships without it)

- **F0.1 — Merge `feat/new-layout` into `develop`.** Given how stale `develop` is, this is closer
  to "make `feat/new-layout` the new `develop`" than a conflict-resolution merge. Needs an explicit
  decision from you on approach (fast-forward vs. PR) before anyone touches it — not something to
  automate silently.
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

### Phase 3 — Post-launch polish (not MVP blockers)

- F3.1 — Biometric app-lock (pattern from FriendsSecrets' `core/biometric`).
- F3.2 — Extend shared `StorageService` adoption to `feature/settings` (deferred earlier this
  session — needs combining ~10 preference keys into one reactive contract without losing
  atomicity).
- F3.3 — Replace the placeholder system notification icon with a branded monochrome asset.
- F3.4 — Fix `AiChatScreen`'s settings icon (`contentDescription = null`) for accessibility/testability.

## Suggested order

Everything code-shaped that didn't need a device, a hosting decision, or a merge call from you is
done (9 PRs, #14–#22). What's left needs you specifically:

1. Review and merge the PRs you want, in whatever order makes sense to you — they're independent
   of each other except that #19 (dead social-auth cleanup) and #20 (collaboration notifications)
   both touch `AuthService`/`FirebaseAnonymousAuthentication`-adjacent files, so merge one before
   rebasing the other if you take both.
2. Decide on F0.1 (the `develop` merge) whenever you're ready — nothing above depends on it having
   happened first, since everything branched from `feat/new-layout`.
3. Once you have a device: run the Maestro suite (F0.3), do the Google Sign-In QA pass (F2.2), and
   capture the screenshots/feature graphic (rest of G4).
4. Pick a host for the legal pages (G3) and wire the URL in.
5. Check the Firestore rules for the new notification writes (G5) in the Firebase Console.

## Process

Each item above ships as its own branch off `feat/new-layout`, with its own PR — never committed
or merged directly. That keeps every change independently reviewable and revertable, and keeps
`feat/new-layout` itself untouched until you decide to merge a given PR. PR numbers are noted next
to each completed item as they land.
