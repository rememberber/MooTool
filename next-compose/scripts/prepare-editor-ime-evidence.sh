#!/usr/bin/env bash
# 为 F01/F04 系统 IME 与列编辑产品主窗手工验收准备隔离数据。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/product-evidence-common.sh
source "${SCRIPT_DIR}/lib/product-evidence-common.sh"

ROOT="$(mootool_evidence_resolve_data_dir)"
JSON_VAULT="${ROOT}/data/vaults/json"
QN_VAULT="${ROOT}/data/vaults/quick-note"
mkdir -p "${JSON_VAULT}" "${QN_VAULT}"

cat > "${JSON_VAULT}/ime-sample.json" <<'EOF'
{"note":"在 JSON EditorHost 使用系统输入法输入中文，验证预编辑与提交"}
EOF

cat > "${QN_VAULT}/ime-sample.md" <<'EOF'
---
title: IME 随手记样本
syntax: text/markdown
---
在随手记 EditorHost 使用系统输入法；列选/闩锁后 IME 提交应写入每一逻辑行。
EOF

export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
mootool_evidence_assert_file "${JSON_VAULT}/ime-sample.json" "ime-sample.json"
mootool_evidence_assert_file "${QN_VAULT}/ime-sample.md" "ime-sample.md"

cat <<EOF
# Editor IME product walkthrough (manual screenshots only)
export MOOTOOL_COMPOSE_DATA_DIR="${ROOT}"
# JSON: ${JSON_VAULT}/ime-sample.json
# 随手记: ${QN_VAULT}/ime-sample.md
$(mootool_evidence_print_run_distributable_hint)
# 登记: docs/evidence/2026-09-16-editor-manual-acceptance/results.md
# 截图: NNN-json-ime-product.png / NNN-quicknote-ime-product.png
EOF
