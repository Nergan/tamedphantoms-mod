#!/usr/bin/env bash
# Зависимости Minecraft 26.3. Гайдбук здесь на Modonomicon: у Patchouli сборки для 26.3 нет.
# Аргументы: <каталог> <KFF> <Fabric API> <Fabric Language Kotlin>
#            <Cloth Config> <Mod Menu> <Placeholder API> <Modonomicon>
set -euo pipefail

OUT_DIR="${1:?destination directory}"
KFF_VERSION="${2:?kotlin-for-forge version_number}"
FABRIC_API_VERSION="${3:?fabric-api version_number}"
FABRIC_KOTLIN_VERSION="${4:?fabric-language-kotlin version_number}"
CLOTH_VERSION="${5:?cloth-config version_number}"
MODMENU_VERSION="${6:?modmenu version_number}"
PLACEHOLDER_VERSION="${7:?placeholder-api version_number}"
MODONOMICON_VERSION="${8:?modonomicon version_number}"
USER_AGENT="${MODRINTH_USER_AGENT:-Nergan/tamedphantoms-mod (https://github.com/Nergan/tamedphantoms-mod)}"

mkdir -p "${OUT_DIR}"

# Третий аргумент — загрузчик, если один номер версии у мода общий на несколько загрузчиков.
fetch_modrinth() {
  local slug="$1"
  local version="$2"
  local loader="${3:-}"
  local json url filename sha512 pick

  json="$(curl -fsSL -A "${USER_AGENT}" "https://api.modrinth.com/v2/project/${slug}/version")"
  pick='first(.[] | select(.version_number == $v and ($l == "" or (.loaders | index($l)) != null)) | .files[] | select(.primary))'
  url="$(echo "${json}" | jq -r --arg v "${version}" --arg l "${loader}" "${pick} | .url // empty")"
  filename="$(echo "${json}" | jq -r --arg v "${version}" --arg l "${loader}" "${pick} | .filename // empty")"
  sha512="$(echo "${json}" | jq -r --arg v "${version}" --arg l "${loader}" "${pick} | .hashes.sha512 // empty")"

  if [[ -z "${url}" || -z "${filename}" || -z "${sha512}" ]]; then
    echo "No primary Modrinth file for ${slug} version ${version} ${loader}" >&2
    exit 1
  fi

  curl -fsSL -A "${USER_AGENT}" -o "${OUT_DIR}/${filename}" "${url}"
  echo "${sha512}  ${OUT_DIR}/${filename}" | sha512sum -c -
}

fetch_modrinth "kotlin-for-forge" "${KFF_VERSION}"
fetch_modrinth "fabric-api" "${FABRIC_API_VERSION}"
fetch_modrinth "fabric-language-kotlin" "${FABRIC_KOTLIN_VERSION}"
fetch_modrinth "cloth-config" "${CLOTH_VERSION}"
fetch_modrinth "modmenu" "${MODMENU_VERSION}"
fetch_modrinth "placeholder-api" "${PLACEHOLDER_VERSION}"
fetch_modrinth "modonomicon" "${MODONOMICON_VERSION}" neoforge
fetch_modrinth "modonomicon" "${MODONOMICON_VERSION}" fabric
