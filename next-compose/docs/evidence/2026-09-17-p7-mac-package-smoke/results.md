# P7 macOS 本机 package 烟雾（2026-09-17）

## 环境

- 主机：macOS Darwin（本机 x86_64）
- `JAVA_HOME`：Zulu 21.0.12.1
- 命令：`MOOTOOL_P7_BUILD_DIST=1 bash scripts/prepare-p7-package-smoke.sh`（仓库根 `next-compose/`）

## 结果

| 步骤 | 结果 |
| --- | --- |
| `check-toolchain.sh` | OK |
| `:composeApp:printTooling` | OK（appVersion=0.1.0, nativePackageVersion=1.1.0, Compose 1.12.0） |
| `:composeApp:verifyNativePackageMetadata` | OK |
| `:composeApp:desktopTest --offline` | OK（DIFF-511 后 **732/732**；含新增 F23 几何单测） |
| `:composeApp:packageDistributionForCurrentOS` | **BUILD SUCCESSFUL**（约 1m14s） |

## 产物（本机 macOS only）

- App 目录：`composeApp/build/compose/binaries/main/app`
- DMG：`composeApp/build/compose/binaries/main/dmg/MooTool Next Compose-1.1.0.dmg`

## 范围说明

- 未在干净机器安装 DMG、未跑公证/Sparkle/卸载隔离验收。
- Windows MSI、Linux DEB/RPM 未在本会话构建（脚本 SKIP 预期行为）。

## 日志

完整 stdout 见同目录 `run.log`（由烟雾脚本重定向生成）。
