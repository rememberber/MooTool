# P7 Windows / Linux 构建说明（DIFF-565）

- 阶段：P7 三平台发行（**仅文档/脚本**，非 macOS 本机构建验收）
- 日期：2026-09-17
- 脚本：`scripts/prepare-p7-win-linux-build.sh`（打印 Win/Linux Gradle 命令清单）
- 身份与路径：见 `docs/data-platform-release.md` §1–2

## 已执行

- macOS 侧未设 `MOOTOOL_P7_BUILD_DIST=1` 运行本脚本（仅输出 checklist）
- offline 门禁仍以 macOS `prepare-p7-package-smoke.sh` + `:composeApp:desktopTest --offline` 为准（DIFF-565 未新增 MSI/DEB 构建日志）

## 未执行 / 未验收

- Windows 10/11 本机 `gradlew.bat :composeApp:packageDistributionForCurrentOS` 与 MSI 安装走查
- Linux 本机 DEB/RPM 构建与 `mootool-next-compose` desktop 启动
- 升级/卸载/签名/公证

## 下一步

- 在目标 OS 上按脚本 checklist 构建并在此目录追加 `run.log` 与安装截图路径
