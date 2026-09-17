#!/usr/bin/env bash
# P7 Windows / Linux 本机构建说明（不代替安装/升级/卸载/公证验收）。
# DIFF-565：仅打印各 OS 推荐 Gradle 命令；macOS 烟雾仍见 prepare-p7-package-smoke.sh。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "# next-compose P7 Win/Linux build notes ($(date -u +%Y-%m-%dT%H:%MZ))"
echo "# Product: MooTool Next Compose (com.rememberber.mootool.next.compose)"
echo "# Record outcomes in docs/evidence/2026-09-17-p7-win-linux-build/results.md"
echo ""

cat <<'EOF'
## 共通前提
- JDK 21（JAVA_HOME）
- 在本产品根目录执行：cd next-compose
- 离线门禁（与 macOS 烟雾一致）：
  export JAVA_HOME=...   # JDK 21
  ./gradlew :composeApp:desktopTest --offline

## Windows（在本机 Windows 上执行，非 macOS 交叉编译）
  cd next-compose
  set JAVA_HOME=C:\Program Files\Java\jdk-21
  gradlew.bat :composeApp:verifyNativePackageMetadata --offline
  gradlew.bat :composeApp:desktopTest --offline
  set MOOTOOL_P7_BUILD_DIST=1
  gradlew.bat :composeApp:packageDistributionForCurrentOS --offline
  rem 产物：MSI（per-user UpgradeCode 见 composeApp 打包配置）
  rem 安装后验证：开始菜单/卸载项显示名须为「MooTool Next Compose」，非裸 MooTool

## Linux（在本机 Linux 上执行）
  cd next-compose
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
  ./gradlew :composeApp:verifyNativePackageMetadata --offline
  ./gradlew :composeApp:desktopTest --offline
  export MOOTOOL_P7_BUILD_DIST=1
  ./gradlew :composeApp:packageDistributionForCurrentOS --offline
  # 产物：DEB/RPM；desktop entry：com.rememberber.mootool.next.compose.desktop
  # 命令/包名：mootool-next-compose（见 docs/data-platform-release.md）

## 未验收（必须手写 results.md）
- Windows MSI 安装、升级、卸载、UpgradeCode 稳定
- Linux DEB/RPM 安装、desktop 启动、XDG 路径隔离
- 代码签名 / 公证 / 商店分发
EOF

echo ""
echo "OK: printed Win/Linux build checklist only (no package built on this host unless MOOTOOL_P7_BUILD_DIST=1 on target OS)."
