# Coverage baseline — Cestou (How-Much)

Status: real, measured baseline (Kover, JVM unit tests only) — first time this number has actually
been computed, not estimated from test-file counts. Update: a real device became available mid-task
(see "androidTest: G11 regression, verified on device" below) — the first `androidTest` in any
library module in this repo now exists and passes.
Owner: bruno
Last updated: 2026-09-09

## Correction to the T3 brief

The task brief for this work item said "Jacoco is not configured anywhere" and estimated coverage
by counting `src/test` files (~132) against `src/main` files (~402). Neither is accurate for this
branch: **[Kover](https://github.com/Kotlin/kotlinx-kover)** (not Jacoco — Kotlin's native coverage
tool, same underlying bytecode-instrumentation approach) has been configured since
`a01a97ca` ("feat: aumento de cobertura de testes", 2026-08-23) via a Gradle convention plugin at
`buildSrc/src/main/kotlin/howmuch.kover.gradle.kts`, applied at the root `build.gradle.kts` as
`id("howmuch.kover")`. It:

- Aggregates coverage across every `:app`, `:core:*`, and `:feature:*` module (not `:wear` or
  `:baselineprofile` — those ship Macrobenchmark/instrumented tests, not JVM unit tests).
- Excludes generated code (Hilt/Room/Moshi `_Factory`/`_Impl`/`_HiltModules`/`BuildConfig`), DI
  modules (`*.di.*`), theme tokens, screen-level Composables/components (no dedicated Robolectric
  layout tests yet — tracked as a follow-up, not this task), navigation graph wiring, and a short
  list of framework wrappers that need instrumentation to test meaningfully (CameraX/ML Kit,
  WorkManager, Wear Data Layer, `GeminiAiAgent`/`OpenRouterAiAgent`).
- Enforces an **80% minimum aggregate line-coverage gate** via `kover.reports.total.verify.rule`,
  already wired into CI (`.github/workflows/tests.yml`, job driven by
  `.github/pipeline-config.yaml`'s `coverage.kover` block, command `./gradlew koverVerify`).

So the CI-enforced gate already existed before this branch. What did **not** exist: a real,
documented baseline number (the gate had never actually been read and written down), and any
prioritized push on the domain/data layers of the purchase/share/login-critical features. That's
what this document and the accompanying tests provide.

## How this was measured

```
./gradlew :koverXmlReport :koverHtmlReport :koverVerify
```

Report: `build/reports/kover/report.xml` (machine-readable, used for the numbers below) and
`build/reports/kover/html/index.html` (browsable).

Two local-only gotchas when reproducing outside CI (neither is a repo bug — CI's
`.github/actions/setup-env` already handles both):

- **`local.properties`** (git-ignored) must point `sdk.dir` at a local Android SDK.
- **`.env`** (git-ignored, read by the `secrets-gradle-plugin`) needs real (or dummy, for a local
  coverage run) values for every key in `.env.example` — an empty value there compiles to an
  invalid empty Java literal (`public static final String X = ;`). Do **not** commit a filled-in
  `.env`.
- Run the root-scoped task explicitly as `:koverXmlReport` (leading colon). Gradle's unqualified
  task-selector behavior (`gradle koverXmlReport`, no colon) runs that task in *every* subproject
  that defines it — including `:wear`, which isn't part of the Kover aggregation and needs a
  `wear/google-services.json` CI provides but a bare local checkout doesn't have.

## Aggregate baseline (JVM unit tests only — no Maestro, no instrumented tests)

| | Before this PR | After this PR | Delta |
|---|---|---|---|
| Line coverage | 82.00% (2936 / 3581) | 84.47% (3025 / 3581) | **+2.47 pp** (+89 lines) |
| Branch coverage | 46.9% (818 / 1745) | 48.9% (854 / 1745) | +2.0 pp |
| CI gate (`koverVerify`, min. 80% line) | pass | pass | — |

The 80% gate was already passing before this PR — the goal here was not "clear the gate" but
"spend the coverage budget on the highest-risk code", per the task's actual priority: `domain/` and
`data/` of the purchase/share/login-critical features.

## Per-module snapshot (after this PR)

| Module | Line coverage | Missed / Total |
|---|---|---|
| `core/data` | 93.5% | 31 / 476 |
| `core/domain` | 93.3% | 9 / 135 |
| `core/auth` | 91.2% | 6 / 68 |
| `core/analytics` | 90.0% | 3 / 30 |
| `core/ai` | 78.2% | 12 / 55 |
| `core/remote-config` | 75.6% | 11 / 45 |
| `core/ui` | 72.8% | 31 / 114 |
| `core/navigation` | 68.3% | 20 / 63 |
| `core/common` | 60.4% | 21 / 53 |
| `feature/settings` | 96.5% | 16 / 451 |
| `feature/profile` | 96.8% | 1 / 31 |
| `feature/shopping` | 92.1% | 52 / 659 |
| `feature/cart` | 90.3% | 20 / 206 |
| `feature/chat` | 89.7% | 6 / 58 |
| `feature/ai-agent` | 84.3% | 11 / 70 |
| `feature/products` | 71.4% | 273 / 955 |
| `feature/auth` | 61.1% | 28 / 72 |

`feature/products` is the module dragging the aggregate down the most, almost entirely because of
`ProductRepositoryImpl`/`RecipeRepositoryImpl`'s Gemini-calling code paths (see "Deliberately not
touched" below).

## Critical paths: `domain/` (UseCases) and `data/` (Repositories)

Per the task's priority order, `feature/shopping`, `feature/products`, `feature/cart`, and
`feature/profile` are the purchase/share/login-relevant features. Two of them
(`feature/cart`, `feature/profile`) **don't have their own `domain/`/`data/` packages** — per
AD-005/AD-007 they call `core/domain` repository interfaces and `core/data` implementations
directly, with no feature-local UseCase/Repository layer. Their business logic lives in
`presentation/viewmodel` instead, which is already well covered (`CartViewModel` 93.7% line,
`ProfileViewModel` 100%) and was left alone here (already good, and out of this task's stated
`domain/`/`data/` priority).

### `feature/shopping/domain/usecase` — 92.3% → 99.3% (after)

All ten use cases are at 100% except `ShoppingCreateUseCase` (96.0%, one untested branch: the
`context.getString(...)` fallback when the AI agent doesn't supply a title) and
`ShoppingUpdateUseCase` (94.1%, pre-existing gap, untouched). `ShoppingJoinUseCase` — the
list-sharing/collaboration entry point — was already at 100%.

### `feature/shopping/data/repository` + `data/model` — already 100% / 75.6% → 100%

`ShoppingRepositoryImpl` was already fully covered. `UserModel` (100% → 0% → 100%, see below) is
the Firestore-facing shape of each list member (owner + collaborators) for the sharing/join flow.

### `feature/products/domain/usecase` — 79.2% → 90.1% (after)

Four use cases had **zero** tests before this PR: `CommonProductAddAllToShoppingUseCase` (the
"add my basic basket to this shopping list" purchase-flow action, also exposed to the AI agent),
`CommonProductAddUseCase`, `CommonProductGetAllUseCase`, `CommonProductRemoveUseCase`. All four are
now at 100%. The remaining gap in this package (`RecipeSearchUseCase`, `ShareShoppingUseCase`,
`ShoppingClearPurchasedUseCase` — 4 lines total) is pre-existing and untouched.

### `feature/products/data/repository` — 55.3% (untouched, see below)

### `core/data/cloud` — 0% → 88.9%

`CloudNetwork` is the `NetworkService` implementation every Firestore-REST-backed repository in
`feature/shopping`/`feature/products` goes through for reads/writes and real-time `observe()` (the
transport underneath list creation, product CRUD, and collaboration). It had **zero** unit
coverage before this PR — a regression here would have silently broken every network call in the
app. Added `CloudNetworkTest` using Ktor's `MockEngine` (new test-only dependency,
`ktor-client-mock`, same `ktor` version already pinned in the catalog): success responses, non-2xx
responses, exceptions, header/query/method/payload wiring, and the `observe()` wrapper.

### `core/auth` (login) — 86.8% → 91.2%

`FirebaseAnonymousAuthentication.authState` — the reactive login state every screen that gates on
"is the user signed in" observes — was defined but never actually collected in a test; only the
synchronous `currentUser` getter (which duplicates the same merge logic) was. Added three cases to
the existing `FirebaseAnonymousAuthenticationTest`: Firebase-user-only, synced-id-only (no Firebase
session), and both present (linked-account id override).

## Deliberately not touched: `ProductRepositoryImpl` / `RecipeRepositoryImpl` Gemini paths

`ProductRepositoryImpl.Helper` (3.2%), `RecipeRepositoryImpl` (33.9%) and its `Helper` (0%) remain
under-covered. Every uncovered line sits behind `generativeModel.generateContent(...)`, where
`generativeModel` is a `GenerativeModel` built inline (`by lazy { GenerativeModel(modelName =
BuildConfig.GEMINI_AGENT, apiKey = BuildConfig.GEMINI_API_KEY) }`) — not constructor-injected, so
it can't be swapped for a fake without a DI refactor of both repositories.

This is exactly **G15** in `.specs/MVP-ROADMAP.md` ("Gemini API key compiled into the APK, no
remote-rotation path... needs its own PR wiring the repositories to read from Remote Config").
Per the `BETA-LAUNCH-PLAN.md` instruction for this task ("don't add regression tests for G12–G16 —
that's each fix's own responsibility when it merges"), this branch does not touch that code. Once
G15 lands (presumably injecting the `GenerativeModel`/a thin wrapper interface behind Hilt), the
`Helper` prompt-building/JSON-parsing functions and the suggestion/translation/image-analysis flows
become trivially unit-testable — flagging that as a natural coverage follow-up for whoever picks up
G15, not duplicating it here.

## Not touched, low value

- `core/domain/services/ImageAnalyzerService.ImageAnalysisResult` and the `StorageService.observe`/
  `.get` reified extensions (0%/2 lines): plain data-class boilerplate (`equals`/`hashCode`/`copy`)
  and inline-function artifacts respectively — not business logic worth a dedicated test.
- `core/ui`, `core/navigation`, `core/common`, `feature/auth` remain the lowest-covered modules but
  fall outside this task's stated priority (`domain/`+`data/` of shopping/products/cart/profile);
  left for a future coverage pass.

## androidTest: G11 regression, verified on device

`.specs/BETA-LAUNCH-PLAN.md`'s real T3 task queue (revision 2, `docs/beta-launch-planning` PR #70 —
not yet on `develop`, hence not visible earlier in this branch's history) has a fourth item this
document didn't originally cover: "backfill a regression test for G11 (camera executor shutdown,
fixed earlier with no test attached)". A real device (Samsung SM-A146M, Android 15) became available
mid-task, so this was closed with an actual instrumented test instead of being deferred again:

- `CameraPreview` (feature/products) gained a default-valued `executorFactory` testing seam (zero
  behavior change for the one production call site) so a test can inject a trackable
  `ExecutorService`.
- `CameraPreviewExecutorLifecycleTest` (new, `feature/products/src/androidTest/`) mounts/disposes
  the composable 4 times and asserts every executor it created is shut down. **Verified to actually
  catch the regression**: temporarily reverting the `DisposableEffect` fix and rerunning on-device
  fails with `"Executor #0 created by CameraPreview was not shut down ... this is the G11 thread
  leak regressing"`; restoring the fix passes.
- Getting this to run at all surfaced a real, repo-wide gap: **every library module's `androidTest`
  crashes at process start** with `IllegalStateException: Default FirebaseApp is not initialized`.
  Only `:app`/`:wear` apply the Google Services plugin, but `core/common`'s `FirebaseInitializer`
  androidx.startup entry is merged into every module's manifest, and `TimberInitializer`,
  `AuthInitializer` (core/auth) and `RemoteConfigInitializer` (core/remote-config) each declare it as
  an androidx.startup dependency — androidx.startup constructs declared dependencies transitively
  **regardless of manifest meta-data removal**, so all five initializers had to be stripped via a new
  `feature/products/src/androidTest/AndroidManifest.xml` (scoped to that module's isolated test APK
  only). This is almost certainly why the repo had exactly one `androidTest` file before this PR
  (`app/src/androidTest/.../ExampleInstrumentedTest.kt`, unused boilerplate) — **any future
  `androidTest` in a `core/*` or `feature/*` module will hit this same crash** and needs the same
  manifest pattern.
- Device also needed two one-time adb settings changes to run any Compose instrumented test at all
  (`always_finish_activities` Developer Option was `1`, and the screen was dozing/asleep, which
  `ActivityScenario` can't launch into): `adb shell settings put global always_finish_activities 0`
  and waking the screen. Neither is a code change; noting it here so whoever runs the next
  instrumented test on this device doesn't have to rediscover it.
- Not run: the `.maestro/` suite (F0.3) — out of scope for this task even with a device now reachable
  (owned by bruno per `STATE.md`/`MVP-ROADMAP.md`, and this session only validated one unit-level
  Compose component, not an end-to-end flow).

## Maestro

No `.maestro/` flows were added or changed in this PR — this was a JVM-unit-test-only task. Per
`.specs/MVP-ROADMAP.md` (F0.3) and `.specs/STATE.md`, the Maestro suite still has never executed in
this environment (no adb/emulator) and running it remains bruno's responsibility on a real device.

## New test-only dependency

`ktor-client-mock` (`io.ktor:ktor-client-mock:2.3.2`, matching the existing `ktor` version in
`gradle/libs.versions.toml`) was added as a `testImplementation` in `core/data/build.gradle.kts` —
the standard Ktor testing fake for exercising an `HttpClient`-based adapter (`CloudNetwork`)
without a real network call, consistent with the `testing-setup` skill's "fake before mock, mock
only when a fake isn't possible" guidance (Ktor's `HttpClient` is a framework dependency we don't
own, so `MockEngine` is the framework-provided fake for it).

## Files touched

New tests:
- `feature/products/src/test/.../domain/usecase/CommonProductAddAllToShoppingUseCaseTest.kt`
- `feature/products/src/test/.../domain/usecase/CommonProductAddUseCaseTest.kt`
- `feature/products/src/test/.../domain/usecase/CommonProductGetAllUseCaseTest.kt`
- `feature/products/src/test/.../domain/usecase/CommonProductRemoveUseCaseTest.kt`
- `feature/shopping/src/test/.../data/model/UserModelTest.kt`
- `core/data/src/test/.../cloud/CloudNetworkTest.kt`

Extended existing tests:
- `core/auth/src/test/.../FirebaseAnonymousAuthenticationTest.kt` (`authState` coverage)
- `feature/shopping/src/test/.../domain/usecase/ShoppingGetByIdUseCaseTest.kt` (`execute()`)
- `feature/shopping/src/test/.../domain/usecase/ShoppingDeleteUseCaseTest.kt` (`execute()`)
- `feature/shopping/src/test/.../domain/usecase/ShoppingMultiDeleteUseCaseTest.kt` (`execute()`)
- `feature/shopping/src/test/.../domain/usecase/ShoppingCreateUseCaseTest.kt` (`execute()`)

New instrumented test (verified on a real device, see above):
- `feature/products/src/androidTest/java/.../scanner/CameraPreviewExecutorLifecycleTest.kt`
- `feature/products/src/androidTest/AndroidManifest.xml` (strips Firebase-dependent startup
  initializers from this module's isolated test APK)

Config:
- `gradle/libs.versions.toml` (added `ktor-client-mock` library entry)
- `core/data/build.gradle.kts` (added the `testImplementation`)
- `feature/products/build.gradle.kts` (added `androidTestImplementation`s + the `executorFactory`
  testing seam in `CameraPreview.kt`, default-valued, no behavior change)

No changes to `buildSrc/src/main/kotlin/howmuch.kover.gradle.kts`, `.github/workflows/tests.yml`,
or `.github/pipeline-config.yaml` — the coverage gate and CI wiring were already correct.
