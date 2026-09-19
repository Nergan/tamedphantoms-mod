#!/usr/bin/env bash
# Скачивает из Modrinth игровые jar Kotlin for Forge и Patchouli.
# Аргументы: <каталог> <версия KFF> <версия Patchouli>
set -euo pipefail

OUT_DIR="${1:?destination directory}"
KFF_VERSION="${2:?kotlin-for-forge version_number}"
PATCHOULI_VERSION="${3:?patchouli version_number}"
USER_AGENT="${MODRINTH_USER_AGENT:-Nergan/tamedphantoms-mod (https://github.com/Nergan/tamedphantoms-mod)}"

mkdir -p "${OUT_DIR}"

fetch_modrinth() {
  local slug="$1"
  local version="$2"
  local json url filename sha512

  json="$(curl -fsSL -A "${USER_AGENT}" "https://api.modrinth.com/v2/project/${slug}/version")"
  url="$(echo "${json}" | jq -r --arg v "${version}" '
    first(.[] | select(.version_number == $v) | .files[] | select(.primary) | .url) // empty
  ')"
  filename="$(echo "${json}" | jq -r --arg v "${version}" '
    first(.[] | select(.version_number == $v) | .files[] | select(.primary) | .filename) // empty
  ')"
  sha512="$(echo "${json}" | jq -r --arg v "${version}" '
    first(.[] | select(.version_number == $v) | .files[] | select(.primary) | .hashes.sha512) // empty
  ')"

  if [[ -z "${url}" || -z "${filename}" || -z "${sha512}" ]]; then
    echo "No primary Modrinth file for ${slug} version ${version}" >&2
    exit 1
  fi

  curl -fsSL -A "${USER_AGENT}" -o "${OUT_DIR}/${filename}" "${url}"
  echo "${sha512}  ${OUT_DIR}/${filename}" | sha512sum -c -
}

fetch_modrinth "kotlin-for-forge" "${KFF_VERSION}"
fetch_modrinth "patchouli" "${PATCHOULI_VERSION}"
