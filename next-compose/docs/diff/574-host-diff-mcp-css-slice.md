# DIFF-574：F10 run* 导入·导出 + F02 diff-editor-grid CSS + Vault MCP 双库 notes search→json read + 帧 169

## 背景

DIFF-573 已做 F02 `TextDiffPresentation.runReadImportFile`、`mooDiffToolbarOptions`/`mooDiffEditorPane`、F10 `HostWiringPresentation.runRestoreBackup`、Vault MCP 双库 **json search→notes read** body offset、Compose 帧 `168`；本条**不重复** 573 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F10 Host run* / 方案导入导出接线

- `HostWiringPresentation.runReadImportProfile` / `runWriteExportProfile` / `inferImportProfileName`；侧栏导入/导出经 Presentation，失败写 `reformat.error.read` / `reformat.error.write` 状态栏（不再裸 `readText()`/`writeText()`）。

### 样式（CSS 组件批次，非 573）

- `mooDiffEditorGrid` / `mooDiffEditorSeam`（F02 `.diff-editor-grid` 容器与中缝 `border-soft`，用于 [TextDiffScreen] 并排/统一编辑区）。

### Vault MCP stdio（双库 + 跨库 search→read）

- `subprocessDualVaultNotesSearchThenJsonReadHonorsBodyOffset`：双库 access 同会话 **notes search offset** 后再 **json read** body offset/length（**非** 573 双库 json search→notes read / 572 双库 notes search→read 链）。

### 证据脚本

- `mootool_evidence_print_text_diff_editor_grid_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F02 Compose 帧 `169` 提示。

### Compose 证据

- `TextDiffEditorGridCaptureTest` → `169-compose-text-diff-editor-grid-tab-focus.png`（非产品主窗）。

## 验证

- `HostWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `TextDiffEditorGridCaptureTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 F02/F10/IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
