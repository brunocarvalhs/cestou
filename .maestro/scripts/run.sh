#!/usr/bin/env bash
# Runs the Maestro suite (or a single flow) with locale-dependent content assertions resolved
# from the *actual* connected device's locale, instead of a hardcoded language.
#
# Why this exists: almost every assertion in .maestro/flows/*.yaml is now anchored on a
# `testTag` (see .maestro/README.md, "Language / locale") and doesn't care what language the
# device is set to. A handful of assertions genuinely need to verify real text content (e.g. the
# "invalid token" error message actually says the right thing) — those are parameterized with
# Maestro env vars (`${VAR}` in the flow YAML) and resolved here based on
# `adb shell getprop persist.sys.locale`, not a fixed language.
#
# Usage:
#   .maestro/scripts/run.sh                              # full suite
#   .maestro/scripts/run.sh flows/join_list_flow.yaml     # single flow
#
# Requires: adb on PATH, a connected/authorized device, JAVA_HOME set, maestro CLI installed.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

TARGET="${1:-test_suite.yaml}"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found on PATH. export PATH=\"\$HOME/Library/Android/sdk/platform-tools:\$PATH\" first." >&2
  exit 1
fi

DEVICE_COUNT=$(adb devices | tail -n +2 | grep -c "device$" || true)
if [ "$DEVICE_COUNT" -eq 0 ]; then
  echo "No adb device attached/authorized. Run 'adb devices -l' and fix that first." >&2
  exit 1
fi

LOCALE="$(adb shell getprop persist.sys.locale | tr -d '\r')"
if [ -z "$LOCALE" ]; then
  # Some OEMs only populate persist.sys.locale-language / -country instead.
  LANG_PART="$(adb shell getprop persist.sys.locale-language | tr -d '\r')"
  COUNTRY_PART="$(adb shell getprop persist.sys.locale-country | tr -d '\r')"
  LOCALE="${LANG_PART}-${COUNTRY_PART}"
fi

echo "Device locale: ${LOCALE:-<empty, assuming en-US default>}"

case "$LOCALE" in
  pt*)
    JOIN_ERROR_TEXT="Token inválido ou expirado"
    ;;
  *)
    # Every other locale falls back to the English default (values/strings.xml) — the app has
    # no es or other translation for this string as of 2026-09-09, only pt-BR and the English
    # default, so anything that isn't pt* renders the English string.
    JOIN_ERROR_TEXT="Invalid or expired token"
    ;;
esac

echo "Running maestro test $TARGET (JOIN_ERROR_TEXT=\"$JOIN_ERROR_TEXT\")"
exec maestro test "$TARGET" -e JOIN_ERROR_TEXT="$JOIN_ERROR_TEXT"
