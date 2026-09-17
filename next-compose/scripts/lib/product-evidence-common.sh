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
