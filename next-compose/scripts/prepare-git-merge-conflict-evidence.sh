#!/usr/bin/env bash
# 在隔离 JSON Vault 内制造 Git merge 冲突（产品主窗 Git 面板验收，见 vault-conflict-product-window/results.md §B）。
set -euo pipefail

if ! command -v git >/dev/null 2>&1; then
  echo "git 未安装，无法准备 merge 冲突仓库" >&2
  exit 1
fi

ROOT="${MOOTOOL_COMPOSE_DATA_DIR:-}"
if [[ -z "${ROOT}" ]]; then
  ROOT="$(mktemp -d /tmp/mootool-compose-git-evidence-XXXX)"
fi

VAULT_JSON="${ROOT}/data/vaults/json"
BARE="${ROOT}/_evidence_git_remote.git"
WORKDIR="${ROOT}/_evidence_git_upstream_clone"
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
git pull origin "${BRANCH}" --no-rebase 2>/dev/null
set -e
popd >/dev/null

echo "export MOOTOOL_COMPOSE_DATA_DIR=${ROOT}"
echo "# JSON Vault（含 merge 冲突）: ${VAULT_JSON}"
echo "# 1) ./gradlew :composeApp:runDistributable"
echo "# 2) JSON 工具 → 打开 Vault Git 面板，选中 conflict.json"
echo "# 3) 使用本地/远端版本 resolve 后「继续合并」"
echo "# 4) 截图: NNN-json-vault-git-merge-conflict-product.png"
