#!/usr/bin/env bash
# 为 Vault 外部冲突产品主窗验收准备隔离数据目录（不修改默认 Application Support）。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/product-evidence-common.sh
source "${SCRIPT_DIR}/lib/product-evidence-common.sh"

ROOT="$(mootool_evidence_resolve_data_dir)"
VAULT_JSON="${ROOT}/data/vaults/json"
mkdir -p "${VAULT_JSON}"

cat > "${VAULT_JSON}/sample.json" <<'EOF'
{
  "fixture": "vault-external-conflict",
  "step": "open in app then edit without saving"
}
EOF

export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
mootool_evidence_assert_file "${VAULT_JSON}/sample.json" "Vault sample.json"

cat <<EOF
# Vault external conflict product walkthrough (manual screenshots only)
export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
# Vault JSON 根: ${VAULT_JSON}
# 外部改写（步骤 3）:
#   printf '%s\\n' '{"disk":true}' > "${VAULT_JSON}/sample.json"
$(mootool_evidence_print_run_distributable_hint)
# 登记: docs/evidence/2026-09-17-vault-conflict-product-window/results.md §A
# 自动化 Overlay 焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/147-compose-json-vault-conflict-overlay-keep-tab-focus.png
EOF
