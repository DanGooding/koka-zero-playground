#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)

ssh -v "$("$repo_root"/terraform/prod-host.sh)"
