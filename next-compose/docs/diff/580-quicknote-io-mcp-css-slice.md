# DIFF-580：F01 随手记导出 run* + IO 失败 toast + 工具栏 io-actions CSS + Vault MCP json read→json search + 帧 175

基线：DIFF-579 `6bcba1cb`。

## 范围

- **F01**：`QuickNoteWiringPresentation.runWriteExportText` / `exportEnabled`；工具栏/更多/树右键导出走 Presentation；导入/导出失败 `reformat.error.read` / `reformat.error.write` + error toast（对齐 DIFF-577～578 链）。
- **CSS**：`mooQuickNoteToolbarIoCluster`（`.io-actions` 导入/导出簇 34dp）。
- **Vault MCP stdio**：双库 `mootool_json_documents_read` → `mootool_json_documents_search` body offset（`subprocessDualVaultJsonReadThenJsonSearchHonorsBodyOffset`）。
- **证据**：`QuickNoteToolbarIoCaptureTest` → `175-compose-quicknote-toolbar-io-tab-focus.png`；`mootool_evidence_print_quicknote_toolbar_io_tab_focus_hint`。

**不重复** DIFF-579：F09 响应另存、`mooDiffImportCluster`、json search→notes search、帧 174。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（JDK 21）
- `./scripts/verify-product-evidence-prep.sh`

## 仍阻塞

六套 CSS 逐选择器、产品主窗 Tab/IME/TCC PNG、P7 三平台安装/公证；F 行仍为待验收。
