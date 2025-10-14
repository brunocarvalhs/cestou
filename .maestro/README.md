# Maestro Tests for FriendsSecrets

This directory contains Maestro tests for the FriendsSecrets app.

## Prerequisites

- Install Maestro: `curl -fsSL "https://get.maestro.mobile.dev" | bash`
- Add Maestro to your PATH: `export PATH="$PATH":"$HOME/.maestro/bin"`
- Have an Android emulator or device connected

## Running Tests

To run all tests:

```bash
maestro test .maestro/test_suite.yaml
```

To run a specific test:

```bash
maestro test .maestro/flows/login_flow.yaml
```

## Test Flows

- **anonymous_login_flow.yaml**: Tests the anonymous login flow
- **login_flow.yaml**: Tests the phone verification login flow
- **home_flow.yaml**: Tests the home screen functionality
- **create_group_flow.yaml**: Tests the group creation flow
- **settings_flow.yaml**: Tests the settings screens

## CI Integration

You can integrate these tests with your CI pipeline. See the [Maestro documentation](https://docs.maestro.dev/getting-started/running-flows-on-ci) for more information.