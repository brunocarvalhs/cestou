# MVP Roadmap — Cestou (How-Much)

Status: Draft
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
| G1 | `feat/new-layout` → `develop` not merged | Nothing else ships until this lands; 215 commits of architecture change is not a normal PR review | M–L (process, not code) |
| G2 | No in-app **account + data deletion** (Firebase-level) | Google Play policy requires apps that support account creation to also offer in-app account deletion, not just clearing local prefs. Today "Delete all data" only clears DataStore + cache. | S–M |
| G3 | Privacy Policy / Terms of Service exist only as **in-app text**, no hosted URL | Play Console requires a reachable Privacy Policy URL at submission time | S |
| G4 | Store listing assets missing | README still has a screenshots placeholder; no feature graphic/description drafted | S |
| G5 | Collaboration notifications have no writer | `NotificationRepository` reads a Firestore `notifications` collection nothing in this repo ever writes to — SHOP-02 ("notify on list changes") is unfulfilled unless an external Cloud Function does it | M |
| ~~G6~~ | ~~`lint-rules/` module has uncommitted deleted files~~ | Done — turned out to be orphaned index state from an abandoned attempt (never wired into `settings.gradle.kts`, fully superseded by the Konsist architecture tests). Unstaged, nothing to commit. | S |
| G7 | Dead `signInWithGoogle`/`signInWithApple` in `AuthService`/`FirebaseAnonymousAuthentication` | Confirmed unused — real login goes through FirebaseUI's `FirebaseAuthScreen`. Misleading to keep as-is. | S |
| G8 | Apple Sign-In never offered | `AuthConfigUseCase` only registers the Google provider; the UI has an unreachable Apple branch. Android-only app, so likely not worth building — candidate for removal instead of implementation. | S (to remove) / M (to implement) |

Already fixed this session, each as its own branch + PR against `feat/new-layout` (not merged
without review): shared `StorageService` for `core/auth` (PR #14), shopping-reminder push
notifications (PR #15), Maestro regression suite (PR #16), this doc (PR #17).

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

- **F1.1 — Account & data deletion (G2).** User story: *as a user, I can permanently delete my
  account and all associated data from within the app.* Acceptance: a "Delete Account" action in
  Settings/Profile calls Firebase Auth `delete()` + removes the user's Firestore documents
  (shopping lists they own, profile), with a confirmation dialog and re-auth if Firebase requires
  it for the sign-in method in use.
- **F1.2 — Hosted Privacy Policy + Terms (G3).** Publish the existing `settings_privacy_content` /
  `settings_terms_content` text as a real hosted page (even a static one), link it from
  `CustomMethodPickerTerms` and the Settings screen, and register the URL in Play Console.
- **F1.3 — Store listing assets (G4).** Screenshots (phone + tablet if supported), feature
  graphic, short/long description in pt/en/es to match `generateLocaleConfig`.

### Phase 2 — Deliver on what the app already promises

- **F2.1 — Wire real-time collaboration notifications (G5).** User story: *as a user in a shared
  list, I get notified when someone else edits or finishes it.* No Cloud Functions (Spark plan) —
  instead, the client that performs the write (edit/finish/join) also writes one `notifications`
  document per other member of that list, gated by a Firestore security rule that only allows a
  user to create a notification addressed to someone else, never to read/write on another user's
  behalf otherwise. This only covers changes made while the app is open on some device; it's not a
  true push notification (would need FCM + a trigger to send it, which again means Cloud Functions
  or a self-hosted sender — out of scope for the free plan). Acceptable trade-off for MVP.
- **F2.2 — Device QA pass on Google Sign-In.** Config is verified correct (`serverClientId` matches
  `google-services.json`); what's unverified is the live flow (SHA-1 fingerprints registered,
  OAuth consent screen published). Needs an actual device run, not code changes.
- **F2.3 — Resolve the Apple Sign-In question (G7, G8).** Default recommendation: remove the dead
  `signInWithGoogle`/`signInWithApple` methods and the unreachable Apple UI branch, since this is
  Android-only and FirebaseUI already owns the real Google flow. Only build Apple for real if
  there's a concrete reason (e.g., a KMP/iOS target on the roadmap).

### Phase 3 — Post-launch polish (not MVP blockers)

- F3.1 — Biometric app-lock (pattern from FriendsSecrets' `core/biometric`).
- F3.2 — Extend shared `StorageService` adoption to `feature/settings` (deferred earlier this
  session — needs combining ~10 preference keys into one reactive contract without losing
  atomicity).
- F3.3 — Replace the placeholder system notification icon with a branded monochrome asset.
- F3.4 — Fix `AiChatScreen`'s settings icon (`contentDescription = null`) for accessibility/testability.

## Suggested order

F0.3 can happen today, independent of everything else. F0.1 (the merge) should happen before Phase
1 work starts, so Phase 1 isn't built twice on two diverging branches. Phase 1 items are
independent of each other and can run in parallel. Phase 2 and 3 can trail the Play Store
submission.

## Process

Each item above ships as its own branch off `feat/new-layout`, with its own PR — never committed
or merged directly. That keeps every change independently reviewable and revertable, and keeps
`feat/new-layout` itself untouched until you decide to merge a given PR. PR numbers are noted next
to each completed item as they land.
