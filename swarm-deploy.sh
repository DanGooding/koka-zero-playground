#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)

DOCKER_HOST=ssh://$("$repo_root"/terraform/prod-host.sh)
export DOCKER_HOST

. "$repo_root"/.env

echo "$GHCR_PAT" \
| docker login \
  ghcr.io \
  -u "$GHCR_USERNAME" \
  --password-stdin

docker stack config \
  --compose-file "$repo_root"/docker-compose.yaml \
  --compose-file "$repo_root"/docker-compose.prod.yaml \
| docker stack deploy \
  koka-zero-playground \
  --prune \
  --detach=false \
  --compose-file - \
  --with-registry-auth
