#!/usr/bin/env bash
###
### Runs a web server hosting the generated site locally.
###
### Author: Ashley Scopes
###
set -o errexit
set -o nounset
set -o pipefail
[[ -v DEBUG ]] && set -o xtrace

function usage() {
  echo "USAGE: ${BASH_SOURCE[0]} [-c] [-h] [-n] [-p <port>] [-r]"
  echo "Build the Maven Plugin site locally and view it within your browser."
  echo ""
  echo "Arguments:"
  echo "     -c          Run mvn clean first."
  echo "     -h          Show this message, then exit."
  echo "     -n          Do not open a new tab in your default browser."
  echo "     -p <port>   Override the port to run on."
  echo "     -r          Fully rebuild the documentation if it already exists."
  echo ""
}

port=8888

while getopts ":chnp:r" opt; do
  case "${opt}" in
    c) readonly clean=true ;;
    h) usage; exit 0 ;;
    n) readonly no_browser=true ;;
    p) port=${OPTARG} ;;
    r) readonly rebuild=true ;;
    :) echo "ERROR: Unknown option -${OPTARG}"; usage; exit 1 ;;
    ?) echo "ERROR: Unknown arguments provided"; usage; exit 1 ;;
  esac
done

readonly host=127.0.0.1
base_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
site_dir=${base_dir}/protobuf-maven-plugin/target/site

if [[ -v clean ]]; then
  cd "${base_dir}"
  ./mvnw clean
fi

if [[ ! -d ${site_dir} ]] || [[ -v rebuild ]]; then
  cd "${base_dir}"
  ./mvnw site -Dlicense.skip -Dcheckstyle.skip -Dinvoker.skip -Dmaven.test.skip -T10
fi
cd "${site_dir}"

python3 -m http.server -b "${host}" "${port}" &
readonly pid=$!
trap 'kill -SIGTERM "$pid" &> /dev/null; trap - EXIT INT TERM' EXIT INT TERM

until curl --connect-timeout 1 --fail --silent -I "http://${host}:${port}" | head -n 1; do
  sleep 1
done

if [[ ! -v no_browser ]]; then
  if command -v termux-open-url &> /dev/null; then
    termux-open-url "http://${host}:${port}"
  else
    python3 -m webbrowser -t "http://${host}:${port}"
  fi
fi

wait
