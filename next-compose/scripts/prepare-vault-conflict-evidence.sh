#!/usr/bin/env bash
# 为 Vault 外部冲突产品主窗验收准备隔离数据目录（不修改默认 Application Support）。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/product-evidence-common.sh
source "${SCRIPT_DIR}/lib/product-evidence-common.sh"

ROOT="$(mootool_evidence_resolve_data_dir)"
VAULT_JSON="${ROOT}/data/vaults/json"
VAULT_QN="${ROOT}/data/vaults/quick-note"
mkdir -p "${VAULT_JSON}" "${VAULT_QN}"

cat > "${VAULT_JSON}/sample.json" <<'EOF'
{
  "fixture": "vault-external-conflict",
  "step": "open in app then edit without saving"
}
EOF

cat > "${VAULT_QN}/sample-external.md" <<'EOF'
---
title: Vault external conflict sample
syntax: text/markdown
---
随手记外部冲突走查：打开后编辑不保存，再改写磁盘文件。
EOF

export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
mootool_evidence_assert_file "${VAULT_JSON}/sample.json" "Vault sample.json"
mootool_evidence_assert_file "${VAULT_QN}/sample-external.md" "Quick note sample-external.md"

cat <<EOF
# Vault external conflict product walkthrough (manual screenshots only)
export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
# Vault JSON 根: ${VAULT_JSON}
# 随手记 Vault 根（可选 §A 第二组 PNG）: ${VAULT_QN}
# 外部改写（步骤 3）:
#   printf '%s\\n' '{"disk":true}' > "${VAULT_JSON}/sample.json"
# 外部删除（步骤 3b，须先打开 sample.json 并保持脏编辑）:
#   rm "${VAULT_JSON}/sample.json"
# 随手记外部删除（步骤 3c，须先打开 sample-external.md 并保持脏编辑）:
#   rm "${VAULT_QN}/sample-external.md"
$(mootool_evidence_print_run_distributable_hint)
$(mootool_evidence_print_vault_conflict_product_hint)
# 登记: docs/evidence/2026-09-17-vault-conflict-product-window/results.md §A
EOF
