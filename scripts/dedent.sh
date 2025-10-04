#!/usr/bin/env bash
###
### Intelligently dedents input from stdin.
###
### Useful for quickly reformatting example snippets when writing documentation.
###
### Author: Ashley Scopes
###
set -o errexit
set -o nounset
[[ -v DEBUG ]] && set -o xtrace

if ! command -v expand >/dev/null; then
  printf "ERROR: expand not found on \$PATH.\n" >&2
  exit 1
fi

min_indent=
lines=()
while IFS=$'\n' read -r line; do
  # Tabs become two spaces. I have no time for tab indentation.
  line=$(expand -it2 <<< "${line}")

  # Sniff indents at the start of lines.
  [[ ${line} =~ ^( *)(.*)$ ]]

  # Don't bother sniffing whitespace indents on blank lines or
  # empty lines as it produces unintentional results.
  if [[ -n ${BASH_REMATCH[2]} ]]; then
    indent=${#BASH_REMATCH[1]}
    # Find the next smallest indent if this line has a smaller indent
    # than previous lines.
    if [[ -z ${min_indent} ]] || ((indent < min_indent)); then
      min_indent=${indent}
    fi
  fi

  lines+=("${line}")
done

# Slice the minimum indent off each line.
for line in "${lines[@]}"; do
  printf "%s\n" "${line:${min_indent}:${#line}}"
done
