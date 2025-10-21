#!/bin/bash

# Este script determina a próxima versão com base nos commits,
# atualiza o build.gradle.kts e o CHANGELOG.md.

set -e # Encerra se um comando falhar.

# Move to the project root directory
cd "$(dirname "$0")/../.."

GRADLE_FILE="app/build.gradle.kts"
CHANGELOG_FILE="CHANGELOG.md"

# 1. Pega os commits desde a última tag.
# --abbrev=0 para pegar o nome da tag exato.
# 2>/dev/null para silenciar erros se não houver tag.
latest_tag=$(git describe --tags --abbrev=0 2>/dev/null) || true

if [ -z "$latest_tag" ]; then
  >&2 echo "Nenhuma tag anterior encontrada. Lendo todos os commits."
  commit_subjects=$(git log --pretty=format:"%s")
else
  >&2 echo "Última tag: $latest_tag. Lendo commits desde então."
  commit_subjects=$(git log "${latest_tag}"..HEAD --pretty=format:"%s")
fi

if [ -z "$commit_subjects" ]; then
    >&2 echo "Nenhum commit novo desde a última tag. Saindo."
    exit 0
fi

# 2. Determina o tipo de incremento da versão.
version_bump="none"
if echo "$commit_subjects" | grep -q "BREAKING CHANGE"; then
  version_bump="major"
elif echo "$commit_subjects" | grep -qE "^feat(\(.*\))?:"; then
  version_bump="minor"
elif echo "$commit_subjects" | grep -qE "^fix(\(.*\))?:|^revert(\(.*\))?:"; then
  version_bump="patch"
fi

if [ "$version_bump" = "none" ]; then
  >&2 echo "Nenhum commit relevante para versionamento. Saindo."
  exit 0
fi
>&2 echo "Tipo de incremento da versão: $version_bump"

# 3. Pega a versão atual e a incrementa.
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

# 4. Atualiza o build.gradle.kts.
sed -i.bak "s/versionName = \"$current_version\"/versionName = \"$new_version\"/" "$GRADLE_FILE"
sed -i.bak "s/versionCode = $current_version_code/versionCode = $new_version_code/" "$GRADLE_FILE"
rm "${GRADLE_FILE}.bak"

# 5. Atualiza o CHANGELOG.md.
today=$(date +%Y-%m-%d)
changelog_body=$(echo "$commit_subjects" | sed 's/^/* /')
existing_changelog=$(tail -n +2 "$CHANGELOG_FILE")

echo "# Changelog" > "$CHANGELOG_FILE"
echo "" >> "$CHANGELOG_FILE"
echo "## [$new_version] - $today" >> "$CHANGELOG_FILE"
echo "" >> "$CHANGELOG_FILE"
echo "$changelog_body" >> "$CHANGELOG_FILE"
echo "" >> "$CHANGELOG_FILE"
echo "$existing_changelog" >> "$CHANGELOG_FILE"

>&2 echo "Versionamento completo."

# 6. Retorna a nova versão para o workflow.
echo "$new_version"
