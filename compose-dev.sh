#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)

docker stack config \
  --compose-file "$repo_root"/docker-compose.yaml \
  --compose-file "$repo_root"/docker-compose.dev.yaml \
| docker compose \
  --project-name koka-zero-playground \
  --file - \
  up \
  --build
