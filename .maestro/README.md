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
is verifying the text.

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
- Every other flow assumes an already-authenticated session and uses plain `launchApp` (no
  `clearState`). If the device isn't logged in, they fail immediately on the first post-login
  assertion — that's expected, not a suite bug. Log in manually via "Continuar com Google" on the
  device, then re-run.

## Test Flows

The full regression suite (`test_suite.yaml`) runs these flows in order. The order matters:
`create_list_flow` creates the "value" list fixture that `product_management_flow`,
`cart_interactions_flow`, and `finish_purchase_flow` all depend on; `cart_interactions_flow` must
run before `finish_purchase_flow`, which finishes (locks) the list, so nothing after it may rely
on it staying editable. All of them require an authenticated session already on the device (see
"Authentication" above) — `test_suite.yaml` does not include `onboarding_flow.yaml`.

- **onboarding_flow.yaml** (not in `test_suite.yaml`, run separately): Tests the pre-login Welcome
  screen (title, description, "Continuar com Google" button, terms footer). Uses `clearState` —
  logs the device out as a side effect.
- **home_flow.yaml**: Tests the home screen's app bar, bottom navigation tabs, and the "Join a
  list" dialog entry point. Does not assert an empty-list state — a real, persisted account may
  already have lists (data syncs from Firestore, not just local storage), so the empty state can't
  be assumed for an authenticated session that isn't freshly created.
- **create_list_flow.yaml**: Tests creating a shopping list ("value") and lands on its (empty) detail screen.
- **product_management_flow.yaml**: Tests adding a product from Suggestions, browsing Search/Recipes, and editing the added product's price.
- **cart_interactions_flow.yaml**: Tests the Share options sheet (Invite to Collaborate / Share as
  Text — opens and closes without acting on either) and the Confirm Item sheet (adds a second,
  unpriced product, marks it purchased, and confirms the price/quantity prompt that triggers).
- **finish_purchase_flow.yaml**: Tests finishing a purchase (total amount, confirm) and confirms the list becomes locked.
- **join_list_flow.yaml**: Tests the "Join a list" dialog's local validation for an invalid/expired code. Joining a real list requires a token generated by another account/device, which is out of scope for a single-device flow.
- **settings_flow.yaml**: Tests reaching Settings from the Profile tab, the section list (General/AI/Shopping/Data/Notifications/Support/About), and the Theme picker.
- **settings_about_flow.yaml**: Tests the About section's Terms, Privacy, Open Source Licenses, and
  Release Notes screens. Does not tap the Support section items — see the flow file header for why.
- **account_data_flow.yaml**: Tests reaching the Data section, that "Delete all data" and "Delete
  Account" open their confirmation sheets, and that Cancel dismisses each without acting. Never
  confirms either action — that would destroy the fixtures the other flows depend on.
- **chat_flow.yaml**: Tests sending a message in the AI Assistant tab, and that the top-bar
  settings icon is reachable by accessibility text.

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

## CI Integration

You can integrate these tests with your CI pipeline. See the [Maestro documentation](https://docs.maestro.dev/getting-started/running-flows-on-ci) for more information.
