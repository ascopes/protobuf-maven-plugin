#!/usr/bin/env bash
###
### Update the project version based upon criteria.
###

set -o errexit
set -o nounset

[[ -n ${DEBUG+undef} ]] && set -o xtrace

function usage() {
  echo "USAGE: ${BASH_SOURCE[0]} [ -h | -m | -M ]"
  echo "Update the version of the project if needed."
  echo ""
  echo "    -h | --help   Show this message and exit."
  echo "    -m | --minor  Bump the minor version."
  echo "    -M | --major  Bump the major version."
  echo ""
  echo "This script will consune your intent to update the major or minor"
  echo "version of this project. If the project is not already prepared to"
  echo "release a new major or minor version, it will update the versions"
  echo "as needed. Otherwise, it will report that nothing needs changing"
  echo "and will exit."
  echo
}

function get_current_version_tuple() {
  echo "Fetching current version..." >&2
  local raw_version
  raw_version=$(./mvnw help:evaluate -q -DforceStdout=true -Dexpression=project.version 2>/dev/null)
  if ! [[ "${raw_version}" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)-SNAPSHOT$ ]]; then
    echo "Illegal version <${raw_version}>. Must be in the format X.Y.Z-SNAPSHOT." >&2
    echo "Please fix this manually." >&2
    return 2
  else
    echo "Current version is ${raw_version}" >&2
    echo "${BASH_REMATCH[1]}"
    echo "${BASH_REMATCH[2]}"
    echo "${BASH_REMATCH[3]}"
  fi
}

function set_version() {
  local version="${*}"
  version="${version// /.}-SNAPSHOT"
  echo "Setting version to ${version}" >&2
  ./mvnw versions:set -q -DnewVersion="${version}" 2>/dev/null
  git add -vA .
  git status
}

function bump_minor_version() {
  local version_tuple
  version_tuple=$(get_current_version_tuple)
  local version
  readarray -t version <<< "${version_tuple}"
  if [[ "${version[2]}" -eq 0 ]]; then
    echo "Already prepared to release a new minor version. Nothing to change." >&2
    return 0
  fi

  version[1]=$((version[1] + 1))
  version[2]=0
  set_version "${version[@]}"
}

function bump_major_version() {
  local version_tuple
  version_tuple=$(get_current_version_tuple)
  local version
  readarray -t version <<< "${version_tuple}"
  if [[ "${version[1]}" -eq 0 ]]; then
    echo "Already prepared to release a new major version. Nothing to change." >&2
    return 0
  fi

  version[0]=$((version[0] + 1))
  version[1]=0
  version[2]=0
  set_version "${version[@]}"
}

cd "$(dirname "${BASH_SOURCE[0]}")/.."

case "${1:-}" in
  "" | -h | --help) usage ;;
  -m | --minor)     bump_minor_version ;;
  -M | --major)     bump_major_version ;;
  *)                echo "ERROR: Unknown argument ${1}."; usage; exit 1 ;;
esac
