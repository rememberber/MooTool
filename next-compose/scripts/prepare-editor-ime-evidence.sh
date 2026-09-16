#!/usr/bin/env bash
# 为 F01/F04 系统 IME 与列编辑产品主窗手工验收准备隔离数据（不修改默认 Application Support）。
set -euo pipefail

ROOT="${MOOTOOL_COMPOSE_DATA_DIR:-}"
if [[ -z "${ROOT}" ]]; then
  ROOT="$(mktemp -d /tmp/mootool-compose-ime-evidence-XXXX)"
fi

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

echo "export MOOTOOL_COMPOSE_DATA_DIR=${ROOT}"
echo "# 1) ./gradlew :composeApp:runDistributable"
echo "# 2) JSON: 打开 ime-sample.json；随手记: 打开 ime-sample.md"
echo "# 3) 系统输入法（拼音等）预编辑→提交；列选+IME 见 docs/evidence/2026-09-16-editor-manual-acceptance/results.md"
echo "# 4) 截图登记: NNN-json-ime-product.png / NNN-quicknote-ime-product.png"
