#!/usr/bin/env bash
# P7 打包前本机烟雾检查（不代替三平台安装/升级/卸载验收）。
# DIFF-563：仍依赖 MOOTOOL_P7_BUILD_DIST=1 才构建当前 OS 安装包；三平台/公证未验收。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "# next-compose P7 package smoke ($(date -u +%Y-%m-%dT%H:%MZ))"
echo "# Run from: $ROOT"
echo ""

if [[ -x "$ROOT/scripts/check-toolchain.sh" ]]; then
  "$ROOT/scripts/check-toolchain.sh"
else
  echo "WARN: scripts/check-toolchain.sh missing"
fi

export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || true)}"
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "ERROR: set JAVA_HOME to JDK 21"
  exit 1
fi

echo ""
echo "## Gradle tooling"
./gradlew :composeApp:printTooling --offline

echo ""
echo "## Native package metadata (no installer build)"
./gradlew :composeApp:verifyNativePackageMetadata --offline

echo ""
echo "## Unit/desktop tests (offline)"
./gradlew :composeApp:desktopTest --offline

echo ""
echo "## Current OS distributable (optional; may take several minutes)"
if [[ "${MOOTOOL_P7_BUILD_DIST:-}" == "1" ]]; then
  echo "MOOTOOL_P7_BUILD_DIST=1 → building packageDistributionForCurrentOS on $(uname -s) only."
  ./gradlew :composeApp:packageDistributionForCurrentOS --offline
  if [[ -x "$ROOT/scripts/rename-dist-artifacts.sh" ]]; then
    echo "# After build, rename artifacts if needed:"
    echo "# ./scripts/rename-dist-artifacts.sh <semver>"
  fi
else
  echo "# Set MOOTOOL_P7_BUILD_DIST=1 to also run packageDistributionForCurrentOS on this host."
  echo "# Windows MSI / Linux DEB·RPM must be built on those OSes (not verified from macOS smoke)."
fi
echo ""
echo ""
echo "## Optional F09 public HTTP smoke (not part of default offline gate)"
echo "# MOOTOOL_HTTP_PUBLIC_SMOKE=1 runs HttpEngineTest.optionalHttpBinPublicGetSmoke (httpbin/localhost allowlist only)."
echo "# Without network, mark 未测 in docs/acceptance.md — see docs/diff/526-http-pdf-merge-smoke-p7-slice.md."
echo ""
echo "OK: smoke checks finished. Record results in docs/acceptance.md; P7 install/sign/notarize still manual."
echo "# See docs/diff/543-git-diff-decoration-f-tools-p7-slice.md — desktopTest gate only, not tri-platform install."
echo "# DIFF-548: F02/F07/F16 历史 restore 单测 + F20 接线单测仍属 offline gate；三平台安装/公证未验收。"
echo "# DIFF-549: F13/F09/F14 metadata + QR/随机接线 + encode/env/hardware/net CSS 批次 + MCP url 单测仍属 offline gate。"
echo "# DIFF-550: 余下 F 工具 metadata + color/calc/host/time 等 CSS 批次 + JSON 检查器/命令盘/托盘·更新关键词仍属 offline gate。"
echo "# DIFF-552: 工作台/首页/分离/收藏 Presentation + home/detached/favorite/settings CSS + F10/F22/F24 exportDirectory 接线仍属 offline gate。"
echo "# DIFF-559: JSON/Host 搜索 Presentation + 导航分组 + F12/F19/F23 引擎守卫 + host/json CSS + 翻译离线边界单测 + Git merge hint 帧 153 仍属 offline gate；三平台安装/公证未验收。"
echo "# DIFF-560: F17/F03 引擎守卫 + Vault Git saveRemote + MCP stdio encode + settings-nav CSS/帧 154 仍属 offline gate；三平台安装/公证未验收。"
