#!/usr/bin/env sh
###
### Configure the GitHub runner for builds.
###
### Note that this has to be a POSIX sh script,
### since bash may be missing or grossly outdated.
###
### Author: Ashley Scopes
###
set -o errexit
set -o nounset

echo "Platform: $(uname -a)"

case "$(uname | tr '[:upper:]' '[:lower:]')" in
  windows|mingw*)
    echo "Enabling long file paths in the registry if possible..."
    reg add "HKLM\SYSTEM\CurrentControlSet\Control\FileSystem" -v LongPathsEnabled -t REG_DWORD -d 1 -f || true
    ;;
  darwin*)
    echo "Installing latest version of bash..."
    brew install bash
    ;;
  *)
    echo "No preparation required."
    ;;
esac

java --version
bash --version
git --version
