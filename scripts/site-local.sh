#!/usr/bin/env bash
###
### Runs a web server hosting the generated site locally.
###
### Author: Ashley Scopes
###
set -o errexit
set -o xtrace

cd "$(git rev-parse --show-toplevel)/protobuf-maven-plugin/target/site"
python3 -m http.server -b 127.0.0.1 8080 &
sleep 1
python -m webbrowser -t http://127.0.0.1:8080
wait < <(jobs -p)
