# DIFF-575：F08/F06 run* 导入·导出 + F08 env header CSS + Vault MCP 双库 json read→notes search + 帧 170

## 背景

DIFF-574 已做 F10 `HostWiringPresentation.runReadImportProfile`/`runWriteExportProfile`、F02 `mooDiffEditorGrid`/`mooDiffEditorSeam`、Vault MCP 双库 **notes search→json read** body offset、Compose 帧 `169`；本条**不重复** 574 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F08 环境变量 run* 导出接线

- `EnvWiringPresentation.runWriteExport`；溢出菜单「导出」经 Presentation 写 UTF-8（不再裸 `file.writeText(EnvEngine.formatExport…)`）。

### F06 配置 run* 导入·导出接线

- `ConfigWiringPresentation.runReadImportFile` / `runWriteExportFile`；Properties/YAML 导入与导出经 Presentation（不再裸 `readText`/`writeText`）。

### 样式（CSS 组件批次，非 574）

- `mooEnvWorkspaceHeader` / `mooEnvScopeCluster`（F08 `.variables-workspace > header` 与 `.environment-scope` 作用域簇，用于 [VariablesScreen]）。

### Vault MCP stdio（双库 + 跨库 read→search）

- `subprocessDualVaultJsonReadThenNotesSearchHonorsBodyOffset`：双库 access 同会话 **json read** body offset 提取 needle 后再 **notes search** offset（**非** 574 双库 notes search→json read / 573 双库 json search→notes read 链）。

### 证据脚本

- `mootool_evidence_print_env_environment_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F08 Compose 帧 `170` 提示。

### Compose 证据

- `EnvScopeTabCaptureTest` → `170-compose-env-environment-tab-focus.png`（非产品主窗）。

## 验证

- `EnvWiringPresentationTest` / `ConfigWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `EnvScopeTabCaptureTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 F08/F06/IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
