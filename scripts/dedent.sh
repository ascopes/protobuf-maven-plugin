#!/usr/bin/env bash
###
### Intelligently dedents input from stdin.
###
### Author: Ashley Scopes
###
set -o errexit
set -o nounset
[[ -v DEBUG ]] && set -o xtrace

min_indent=
lines=()
while IFS=$'\n' read -r line; do
  line=$(expand -t2 <<< "${line}")
  [[ "${line}" =~ ^( *).*$ ]]
  indent=${#BASH_REMATCH[1]}
  if [[ -z "${min_indent}" ]] || ((indent < min_indent)); then
    min_indent=${indent}
  fi
  lines+=("${line}")
done

for line in "${lines[@]}"; do
  echo "${line:${min_indent}:${#line}}"
done

