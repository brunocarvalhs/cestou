#!/bin/bash

# Script de versionamento automático para Android (build.gradle.kts + CHANGELOG.md)
# Detecta commits novos e incrementa a versão de forma flexível.

set -euo pipefail

# Move para o diretório raiz do projeto
cd "$(dirname "$0")/../.."

CONFIG_FILE=".github/pipeline-config.yaml"

# Função para ler do pipeline-config.yaml usando yq
get_config() {
    yq "$1" "$CONFIG_FILE"
}

GRADLE_FILE=$(get_config '.paths.gradle_file')
CHANGELOG_FILE=$(get_config '.paths.changelog')
WEAR_GRADLE_FILE="wear/build.gradle.kts"

# 1️⃣ Versão atual (build.gradle.kts) e última tag = última release publicada
current_version=$(grep "versionName = " "$GRADLE_FILE" | sed 's/.*versionName = "\(.*\)"/\1/')
latest_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

# Retorna 0 se a versão $1 é maior que $2 (X.Y.Z)
is_newer() {
  [ "$1" != "$2" ] && [ "$(printf '%s\n%s\n' "$1" "$2" | sort -V | tail -n1)" = "$1" ]
}

# 2️⃣ Se a branch já traz versionName acima da última tag (ex.: a develop já
# preparou 2.7.0 e o CHANGELOG), não há bump novo: sai sem imprimir nada, para o
# workflow não criar commit vazio. A release usa o versionName atual.
if [ -n "$latest_tag" ] && is_newer "$current_version" "$latest_tag"; then
  >&2 echo "versionName $current_version já está acima da última tag $latest_tag - sem bump, usando a versão preparada."
  exit 0
fi

# 3️⃣ Commits desde a última tag. Não filtra pelo CHANGELOG: ele é gerado pelo
# próprio script e não diz o que já foi lançado (o que causava "nenhum bump").
if [ -z "$latest_tag" ]; then
  >&2 echo "Nenhuma tag anterior encontrada. Lendo todos os commits."
  commit_subjects_raw=$(git log --pretty=format:"%s")
else
  >&2 echo "Última tag: $latest_tag. Lendo commits desde então."
  commit_subjects_raw=$(git log "${latest_tag}"..HEAD --pretty=format:"%s")
fi

commit_subjects_array=()
while IFS= read -r subject; do
  if [ -n "$subject" ]; then
    commit_subjects_array+=("$subject")
  fi
done <<< "$commit_subjects_raw"

if [ "${#commit_subjects_array[@]}" -eq 0 ]; then
  >&2 echo "Nenhum commit novo desde a última tag. Saindo."
  exit 0
fi

# 2.5️⃣ Tipos que, sozinhos, não justificam uma versão nova (preset Angular /
# semantic-release padrão): docs, ci, chore, style, test, refactor, build.
# Um "!" logo após o tipo (ex.: "chore!: ...") sempre marca BREAKING CHANGE,
# não importa o tipo - por isso NÃO entra nesse filtro, cai pra baixo e vira
# major. feat/fix/perf/BREAKING CHANGE (e qualquer tipo não listado aqui, por
# segurança) sempre justificam bump.
only_non_bumping_types=true
for subject in "${commit_subjects_array[@]}"; do
    if ! echo "$subject" | grep -qiE "^(docs?|ci|chore|style|test|refactor|build)(\([^)]*\))?:"; then
        only_non_bumping_types=false
        break
    fi
done

if [ "$only_non_bumping_types" = true ]; then
    >&2 echo "Todos os commits novos são doc/ci/chore/style/test/refactor/build - nenhum bump de versão necessário. Saindo."
    exit 0
fi

# 3️⃣ Determina o tipo de incremento da versão (mais flexível)
version_bump="patch"  # padrão é patch se houver commits novos

for subject in "${commit_subjects_array[@]}"; do
    if echo "$subject" | grep -qiE "BREAKING CHANGE|^[a-z]+(\([^)]*\))?!:"; then
        version_bump="major"
        break
    elif echo "$subject" | grep -qi "^feat"; then
        # Se ainda não for major, define como minor
        if [ "$version_bump" != "major" ]; then
            version_bump="minor"
        fi
    fi
done

>&2 echo "Tipo de incremento da versão: $version_bump"

# 4️⃣ Pega a versão atual e incrementa
current_version=$(grep "versionName = " "$GRADLE_FILE" | sed 's/.*versionName = "\(.*\)"/\1/')
current_version_code=$(grep "versionCode = " "$GRADLE_FILE" | sed 's/.*versionCode = \([0-9]*\).*/\1/')

major=$(echo "$current_version" | cut -d. -f1)
minor=$(echo "$current_version" | cut -d. -f2)
patch=$(echo "$current_version" | cut -d. -f3)

case "$version_bump" in
  "major") major=$((major + 1)); minor=0; patch=0 ;;
  "minor") minor=$((minor + 1)); patch=0 ;;
  "patch") patch=$((patch + 1)) ;;
esac

new_version="$major.$minor.$patch"
new_version_code=$((current_version_code + 1))

>&2 echo "Incrementando versão de $current_version para $new_version"

# 5️⃣ Atualiza o build.gradle.kts
sed -i.bak "s/versionName = \"$current_version\"/versionName = \"$new_version\"/" "$GRADLE_FILE"
sed -i.bak "s/versionCode = $current_version_code/versionCode = $new_version_code/" "$GRADLE_FILE"
rm -f "${GRADLE_FILE}.bak"

# O wear não tem contador próprio: o versionCode dele é derivado do celular
# (100000 + versionCode do app, em wear/build.gradle.kts). Já foi bumpado no
# passo acima, então não mexemos no arquivo do wear aqui. Incrementar o número
# literal quebraria a fórmula e reaproveitaria códigos já usados no Play.

# 6️⃣ Atualiza o CHANGELOG.md
today=$(date +%Y-%m-%d)
changelog_body=$(printf "%s\n" "${commit_subjects_array[@]}" | sed 's/^/* /')

# Mantém o conteúdo existente, se houver
existing_changelog=""
if [ -f "$CHANGELOG_FILE" ]; then
    existing_changelog=$(tail -n +2 "$CHANGELOG_FILE" || echo "")
fi

{
  echo "# Changelog"
  echo ""
  echo "## [$new_version] - $today"
  echo ""
  echo "$changelog_body"
  echo ""
  echo "$existing_changelog"
} > "$CHANGELOG_FILE"

>&2 echo "Versionamento completo."

# 7️⃣ Retorna a nova versão para o workflow
echo "$new_version"
