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
  echo "# Compose §A 证据 sample.json 冲突「重新加载」焦点帧: docs/evidence/2026-09-15-inspector-screencapture/windows/200-compose-vault-external-conflict-sample-reload-tab-focus.png"
  echo "# Compose §A 证据 sample.json 外部删除「保存副本」焦点帧: docs/evidence/2026-09-15-inspector-screencapture/windows/205-compose-vault-external-conflict-sample-deleted-savecopy-tab-focus.png"
  echo "# 产品主窗 §A/B PNG 仍须 runDistributable + 手工截图（见 vault-conflict-product-window/results.md）"
}

mootool_evidence_print_vault_external_conflict_sample_tab_focus_hint() {
  echo "# Compose F04 §A 证据 sample.json 冲突「重新加载」焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/200-compose-vault-external-conflict-sample-reload-tab-focus.png"
  echo "# 产品主窗 §A 外部冲突叠层仍须 runDistributable + 手工 PNG"
}

mootool_evidence_print_vault_external_conflict_deleted_sample_tab_focus_hint() {
  echo "# Compose F04 §A 证据 sample.json 外部删除冲突「保存副本」焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/205-compose-vault-external-conflict-sample-deleted-savecopy-tab-focus.png"
  echo "# 产品主窗 §A 外部删除冲突叠层仍须 runDistributable + 手工 PNG（步骤 3b: rm sample.json）"
}

mootool_evidence_print_quicknote_external_conflict_sample_tab_focus_hint() {
  echo "# Compose F01 §A 证据 sample-external.md 冲突「保留编辑」焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/202-compose-quicknote-external-conflict-keep-tab-focus.png"
  echo "# 产品主窗 F01 外部冲突叠层仍须 runDistributable + 手工 PNG"
}

mootool_evidence_print_quicknote_external_conflict_deleted_sample_tab_focus_hint() {
  echo "# Compose F01 §A 证据 sample-external.md 外部删除冲突「保存副本」焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/206-compose-quicknote-external-conflict-deleted-savecopy-tab-focus.png"
  echo "# 产品主窗 F01 外部删除冲突叠层仍须 runDistributable + 手工 PNG（步骤 3c: rm sample-external.md）"
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

mootool_evidence_print_json_inspector_copy_result_tab_focus_hint() {
  echo "# Compose F04 JSON 检查器结果区「复制」按钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/208-compose-json-inspector-copy-result-tab-focus.png"
  echo "# 产品窗 F04 检查器结果复制 Tab 走查仍须 runDistributable + 手工 PNG"
}

mootool_evidence_print_json_inspector_duplicate_path_tab_focus_hint() {
  echo "# Compose F04 JSON 检查器结构面板重复键路径 Tab 焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/215-compose-json-inspector-duplicate-path-tab-focus.png"
  echo "# 产品窗 F04 检查器重复键路径 Tab 走查仍须 runDistributable + 手工 PNG"
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

mootool_evidence_print_config_convert_tab_focus_hint() {
  echo "# Compose F06 配置转换 Tab 行焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/171-compose-config-convert-tab-focus.png"
  echo "# 产品窗 F06 转换 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_host_apply_button_tab_focus_hint() {
  echo "# Compose F10「切换 Host」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/172-compose-host-apply-button-tab-focus.png"
  echo "# 产品窗 F10 Host 应用行 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_json_toolbar_import_tab_focus_hint() {
  echo "# Compose F04 JSON 工具栏导入钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/173-compose-json-toolbar-import-tab-focus.png"
  echo "# 产品窗 F04 JSON「复制」/导入 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_text_diff_import_cluster_tab_focus_hint() {
  echo "# Compose F02 文本对比左右导入簇焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/174-compose-text-diff-import-cluster-tab-focus.png"
  echo "# 产品窗 F02 导入 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_quicknote_toolbar_io_tab_focus_hint() {
  echo "# Compose F01 随手记工具栏导入/导出簇焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/175-compose-quicknote-toolbar-io-tab-focus.png"
  echo "# 产品窗 F01 随手记 Vault 导入/导出 Tab 走查 PNG 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_qr_preview_save_tab_focus_hint() {
  echo "# Compose F17 二维码预览保存钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/176-compose-qr-preview-save-tab-focus.png"
  echo "# 产品窗 F17 保存 PNG Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_pdf_toolbar_merge_tab_focus_hint() {
  echo "# Compose F24 PDF 工具栏合并钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/177-compose-pdf-toolbar-merge-tab-focus.png"
  echo "# 产品窗 F24 拆分/合并 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_image_library_export_tab_focus_hint() {
  echo "# Compose F23 图库底栏导出钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/178-compose-image-library-export-tab-focus.png"
  echo "# 产品窗 F23 图库导出 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_translation_now_tab_focus_hint() {
  echo "# Compose F20 翻译「立即翻译」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/179-compose-translation-now-tab-focus.png"
  echo "# 产品窗 F20 翻译工具栏 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_time_to_local_tab_focus_hint() {
  echo "# Compose F18 时间「转本地时间」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/180-compose-time-to-local-tab-focus.png"
  echo "# 产品窗 F18 时间转换 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_quicknote_vault_footer_rename_tab_focus_hint() {
  echo "# Compose F01 Vault 底栏重命名钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/181-compose-quicknote-vault-footer-rename-tab-focus.png"
  echo "# 产品窗 F01 Vault 底栏 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_env_add_variable_tab_focus_hint() {
  echo "# Compose F08 环境变量「添加」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/182-compose-env-add-variable-tab-focus.png"
  echo "# 产品窗 F08 环境变量 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_color_screen_pick_tab_focus_hint() {
  echo "# Compose F22 调色板屏幕取色钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/183-compose-color-screen-pick-tab-focus.png"
  echo "# 产品窗 F22 取色 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_http_send_tab_focus_hint() {
  echo "# Compose F09 HTTP「发送」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/184-compose-http-send-tab-focus.png"
  echo "# 产品窗 F09 HTTP 发送 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_net_ping_command_tab_focus_hint() {
  echo "# Compose F11 网络 PING 命令钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/185-compose-net-ping-command-tab-focus.png"
  echo "# 产品窗 F11 网络 PING Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_hardware_refresh_tab_focus_hint() {
  echo "# Compose F25 硬件刷新钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/186-compose-hardware-refresh-tab-focus.png"
  echo "# 产品窗 F25 硬件刷新 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_runtime_run_tab_focus_hint() {
  echo "# Compose F05 代码运行「运行」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/187-compose-runtime-run-tab-focus.png"
  echo "# 产品窗 F05 代码运行 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_crypto_generate_key_tab_focus_hint() {
  echo "# Compose F14 加解密「生成密钥对」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/188-compose-crypto-generate-key-tab-focus.png"
  echo "# 产品窗 F14 加解密 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_calculator_evaluate_tab_focus_hint() {
  echo "# Compose F21 计算器「=」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/189-compose-calculator-evaluate-tab-focus.png"
  echo "# 产品窗 F21 计算器 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_reformat_format_tab_focus_hint() {
  echo "# Compose F03 格式化「格式化」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/190-compose-reformat-format-tab-focus.png"
  echo "# 产品窗 F03 格式化 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_image_svg_start_tab_focus_hint() {
  echo "# Compose F23 SVG「开始矢量化」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/191-compose-image-svg-start-tab-focus.png"
  echo "# 产品窗 F23 图片 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_http_response_save_tab_focus_hint() {
  echo "# Compose F09 HTTP「另存响应」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/192-compose-http-response-save-tab-focus.png"
  echo "# 产品窗 F09 HTTP Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_time_to_timestamp_tab_focus_hint() {
  echo "# Compose F18 时间「转时间戳」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/193-compose-time-to-timestamp-tab-focus.png"
  echo "# 产品窗 F18 时间 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_env_export_tab_focus_hint() {
  echo "# Compose F08 环境变量「导出」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/194-compose-env-export-tab-focus.png"
  echo "# 产品窗 F08 环境变量 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_json_inspector_path_query_tab_focus_hint() {
  echo "# Compose F04 JSON 检查器 JSONPath「查询」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/195-compose-json-inspector-path-query-tab-focus.png"
  echo "# 产品窗 F04 JSON 检查器 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_settings_editor_font_size_row_tab_focus_hint() {
  echo "# Compose A01 设置 JSON 字号行焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/196-compose-settings-editor-font-size-row-tab-focus.png"
  echo "# 产品窗 A01 设置页走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_crypto_verify_tab_focus_hint() {
  echo "# Compose F14 加解密「验签」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/197-compose-crypto-verify-tab-focus.png"
  echo "# 产品窗 F14 加解密 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_json_inspector_infer_schema_tab_focus_hint() {
  echo "# Compose F04 JSON 检查器「生成 JSON Schema」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/198-compose-json-inspector-infer-schema-tab-focus.png"
  echo "# 产品窗 F04 JSON 检查器 Tab 走查仍须 runDistributable + 手工截图"
}

mootool_evidence_print_git_merge_continue_tab_focus_hint() {
  echo "# Compose A03 Vault Git merge「继续合并」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/199-compose-git-merge-continue-tab-focus.png"
  echo "# 产品窗 §B 冲突清零后 continue 仍须 runDistributable + 手工截图"
}

mootool_evidence_print_git_merge_push_disabled_tab_focus_hint() {
  echo "# Compose A03 Vault Git merge 进行中 push 禁用态帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/204-compose-git-merge-push-disabled-tab-focus.png"
  echo "# 产品窗 §B merge 冲突期 push/pull 禁用仍须 runDistributable + 手工 PNG"
}

mootool_evidence_print_git_merge_product_hint() {
  echo "# Compose merge hint 焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/153-compose-git-merge-flow-hint-tab-focus.png"
  echo "# Compose merge ours 焦点帧: docs/evidence/2026-09-15-inspector-screencapture/windows/158-compose-git-merge-resolve-tab-focus.png"
  echo "# Vault Git merge 产品窗 §B（对齐 GitMergeProductFlowPresentation / conflict.json）:"
  echo "# 1) F04 JSON Vault 打开 Git 面板 → 变更列表应自动选中 conflict.json（脚本产物）"
  echo "# 2) 对 conflict.json 使用 ours/theirs → 冲突计数归零"
  echo "# 3) merge 进行中 push/pull 应禁用、冲突未清时「提交」应禁用（帧 204/212；GitVaultRemotePresentation / GitOperationPresentation.commitEnabled）"
  echo "# 4) 「继续合并」完成 merge（仍须 runDistributable 手工 PNG）"
  echo "# 登记 PNG: docs/evidence/2026-09-17-vault-conflict-product-window/results.md §B"
}

mootool_evidence_print_editor_ime_column_latch_tab_focus_hint() {
  echo "# Compose F04 JSON「列编辑」闩锁钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/201-compose-json-column-edit-latch-tab-focus.png"
  echo "# Compose F01 随手记「列编辑」闩锁钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/203-compose-quicknote-column-edit-latch-tab-focus.png"
  echo "# 产品窗系统 IME/列选+IME 仍须 runDistributable + 手工 PNG（见 editor-manual-acceptance/results.md）"
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

mootool_evidence_assert_git_rebase_conflict() {
  local vault_json="$1"
  if [[ ! -d "${vault_json}/.git" ]]; then
    echo "verify failed: no .git under ${vault_json}" >&2
    exit 1
  fi
  if [[ ! -d "${vault_json}/.git/rebase-merge" && ! -d "${vault_json}/.git/rebase-apply" ]]; then
    echo "verify failed: rebase-merge/rebase-apply missing (not in rebase)" >&2
    exit 1
  fi
  local unmerged
  unmerged="$(git -C "${vault_json}" diff --name-only --diff-filter=U 2>/dev/null || true)"
  if [[ -z "${unmerged}" ]]; then
    echo "verify failed: no unmerged paths in ${vault_json}" >&2
    exit 1
  fi
}

mootool_evidence_print_git_rebase_continue_tab_focus_hint() {
  echo "# Compose A03 Vault Git rebase「继续合并 / Rebase」钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/207-compose-git-rebase-continue-tab-focus.png"
  echo "# Compose A03 Vault Git rebase §C 走查 hint + 刷新钮焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/209-compose-git-rebase-flow-hint-tab-focus.png"
  echo "# Compose A03 Vault Git rebase §C ours/theirs「采用 ours」焦点帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/210-compose-git-rebase-resolve-tab-focus.png"
  echo "# Compose A03 Vault Git rebase 进行中 push/pull 禁用态帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/211-compose-git-rebase-push-pull-disabled-tab-focus.png"
  echo "# Compose A03 Vault Git merge 冲突期提交禁用态帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/212-compose-git-merge-commit-disabled-tab-focus.png"
  echo "# Compose A03 Vault Git rebase 冲突期提交禁用态帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/213-compose-git-rebase-commit-disabled-tab-focus.png"
  echo "# Compose A03 Vault Git rebase 冲突期 fetch 可用 / pull 禁用帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/214-compose-git-rebase-fetch-enabled-pull-disabled-tab-focus.png"
  echo "# Compose A03 未解决冲突期 push 禁用态帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/216-compose-git-push-disabled-unresolved-conflicts-tab-focus.png"
  echo "# Compose A03 未解决冲突期 pull 禁用态帧（非产品主窗）: docs/evidence/2026-09-15-inspector-screencapture/windows/217-compose-git-pull-disabled-unresolved-conflicts-tab-focus.png"
  echo "# 产品窗 §C 冲突清零后 continue 仍须 runDistributable + 手工 PNG"
}

mootool_evidence_print_git_rebase_product_hint() {
  echo "# Vault Git rebase 产品窗 §C（对齐 prepare-git-rebase-conflict-evidence.sh / conflict.json）:"
  echo "# 1) F04 JSON Vault 打开 Git 面板 → 变更列表应自动选中 conflict.json"
  echo "# 2) 状态区应显示变基进行中（git.operationRebase）与 §C 走查提示（自动选中 conflict.json 时为 git.rebaseProductFlowResolve）"
  echo "# 3) 对 conflict.json 使用 ours/theirs → 冲突计数归零"
  echo "# 4) rebase 进行中 push/pull 应禁用、fetch 仍可用（GitVaultRemotePresentation；Compose 帧 211/214）、冲突未清时「提交」应禁用（帧 213 / commitEnabled）"
  echo "# 5) 「继续合并 / Rebase」完成 rebase（Compose 帧 207；仍须 runDistributable 手工 PNG）"
  echo "# 登记 PNG: docs/evidence/2026-09-17-vault-conflict-product-window/results.md §C"
}
