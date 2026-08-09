#!/bin/bash

# Script de versionamento automático para Android (build.gradle.kts + CHANGELOG.md)
# Detecta commits novos e incrementa a versão de forma flexível.

set -euo pipefail

# Move para o diretório raiz do projeto
cd "$(dirname "$0")/../.."

GRADLE_FILE="app/build.gradle.kts"
CHANGELOG_FILE="CHANGELOG.md"

# 1️⃣ Pega a última tag (se houver)
latest_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

if [ -z "$latest_tag" ]; then
  >&2 echo "Nenhuma tag anterior encontrada. Lendo todos os commits."
  commit_subjects_raw=$(git log --pretty=format:"%s")
else
  >&2 echo "Última tag: $latest_tag. Lendo commits desde então."
  commit_subjects_raw=$(git log "${latest_tag}"..HEAD --pretty=format:"%s")
fi

# 2️⃣ Filtra commits que já estão no CHANGELOG
commit_subjects_array=()
while IFS= read -r subject; do
  if [ -n "$subject" ] && ! grep -qF "$subject" "$CHANGELOG_FILE" 2>/dev/null; then
    commit_subjects_array+=("$subject")
  fi
done <<< "$commit_subjects_raw"

commit_subjects=$(printf "%s\n" "${commit_subjects_array[@]}" | sed '/^[[:space:]]*$/d')

if [ -z "$commit_subjects" ]; then
    >&2 echo "Nenhum commit novo para adicionar ao changelog. Saindo."
    exit 0
fi

# 3️⃣ Determina o tipo de incremento da versão (mais flexível)
version_bump="patch"  # padrão é patch se houver commits novos

for subject in "${commit_subjects_array[@]}"; do
    if echo "$subject" | grep -qi "BREAKING CHANGE"; then
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
