#!/usr/bin/env bash

set -euo pipefail

# TODO: dont hardcode
BUILD_HOST=dan@192.168.0.30

DEPLOY_HOST=$(./prod-host.sh)

# interactively connect to DEPLOY_HOST from REMOTE_HOST
# in case DEPLOY_HOST was newly created 
# and we need the user to accept the ssh key fingerprint
ssh $BUILD_HOST -tt -- "ssh $DEPLOY_HOST -- true"

# from https://discourse.nixos.org/t/cross-platform-deployments/56606
nixos-rebuild switch \
  --build-host $BUILD_HOST \
  --target-host $DEPLOY_HOST \
  --flake .#main
