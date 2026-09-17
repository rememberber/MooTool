# DIFF-573：F02/F10 run* 导入·恢复 + diff CSS + Vault MCP 双库 json search→notes read + 帧 168

## 背景

DIFF-572 已做 F03 `ReformatWiringPresentation.runReadSourceFile`/`runWriteResult`/`inferTypeFromFileName`、`mooFileDropRow`、Vault MCP 双库 **notes search→read** body offset、Compose 帧 `167`；本条**不重复** 572 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F02 文本对比 run* / 导入接线

- `TextDiffPresentation.runReadImportFile`；左右导入经 Presentation，失败写 `reformat.error.read` 状态栏（不再裸 `readText()`）。
- 高亮/忽略空白与并排/统一视图分行：`mooDiffToolbarOptions` / `mooDiffNavCluster`（对齐 `.diff-toolbar__options` 与导航簇）。

### F10 Host 备份恢复接线

- `HostWiringPresentation.runRestoreBackup`；「恢复备份」经 Presentation 调用 `HostEngine.readSystem` + `HostEngine.restore`（不再内联 `runCatching`）。

### 样式（CSS 组件批次，非 572）

- `mooDiffToolbarOptions`（F02 `.diff-toolbar__options` 34dp 行高）。
- `mooDiffEditorPane`（`.diff-editor-grid > div` 10dp 内边距，用于 `DiffEditorPane`）。

### Vault MCP stdio（双库 + 跨库 search→read）

- `subprocessDualVaultJsonSearchThenNotesReadHonorsBodyOffset`：双库 access 同会话 **json search offset** 后再 **notes read** body offset/length（**非** 571/572 同库 search→read / 566 双库 read-only 链）。

### 证据脚本

- `mootool_evidence_print_text_diff_highlight_options_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F02 Compose 帧 `168` 提示。

### Compose 证据

- `TextDiffHighlightModeCaptureTest` → `168-compose-text-diff-highlight-options-tab-focus.png`（非产品主窗）。

## 验证

- `TextDiffPresentationTest` / `HostWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `TextDiffHighlightModeCaptureTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 F02/F10/IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
