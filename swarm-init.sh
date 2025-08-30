#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)

DOCKER_HOST=ssh://$("$repo_root"/terraform/prod-host.sh)
export DOCKER_HOST

private_ip=$("$repo_root"/terraform/prod-host-private-ip.sh)

docker swarm init \
  --advertise-addr "$private_ip"
