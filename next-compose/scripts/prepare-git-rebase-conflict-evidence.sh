#!/usr/bin/env bash
# 在隔离 JSON Vault 内制造 Git rebase 冲突（产品主窗 Git 面板 §C 验收）。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/product-evidence-common.sh
source "${SCRIPT_DIR}/lib/product-evidence-common.sh"

if ! command -v git >/dev/null 2>&1; then
  echo "git 未安装，无法准备 rebase 冲突仓库" >&2
  exit 1
fi

ROOT="$(mootool_evidence_resolve_data_dir)"
VAULT_JSON="${ROOT}/data/vaults/json"
BARE="${ROOT}/_evidence_git_rebase_remote.git"
WORKDIR="${ROOT}/_evidence_git_rebase_upstream_clone"
mkdir -p "${VAULT_JSON}"

export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
export GIT_CONFIG_GLOBAL=/dev/null
export GIT_CONFIG_SYSTEM=/dev/null
export GIT_CONFIG_COUNT=1
export GIT_CONFIG_KEY_0=init.defaultBranch
export GIT_CONFIG_VALUE_0=main

rm -rf "${BARE}" "${WORKDIR}"
git init --bare "${BARE}"

pushd "${VAULT_JSON}" >/dev/null
if [[ -d .git ]]; then
  rm -rf .git
fi
git init -b main
git config user.email "mootool-evidence@local"
git config user.name "MooTool Evidence"
printf '%s\n' '{"side":"base"}' > conflict.json
git add conflict.json
git commit -m "Base"
BRANCH="$(git branch --show-current)"
REMOTE_URL="file://${BARE}"
git remote add origin "${REMOTE_URL}"
git push -u origin "${BRANCH}"

git clone "${REMOTE_URL}" "${WORKDIR}"
pushd "${WORKDIR}" >/dev/null
printf '%s\n' '{"side":"remote"}' > conflict.json
git add conflict.json
git commit -m "Remote"
git push origin "${BRANCH}"
popd >/dev/null

printf '%s\n' '{"side":"local"}' > conflict.json
git add conflict.json
git commit -m "Local"
set +e
git pull origin "${BRANCH}" --rebase 2>/dev/null
set -e
popd >/dev/null

mootool_evidence_assert_file "${VAULT_JSON}/conflict.json" "conflict.json"
mootool_evidence_assert_git_rebase_conflict "${VAULT_JSON}"

cat <<EOF
# Vault Git rebase conflict product walkthrough (manual screenshots only)
export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
# JSON Vault（含 rebase 冲突）: ${VAULT_JSON}
# 预期: git -C "${VAULT_JSON}" diff --name-only --diff-filter=U → conflict.json
$(mootool_evidence_print_run_distributable_hint)
$(mootool_evidence_print_git_rebase_product_hint)
# 截图: NNN-json-vault-git-rebase-conflict-product.png
EOF
