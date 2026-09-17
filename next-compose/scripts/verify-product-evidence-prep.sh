#!/usr/bin/env bash
# Non-interactive smoke for product-window evidence prep scripts (no runDistributable).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT}"

echo "== verify vault external conflict prep =="
VAULT_ROOT="$(mktemp -d /tmp/mootool-compose-evidence-XXXX)"
export MOOTOOL_COMPOSE_DATA_DIR="${VAULT_ROOT}"
./scripts/prepare-vault-conflict-evidence.sh >/dev/null
test -f "${VAULT_ROOT}/data/vaults/json/sample.json"
echo "ok sample.json"

echo "== verify git merge conflict prep =="
if ! command -v git >/dev/null 2>&1; then
  echo "skip git merge prep (git not installed)"
else
  GIT_ROOT="$(mktemp -d /tmp/mootool-compose-git-evidence-XXXX)"
  export MOOTOOL_COMPOSE_DATA_DIR="${GIT_ROOT}"
  ./scripts/prepare-git-merge-conflict-evidence.sh >/dev/null
  test -f "${GIT_ROOT}/data/vaults/json/conflict.json"
  test -f "${GIT_ROOT}/data/vaults/json/.git/MERGE_HEAD"
  unmerged="$(git -C "${GIT_ROOT}/data/vaults/json" diff --name-only --diff-filter=U)"
  test "${unmerged}" = "conflict.json"
  echo "ok merge conflict on conflict.json"
fi

echo "== verify editor IME prep =="
IME_ROOT="$(mktemp -d /tmp/mootool-compose-ime-evidence-XXXX)"
export MOOTOOL_COMPOSE_DATA_DIR="${IME_ROOT}"
./scripts/prepare-editor-ime-evidence.sh >/dev/null
test -f "${IME_ROOT}/data/vaults/json/ime-sample.json"
test -f "${IME_ROOT}/data/vaults/quick-note/ime-sample.md"
echo "ok ime-sample files"

echo "all product evidence prep checks passed"
