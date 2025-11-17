#!/usr/bin/env bash
###
### Script used during CI to install gRPC onto GitHub runners.
###
### Users should avoid running this on their machines.
###
### Author: Ashley Scopes
###
set -o errexit
set -o nounset
[[ -v DEBUG ]] && set -o xtrace

cd "$(dirname "${BASH_SOURCE[0]}")/.."
echo "Checking gRPC version from IT pom.xml..."
# Cannot use Maven to query this, as the POM is a filtered template, so will be
# unparsable.
version=$(grep -oE "<grpc.version>.*?</grpc.version>" protobuf-maven-plugin/src/it/setup/pom.xml \
    | sed -E 's@</?grpc.version>@@g')

echo "Checking OS and CPU..."

lower() {
  tr '[:upper:]' '[:lower:]'
}

case "$(uname | lower)" in
  linux*)
    readonly os_name=linux
    case "$(uname -m | lower)" in
      aarch64)
        readonly os_arch=aarch_64
        ;;
      *)
        readonly os_arch=x86_64
        ;;
    esac
    ;;
  darwin*)
    # Only support for aarch64 in new macOS versions.
    readonly os_name=osx
    case "$(uname -m | lower)" in
      aarch64)
        readonly os_arch=aarch_64
        ;;
      *)
        readonly os_arch=x86_64
        ;;
    esac
    ;;
  windows*|mingw*)
    # No arm64 version available, we assume Prism on ARM for Windows will
    # correctly translate this.
    readonly os_name=windows
    readonly os_arch=x86_64
    ;;
  *)
    echo "ERROR: unknown platform '$(uname)' ($(uname -a))" >&2
    exit 2
    ;;
esac


readonly url=https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/${version}/protoc-gen-grpc-java-${version}-${os_name}-${os_arch}.exe

target_dir=$(mktemp -d); readonly target_dir
readonly target=${target_dir}/protoc-gen-grpc-java
echo "${target_dir}" >> "${GITHUB_PATH}"
export PATH="${PATH}:${target_dir}"

echo "Downloading protoc-gen-grpc-java ${version} for OS ${os_name} and CPU ${os_arch}"
echo "Installing ${url} to ${target}"

curl --fail "${url}" -o "${target}"

echo "Marking ${target} as executable (if possible)"
chmod -v 777 "${target}" || :

if [[ $(command -v protoc-gen-grpc-java) = "${target}" ]]; then
  echo "Installation successful"
else
  echo -n "Failed to add protoc-gen-grpc-java to path, path entry points to: "
  command -v protoc-gen-grpc-java || :
  exit 2
fi
