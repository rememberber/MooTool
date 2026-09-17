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
mootool_evidence_print_reformat_file_drop_tab_focus_hint
mootool_evidence_print_text_diff_highlight_options_tab_focus_hint
mootool_evidence_print_text_diff_editor_grid_tab_focus_hint
