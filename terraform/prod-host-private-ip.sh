#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)

terraform output \
  --state="$REPO_ROOT"/terraform/terraform.tfstate \
  --json \
| jq .main_private_ipv4.value -r
