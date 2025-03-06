#!/usr/bin/env bash
###
### Script that helps automate Nexus auto-staging promotion without using the
### Nexus Maven Plugin which will break when we exclude the acceptance tests from
### being deployed.
###
### This script will:
###     1. Find all open repositories under the current user account on Nexus.
###     2. Filter out any non-"open" repositories.
###     3. Find the repository that corresponds to the given group ID, artifact ID, and version.
###     4. Invoke the close operation on the repository
###     5. Wait for the close operation to end (or time out)
###     6. Check that the close operation succeeded (i.e. all Nexus rules for POM content, signing,
###        artifact inclusion, documentation, etc are all green)
###     7. Trigger a promotion (release to Maven Central) or drop (discard the release entirely).
###
### I have written this in such a way that I can hopefully reuse it elsewhere in the future.
###
### Note: this targets Sonatype Nexus Manager v2.x, not v3.x.
###
### Author: ascopes
###
set -o errexit
set -o nounset
[[ -n ${DEBUG+defined} ]] && set -o xtrace

function usage() {
  echo "USAGE: ${BASH_SOURCE[0]} [-h] -a <artifactId> -g <groupId> -v <version> -u <userName> -p <password> -s <server>"
  echo "    -a    <artifactId>   The base artifact ID to use. This can be any artifact ID in the project"
  echo "                         and is only used to determine the correct staging repository on Nexus"
  echo "                         to deploy."
  echo "    -d                   Drop rather than promote. Default is to promote."
  echo "    -g    <groupId>      The group ID for the artifact ID to look for."
  echo "    -v    <version>      The expected deployed version to look for."
  echo "    -u    <userName>     The Nexus username to use."
  echo "    -p    <password>     The Nexus password to use."
  echo "    -s    <server>       The Nexus server to use."
  echo "    -h                   Show this message and exit."
  echo
}

artifact_id=""
operation="promote"
group_id=""
version=""
username=""
password=""
server=""

while getopts "a:dg:hp:s:u:v:" opt; do
  case "${opt}" in
  a)
    artifact_id="${OPTARG}"
    ;;
  d)
    operation="drop"
    ;;
  g)
    group_id="${OPTARG}"
    ;;
  h)
    usage
    exit 0
    ;;
  p)
    password="${OPTARG}"
    ;;
  s)
    # Remove https:// or http:// at the start, remove trailing forward-slash
    # shellcheck disable=SC2001
    server="$(sed 's#^http://##g; s#https://##g; s#/$##g' <<<"${OPTARG}")"
    ;;
  u)
    username="${OPTARG}"
    ;;
  v)
    version="${OPTARG}"
    ;;
  ? | *)
    echo "ERROR: Unrecognised argument"
    usage
    exit 1
    ;;
  esac
done

for required_arg in artifact_id group_id password server username version; do
  if [[ -z "${!required_arg}" ]]; then
    echo "ERROR: Missing required argument: ${required_arg}" >&2
    usage
    exit 1
  fi
done

for command in base64 curl jq; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "ERROR: ${command} is not on the \$PATH" >&2
    exit 2
  fi
done

function print() {
  printf "%s" "${*}"
}

function try-jq() {
  local file
  file="$(mktemp)"
  trap 'rm -f "${file}"' EXIT INT TERM

  # pipe into file
  cat > "${file}"

  if ! jq 2>&1 > /dev/null < "${file}"; then
    echo -e "\e[1;31mJQ failed to parse the HTTP response. Content was:\e[0m" >&2
    cat "${file}" >&2
    return 99
  fi

  jq "${@}" < "${file}"
}

function accept-json-header() {
  print "Accept: application/json"
}

function authorization-header() {
  print "Authorization: Basic $(print "${username}:${password}" | base64)"
}

function content-type-json-header() {
  print "Content-Type: application/json"
}

function get-staging-repositories() {
  local url="https://${server}/service/local/staging/profile_repositories"
  echo -e "\e[1;33m[GET ${url}]\e[0m Retrieving repository IDs... (this may be slow) " >&2

  if curl \
    -X GET \
    --fail \
    --silent \
    --header "$(accept-json-header)" \
    --header "$(authorization-header)" \
    "${url}" |
    try-jq -e -r '.data[] | select(.type == "open" or .type == "closed") | .repositoryId'; then

    echo -e "\e[1;32mRetrieved all repository IDs successfully\e[0m" >&2
    return 0
  else
    echo -e "\e[1;31mFailed to retrieve the repository IDs\e[0m" >&2
    return 100
  fi
}

function is-artifact-in-repository() {
  # Group ID has . replaced with /
  local path="${group_id//./\/}/${artifact_id}/${version}"
  local repository_id="${1?Pass the repository ID}"
  local url="https://${server}/service/local/repositories/${repository_id}/content/${path}/"

  echo -e "\e[1;33m[GET ${url}]\e[0m" >&2
  if curl \
    -X GET \
    --fail \
    --silent \
    --header "$(accept-json-header)" \
    --header "$(authorization-header)" \
    "${url}" |
    try-jq '.' > /dev/null; then

    echo -e "\e[1;32mFound artifact in repository ${repository_id}, will close this repository\e[0m" >&2
    return 0
  else
    echo -e "\e[1;31mArtifact is not present in repository ${repository_id}, skipping\e[0m" >&2
    return 101
  fi
}

function find-correct-repository-id() {
  local repository_id
  for repository_id in $(get-staging-repositories); do
    if is-artifact-in-repository "${repository_id}"; then
      echo "${repository_id}"
      return 0
    fi
  done

  echo -e "\e[1;31mERROR: Could not find the artifact in any open repositories\e[0m" >&2
  return 102
}

function close-staging-repository() {
  local repository_id="${1?Pass the repository ID}"
  local url="https://${server}/service/local/staging/bulk/close"
  local payload
  
  payload="$(
    jq -cn '{ data: { description: $description, stagedRepositoryIds: [ $repository_id ] } }' \
      --arg description "" \
      --arg repository_id "${repository_id}"
  )"

  echo "Waiting a few seconds to mitigate eventual consistency on Nexus" >&2
  sleep 10

  echo -e "\e[1;33m[POST ${url} ${payload}]\e[0m Triggering the closure process" >&2

  if curl \
    -X POST \
    --fail \
    --silent \
    --header "$(accept-json-header)" \
    --header "$(content-type-json-header)" \
    --header "$(authorization-header)" \
    --data "${payload}" \
    "${url}"; then

    echo -e "\e[1;32mStarted closure successfully\e[0m" >&2
    return 0
  else
    echo -e "\e[1;31mFailed to start closure\e[0m" >&2
    return 103
  fi
}

function wait-for-closure-to-end() {
  local repository_id="${1?Pass the repository ID}"
  local url="https://${server}/service/local/staging/repository/${repository_id}/activity"

  echo -e "\e[1;33m[GET ${url}]\e[0m Waiting for the repository to complete the closure process" >&2
  local attempt=1
  while true; do
    # In our case, the "close" activity will gain the attribute named "stopped" once the process
    # is over (we then need to check if it passed or failed separately).
    if curl \
      -X GET \
      --fail \
      --silent \
      --header "$(accept-json-header)" \
      --header "$(authorization-header)" \
      "${url}" |
      try-jq -e '.[] | select(.name == "close") | .stopped != null' >/dev/null; then

      echo -e "\e[1;32mClosure process completed after ${attempt} attempts (@ $(date))}\e[0m" >&2
      return 0
    else
      echo -e "\e[1;32mStill waiting for closure to complete... - attempt $attempt (@ $(date))\e[0m" >&2
      ((attempt++))
    fi
    sleep 5
  done
}

function ensure-closure-succeeded() {
  local repository_id="${1?Pass the repository ID}"
  local url="https://${server}/service/local/staging/repository/${repository_id}/activity"

  echo -e "\e[1;33m[GET ${url}]\e[0m Checking the closure process succeeded" >&2
  # Closure has succeeded if the "close" activity has an event named "repositoryClosed" somewhere.

  if curl \
    -X GET \
    --fail \
    --silent \
    --header "$(accept-json-header)" \
    --header "$(authorization-header)" \
    "${url}" |
    try-jq -ce '.[] | select(.name == "close") | .events[] | select(.name == "repositoryClosed")'; then

    echo -e "\e[1;32mRepository closed successfully\e[0m" >&2
    return 0
  else
    echo -e "\e[1;31mERROR: Repository failed to close, you should check this on the Nexus dashboard\e[0m" >&2
    return 105
  fi
}

function trigger-drop-or-promote() {
  local repository_id="${1?Pass the repository ID}"
  local url="https://${server}/service/local/staging/bulk/${operation}"
  local payload
  payload="$(
    jq -cn '{ data: { description: $description, stagedRepositoryIds: [ $repository_id ] } }' \
      --arg description "" \
      --arg repository_id "${repository_id}"
  )"

  echo -e "\e[1;33m[POST ${url} ${payload}]\e[0m ${operation^} the staging release" >&2

  if curl \
    -X POST \
    --fail \
    --silent \
    --header "$(accept-json-header)" \
    --header "$(content-type-json-header)" \
    --header "$(authorization-header)" \
    --data "${payload}" \
    "${url}" |
    try-jq -ce '.'; then

    echo -e "\e[1;32m${operation^} succeeded\e[0m" >&2
    return 0
  else
    echo -e "\e[1;31mERROR: ${operation^} failed\e[0m" >&2
    return 106
  fi
}

repository_id="$(find-correct-repository-id)"
close-staging-repository "${repository_id}"
wait-for-closure-to-end "${repository_id}"
ensure-closure-succeeded "${repository_id}"
trigger-drop-or-promote "${repository_id}" || :

echo -e "\e[1;32mRelease ${operation} for repository ${repository_id} completed. Have a nice day :-)\e[0m" >&2
