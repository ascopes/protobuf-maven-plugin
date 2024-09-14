#!/usr/bin/env bash
###
### Runs a web server hosting the generated site locally.
###
### Author: Ashley Scopes
###
set -o errexit
[[ -n ${DEBUG+undef} ]] && set -o xtrace

cd "$(git rev-parse --show-toplevel)/protobuf-maven-plugin/target/site"
python3 -m http.server -b 127.0.0.1 7000 &
readonly pid=$!
trap 'kill -SIGTERM $pid' EXIT INT TERM
sleep 1
python3 -m webbrowser -t http://127.0.0.1:7000
wait < <(jobs -p)
