# DIFF-561：MCP stdio 余工具 + F03 引擎可测化 + 更新下载流 + env/net/vault UI + CSS/帧 155

## 背景

DIFF-560 已做 F17/F03 `enabled` 守卫、Vault Git `saveRemoteEnabled`、MCP stdio `mootool_encode`、设置分类导航 `mooSettingsNavItem` 与 Compose 帧 `154`；本条**不重复** 560 qr/reformat 守卫、git saveRemote、settings nav 154、mcp encode 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### MCP stdio（余下 `mootool_*`）

- `AiIntegrationMcpHashDiffJsonQueryTest`：`mootool_hash` / `mootool_diff` / `mootool_json_query` 子进程往返。
- `AiIntegrationMcpTimestampUuidTest`：`mootool_timestamp` / `mootool_uuid` 子进程往返。

### F03 格式化（引擎，非仅守卫）

- `ReformatWiringPresentation.runFormat` / `formatErrorMessage`；`ReformatScreen` 经 Presentation 调用 `ReformatEngine`。

### 更新下载流

- `UpdateCoordinatorDownloadFlowTest`：mock feed/bytes fetcher 断言 `check(autoDownload)` 与 `applyAutoDownloadSetting` → `ready` + 安装包路径。

### Vault / JSON 底栏

- `JsonVaultFooterPresentation.canRename` / `canShowVaultActions`；重命名钮 `enabled` + `mooJsonVaultFooterActions`。

### F08 / F11 / F25 UI 深度

- `EnvWiringPresentation.deleteRowEnabled` / `confirmDeleteEnabled` 接线。
- `NetWiringPresentation.stopEnabled` / `outputActionsEnabled` / `runCommandEnabled` 接线。
- `HardwareWiringPresentation.interfacesCommandEnabled`（单测登记，供后续顶栏命令扩展）。

### 样式（CSS 组件批次）

- `mooSettingsUpdateActionsRow` / `mooJsonVaultFooterActions`；`SettingsAboutPanel` 下载钮可注入 `downloadButtonModifier`。

### 设置 · 更新 UI 证据

- `SettingsAboutCaptureTest` → `155-compose-settings-about-update-download-tab-focus.png`（非产品主窗）。

## 验证

- `AiIntegrationMcpHashDiffJsonQueryTest` / `AiIntegrationMcpTimestampUuidTest` / `UpdateCoordinatorDownloadFlowTest` / `ReformatWiringPresentationTest` / `JsonVaultFooterPresentationTest` / `EnvWiringPresentationTest` / `NetWiringPresentationTest` / `HardwareWiringPresentationTest` / `SettingsAboutCaptureTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Vault Git/IME 手工 PNG、P7 三平台安装/公证、Vault MCP stdio 端到端扩展、其余 substantial 引擎/UI、目标未达成。
