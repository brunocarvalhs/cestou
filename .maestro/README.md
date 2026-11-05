# Maestro tests for Cestou (How-Much)

End-to-end flows for the Cestou Android app (`br.com.brunocarvalhs.howmuch`).

All selectors are `testTag`s (exposed as resource-ids), never visible text: the app runs in pt-BR
or English, so text asserts break with the device language. The only text used is data the flow
itself typed (a list name, a message).

## Prerequisites

- **Java JDK** (`export JAVA_HOME=$(/usr/libexec/java_home)`).
- **Maestro CLI**: `curl -fsSL "https://get.maestro.mobile.dev" | bash`, then
  `export PATH="$PATH":"$HOME/.maestro/bin"`.
- **A device or emulator** with the debug build installed: `./gradlew :app:installDebug`.
- **A test account** (e-mail and password). Pass it through the environment, never write it in a
  flow: `-e EMAIL=... -e PASSWORD=...`. If the account has no name yet, the flows fill in the
  name step (`subflows/complete_name.yaml`).

## Running

```bash
# whole suite, in order
maestro test -e EMAIL=you@example.com -e PASSWORD=... .maestro/test_suite.yaml

# one flow
maestro test -e EMAIL=you@example.com -e PASSWORD=... .maestro/flows/05_lists_flow.yaml
```

`workspace.yaml` limits the tests to `flows/`; `subflows/` are only called through `runFlow`.

## Flows

| Flow | Covers |
|---|---|
| `01_auth_flow` | Welcome, e-mail login (wrong password, valid login), password reset and sign-up screens (no account is created), name step |
| `02_home_flow` | Home elements, list filters, search and clear, join-a-list dialog (invalid code), bottom navigation |
| `03_notifications_flow` | Bell, filters, mark all as read |
| `04_history_insights_flow` | History tab and spending analysis (when there are finished purchases) |
| `05_lists_flow` | Create sheet (cancel and create), list detail actions, context menu, edit (budget, categorize), duplicate, delete |
| `06_products_flow` | Product form, category picker, camera step, mark purchased, edit, clear-purchased menu, swipe to delete |
| `07_finish_purchase_flow` | Finish sheet (establishment), locked list, reopen from Home |
| `08_ai_chat_flow` | Open the chat from Home, send a message, history, AI settings, close by button and by back |
| `09_settings_flow` | Theme, language and currency, AI, shopping, notifications, data (cancel only), about |
| `10_subscription_flow` | Subscription card, paywall or manage screen (never buys) |
| `11_share_flow` | Share options sheet, invite QR code, token generation |
| `12_budget_goal_flow` | Monthly goal (set and removed on the test account), Home card, month line on a list |
| `13_ai_notification_flow` | Send an AI request, background the app, reopen on the chat (the notification itself is checked with `adb shell dumpsys notification`) |
| `14_sign_out_flow` | Sign out; must be last, it ends the session |

Flow `01` clears the app state; the others reuse the session (`subflows/ensure_logged_in.yaml`
logs in only if the welcome screen shows up). Flows that create data use lists named
`maestro-<timestamp>` and delete them at the end.

## Safety rules

- The flows never confirm "Delete all data" or "Delete account"; they only open the confirmation
  sheets and cancel. Confirming would destroy the shared test account.
- Nothing buys a subscription.
- Only lists created by the flow itself are deleted.

## Adding tags

When a flow needs an element without a tag, add `Modifier.testTag("<area>_<element>")` in the
Composable. Sheets, dialogs and menus are separate windows, so apply `Modifier.exposeTestTags()`
(`core:ui`) to them; `CestouModalBottomSheet` already does.

## Known gaps

- Real sharing between two accounts, Wear OS pairing and AI photo or barcode recognition are not
  covered: they need a second device or account, or a live camera feed.
- The AI answer is not asserted (it depends on network and provider); only that the message is sent.

## CI

See the [Maestro documentation](https://docs.maestro.dev/getting-started/running-flows-on-ci).
