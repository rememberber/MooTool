#!/usr/bin/env bash
# 隔离数据目录 + 合法 Vault Git 远程，供设置「Vault / Git 同步」与 JSON Vault Git 面板产品窗走查。
set -euo pipefail

if ! command -v git >/dev/null 2>&1; then
  echo "git 未安装，无法准备 Vault Git 样本" >&2
  exit 1
fi

ROOT="${MOOTOOL_COMPOSE_DATA_DIR:-}"
if [[ -z "${ROOT}" ]]; then
  ROOT="$(mktemp -d /tmp/mootool-compose-vault-git-settings-XXXX)"
fi

VAULT_JSON="${ROOT}/data/vaults/json"
BARE="${ROOT}/_evidence_vault_git_settings.git"
mkdir -p "${VAULT_JSON}" "${ROOT}/config"

export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
export GIT_CONFIG_GLOBAL=/dev/null
export GIT_CONFIG_SYSTEM=/dev/null
export GIT_CONFIG_COUNT=1
export GIT_CONFIG_KEY_0=init.defaultBranch
export GIT_CONFIG_VALUE_0=main

rm -rf "${BARE}"
git init --bare "${BARE}"
REMOTE_URL="$(cd "${BARE}" && pwd | sed 's|^|file://|')"

pushd "${VAULT_JSON}" >/dev/null
if [[ -d .git ]]; then
  rm -rf .git
fi
git init -b main
git config user.email "mootool-evidence@local"
git config user.name "MooTool Evidence"
printf '%s\n' '{"sample":true}' > sample.json
git add sample.json
git commit -m "Vault Git settings evidence"
git remote add origin "${REMOTE_URL}"
popd >/dev/null

cat <<EOF
# Vault Git settings product walkthrough (manual screenshots only)
export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
# 1. ./gradlew :composeApp:runDistributable --offline
# 2. 设置 → Vault → 填写 gitRemote=${REMOTE_URL} / gitUsername / gitToken（可选）
# 3. JSON → Vault Git：fetch / sync 应识别 origin；非法 remote 保存后加载应被清空（DIFF-508）
# 4. 登记 PNG 到 docs/evidence/（勿提交无关证据帧）
EOF
