# DIFF-553：F05/F11/F23 引擎接线 + JSON Vault 搜索 + 设置行 + 列编辑/IME 证据 + CSS + MCP/命令盘 + P7

## 背景

DIFF-552 已做工作台/首页/分离/收藏/`ToolsExportWiringPresentation`；本条**不重复** 552 的 `HomePresentation`/`DetachedToolPresentation`/`FavoritePresentation`/`WorkbenchNavPresentation` 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial F 工具缺口、目标未达成。

## 行为

### F05 / F11 / F23 引擎（非 metadata）

- `CodeRunWiringPresentation`：设置 → `CodeRunPaths`、可用运行时计数、配置横幅/`canRun` 守卫；`CodeRunScreen` 接线。
- `NetWiringPresentation`：`PortScan` 启动前 trim + `NetEngine.parsePortSpec`；扫描钮启用与 `runAction` 共用。
- `ImageSvgWiringPresentation`：SVG 颜色/噪点字段 clamp；`ImageScreen` 对话框接线。

### JSON Vault 搜索

- `JsonVaultSearchPresentation.normalizeQuery`；`VaultSearchIndex` 共用；JSON Vault `MooCompactSearch` + `mooJsonVaultSearch`。

### A01 设置行

- `SettingsRowPresentation` 行内边距/标签间距常量；`SettingRow` + `mooSettingsSettingRow`。

### 列编辑 / IME 证据

- `EditorColumnEditPresentation`：IME 样本文件名 + 命令盘关键词；`mootool_evidence_print_editor_column_ime_hint`；`verify-product-evidence-prep.sh` 输出 hint。

### 样式（CSS 组件批次）

- `mooRuntimeOutputPane` / `mooJsonVaultSearch` / `mooNetOutputMonospace` / `mooSettingsSettingRow` / `mooImageToolToolbar`

### A02 / MCP / 命令盘

- `CommandSearchCatalog`：editor 列编辑/IME、`ai` 增 `json_query`/`diff`/`hash`、runtime 增 `detect`/`codrun`。
- `ToolRegistry`：F04 vault/snippet、F05 runtime/detect 关键词。
- `MooToolMcpToolsTest.jsonQueryReturnsArrayMatchesForValuesPath`（既有 MCP 链登记）。

### P7 / 证据脚本

- 延续 DIFF-552 `prepare-p7-package-smoke.sh` `bash -n`；列编辑/IME 产品窗 hint（非 runDistributable PNG）。

## 验证

- `CodeRunWiringPresentationTest` / `NetWiringPresentationTest` / `JsonVaultSearchPresentationTest` / `ImageSvgWiringPresentationTest` / `EditorColumnEditPresentationTest`
- `CommandSearchCatalogTest`（column / json_query / detect）
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗全工具 Tab/系统 IME 手工 PNG、P7 三平台安装/公证、其余 F 工具 substantial 引擎/UI、目标未达成。
