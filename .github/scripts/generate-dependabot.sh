#!/bin/bash

# GitHub parses .github/dependabot.yml directly - it is not a workflow, so it
# can't read security.dependabot in pipeline-config.yaml at run time. This
# script is how that section becomes the real source of truth anyway: it
# renders the file from the config, and validate-pipeline-config.sh calls it
# in --check mode on every PR to fail loudly if someone hand-edits
# dependabot.yml without updating pipeline-config.yaml (or forgets to
# re-render after changing the config).
#
# Usage:
#   generate-dependabot.sh          # writes .github/dependabot.yml
#   generate-dependabot.sh --check  # fails if the committed file would change

set -euo pipefail

cd "$(dirname "$0")/../.."

CONFIG_FILE=".github/pipeline-config.yaml"
OUT_FILE=".github/dependabot.yml"
MODE="${1:-write}"

render() {
  echo "# Generated from security.dependabot in pipeline-config.yaml by"
  echo "# .github/scripts/generate-dependabot.sh - do not hand-edit, re-run that"
  echo "# script (or open a PR that changes pipeline-config.yaml, tests.yml"
  echo "# regenerates and fails the drift check for you) instead."
  echo "version: 2"
  echo "updates:"

  local interval limit count
  interval=$(yq '.security.dependabot.schedule_interval' "$CONFIG_FILE")
  limit=$(yq '.security.dependabot.open_pull_requests_limit' "$CONFIG_FILE")
  count=$(yq '.security.dependabot.ecosystems | length' "$CONFIG_FILE")

  for i in $(seq 0 $((count - 1))); do
    local id directory
    id=$(yq ".security.dependabot.ecosystems[$i].id" "$CONFIG_FILE")
    directory=$(yq ".security.dependabot.ecosystems[$i].directory" "$CONFIG_FILE")
    echo "  - package-ecosystem: \"$id\""
    echo "    directory: \"$directory\""
    echo "    schedule:"
    echo "      interval: \"$interval\""
    echo "    open-pull-requests-limit: $limit"
    echo "    labels:"
    yq -o=json ".security.dependabot.ecosystems[$i].labels" "$CONFIG_FILE" | jq -r '.[]' | while read -r label; do
      echo "      - \"$label\""
    done
  done
}

if [ "$(yq '.security.dependabot.enabled' "$CONFIG_FILE")" != "true" ]; then
  echo "security.dependabot.enabled is false - leaving $OUT_FILE untouched." >&2
  exit 0
fi

if [ "$MODE" == "--check" ]; then
  if ! diff -u "$OUT_FILE" <(render) > /tmp/dependabot.diff; then
    echo "❌ $OUT_FILE está fora de sincronia com security.dependabot em $CONFIG_FILE:"
    cat /tmp/dependabot.diff
    echo ""
    echo "Rode '.github/scripts/generate-dependabot.sh' e comite o resultado."
    exit 1
  fi
  echo "OK: $OUT_FILE está em sincronia com $CONFIG_FILE."
else
  render > "$OUT_FILE"
  echo "$OUT_FILE gerado a partir de $CONFIG_FILE."
fi
