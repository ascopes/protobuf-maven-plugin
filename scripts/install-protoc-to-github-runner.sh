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

cd "$(dirname "${BASH_SOURCE[0]}")/.."
echo "Checking protobuf version from pom.xml..."
# Use of sed+tail works around the fact Maven 4.x needs --raw-streams to not output
# other noise at the start of the line, but Maven 3.8 does not support this at all.
version=$(./mvnw help:evaluate -Dexpression=protobuf.version -DforceStdout=true --quiet | sed 's/ /\n/g' | tail -1)

echo "Checking OS and CPU..."

case "$(uname)" in
  Linux)
    readonly os_name=linux
    case "$(uname -m)" in
      aarch64)
        readonly os_arch=aarch_64
        ;;
      *)
        readonly os_arch=x86_64
        ;;
    esac
    ;;
  Darwin)
    # Only support for aarch64 in new macOS versions.
    readonly os_name=osx
    readonly os_arch=aarch_64
    ;;
  *)
    # No arm64 version available, we assume Prism on ARM for Windows will
    # correctly translate this.
    readonly os_name=windows
    readonly os_arch=x86_64
    ;;
esac


readonly url=https://repo1.maven.org/maven2/com/google/protobuf/protoc/${version}/protoc-${version}-${os_name}-${os_arch}.exe

# shellcheck disable=SC2155
readonly target_dir=$(mktemp -d)
readonly target=${target_dir}/protoc
echo "${target_dir}" >> "${GITHUB_PATH}"
export PATH="${PATH}:${target_dir}"


echo "Downloading protoc ${version} for OS ${os_name} and CPU ${os_arch}"
echo "Installing ${url} to ${target}"

curl --fail "${url}" -o "${target}"

echo "Marking ${target} as executable (if possible)"
chmod -v 777 "${target}" || :

if [[ $(command -v protoc) = "${target}" ]]; then
  echo "Installation successful"
  protoc --version
else
  echo -n "Failed to add protoc to path, path entry points to: "
  command -v protoc || :
  exit 2
fi
