# DIFF-578：F04/F06/F03 IO 失败 toast + Vault MCP 双库 json read→notes read + 帧 173

## 背景

DIFF-577 已做 F10 Host 导入·导出失败 toast + F08 导出失败 toast + F02 导入失败 toast + Vault MCP 双库 **notes read→json read** body offset + 证据 hint + Compose 帧 `172`；本条**不重复** 577 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F04 JSON 工具栏导入·导出失败可见

- `JsonScreen.importFile`/`exportFile` 在 `ImportOutcome.Failure`/`WriteExportOutcome.Failure` 时写 `reformat.error.read`/`reformat.error.write` 到状态栏 + **error toast**（576 仅接 run*，失败无 toast）。

### F06 配置转换导出失败可见

- `ConfigConvertScreen.exportText` 失败时写 `reformat.error.write` + **error toast**（576 仅导入失败 toast；导出仍用 `config.error.write` 且无 toast）。

### F03 格式化文件 Tab 导入·另存失败可见

- `ReformatScreen` 文件 Tab `loadSourceFile`/`saveResult` 失败时写状态栏 + **error toast**（对齐 F06/F08 导出失败链）。

### 样式（CSS 组件批次，非 577）

- `mooJsonToolbarIoCluster`（F04 工具栏 `.io-actions` 导入/导出簇 34dp 行高，用于 [JsonScreen]）。

### Vault MCP stdio（双库 + 跨库 read→read）

- `subprocessDualVaultJsonReadThenNotesReadHonorsBodyOffset`：双库 access 同会话 **json read** body 提取 notes 路径 hint 后再 **notes read** body offset/length（**非** 577 notes read→json read / 576 notes read→json search 链）。

### 证据脚本

- `mootool_evidence_print_json_toolbar_import_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F04 Compose 帧 `173` 提示。

### Compose 证据

- `JsonToolbarImportCaptureTest` → `173-compose-json-toolbar-import-tab-focus.png`（`mooJsonToolbarIoCluster`，非产品主窗）。

## 验证

- `ConfigWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `JsonToolbarImportCaptureTest` / 既有 F03/F04/F06 单测
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 F04/F06/F03/IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
