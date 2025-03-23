#!/usr/bin/env bash
###
### Script used during CI to install protoc onto GitHub runners.
###
### Users should avoid running this on their machines.
###
### Author: Ashley Scopes
###
set -o errexit
set -o nounset
[[ -n ${DEBUG+defined} ]] && set -o xtrace

readonly version=4.30.1

case "$(uname)" in
  Linux)
    readonly os_name=linux
    ;;
  Darwin)
    readonly os_name=osx
    ;;
  *)
    readonly os_name=windows
    ;;
esac

readonly url=https://repo1.maven.org/maven2/com/google/protobuf/protoc/${version}/protoc-${version}-${os_name}-x86_64.exe
# shellcheck disable=SC2155
readonly target_dir=$(mktemp -d)
readonly target=${target_dir}/protoc
echo "${target_dir}" >> "${GITHUB_PATH}"
export PATH="${PATH}:${target_dir}"

echo "Installing ${url} to ${target}"
curl --fail "${url}" -o "${target}"
chmod -v 777 "${target}"

if [[ $(command -v protoc) = "${target}" ]]; then
  echo "Installation successful"
  protoc --version
else
  echo -n "Failed to add protoc to path, path entry points to: "
  command -v protoc
  exit 2
fi
