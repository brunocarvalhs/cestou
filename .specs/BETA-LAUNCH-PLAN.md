# Beta Launch Plan — Cestou (How-Much)

Status: Active — **all code items in the T1–T6 queue are merged into `develop`.** What remains is
device/console/owner work, not engineering work. See "Revision 3" and the annotated readiness
checklist at the bottom.
Owner: `tech-lead` (document), bruno (go/no-go)
Last updated: 2026-09-10 (revision 3)

This document sits on top of `.specs/MVP-ROADMAP.md` (the gap list and its numbering are the source
of truth for *what* is broken) and `.specs/STATE.md` (architecture decisions + handoff). It exists to
sequence four workstreams — feature/bug closure, test coverage, analytics instrumentation, and
beta/store readiness — into one ordered plan, with an owner per item. Owners map to the custom
subagents in `.claude/agents/`: `tech-lead`, `pm`, `android-engineer-features`,
`android-engineer-quality`, `data-engineer`, `marketing`.

Companion documents, owned by other agents — do not edit them here:
- `.specs/MVP-ROADMAP.md` § "Beta Launch Priority" — `pm`. Defines *what gates* the beta.
- `.specs/ANALYTICS-PLAN.md` — `data-engineer`. Defines *which events/funnels* to watch.
- `.specs/BETA-KPI.md` — `marketing`, in progress at time of writing. Defines *success thresholds*.

**Ground rule (unchanged from `MVP-ROADMAP.md`):** one item = one branch off `develop` = one PR back
into `develop`. Never commit or merge directly. `develop` → `master` (or whatever branch actually
builds the beta release) stays a decision only bruno makes.

---

## Revision 3 — readiness re-check (2026-09-10)

The whole T1–T6 queue landed in one execution round. Verified against `origin/develop` (not against
PR descriptions): `git log origin/develop` carries the merge commits for #69, #71, #72, #73, #77,
#78, #79, and the fixes are present in the code (`ScannerViewModel.isJoining`,
`CartViewModel.flatMapLatest`, `ProductSearchViewModel.debounce`, the `.takeIf { it.isNotBlank() }`
guard at all three Gemini key call sites).

Three things this re-check changes, none of them cosmetic:

### 1. G9 / PR #67 is **not** "waiting on a merge click" — it is red and stale

This was recorded in revision 2 (and in `MVP-ROADMAP.md`) as a pure repo-owner decision. That is now
wrong, and it is the single most misleading line in the previous revision:

- **CI is failing.** `🔍 Static Analysis (Detekt)` fails, and with it the `✅ PR Gate`. The cause is
  one line: `feature/shopping/src/test/java/.../ShoppingRepositoryImplTest.kt:220` — `MaximumLineLength`
  (>120 chars). One weighted issue, one line to wrap.
- **The branch is 18 commits behind `develop`** (`mergeStateStatus: BEHIND`), predating every merge
  from this round.

So bruno cannot merge it even if he wants to. Somebody has to wrap that line and update the branch
first. Assigned below as **T7**; it is a ~5-minute job that has been silently blocking the oldest
open beta blocker since 2026-09-09.

### 2. "Analytics instrumented **and verified**" was one checklist line covering two states

The instrumentation merged (#71), but `ANALYTICS-PLAN.md` says plainly: "None of the new events have
been confirmed in DebugView yet — the instrumentation is code-complete." Merging the PR does not
satisfy the verification half. The checklist below splits the line so the unverified half stays
visible instead of being absorbed by the merged half.

### 3. G16 / G12 / G14 shipped on unit tests only — recorded, not glossed over

None of the three has been exercised on a device or in an emulator. Their evidence is JVM unit tests
(rapid-keystrokes-single-search, N-settings-emissions-one-collector, Firestore-emission-reaches-state)
plus code review. That is **accepted** — all three were graded non-blocking precisely because their
triggers are atypical, and all three are cancellation/flow-plumbing changes whose failure mode is a
stale or missing UI update, not data corruption. But "unit-tested" is not "verified", and the
distinction belongs in writing: if any of the three regresses, it will surface in the beta cohort,
not in CI. Whoever runs F0.3 on a device should walk product search, the cart with a settings change
mid-session, and a profile edit, and say so.

Also noted, deliberately **not** promoted to a gate (see "Residual risks" at the end): the Maestro
work in draft PR #75 surfaced a hardcoded English `Text("Checkout")` on the cart's primary
finish-purchase button, which never localizes for a pt-BR cohort.

---

## Revision 2 — what changed and why

### 1. Resolved: the `pm` discrepancy on "G12–G16 closed" — **split accepted**

Revision 1's "Definition of ready for beta" listed `G12–G16 closed` as one flat checklist line. The
`pm` persona pass (`MVP-ROADMAP.md` § "Beta Launch Priority") disagreed, calling G13 and G15 real
blockers and G12/G14/G16 desirable-not-blocking.

**Decision: accept the split.** G13 and G15 gate the beta; G12, G14 and G16 do not.

Rationale — the flat line was a **grouping of provenance, not a grading of risk**. G12–G16 arrived
together because one bug-audit pass on 2026-09-09 produced them, and revision 1 inherited that
grouping uncritically. Sharing a discovery date is not an argument for sharing a release gate. The
`pm` pass is the first time these five were actually graded against user-visible impact, and its
reasoning holds up against the code:

- **G13 — blocker, and the code is more damning than the persona argument.** While validating this
  I traced the call chain and confirmed it is live in production code:
  `ShoppingGraph.kt:151` → `QrCodeScanner(onTokenScanned)` →
  `QrCodeScanner.kt:53 CameraPreview(onBarcodeScanned = onTokenScanned)` →
  `BarcodeAnalyzer.kt:22` fires **per analyzed camera frame** → `ScannerViewModel.onTokenScanned`
  (`ScannerViewModel.kt:38`, no in-flight guard) → `ShoppingJoinUseCase` → one notification write per
  other member, per frame. The trigger is "hold the phone still over a QR code", i.e. the intended
  use of the screen. This is not a persona judgement call; it reproduces on the happy path.
- **G15 — blocker on a different axis (business risk, not persona pain).** The gate is not "a tester
  notices"; it is "the APK leaves internal hands". Correctly graded.
- **G12 / G14 / G16 — not blockers.** Each requires an atypical trigger (repeated settings writes;
  simultaneous multi-device profile edit; fast typing on a slow network) and none corrupts persisted
  data. G16 is the closest call and the `pm` says so explicitly; I agree with the verdict — the stale
  result lands in a dropdown the user must still tap to confirm, so the worst case is a flicker, not
  a wrong item silently saved.

Two amendments I am adding on top of the `pm`'s split:

1. **G12/G14/G16 stay in the beta *window*, just not on the *gate*.** They ship during the beta as
   normal PRs. "Not a blocker" must not decay into "not scheduled".
2. **G16 has a sequencing constraint the persona pass could not see:** `ProductSearchViewModel.kt`
   is already modified by the committed analytics work on `feat/beta-analytics-instrumentation`.
   G16 must branch *after* that lands on `develop`, or the two edits collide in the same file.

### 2. Correction to a claim in `.specs/ANALYTICS-PLAN.md` (for `data-engineer`, not edited here)

`ANALYTICS-PLAN.md` § "Note on 'escanear código de barras'" states that
`BarcodeAnalyzer`/`CameraPreview`'s `onBarcodeScanned` callback is "**not wired to any use case** —
dead code, not a real user flow today". **This is incorrect.** It is wired, but from a *different
module*: `feature/shopping`'s `QrCodeScanner` imports `feature.products`'s `CameraPreview` and passes
`onBarcodeScanned = onTokenScanned`. A grep scoped to `feature/products/` would show it unused; the
consumer is in `feature/shopping/`.

Consequences:
- The QR-join scan path **is** a live user flow and **is** the G13 blocker. It is already
  instrumented (`shopping_list_joined` with `join_method=qr_scan`) — so the analytics coverage is
  fine; only the doc's justification is wrong.
- What is genuinely absent is **product barcode/SKU lookup** — no such use case exists. The
  conclusion "if someone wires up a literal barcode scanner it needs its own event" stands.
- This cross-module import is also a concrete instance of **G10 / AD-005**: `feature/shopping`
  reaches into `feature/products`'s `presentation/components/`, not its `navigation` entry point.
  Logged for the G10 design pass; not fixed now.

`data-engineer` should correct that paragraph in its own document.

### 3. G15 fix reviewed — correct, but **not complete** and **on the wrong branch**

A near-finished G15 fix exists as **uncommitted working-tree changes**, currently sitting on
`feat/beta-analytics-instrumentation` (someone else's branch, whose own commit is clean) instead of
`fix/gemini-key-remote-config`. Review verdict and recovery instructions are in the task queue below.

---

## Task queue — ordered, assignable

**Queue status as of 2026-09-10 — T1–T6 all merged into `develop`. Only T7 is open.**

| Task | Item | PR | Status |
|---|---|---|---|
| T1 | G15 — Gemini key via Remote Config | #69 | Merged 2026-09-09. Blank-value guard included. **Console rotation still owed by bruno.** |
| T2 | G13 — QR-join scan debounce | #73 | Merged 2026-09-09 |
| T3 | Coverage baseline + critical-path tests | #72 | Merged 2026-09-09. Kover (not Jacoco) was already configured; real baseline 82.00% → **84.47%** line. Includes the G11 regression test, which **did** run on a device. |
| T4 | G16 — product search debounce | #77 | Merged 2026-09-10. Unit tests only. |
| T5 | G14 — profile emission reconciled | #79 | Merged 2026-09-10. Unit tests only. |
| T6 | G12 — cart collector leak (`flatMapLatest`) | #78 | Merged 2026-09-10. Unit tests only. |
| — | Analytics instrumentation | #71 | Merged 2026-09-09. **Code-complete, not DebugView-verified.** |
| **T7** | **Unblock PR #67 (G9): Detekt + rebase** | — | **Open — see below. Blocks the oldest beta blocker.** |

The original briefs for T1–T6 are kept below unchanged, as the record of what was asked for.

Tasks are listed in execution order. Follow SDD (`.agents/skills/spec-driven/SKILL.md`): each task =
one spec, one branch off `develop`, one PR into `develop`.

---

### T7 — unblock PR #67 (G9) so it is actually mergeable `[BLOCKER]`

- **Owner:** `android-engineer-features`
- **Branch:** `fix/shopping-update-positions` (the existing PR #67 branch — this is the one case where
  the work goes onto an existing branch, because the goal is to make *that* PR green, not to open a
  competing one)
- **Do not** re-implement the fix. The fix itself is done and reviewed; only its CI is red.

Two things, nothing else:

1. Wrap `feature/shopping/src/test/java/br/com/brunocarvalhs/howmuch/feature/shopping/data/repository/ShoppingRepositoryImplTest.kt:220`
   to ≤120 chars. That single `MaximumLineLength` violation is the entire Detekt failure
   ("Analysis failed with 1 weighted issues") and therefore the entire `PR Gate` failure.
2. Bring the branch up to date with `develop` (it is 18 commits behind, `mergeStateStatus: BEHIND`),
   the same way the other PRs in this round did it — merge `develop` in, do not force-push a rebase
   over a branch bruno may already be looking at.

Then re-request review. **Merging remains bruno's call** — this task only removes the reason he
cannot.

### Legend

- **BLOCKER** = gates inviting real customers into the beta.
- **WINDOW** = ships during the beta, does not gate go/no-go.

---

### T1 — G15: finish and commit the Gemini API key fix `[BLOCKER]`

- **Owner:** `android-engineer-features`
- **Branch:** `fix/gemini-key-remote-config` (already exists locally, currently **identical to
  `develop`** — zero commits ahead)
- **Depends on:** nothing. **Start here.**
- **Blocks:** T2 (G13) — see the ordering note there.

#### Step 0 — recover the branch safely (do this before anything else)

The fix is **uncommitted** and the checked-out branch is wrong. It has been verified safe to move:

```
git branch --show-current      # expect: feat/beta-analytics-instrumentation
git status --short             # expect the 8 modified paths below, still present
git checkout fix/gemini-key-remote-config
git status --short             # expect: the same modified paths, carried over
```

**Why this is safe (verified, do not skip the verification):** Git carries uncommitted changes across
a checkout and only refuses when a locally-modified file *differs between the two branches*.
`fix/gemini-key-remote-config` is currently equal to `develop`, and none of the eight modified paths
is touched by the one commit on `feat/beta-analytics-instrumentation` (that commit touches
`presentation/viewmodel/` files and `core/analytics`; the G15 diff touches `data/repository/`,
`data/service/` and one `build.gradle.kts`). No overlap ⇒ no conflict ⇒ clean carry-over.

**If `git checkout` errors** for any reason, stop and do **not** force it. Recover with:
`git stash push -m g15` → `git checkout fix/gemini-key-remote-config` → `git stash pop`. Never use
`git checkout -f`, `git reset --hard`, or `git clean` — the fix exists only in the working tree and
is unrecoverable if discarded.

**Do NOT commit these two paths into the G15 PR:**
- `.specs/MVP-ROADMAP.md` — uncommitted `pm` work (the Beta Launch Priority section). It rides along
  in the working tree; leave it unstaged, it belongs to the `pm`'s own PR.
- `.claude/agents/` — untracked, separate concern.

Stage explicitly. Do not use `git commit -a` or `git add .`.

#### Step 1 — review verdict on the existing diff

The eight modified paths are:

```
feature/ai-agent/.../data/service/AiAgentFactoryImpl.kt        (+ its test)
feature/products/build.gradle.kts
feature/products/.../data/repository/ProductRepositoryImpl.kt  (+ its test)
feature/products/.../data/repository/RecipeRepositoryImpl.kt   (+ its test)
```

**All three call sites from the roadmap are covered — confirmed by grep.** The roadmap named
`ProductRepositoryImpl`, `RecipeRepositoryImpl` and `GeminiAiAgent.kt:29-30`. The third is handled
*indirectly and correctly*: `GeminiAiAgent` already had an `apiKey: String = BuildConfig.GEMINI_API_KEY`
constructor default, and the fix injects the resolved key from `AiAgentFactoryImpl` — the only
construction site. `GeminiAiAgent.kt` itself therefore needs no edit, and the call
`GeminiAiAgent(session, registry, apiKey = geminiApiKey)` compiles (named argument skips `modelName`).
After the fix, no production code path reaches `BuildConfig.GEMINI_API_KEY` except as a declared
fallback default. Wiring is sound: `feature/ai-agent` already depended on `:core:remote-config`,
`feature/products` correctly gains it, and `RemoteVariableService` is bound in
`RemoteConfigModule` (`SingletonComponent`), so Hilt resolves.

**Fallback semantics verified as safe:** `FirebaseRemoteConfigService.getString` returns the caller's
`default` when `value.source == VALUE_SOURCE_STATIC`, i.e. when the key was never fetched/activated.
So an unconfigured Firebase console cannot break AI — it falls back to the compiled key.

#### Step 2 — close these four gaps before opening the PR

1. **No regression test proves the fix.** All four test edits merely widen constructors with mocks;
   none asserts the behaviour G15 exists to guarantee. Add, for `ProductRepositoryImpl`,
   `RecipeRepositoryImpl` and `AiAgentFactoryImpl`: (a) a remote value is used when present,
   (b) `BuildConfig.GEMINI_API_KEY` is used when the remote value is absent. Without this the fix can
   silently regress. This is the main reason the PR is not ready.
2. **Blank-value guard.** `getString` only falls back on `VALUE_SOURCE_STATIC`. A key published to the
   console as an empty or whitespace string is returned verbatim, producing an empty API key and
   breaking all AI at once — the exact incident this fix is supposed to make survivable. Harden each
   call site to `.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY`, or add the guard inside
   `getString`. Prefer the call sites; changing `core/remote-config` widens the blast radius.
3. **`relaxed = true` weakens the factory test.** `AiAgentFactoryImplTest` uses a relaxed
   `RemoteVariableService`, so `getString` silently returns `""` — meaning the test currently
   exercises the empty-key path without noticing. Stub it explicitly.
4. **Document the rotation limitation in the PR description.** `ProductRepositoryImpl` and
   `RecipeRepositoryImpl` are `@Singleton` (`ProductDataModule.kt:19-25`) and hold `generativeModel`
   `by lazy`, so a rotated key only takes effect **after an app restart**, not on the next
   `fetchAndActivate`. `AiAgentFactoryImpl.create()` re-reads per call and is unaffected. Restart-scoped
   rotation is acceptable for the beta — the goal was "rotate without shipping a release", which is
   met — but it must be written down, not discovered during an incident. Do **not** refactor the
   singletons in this PR.

Also state plainly in the PR description: the compiled fallback means the key **is still extractable
from the APK**. The mitigation this delivers is *fast rotation*, not *secrecy*. Revoking the old key
at the provider remains a manual step for bruno.

#### Step 3 — PR

Open `fix/gemini-key-remote-config` → `develop`. Title referencing G15. Request `tech-lead` review.

---

### T2 — G13: debounce the QR-join scanner `[BLOCKER]`

- **Owner:** `android-engineer-features`
- **Branch:** `fix/qr-join-scan-debounce` off `develop`
- **Depends on:** **T1 committed and its PR open.** Not merged — open is enough. The constraint is to
  keep the G15 diff from being absorbed into a second uncommitted pile, which is exactly how it ended
  up on the wrong branch. Do not start T2 while T1 is uncommitted.
- **Files:** `feature/shopping/.../presentation/viewmodel/ScannerViewModel.kt` (primary),
  `feature/products/.../presentation/components/scanner/BarcodeAnalyzer.kt`,
  `feature/shopping/.../presentation/components/scanner/QrCodeScanner.kt`

Spec before implementing. Two defence layers, both wanted:

1. **In-flight/one-shot guard in `ScannerViewModel`** — the authoritative fix. `onTokenScanned`
   currently launches a coroutine per call with no guard (`ScannerViewModel.kt:38-58`). Reject
   re-entry while a join is in flight, and after a successful join (the screen navigates back
   anyway). Prefer a ViewModel-held flag or a single tracked `Job` over a UI-layer-only debounce —
   state that survives recomposition is the point.
2. **Throttle in `BarcodeAnalyzer`** — defence in depth: ignore repeats of the same `rawValue` inside
   a short window.

Careful points:
- `BarcodeAnalyzer` lives in `feature/products` but its only consumer is `feature/shopping`. Changing
  its semantics is a cross-module behavioural change. Keep the change conservative and note the
  AD-005 violation in the PR; **do not** attempt to relocate the component here — that is G10 work.
- Preserve analytics: `shopping_list_joined` / `shopping_list_join_failed` must still fire exactly
  once per genuine attempt. Suppressed duplicate frames must not emit events. Coordinate with
  `data-engineer` if event semantics need to change.
- Failure must not deadlock the screen: after a *failed* join the user has to be able to rescan.
  Release the guard on failure; hold it on success.

Regression test required in the same PR: repeated `onTokenScanned` with the same token invokes
`ShoppingJoinUseCase` exactly once; a rescan after failure is allowed.

---

### T3 — Jacoco baseline + critical-path coverage `[BLOCKER — the coverage half]`

- **Owner:** `android-engineer-quality`
- **Branch:** `chore/jacoco-coverage-baseline` off `develop`
- **Depends on:** nothing — **runs in parallel with T1 and T2, starting now.** Different files.

1. Configure Jacoco per `.agents/skills/testing-setup/SKILL.md`. Coverage is currently not measured
   at all, only inferred from file counts (~402 `src/main` vs. ~132 `src/test`, 1 `src/androidTest`).
   Produce a real per-module baseline first — the number is the deliverable, not a target.
2. Then raise coverage on the purchase/share/login critical paths the `pm` flagged, prioritising
   `domain/` (UseCases) and `data/` (Repositories) over `presentation/`, in `feature/shopping`,
   `feature/products`, `feature/cart`, `feature/profile`.
3. **Do not** write G13/G15 regression tests here — those ship inside T1/T2. Avoid the collision.
4. Backfill a regression test for G11 (camera executor shutdown, fixed earlier with no test attached).
5. `.maestro/` flows may be edited statically, but every flow stays "authored, not verified" until it
   runs on a device. Never report Maestro as passing from this environment.

Split into a baseline PR and one or more coverage PRs if the diff gets large. Do not bundle a
tooling change with a large test addition.

---

### T4 — G16: debounce `ProductSearchViewModel.search()` `[WINDOW]`

- **Owner:** `android-engineer-features`
- **Branch:** `fix/product-search-debounce` off `develop`
- **Depends on:** **`feat/beta-analytics-instrumentation` merged into `develop`.** That branch already
  modifies `ProductSearchViewModel.kt` and its test. Branching before it lands guarantees a conflict.
  Also start after T2 is committed.
- **File:** `feature/products/.../presentation/viewmodel/ProductSearchViewModel.kt:55-90`

Debounce + cancel-previous-`Job`. Do not let a stale response overwrite `_uiState` for a query the
user has moved on from. Preserve the `product_search_performed` event semantics — after debouncing,
one event per settled query, not per keystroke; confirm the intended granularity with
`data-engineer` before changing it.

Regression test: rapid successive queries leave state reflecting the last query only.

---

### T5 — G14: `ProfileViewModel.observeProfile()` discards its emission `[WINDOW]`

- **Owner:** `android-engineer-features`
- **Branch:** `fix/profile-observe-emission` off `develop`
- **Depends on:** T4 committed.
- **File:** `feature/profile/.../presentation/viewmodel/ProfileViewModel.kt:47-54`

Confirm `UserProfile` vs. `authService.currentUser` field parity **before** merging the two sources —
that reconciliation is the actual risk, not the Flow plumbing. If parity is imperfect, spec the
precedence rules explicitly in the PR rather than guessing.

---

### T6 — G12: `CartViewModel.observeData()` duplicate collectors `[WINDOW]`

- **Owner:** `android-engineer-features`
- **Branch:** `fix/cart-observe-flatmaplatest` off `develop`
- **Depends on:** T5 committed. **Last of the bug fixes, deliberately.**
- **File:** `feature/cart/.../presentation/viewmodel/CartViewModel.kt:98-127`

`flatMapLatest` instead of a nested `launch` per settings emission. The roadmap already calls this
"moderate risk, touches the cart's core observe loop". Ship it alone; do not bundle anything else
touching cart. Note that `feat/beta-analytics-instrumentation` touched `FinishPurchaseViewModel` in
the same module — rebase on current `develop` first.

Regression test: N settings emissions produce exactly one active product collector.

---

### Not assigned to the Android engineers

| Item | Owner | Note |
|---|---|---|
| **G9** | bruno | Fix done and tested — PR #67. Merge decision, not more code. **Do not duplicate.** |
| **G10** | `tech-lead` | Design pass only, post-beta. Two concrete instances now logged: `feature/shopping` → `feature/products` `CameraPreview`, and the `feature.settings` imports. Engineers must not opportunistically "fix" these inside T1–T6. |
| Analytics | `data-engineer` | Committed on `feat/beta-analytics-instrumentation`, tests green. Needs the `BarcodeAnalyzer` doc correction (§2 above) and a PR into `develop`. |
| Store/beta readiness, KPIs | `marketing` | `.specs/BETA-KPI.md` in progress — `tech-lead` must not create or edit that file. |

---

## What's genuinely blocked — needs bruno, not more agent work

| Item | What's needed | Notes |
|---|---|---|
| **G9** | Review/merge PR #67 — **after T7 turns it green** | Superseded by revision 3: the fix is done and tested, but CI is red (one Detekt line-length violation) and the branch is 18 commits behind. Not mergeable today. |
| **G3** | Pick a host for `docs/legal/privacy.html` / `terms.html` (`cestou.app` vs. GitHub Pages) | Blocks wiring the URL into `CustomMethodPickerTerms`, Settings, and the Play Console listing — also a Play Store submission requirement. |
| **G4 (remainder)** | Screenshots + feature graphic | Needs a real device/emulator — none available in this environment. |
| **G5** | Manually confirm Firestore rules allow a user to create a `notifications` doc addressed to someone else | Lives in the Firebase Console, outside this repo. |
| **F0.3** | Run `maestro test .maestro/test_suite.yaml` on a real device | No adb/emulator here. |
| **F2.2** | Google Sign-In QA pass on a real device | Config verified correct statically; live flow (SHA-1, OAuth consent screen) unverified. |
| Gemini key rotation | After T1 merges, publish `gemini_api_key` in the Firebase console and revoke the compromised/compiled key at the provider | The code change alone does not rotate anything. |
| Release branching | Decide whether the beta ships from `develop` directly (Play Console internal testing) or needs `develop` → `master` first | Yours alone, same category as the original G1 merge decision. |

---

## Definition of "ready for beta" — re-checked 2026-09-10

Split per the `pm` pass (see revision note §1). Owned by `pm`, cross-checked with `tech-lead`.
Every line below is graded against `origin/develop` and the live PR state, not against intent.

Legend: **[CLOSED]** verified on `develop` · **[IN PROGRESS]** engineering work still owed ·
**[BLOCKED — bruno]** no agent can advance it.

### Gates go/no-go — must all be true before inviting real customers

- [x] **G15 code** — **[CLOSED — PR #69]**. All three call sites read `RemoteVariableKeys.GEMINI_API_KEY`
      with `.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY`. AD-008 recorded.
- [ ] **G15 rotation** — **[BLOCKED — bruno]**. Publish `gemini_api_key` in Firebase Remote Config and
      **revoke the compiled key at Google AI Studio**. Until the old key is revoked, G15's actual risk
      (an extractable key on testers' devices) is unchanged — the code only made rotation *possible*.
      This is the half of G15 that matters for go/no-go.
- [x] **G13** — **[CLOSED — PR #73]**. `ScannerViewModel.isJoining` guard + `BarcodeAnalyzer` throttle;
      guard released on failure, held on success. Not exercised on a device (no camera here).
- [ ] **G9 (PR #67)** — **[IN PROGRESS → then BLOCKED — bruno]**. Not merge-ready: Detekt fails on one
      >120-char line in `ShoppingRepositoryImplTest.kt:220`, and the branch is 18 commits behind
      `develop`. **T7** fixes both; the merge itself stays bruno's.
- [ ] **G3** — **[BLOCKED — bruno]**. Decide `cestou.app` vs. GitHub Pages. Then the URL must be wired
      in **three** places, not one: `CustomMethodPickerTerms`, Settings, and the Play Console
      "Privacy Policy URL" field. The wiring is a small engineering task that cannot start before the
      decision.
- [ ] **G4 (remainder)** — **[BLOCKED — bruno]**. Screenshots (min. 2) + 1024×500 feature graphic.
      Needs a device; Play Console refuses to publish even a test track without them.
- [ ] **G5** — **[BLOCKED — bruno]**. Confirm in the Firebase Console that the rules let a user create
      a `notifications` doc addressed to another user. Cheap to check, high downside if wrong — this
      is the first rule the app exercises against other people's real data.
- [ ] **F0.3** — **[BLOCKED — bruno, partially in progress]**. Draft PR #75 fixed two real suite bugs
      (`clearState` logging the session out; hardcoded English selectors → `testTag`). Only
      `onboarding_flow.yaml` has ever passed end-to-end. The other 8 flows need an authenticated
      session on the device, whose wireless adb keeps dropping. **Nobody has yet seen the core loop
      pass end-to-end on hardware.**
- [ ] **F2.2** — **[BLOCKED — bruno]**. Google Sign-In on a device. Config is statically correct;
      SHA-1 registration and the OAuth consent screen are unverified. Also a hard prerequisite for
      F0.3, since the suite needs a logged-in session.
- [x] **Analytics instrumented** — **[CLOSED — PR #71]**. Key-flow events exist and are unit-tested.
- [ ] **Analytics verified in DebugView** — **[BLOCKED — bruno]**. Zero events confirmed in DebugView
      so far (`ANALYTICS-PLAN.md` says so explicitly). Fold this into the F0.3 device pass: walk each
      funnel once with DebugView open. Shipping unverified analytics means a beta that cannot be
      measured — which is the reason to run a beta instead of just releasing.
- [x] **Coverage baseline + critical-path coverage** — **[CLOSED — PR #72]**. Kover, not Jacoco, and it
      was already configured with an 80% CI gate; the deliverable was the first real measurement:
      **82.00% → 84.47%** line (branch 46.9% → 48.9%). Zero-coverage gaps closed in `CloudNetwork`,
      four `feature/products` use cases, and `core/auth`'s `authState`. Known remaining hole:
      `ProductRepositoryImpl`/`RecipeRepositoryImpl` Gemini paths (~55%/34%), untestable until the
      `GenerativeModel` is injected — a natural follow-up now that G15 has touched that construction.
- [ ] **Play Console beta track + release notes** — **[IN PROGRESS / BLOCKED — bruno]**. `marketing`
      delivered the checklist, invite copy and pt-BR release notes (`BETA-STORE-READINESS.md`). Every
      remaining step is inside the Console: create the app, Internal testing track, upload a signed
      build, tester list, Data Safety form, content rating.
- [ ] **Beta success thresholds** — **[IN PROGRESS]**. `.specs/BETA-KPI.md` exists with concrete floors
      (activation ≥60%, purchase completion ≥50%, join success ≥70%) but is still marked
      "Draft — para revisão do `pm`". Needs `pm` sign-off, not new work.

### Scheduled inside the beta window — does **not** gate go/no-go

- [x] **G16** — **[CLOSED — PR #77]** · unit tests only, no device verification
- [x] **G14** — **[CLOSED — PR #79]** · unit tests only, no device verification
- [x] **G12** — **[CLOSED — PR #78]** · unit tests only, no device verification

All three landed inside the same round as the blockers rather than during the beta window — better
than planned. Recorded honestly: their evidence is JVM unit tests plus review (see revision 3 §3).
Accepted for their risk class; do not restate them elsewhere as "verified".

### Residual risks — known, deliberately not promoted to gates

Listed so they are decided rather than forgotten. None of these is a new checklist item; each needs a
call from `pm`/bruno before it becomes one.

1. **Hardcoded `Text("Checkout")`** in `feature/cart/.../components/CartBottomBar.kt:64` — the primary
   finish-purchase button, on the flow BETA-KPI §2 measures, showing English to a pt-BR cohort.
   Cheapest real-persona-visible fix on this page. Same class: `SettingsHeader.kt:51` and
   `LinkWearDeviceScreen.kt:44` have literal `"Voltar"` content descriptions.
2. **`MobileRoutes.Notifications` has no reachable entry point** — a destination nothing navigates to.
   Dead route, not a user-facing break; relevant because G5's notifications land nowhere visible.
3. **G15 rotation is restart-scoped** for `ProductRepositoryImpl`/`RecipeRepositoryImpl` (`@Singleton`
   + `by lazy`); `AiAgentFactoryImpl` rotates per call. Acceptable, but an incident response must know
   it means "testers pick up the new key on next app start", not immediately.
4. **`AnalyticsTracker.setUserId` is unplugged** — cohort behaviour is countable in aggregate but no
   individual tester journey can be reconstructed. This shapes what BETA-KPI can honestly claim.
5. **No device verification exists for anything merged this round except G11.** CI has no emulator
   step for these paths; the first hardware exercise of G12/G13/G14/G16 will be the beta itself.

### Post-beta

- **G10** and F3.1/F3.2/F3.3/F3.5 — architecture debt and polish. No persona impact. Not in scope.
  G10 now has an owner: the `android-engineer-architecture` subagent, added this round.

### Bottom line

Engineering is done for the gate: **7 code items merged (#69, #71, #72, #73, #77, #78, #79)** and one
5-minute unblock left (**T7**, PR #67). Every other open gate needs bruno with a device, a Firebase or
Play console, or a hosting decision. **The beta is not ready to invite real customers** — not because
code is missing, but because the core loop has never been executed on hardware (F0.3/F2.2), the
compromised Gemini key has not been revoked, and there is no legal-page URL to publish a listing with.
