#!/usr/bin/env bash
# Shared helpers for product-window evidence prep scripts (next-compose).
set -euo pipefail

mootool_evidence_script_dir() {
  cd "$(dirname "${BASH_SOURCE[0]}")" && pwd
}

mootool_evidence_compose_root() {
  local lib_dir
  lib_dir="$(mootool_evidence_script_dir)"
  cd "${lib_dir}/../.." && pwd
}

mootool_evidence_resolve_data_dir() {
  if [[ -n "${MOOTOOL_COMPOSE_DATA_DIR:-}" ]]; then
    printf '%s' "${MOOTOOL_COMPOSE_DATA_DIR}"
    return 0
  fi
  mktemp -d /tmp/mootool-compose-evidence-XXXX
}

mootool_evidence_print_run_distributable_hint() {
  local compose_root
  compose_root="$(mootool_evidence_compose_root)"
  echo "# cd ${compose_root} && MOOTOOL_COMPOSE_DATA_DIR=\"\${MOOTOOL_COMPOSE_DATA_DIR}\" ./gradlew :composeApp:runDistributable --offline"
}

mootool_evidence_print_http_public_smoke_hint() {
  local compose_root
  compose_root="$(mootool_evidence_compose_root)"
  echo "# Optional F09 httpbin/localhost only (default CI skips): MOOTOOL_HTTP_PUBLIC_SMOKE=1 MOOTOOL_HTTP_SMOKE_URL=\"https://httpbin.org/get\" cd ${compose_root} && ./gradlew :composeApp:desktopTest --tests com.rememberber.mootool.next.compose.domain.HttpEngineTest.optionalHttpBinPublicGetSmoke --offline"
}

mootool_evidence_print_http_multipart_smoke_hint() {
  local compose_root
  compose_root="$(mootool_evidence_compose_root)"
  echo "# Optional F09 multipart POST (httpbin/localhost only, default CI skips): MOOTOOL_HTTP_MULTIPART_SMOKE=1 cd ${compose_root} && ./gradlew :composeApp:desktopTest --tests com.rememberber.mootool.next.compose.domain.HttpEngineTest.optionalHttpBinMultipartPostSmoke --offline"
}

mootool_evidence_print_vault_conflict_product_hint() {
  echo "# Compose Overlay 焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/147-compose-json-vault-conflict-overlay-keep-tab-focus.png"
  echo "# 产品主窗 §A/B PNG 仍须 runDistributable + 手工截图（见 vault-conflict-product-window/results.md）"
}

mootool_evidence_print_http_response_tab_focus_hint() {
  echo "# Compose F09 响应区标题行焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/162-compose-http-response-head-tab-focus.png"
  echo "# 产品主窗 HTTP 响应 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_http_collection_saved_item_tab_focus_hint() {
  echo "# Compose F09 集合 .http-saved-item 焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/163-compose-http-saved-item-tab-focus.png"
  echo "# 产品窗 HTTP 集合搜索 Tab 走查 PNG 仍须 runDistributable + 手工截图（与 118/132 互补）"
}

mootool_evidence_print_host_profile_item_tab_focus_hint() {
  echo "# Compose F10 Host .host-profile 方案行焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/164-compose-host-profile-item-tab-focus.png"
  echo "# 产品窗 Host 方案搜索 Tab 走查 PNG 仍须 runDistributable + 手工截图（与 133 搜索框互补）"
}

mootool_evidence_print_config_validate_tab_focus_hint() {
  echo "# Compose F06 YAML 校验 Tab 按钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/165-compose-config-validate-tab-focus.png"
  echo "# 产品窗 F06 校验 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_json_inspector_copy_path_tab_focus_hint() {
  echo "# Compose F04 JSON 检查器 JSONPath「复制」按钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/166-compose-json-inspector-copy-path-tab-focus.png"
  echo "# 产品窗 JSON「复制」外描边 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_reformat_file_drop_tab_focus_hint() {
  echo "# Compose F03 文件 Tab file-drop-row 选择按钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/167-compose-reformat-file-drop-tab-focus.png"
  echo "# 产品窗 F03 文件 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_text_diff_highlight_options_tab_focus_hint() {
  echo "# Compose F02 高亮选项行焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/168-compose-text-diff-highlight-options-tab-focus.png"
  echo "# 产品窗 F02 文本对比 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_text_diff_editor_grid_tab_focus_hint() {
  echo "# Compose F02 .diff-editor-grid 并排编辑区焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/169-compose-text-diff-editor-grid-tab-focus.png"
  echo "# 产品窗 F02 文本对比 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_env_environment_tab_focus_hint() {
  echo "# Compose F08 .variables-workspace > header 环境 Tab 焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/170-compose-env-environment-tab-focus.png"
  echo "# 产品窗 F08 环境变量 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_git_merge_product_hint() {
  echo "# Compose merge hint 焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/153-compose-git-merge-flow-hint-tab-focus.png"
  echo "# Vault Git merge 产品窗 §B（对齐 GitMergeProductFlowPresentation / conflict.json）:"
  echo "# 1) F04 JSON Vault 打开 Git 面板 → 变更列表应自动选中 conflict.json（脚本产物）"
  echo "# 2) 对 conflict.json 使用 ours/theirs → 冲突计数归零"
  echo "# 3) 「继续合并」完成 merge（仍须 runDistributable 手工 PNG）"
  echo "# 登记 PNG: docs/evidence/2026-09-17-vault-conflict-product-window/results.md §B"
}

mootool_evidence_print_editor_column_ime_hint() {
  local compose_root
  compose_root="$(mootool_evidence_compose_root)"
  echo "# Column edit + system IME product window (manual PNG, not Compose frame):"
  echo "# cd ${compose_root} && ./scripts/prepare-editor-ime-evidence.sh"
  echo "# Samples: data/vaults/json/ime-sample.json + data/vaults/quick-note/ime-sample.md"
  echo "# Walkthrough: Alt-drag / latch column select → ASCII commit; then system IME preedit/commit"
  echo "# Register: docs/evidence/2026-09-16-editor-manual-acceptance/results.md"
}

mootool_evidence_print_p7_smoke_hint() {
  local compose_root
  compose_root="$(mootool_evidence_compose_root)"
  echo "# P7 package smoke (offline gate + optional MOOTOOL_P7_BUILD_DIST=1 on current OS):"
  echo "# cd ${compose_root} && ./scripts/prepare-p7-package-smoke.sh"
  echo "# Record in docs/acceptance.md; tri-platform install/sign/notarize still manual."
}

mootool_evidence_print_tray_tcc_hint() {
  local compose_root
  compose_root="$(mootool_evidence_compose_root)"
  echo "# Tray / F22 / F23 screen-capture TCC (manual PNG): deny Screen Recording then tray pick-color + color board picker; baseline frame: docs/evidence/2026-09-17-tray-tcc-screencapture/reference/57-color-baseline.png"
  echo "# cd ${compose_root} && ./scripts/prepare-tray-screencapture-evidence.sh"
}

mootool_evidence_assert_file() {
  local path="$1"
  local label="$2"
  if [[ ! -f "${path}" ]]; then
    echo "verify failed: missing ${label}: ${path}" >&2
    exit 1
  fi
}

mootool_evidence_assert_git_merge_conflict() {
  local vault_json="$1"
  if [[ ! -d "${vault_json}/.git" ]]; then
    echo "verify failed: no .git under ${vault_json}" >&2
    exit 1
  fi
  if [[ ! -f "${vault_json}/.git/MERGE_HEAD" ]]; then
    echo "verify failed: MERGE_HEAD missing (not in merge)" >&2
    exit 1
  fi
  local unmerged
  unmerged="$(git -C "${vault_json}" diff --name-only --diff-filter=U 2>/dev/null || true)"
  if [[ -z "${unmerged}" ]]; then
    echo "verify failed: no unmerged paths in ${vault_json}" >&2
    exit 1
  fi
}
