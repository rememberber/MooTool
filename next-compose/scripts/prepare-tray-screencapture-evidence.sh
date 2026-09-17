#!/usr/bin/env bash
# 托盘 / 调色板 / 图片工具屏幕录制 TCC 产品窗验收准备（无 GUI；登记手工步骤与基线帧路径）。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=lib/product-evidence-common.sh
source "${SCRIPT_DIR}/lib/product-evidence-common.sh"

COMPOSE_ROOT="$(mootool_evidence_compose_root)"
REF_DIR="${COMPOSE_ROOT}/docs/evidence/2026-09-17-tray-tcc-screencapture/reference"
SRC="${COMPOSE_ROOT}/docs/evidence/2026-09-15-inspector-screencapture/windows/57-color.png"
DEST="${REF_DIR}/57-color-baseline.png"

mkdir -p "${REF_DIR}"
if [[ ! -f "${DEST}" ]]; then
  mootool_evidence_assert_file "${SRC}" "color tool baseline PNG"
  cp "${SRC}" "${DEST}"
fi
mootool_evidence_assert_file "${DEST}" "restored tray TCC baseline PNG"

cat <<EOF
# Tray / screen-capture TCC product walkthrough (manual screenshots only)
# 1) 系统设置 → 隐私 → 屏幕录制：取消勾选 MooTool（或首次启动拒绝）
# 2) 托盘菜单 → 取色 / 区域截图：应出现 color.error.permission（见 ScreenCaptureFailureMessagesTest）
# 3) F22 调色板 / F23 图片助手屏幕取色：同上 + 可选打开系统设置双行文案
# 4) 授予权限后重试成功；登记 docs/evidence/2026-09-17-tray-tcc-screencapture/results.md
# Baseline UI frame (非 TCC 对话框): ${DEST}
$(mootool_evidence_print_run_distributable_hint)
$(mootool_evidence_print_tray_tcc_hint)
EOF
