#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)

DOCKER_HOST=ssh://$("$repo_root"/terraform/prod-host.sh)
export DOCKER_HOST

# TODO: get the private IP addr programatically
docker swarm init \
  --advertise-addr ens4
