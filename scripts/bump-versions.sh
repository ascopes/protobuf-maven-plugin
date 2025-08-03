#!/usr/bin/env bash
###
### Update the project version based upon criteria.
###
### Author: Ashley Scopes
###
set -o errexit
set -o nounset
[[ -v DEBUG ]] && set -o xtrace

function usage() {
  echo "USAGE: ${BASH_SOURCE[0]} [ -h | -m | -M ]"
  echo "Update the version of the project if needed."
  echo ""
  echo "    -h | --help   Show this message and exit."
  echo "    -m | --minor  Bump the minor version."
  echo "    -M | --major  Bump the major version."
  echo ""
  echo "This script will update the major or minor version of this project."
  echo ""
  echo "If the project is not already prepared to release a new major or minor"
  echo "version, it will update the versions as needed. Otherwise, it will"
  echo "report that nothing needs any changes, before exiting."
  echo
}

function get_current_version_tuple() {
  echo "Fetching current version..." >&2
  local raw_version
  # Use of sed and tail here works around the fact Maven 4 outputs content with a prefix by
  # default. There are flags to control this but these flags do not exist in Maven 3.8, so
  # this is the only cross-compatible solution.
  raw_version=$(
      ./mvnw help:evaluate -q -DforceStdout=true -Dexpression=project.version 2>/dev/null \
      | sed 's/ /\n/g' \
      | tail -n1
  )
  if [[ ! ${raw_version} =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)-SNAPSHOT$ ]]; then
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
  if ((version[2] == 0)); then
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
  if (( version[1] == 0 )); then
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
  *)                echo "ERROR: Unknown argument ${1}." >&2; usage; exit 1 ;;
esac
