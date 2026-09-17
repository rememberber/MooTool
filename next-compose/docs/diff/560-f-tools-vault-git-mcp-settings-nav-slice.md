# DIFF-560：F17/F03 引擎守卫 + Vault Git remote + MCP stdio encode + 设置分类导航 CSS/帧 + P7

## 背景

DIFF-559 已做 JSON/Host 搜索、工作台分组标题、F12/F19/F23 引擎守卫、host/json CSS、翻译离线边界单测与 Git merge hint 帧 `153`；本条**不重复** 559 vaultsearch/profilesearch/grouplabel/translation/merge hint 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### F17 二维码 / F03 格式化（引擎 UI）

- `QrWiringPresentation`：`canGenerate` / `hasPngOutput` / `canRecognize` / `canCopyRecognition`；`QrCodeScreen` 工具栏 `enabled` 接线。
- `ReformatWiringPresentation.canRunFormat`；`ReformatScreen` 格式化钮接线。

### Vault Git remote

- `GitVaultRemotePresentation.saveRemoteEnabled` 集中 busy + `configureRemoteEnabled`；`VaultGitDialog` 保存/删除 remote 钮。

### 样式（CSS 组件批次）

- `mooSettingsNavItem`（对齐 Electron `.settings-nav__item` 36dp）；`SettingsNavItem` 接线。

### MCP stdio

- `AiIntegrationMcpEncodeTest`：子进程 `mootool_encode` url 往返。

### 设置分类 UI 证据

- `ToolbarFocusCaptureTest` → `154-compose-settings-runtime-category-nav-tab-focus.png`（带分类 glyph，非产品主窗）。

### A02 / P7

- 命令盘 `saveremote`/`coderun` 关键词。
- `prepare-p7-package-smoke.sh` 注释 DIFF-560 offline gate 范围。

## 验证

- `QrWiringPresentationTest` / `ReformatWiringPresentationTest` / `GitVaultRemotePresentationTest` / `AiIntegrationMcpEncodeTest` / `CommandSearchCatalogTest` / `ToolbarFocusCaptureTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Vault Git/IME 手工 PNG、P7 三平台安装/公证、其余 substantial 引擎/UI、MCP stdio 其余工具、目标未达成。
