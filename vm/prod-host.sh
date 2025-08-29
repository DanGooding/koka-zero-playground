#!/usr/bin/env bash

set -euo pipefail

ADDR=$(
  terraform output \
  --state=../terraform/terraform.tfstate \
  --json \
  | jq .main_ipv4.value -r
)

echo root@$ADDR
