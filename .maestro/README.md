# Maestro Tests for Cestou (How-Much)

This directory contains Maestro tests for the Cestou app (`br.com.brunocarvalhs.howmuch`).

## Prerequisites

- **Java JDK**: Maestro requires Java. Ensure `JAVA_HOME` is set.
  - On macOS: `export JAVA_HOME=$(/usr/libexec/java_home)`
- **Maestro CLI**: `curl -fsSL "https://get.maestro.mobile.dev" | bash`
- **PATH**: Add Maestro to your PATH: `export PATH="$PATH":"$HOME/.maestro/bin"`
- **Running Emulator/Device**: Have an Android emulator or device connected, with a debug build installed.

## Running Tests

Prefer the wrapper script over calling `maestro test` directly — it resolves the device's real
locale for the couple of assertions that need it (see "Language / locale" below) instead of
assuming one:

```bash
.maestro/scripts/run.sh                              # full regression suite
.maestro/scripts/run.sh flows/home_flow.yaml          # a specific flow
```

Calling `maestro test` directly still works for every flow whose assertions are all `id:`-based
(most of them, see below) — it only matters for `join_list_flow.yaml`, which needs
`-e JOIN_ERROR_TEXT=...` supplied (the script does this for you):

```bash
maestro test .maestro/test_suite.yaml
maestro test .maestro/flows/home_flow.yaml
```

## Language / locale

**Rule: `testTag`/`id:` for anything structural, plain text only for content that's actually under
test.** The app has zero `Modifier.testTag` usage before this suite's first real run (2026-09-09) —
every flow used to anchor on visible display text, which broke two ways depending on which
environment ran it: hardcoded English text failed on a real pt-BR device, and (if "fixed" by
hardcoding pt-BR instead, which this suite briefly did before catching it) would just as surely
fail on a CI emulator defaulting to en-US. Neither language is the "right" hardcode — the suite
needs to not care.

So:

- **Structural assertions** (the right screen was reached, a button/field/tab exists, navigation
  worked) use `Modifier.testTag(...)` added directly to the Compose element, and
  `tapOn:`/`assertVisible: { id: "..." }` in the flow YAML. Tags are scoped to only the elements a
  flow actually touches — this isn't a blanket `testTag` sweep of the whole app. Where the element
  is data-driven (e.g. `SettingItem`/`SettingSection` in `SettingsScreen.kt`), the tag is derived
  from something locale-independent already in the data — the destination route's class name, or
  the string resource's *entry name* (via `context.resources.getResourceEntryName(resId)`) — never
  the localized display text itself.
- **Content assertions** (does this specific error message say the right thing) still use plain
  text, because the text itself is what's under test. There's exactly one of these in the suite
  today: `join_list_flow.yaml`'s invalid-token error. It's parameterized as `${JOIN_ERROR_TEXT}`
  and resolved by `.maestro/scripts/run.sh` from `adb shell getprop persist.sys.locale` — pt-BR
  gets the real pt-BR string, everything else falls back to the English default. If you run that
  flow with plain `maestro test` instead of the wrapper script, pass `-e JOIN_ERROR_TEXT="..."`
  yourself or the assertion will fail on an unresolved `${JOIN_ERROR_TEXT}` literal.
- A few assertions are plain text *and* locale-safe by construction, not because we picked a
  language: `"Cestou"` (app_name is identical across every `values*/strings.xml` in the repo — no
  translation exists to diverge from), user-entered fixture data (`"value"`, `"Oi"`), and
  `"Arroz"` (a hardcoded common-product name in Kotlin source, not a string resource at all).
  `onboarding_flow.yaml` is entirely in this bucket: `feature/auth`'s Welcome-screen strings have
  no `values-*/strings.xml` override, so the single default resource (already Portuguese) renders
  the same regardless of device locale.

If you add a new flow: default to adding a `testTag` at the element you need to select, not to
grepping for the "right" language string. Only reach for plain text when the test's actual point
is verifying the text. **This is a standing convention for this suite going forward, not just
advice for the flows that existed when it was written** — prefer `testTag` over plain text for any
new structural assertion, on principle, even when today's resources happen to make a plain-text
assertion locale-safe (see `login_flow.yaml`'s header for why "safe today" isn't a guarantee).

### FIXED 2026-09-10: `testTag` was invisible to Maestro on a real device — not enabled app-wide

**Found 2026-09-10 while adding `login_flow.yaml`; fixed the same day.** Every `id:`/`testTag`
selector in this entire suite — not just the new ones — used to fail to resolve against a real
device, including selectors that predate this discovery (`nav_ShoppingList`, `join_list_icon`,
`create_list_fab`, every `edit_shopping_*`/`ai_settings_*`/`notification_settings_*` tag, etc.).
This was **not** a per-flow bug; it was a single missing app-wide wiring step.

Root cause: Jetpack Compose's `Modifier.testTag(...)` only exists inside Compose's own internal
semantics tree by default. It is **not** exposed as `resource-id` in the platform accessibility
tree that Maestro (and UiAutomator/Espresso generally) reads on Android, unless a
`Modifier.semantics { testTagsAsResourceId = true }` is applied to a root/ancestor composable.
`grep -rn "testTagsAsResourceId" .` across the whole repo returns zero matches — `app/src/main/
java/.../MainActivity.kt`'s `setContent { CestouApp(...) }` (the app's one Compose root) never
sets it, so no `testTag` anywhere in the app, in any feature module, has ever actually been
selectable by `id:` on a real device.

**Evidence**: adding `welcome_title`/`welcome_description`/`welcome_google_button` `testTag`s to
`feature/auth`'s `WelcomeScreen.kt`/`CustomMethodPicker.kt` (confirmed compiling and rendering
correctly — the elements are visibly on screen in every screenshot) still failed
`assertVisible: { id: "welcome_title" }` in Maestro. Pulling the raw screen hierarchy Maestro
captured, and separately an `adb shell uiautomator dump` of the *authenticated* home screen (where
`nav_ShoppingList` etc. live), both show `resource-id` is empty/absent on every single
app-rendered node — only OS-owned ids like `android:id/content` and
`com.android.systemui:id/...` appear. A one-off diagnostic using a plain-text selector
(`tapOn: "Continuar com Google"`, not committed to the repo) tapped the exact same button
successfully, proving the UI and tap mechanics are fine — this is purely a testability-wiring gap.

**The fix**, landed in `app/.../MainActivity.kt`: `setContent` now wraps `CestouApp` in a `Box`
carrying `Modifier.semantics { testTagsAsResourceId = true }`, gated to `BuildConfig.DEBUG` so
release builds never expose internal test tags as `resource-id` to accessibility services. This
was a deliberate Compose-accessibility-semantics change (it repurposes `resource-id`/traversal
semantics), so debug-only scoping was chosen specifically so it has zero effect on what TalkBack
and other accessibility services see in production — see [Maestro's own Jetpack Compose
guidance](https://docs.maestro.dev/get-started/supported-platform/android/jetpack), which notes
Maestro reads resource identifiers "when available" but doesn't itself enable this wiring; the app
has to opt in.

Confirmed working: `welcome_title`/`welcome_description` (`feature/auth`'s `WelcomeScreen.kt`) now
resolve via `assertVisible: { id: "welcome_title" }` on a real device, where they previously did
not. Before this fix landed, several "passing" flows reported in earlier session handoffs either
never reached their `id:` assertions (blocked by something else first, e.g. the App Check
forced-sign-out) or were not actually re-verified after this class of selector was introduced —
don't take a pre-2026-09-10 "PASS" involving `id:` selectors as current truth; re-run the flow to
confirm.

## Authentication

The app uses real Google Sign-In. Maestro cannot drive the Google account picker/OAuth consent
UI reliably or safely (it's outside the app's control and specific to whichever Google account is
on the test device) — **no flow in this suite attempts it.**

- **`onboarding_flow.yaml`** is the only flow that uses `launchApp: clearState: true`. Clearing
  state logs the device out of its (local-only) Google Sign-In session, so this flow only asserts
  the pre-login Welcome screen and never taps "Continuar com Google". It is deliberately **excluded
  from `test_suite.yaml`** — running it logs the device out, which would break every other flow
  that assumes an authenticated session. Run it on its own, and only if you've confirmed with
  whoever owns the device's session that it's OK to log out (or the session is already logged out).
- **`login_flow.yaml`** goes one step further than `onboarding_flow.yaml`: it also taps "Continuar
  com Google", then stops immediately — it never proceeds into, dismisses, or otherwise interacts
  with the Google account picker/OAuth consent UI that appears next (or, on a device with a single
  already-consented Google account, whatever the OS resolves silently in its place). It does **not**
  use `clearState` — it assumes the device is already on the logged-out Welcome screen. Like
  `onboarding_flow.yaml`, it's deliberately **excluded from `test_suite.yaml`** for the same reason:
  the tap surfaces (or silently resolves through) real account UI mid-run, which the rest of the
  suite isn't prepared for.
- Every other flow assumes an already-authenticated session and uses plain `launchApp` (no
  `clearState`). If the device isn't logged in, they fail immediately on the first post-login
  assertion — that's expected, not a suite bug. Log in manually via "Continuar com Google" on the
  device, then re-run.

## Test Flows

The full regression suite (`test_suite.yaml`) runs these flows in order. The order matters:
`create_list_flow` creates the "value" list fixture that `list_management_flow`,
`product_management_flow`, `cart_interactions_flow`, and `finish_purchase_flow` all depend on;
`cart_interactions_flow` must run before `finish_purchase_flow`, which finishes (locks) the list,
so nothing after it may rely on it staying editable. All of them require an authenticated session
already on the device (see "Authentication" above) — `test_suite.yaml` does not include
`onboarding_flow.yaml`.

- **onboarding_flow.yaml** (not in `test_suite.yaml`, run separately): Tests the pre-login Welcome
  screen (title, description, "Continuar com Google" button, terms footer). Uses `clearState` —
  logs the device out as a side effect.
- **login_flow.yaml** (not in `test_suite.yaml`, run separately): Tests the pre-login Welcome
  screen via `testTag` (`welcome_title`/`welcome_description`/`welcome_google_button`, added
  2026-09-10 specifically for this flow — `feature/auth` had zero `testTag` usage before) and taps
  "Continuar com Google", stopping immediately after — see "Authentication" above and the flow's
  own header comment for exactly what is and isn't in scope past that tap.
- **home_flow.yaml**: Tests the home screen's app bar, bottom navigation tabs, and the "Join a
  list" dialog entry point. Does not assert an empty-list state — a real, persisted account may
  already have lists (data syncs from Firestore, not just local storage), so the empty state can't
  be assumed for an authenticated session that isn't freshly created.
- **create_list_flow.yaml**: Tests creating a shopping list ("value") and lands on its (empty) detail screen.
- **list_management_flow.yaml**: Tests the "value" list card's long-press menu (Editar/Duplicar/
  Compartilhar/Finalizar/Deletar — only Editar is actually entered, the rest are destructive to the
  shared fixture and only asserted present) and, one level deeper, the Edit-list screen plus the
  QR invite screen (`QrCodeBottomSheet`) it opens. This is a second, independent entry point to the
  same QR screen `cart_interactions_flow.yaml` also reaches from the cart's own share icon.
- **product_management_flow.yaml**: Tests adding a product from Suggestions, browsing Search/Recipes, and editing the added product's price.
- **cart_interactions_flow.yaml**: Tests the Share options sheet (Invite to Collaborate, followed
  through to the QR invite screen and back — see "Language / locale" note below on why that's safe
  to do for real — and Share as Text, which opens the OS share sheet and is only asserted present)
  and the Confirm Item sheet (adds a second, unpriced product, marks it purchased, and confirms the
  price/quantity prompt that triggers).
- **finish_purchase_flow.yaml**: Tests finishing a purchase (total amount, confirm) and confirms the list becomes locked.
- **join_list_flow.yaml**: Tests the "Join a list" dialog's local validation for an invalid/expired code. Joining a real list requires a token generated by another account/device, which is out of scope for a single-device flow.
- **settings_flow.yaml**: Tests reaching Settings from the Profile tab, the section list
  (General/AI/Shopping/Data/Notifications/Support/About), the Theme picker, and — new — actually
  drilling into the AI, Shopping, and Notifications screens (model/prompt/creativity fields,
  sorting/reminders fields, and the enable-notifications row respectively). AI and Shopping are
  safe to back out of without mutating anything (their ViewModels re-save the unchanged in-memory
  state on back); Notifications is asserted structurally only and its switch is never tapped,
  because its `onCheckedChange` persists immediately rather than only on save/back.
- **settings_about_flow.yaml**: Tests the About section's Terms, Privacy, Open Source Licenses, and
  Release Notes screens. Does not tap the Support section items — see the flow file header for why.
- **account_data_flow.yaml**: Tests reaching the Data section, that "Delete all data" and "Delete
  Account" open their confirmation sheets, and that Cancel dismisses each without acting. Never
  confirms either action — that would destroy the fixtures the other flows depend on.
- **chat_flow.yaml**: Tests sending a message in the AI Assistant tab, and that the top-bar
  settings icon (`ai_chat_settings_button`) is present (not yet followed into `AiSettings` from
  here — `settings_flow.yaml` covers that screen from the Profile-tab entry point instead).

## Notes / known gaps

- Sharing/collaboration between two real accounts (accepting a real invite token, seeing a second
  member's presence), Wear OS pairing (`PairingCode`/`LinkPhone`/`LinkWearDevice` routes in
  `core/navigation/mobile/MobileRoutes.kt`), and AI photo/barcode recognition are not covered here
  — they need either a second device/account or a live camera feed, which Maestro flows alone
  can't provide.
- `feature/cart`'s `ProductHistoryRoute` (item history sheet) is a special case of the
  two-accounts gap above, not a missing flow: it only renders once a list has 2+ members
  (`CartScreen.kt`'s `showAttribution`), which a single test account can never produce. Its
  sibling destinations `ConfirmItemRoute` and `ShareOptionsRoute` *are* reachable single-account
  and are covered by `cart_interactions_flow.yaml`.
- `core/navigation/mobile/MobileRoutes.kt`'s `Notifications` destination is registered in
  `ShoppingGraph.kt` but (as of 2026-09-09) has no reachable UI entry point anywhere in the app —
  not a Maestro gap, a real app gap worth its own ticket (see `.specs/STATE.md`).
- `feature/settings`'s Support section items (`SupportContact`, `SupportBugReport`,
  `SupportFeedback`, `AppRate`) are reachable from Settings but not covered — each one hands off
  immediately to an external app (email client, Play Store) with no in-app confirmation step to
  assert against; see `settings_about_flow.yaml`'s header comment. Their visibility is covered by
  `settings_flow.yaml`. The rest of the About section (Terms, Privacy, Open Source Licenses,
  Release Notes) *is* covered by `settings_about_flow.yaml`.
- `LanguageSettings` and `CurrencySettings` (both in the General section) render
  `PlaceholderSettingsScreen` — not implemented yet — so there's nothing beyond their list-item
  visibility (already covered by `settings_flow.yaml`) worth asserting.

## Real app bugs found while writing/running these flows (not suite bugs)

Flagged for `tech-lead` triage, not fixed here — same category as the `CartBottomBar.kt:64`
hardcoded-English "Checkout" button found during the original 2026-09-09 testTag pass (see
`.specs/STATE.md` "Residual risks").

- **`testTagsAsResourceId` is never enabled anywhere in the app, so every `testTag`/`id:` selector
  in this suite is currently invisible to Maestro on a real device** (found 2026-09-10 while adding
  `login_flow.yaml`) — see "Language / locale" above ("CRITICAL" subsection) for the full
  root-cause writeup and evidence. One-line fix candidate in `app/.../MainActivity.kt`, but has
  accessibility-semantics implications worth a deliberate `tech-lead`/`android-engineer-features`
  call, not a QA-flow drive-by patch.
- **`EditShoppingViewModel.shareToken()` never updates `EditShoppingUiState.sharingToken`**
  (`feature/shopping`, found 2026-09-10 while adding `list_management_flow.yaml`). It navigates to
  `QrCodeBottomSheet` correctly, but the Edit-list screen's own "already generated a token" Card
  branch in `EditShoppingContent.kt` (guarded by `sharingToken != null`) can never render — the
  field is dead state. Cosmetic (the "Generate token" button just never flips to a token-display
  card after actually generating one), not a functional break, since the QR screen itself still
  works.
- **`onboarding_flow.yaml`'s single-sentence terms assertion doesn't reflect the current UI**
  (found 2026-09-10 re-running the suite after `fix/wire-legal-urls`, PR #83, merged into
  `develop`). `CustomMethodPickerTerms` (`feature/auth`) now renders "Termos de Uso" and "Política
  de Privacidade" as two separately clickable link `Text` nodes instead of one plain sentence —
  intentional UI change, not a bug, but it silently broke this flow's old assertion until this
  session's re-run caught it. Fixed in the flow (see its header); flagging here only as a reminder
  that any future terms-copy change needs the same flow updated again, and that this class of
  drift (a flow's assumption about a screen's exact node structure going stale after an unrelated
  UI PR merges) can happen to *any* plain-text assertion — a reason to keep favoring `testTag` per
  the "Language / locale" section above even where locale isn't the concern.
- **The 2026-09-10 fix above was itself still wrong** (found 2026-09-11 actually running the flow
  on device, not assumed from the previous session's notes):
  `assertVisible: "Ao continuar, você concorda com nossos "` (trailing space) never matched.
  `welcome_terms_prefix` (`feature/auth` `values/strings.xml`) is declared with a trailing space in
  the unquoted XML source, but Android's resource compiler trims leading/trailing whitespace on
  unquoted string resources, so the space never survives into the compiled string — confirmed from
  this run's screen-hierarchy JSON dump, not guessed. Fixed the assertion (dropped the trailing
  space) and switched the two link assertions to `welcome_terms_of_use_link`/
  `welcome_privacy_policy_link` `testTag`s (already present in `CustomMethodPicker.kt`, unused by
  any flow until now) instead of plain text. `onboarding_flow.yaml` now runs and passes end-to-end
  on device — this was also the first on-device confirmation that the 2026-09-10
  `testTagsAsResourceId` fix (see "Language / locale" above) actually works, since it depends on
  `id:` selectors resolving.
- **`login_flow.yaml` had drifted out of scope** (found 2026-09-11 re-reading the flow before
  running it): two lines appended after the `welcome_google_button` tap
  (`waitForAnimationToEnd` + `tapOn: "Sign in"`) actually drove the Google OAuth consent screen's
  own "Sign in" button — directly contradicting this file's own "Authentication" section and the
  flow's header comment, both of which say the flow stops immediately after the in-app button tap
  and never touches the picker. Removed; confirmed on device that the flow now stops with the real
  account picker on screen, untouched, exactly as documented.

## CI Integration

You can integrate these tests with your CI pipeline. See the [Maestro documentation](https://docs.maestro.dev/getting-started/running-flows-on-ci) for more information.
