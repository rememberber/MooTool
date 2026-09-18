#!/usr/bin/env bash
# Non-interactive smoke for product-window evidence prep scripts (no runDistributable).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT}"

echo "== verify vault external conflict prep =="
VAULT_ROOT="$(mktemp -d /tmp/mootool-compose-evidence-XXXX)"
export MOOTOOL_COMPOSE_DATA_DIR="${VAULT_ROOT}"
./scripts/prepare-vault-conflict-evidence.sh >/dev/null
test -f "${VAULT_ROOT}/data/vaults/json/sample.json"
test -f "${VAULT_ROOT}/data/vaults/quick-note/sample-external.md"
echo "ok sample.json + quick-note sample-external.md"

echo "== verify git merge conflict prep =="
if ! command -v git >/dev/null 2>&1; then
  echo "skip git merge prep (git not installed)"
else
  GIT_ROOT="$(mktemp -d /tmp/mootool-compose-git-evidence-XXXX)"
  export MOOTOOL_COMPOSE_DATA_DIR="${GIT_ROOT}"
  ./scripts/prepare-git-merge-conflict-evidence.sh >/dev/null
  test -f "${GIT_ROOT}/data/vaults/json/conflict.json"
  test -f "${GIT_ROOT}/data/vaults/json/.git/MERGE_HEAD"
  unmerged="$(git -C "${GIT_ROOT}/data/vaults/json" diff --name-only --diff-filter=U)"
  test "${unmerged}" = "conflict.json"
  echo "ok merge conflict on conflict.json"
fi

echo "== verify git rebase conflict prep =="
if ! command -v git >/dev/null 2>&1; then
  echo "skip git rebase prep (git not installed)"
else
  REBASE_ROOT="$(mktemp -d /tmp/mootool-compose-git-rebase-evidence-XXXX)"
  export MOOTOOL_COMPOSE_DATA_DIR="${REBASE_ROOT}"
  ./scripts/prepare-git-rebase-conflict-evidence.sh >/dev/null
  test -f "${REBASE_ROOT}/data/vaults/json/conflict.json"
  test -d "${REBASE_ROOT}/data/vaults/json/.git/rebase-merge" \
    -o -d "${REBASE_ROOT}/data/vaults/json/.git/rebase-apply"
  unmerged="$(git -C "${REBASE_ROOT}/data/vaults/json" diff --name-only --diff-filter=U)"
  test "${unmerged}" = "conflict.json"
  echo "ok rebase conflict on conflict.json"
fi

echo "== verify editor IME prep =="
IME_ROOT="$(mktemp -d /tmp/mootool-compose-ime-evidence-XXXX)"
export MOOTOOL_COMPOSE_DATA_DIR="${IME_ROOT}"
./scripts/prepare-editor-ime-evidence.sh >/dev/null
test -f "${IME_ROOT}/data/vaults/json/ime-sample.json"
test -f "${IME_ROOT}/data/vaults/quick-note/ime-sample.md"
echo "ok ime-sample files"

echo "== verify tray screencapture prep =="
TRAY_ROOT="$(mktemp -d /tmp/mootool-compose-tray-evidence-XXXX)"
export MOOTOOL_COMPOSE_DATA_DIR="${TRAY_ROOT}"
./scripts/prepare-tray-screencapture-evidence.sh >/dev/null
test -f "${ROOT}/docs/evidence/2026-09-17-tray-tcc-screencapture/reference/57-color-baseline.png"
echo "ok tray baseline png"

echo "== verify p7 package smoke script syntax =="
bash -n "${ROOT}/scripts/prepare-p7-package-smoke.sh"
echo "ok prepare-p7-package-smoke.sh"

echo "all product evidence prep checks passed"
# shellcheck source=lib/product-evidence-common.sh
source "${ROOT}/scripts/lib/product-evidence-common.sh"
mootool_evidence_print_editor_column_ime_hint
mootool_evidence_print_p7_smoke_hint
mootool_evidence_print_http_public_smoke_hint
mootool_evidence_print_http_multipart_smoke_hint
mootool_evidence_print_tray_tcc_hint
mootool_evidence_print_http_response_tab_focus_hint
mootool_evidence_print_http_collection_saved_item_tab_focus_hint
mootool_evidence_print_host_profile_item_tab_focus_hint
mootool_evidence_print_config_validate_tab_focus_hint
mootool_evidence_print_json_inspector_copy_path_tab_focus_hint
mootool_evidence_print_json_inspector_copy_result_tab_focus_hint
mootool_evidence_print_json_inspector_duplicate_path_tab_focus_hint
mootool_evidence_print_reformat_file_drop_tab_focus_hint
mootool_evidence_print_text_diff_highlight_options_tab_focus_hint
mootool_evidence_print_text_diff_editor_grid_tab_focus_hint
mootool_evidence_print_env_environment_tab_focus_hint
mootool_evidence_print_config_convert_tab_focus_hint
mootool_evidence_print_host_apply_button_tab_focus_hint
mootool_evidence_print_json_toolbar_import_tab_focus_hint
mootool_evidence_print_text_diff_import_cluster_tab_focus_hint
mootool_evidence_print_quicknote_toolbar_io_tab_focus_hint
mootool_evidence_print_qr_preview_save_tab_focus_hint
mootool_evidence_print_pdf_toolbar_merge_tab_focus_hint
mootool_evidence_print_image_library_export_tab_focus_hint
mootool_evidence_print_translation_now_tab_focus_hint
mootool_evidence_print_time_to_local_tab_focus_hint
mootool_evidence_print_quicknote_vault_footer_rename_tab_focus_hint
mootool_evidence_print_env_add_variable_tab_focus_hint
mootool_evidence_print_color_screen_pick_tab_focus_hint
mootool_evidence_print_http_send_tab_focus_hint
mootool_evidence_print_net_ping_command_tab_focus_hint
mootool_evidence_print_hardware_refresh_tab_focus_hint
mootool_evidence_print_runtime_run_tab_focus_hint
mootool_evidence_print_crypto_generate_key_tab_focus_hint
mootool_evidence_print_calculator_evaluate_tab_focus_hint
mootool_evidence_print_reformat_format_tab_focus_hint
mootool_evidence_print_image_svg_start_tab_focus_hint
mootool_evidence_print_http_response_save_tab_focus_hint
mootool_evidence_print_time_to_timestamp_tab_focus_hint
mootool_evidence_print_env_export_tab_focus_hint
mootool_evidence_print_json_inspector_path_query_tab_focus_hint
mootool_evidence_print_settings_editor_font_size_row_tab_focus_hint
mootool_evidence_print_crypto_verify_tab_focus_hint
mootool_evidence_print_json_inspector_infer_schema_tab_focus_hint
mootool_evidence_print_git_merge_continue_tab_focus_hint
mootool_evidence_print_git_merge_push_disabled_tab_focus_hint
mootool_evidence_print_git_rebase_continue_tab_focus_hint
mootool_evidence_print_vault_external_conflict_sample_tab_focus_hint
mootool_evidence_print_vault_external_conflict_deleted_sample_tab_focus_hint
mootool_evidence_print_editor_ime_column_latch_tab_focus_hint
mootool_evidence_print_quicknote_external_conflict_sample_tab_focus_hint
mootool_evidence_print_quicknote_external_conflict_deleted_sample_tab_focus_hint
