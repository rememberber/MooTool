#!/usr/bin/env bash
# 为 Vault 外部冲突产品主窗验收准备隔离数据目录（不修改默认 Application Support）。
# 用法：eval "$(./scripts/prepare-vault-conflict-evidence.sh)" 后启动 runDistributable。
set -euo pipefail

ROOT="${MOOTOOL_COMPOSE_DATA_DIR:-}"
if [[ -z "${ROOT}" ]]; then
  ROOT="$(mktemp -d /tmp/mootool-compose-evidence-XXXX)"
fi

VAULT_JSON="${ROOT}/data/vaults/json"
mkdir -p "${VAULT_JSON}"

cat > "${VAULT_JSON}/sample.json" <<'EOF'
{
  "fixture": "vault-external-conflict",
  "step": "open in app then edit without saving"
}
EOF

export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"

echo "export MOOTOOL_COMPOSE_DATA_DIR=${ROOT}"
echo "# Vault JSON 根: ${VAULT_JSON}"
echo "# 1) cd next-compose && ./gradlew :composeApp:runDistributable"
echo "# 2) 打开 JSON → Vault 打开 sample.json → 编辑器改内容勿保存"
echo "# 3) 外部改写: printf '%s\\n' '{\"disk\":true}' > \"${VAULT_JSON}/sample.json\""
echo "# 4) ≤1s 后应弹出外部冲突对话框；截图登记见 docs/evidence/2026-09-17-vault-conflict-product-window/results.md"
