#!/bin/bash

# "Teste unitário" do pipeline: valida o formato de .github/pipeline-config.yaml
# antes de qualquer job de verdade rodar. Como esse arquivo é a única fonte de
# controle do que cada workflow executa (checks, targets de build, etc.), um
# campo faltando ou mal escrito aqui quebraria silenciosamente um job do
# GitHub Actions de um jeito confuso. Rodar isso primeiro falha rápido e com
# uma mensagem clara.

set -euo pipefail

cd "$(dirname "$0")/../.."

CONFIG_FILE=".github/pipeline-config.yaml"
errors=0

fail() {
  echo "❌ $1"
  errors=$((errors + 1))
}

require_field() {
  local path="$1"
  local value
  value=$(yq "$path" "$CONFIG_FILE")
  if [ "$value" == "null" ] || [ -z "$value" ]; then
    fail "$path é obrigatório"
  fi
}

# --- checks[]: cada check precisa do mínimo para rodar na matrix de tests.yml ---
# `stage` é opcional (default 1), mas só os valores 1 e 2 são reconhecidos pelos
# workflows hoje - qualquer outro valor faz o check sumir silenciosamente (não
# entra no stage 1 nem no stage 2), por isso é validado aqui.
checks_count=$(yq '.checks | length' "$CONFIG_FILE")
if [ "$checks_count" -eq 0 ]; then
  fail "checks[] não pode ser vazio"
fi
for i in $(seq 0 $((checks_count - 1))); do
  for field in name display_name command enabled; do
    require_field ".checks[$i].$field"
  done

  stage=$(yq ".checks[$i].stage // 1" "$CONFIG_FILE")
  if [ "$stage" != "1" ] && [ "$stage" != "2" ]; then
    fail "checks[$i].stage deve ser 1 ou 2 (valor atual: $stage) - qualquer outro valor faz o check nunca rodar"
  fi
done

# --- build.targets[]: cada target vira um ou mais itens na matrix de build.yml ---
targets_count=$(yq '.build.targets | length' "$CONFIG_FILE")
if [ "$targets_count" -eq 0 ]; then
  fail "build.targets[] não pode ser vazio"
fi
for i in $(seq 0 $((targets_count - 1))); do
  for field in name module google_services_required; do
    require_field ".build.targets[$i].$field"
  done

  module=$(yq ".build.targets[$i].module" "$CONFIG_FILE")
  if [[ "$module" != :* ]]; then
    fail "build.targets[$i].module deve começar com ':' (ex: \":app\"), valor atual: $module"
  fi

  variants_count=$(yq ".build.targets[$i].variants | length" "$CONFIG_FILE")
  if [ "$variants_count" -eq 0 ]; then
    fail "build.targets[$i].variants[] não pode ser vazio"
  fi
  for j in $(seq 0 $((variants_count - 1))); do
    for field in name gradle_task enabled path artifact_name; do
      require_field ".build.targets[$i].variants[$j].$field"
    done
  done
done

# --- coverage.kover: usado tanto para o gate quanto pro nome/mensagens do job ---
require_field '.coverage.kover.enabled'
if [ "$(yq '.coverage.kover.enabled' "$CONFIG_FILE")" == "true" ]; then
  require_field '.coverage.kover.command'
  require_field '.coverage.kover.display_name'
fi

if [ "$errors" -gt 0 ]; then
  echo ""
  echo "$errors problema(s) encontrado(s) em $CONFIG_FILE."
  exit 1
fi

echo "OK: $CONFIG_FILE válido ($checks_count checks, $targets_count build targets)."
