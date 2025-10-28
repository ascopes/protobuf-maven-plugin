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

readonly host=127.0.0.1
readonly port=9000
base_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
site_dir=${base_dir}/protobuf-maven-plugin/target/site

if [[ ! -d ${site_dir} ]] || [[ "${*}" =~ .*--build.* ]]; then
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

if command -v termux-open-url &> /dev/null; then
  termux-open-url "http://${host}:${port}"
else
  python3 -m webbrowser -t "http://${host}:${port}"
fi

wait
