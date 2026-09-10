# Beta Launch Plan — Cestou (How-Much)

Status: Active — reconciled with the `pm` Beta Launch Priority pass and with the real state of the
working tree.
Owner: `tech-lead` (document), bruno (go/no-go)
Last updated: 2026-09-09 (revision 2)

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

Both Android engineers are idle pending this queue. Tasks are listed in execution order. Follow SDD
(`.agents/skills/spec-driven/SKILL.md`): each task = one spec, one branch off `develop`, one PR into
`develop`.

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
| **G9** | Review/merge PR #67 | Fix is done and tested; a merge decision, not more code. |
| **G3** | Pick a host for `docs/legal/privacy.html` / `terms.html` (`cestou.app` vs. GitHub Pages) | Blocks wiring the URL into `CustomMethodPickerTerms`, Settings, and the Play Console listing — also a Play Store submission requirement. |
| **G4 (remainder)** | Screenshots + feature graphic | Needs a real device/emulator — none available in this environment. |
| **G5** | Manually confirm Firestore rules allow a user to create a `notifications` doc addressed to someone else | Lives in the Firebase Console, outside this repo. |
| **F0.3** | Run `maestro test .maestro/test_suite.yaml` on a real device | No adb/emulator here. |
| **F2.2** | Google Sign-In QA pass on a real device | Config verified correct statically; live flow (SHA-1, OAuth consent screen) unverified. |
| Gemini key rotation | After T1 merges, publish `gemini_api_key` in the Firebase console and revoke the compromised/compiled key at the provider | The code change alone does not rotate anything. |
| Release branching | Decide whether the beta ships from `develop` directly (Play Console internal testing) or needs `develop` → `master` first | Yours alone, same category as the original G1 merge decision. |

---

## Definition of "ready for beta"

Split per the `pm` pass (see revision note §1). Owned by `pm`, cross-checked with `tech-lead`.

### Gates go/no-go — must all be true before inviting real customers

- [ ] **G15** closed (T1 merged) **and** the key rotated + old key revoked in the Firebase/Gemini consoles
- [ ] **G13** closed (T2 merged)
- [ ] **G9** (PR #67) merged
- [ ] **G3** hosting decided and legal links wired in-app
- [ ] **G4** screenshots/feature graphic captured
- [ ] **G5** Firestore rules manually confirmed
- [ ] **F0.3** Maestro suite executed on a real device — first time it has ever run
- [ ] **F2.2** Google Sign-In QA pass on a real device
- [ ] Critical flows instrumented and verified visible in Firebase DebugView (analytics PR merged)
- [ ] Jacoco baseline established and critical-path `domain`/`data` coverage raised (T3)
- [ ] Play Console beta track checklist complete, release notes written
- [ ] Beta success thresholds agreed (`marketing`, `.specs/BETA-KPI.md`)

### Scheduled inside the beta window — does **not** gate go/no-go

- [ ] **G16** closed (T4)
- [ ] **G14** closed (T5)
- [ ] **G12** closed (T6)

Non-blocking is not a synonym for unscheduled: if T4–T6 have not landed by the end of the beta
window, that is a planning failure to raise with `pm`, not an implicit deferral to post-beta.

### Post-beta

- **G10** and F3.1/F3.2/F3.3/F3.5 — architecture debt and polish. No persona impact. Not in scope.

None of the device-dependent items above can be checked off from this environment — they need bruno
with a physical device or emulator.
