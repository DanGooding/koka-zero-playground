#!/usr/bin/env bash

set -euo pipefail
set -x

REPO_ROOT=$(git rev-parse --show-toplevel)

# expects BUILD_HOST to be set here
. "$REPO_ROOT"/vm/.env

DEPLOY_HOST=$("$REPO_ROOT"/terraform/prod-host.sh)

# interactively connect to DEPLOY_HOST from REMOTE_HOST
# in case DEPLOY_HOST was newly created 
# and we need the user to accept the ssh key fingerprint
ssh $BUILD_HOST -tt -- "ssh $DEPLOY_HOST -- true"

# from https://discourse.nixos.org/t/cross-platform-deployments/56606
nix run nixpkgs#nixos-rebuild -- \
  switch \
  --fast \
  --build-host $BUILD_HOST \
  --target-host $DEPLOY_HOST \
  --flake .#main
