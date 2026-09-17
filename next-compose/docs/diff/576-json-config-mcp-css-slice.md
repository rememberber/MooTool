# DIFF-576：F04 JSON 导入·导出 run* + F06 导入失败可见 + io-actions CSS + Vault MCP 双库 notes read→json search + 帧 171

## 背景

DIFF-575 已做 F08 `EnvWiringPresentation.runWriteExport`、F06 `ConfigWiringPresentation.runReadImportFile`/`runWriteExportFile`、F08 `mooEnvWorkspaceHeader`/`mooEnvScopeCluster`、Vault MCP 双库 **json read→notes search** body offset、Compose 帧 `170`；本条**不重复** 575 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F04 JSON 工具栏/拖放导入·导出 run*

- `JsonWiringPresentation.runReadImportFile` / `runWriteExportFile`；工具栏导入/导出与非 Vault `.json` 拖放读文件经 Presentation（不再裸 `readText`/`writeText`）。

### F06 配置导入失败可见

- `ConfigConvertScreen.importText` 在 `ImportOutcome.Failure` 时写 `reformat.error.read` 状态栏 + error toast（575 静默返回 null 已修）。

### 样式（CSS 组件批次，非 575）

- `mooConfigConvertActions`（F06 转换 Tab `.io-actions` 中栏 110–160dp，用于 [ConfigConvertScreen]）。

### Vault MCP stdio（双库 + 跨库 read→search）

- `subprocessDualVaultNotesReadThenJsonSearchHonorsBodyOffset`：双库 access 同会话 **notes read** body offset 提取 needle 后再 **json search** offset（**非** 575 双库 json read→notes search / 574 双库 notes search→json read 链）。

### 证据脚本

- `mootool_evidence_print_config_convert_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F06 Compose 帧 `171` 提示。

### Compose 证据

- `ConfigConvertTabCaptureTest` → `171-compose-config-convert-tab-focus.png`（非产品主窗）。

## 验证

- `JsonWiringPresentationTest` / `ConfigWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `ConfigConvertTabCaptureTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 F04/F06/IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
