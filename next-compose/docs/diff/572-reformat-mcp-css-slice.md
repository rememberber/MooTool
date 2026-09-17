# DIFF-572：F03 run* 文件 IO + `.file-drop-row` CSS + Vault MCP 双库 notes search→read + 帧 167

## 背景

DIFF-571 已做 F04 `JsonWiringPresentation.run*`、`mooJsonInspectorSectionResult`/`mooJsonInspectorPathActions`、Vault MCP 双库 **json search→read** body offset、Compose 帧 `166`；本条**不重复** 571 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F03 格式化 run* / 文件 Tab 接线

- `ReformatWiringPresentation.runReadSourceFile` / `runWriteResult` / `inferTypeFromFileName`（导入推断 Nginx/Java/XML/HTML）。
- `ReformatScreen` 文件 Tab 导入/另存经 Presentation，不再分散 `runCatching` + 直接读写。

### 样式（CSS 组件批次，非 571）

- `mooFileDropRow`（Electron `.file-drop-row` 34dp 行高 + 10dp 间距，用于 `FileDropRow`）。

### Vault MCP stdio（双库 + search→read 余量）

- `subprocessDualVaultNotesSearchThenReadHonorsBodyOffset`：双库 access 同会话 **notes search offset** 后再 **read** body offset/length（**非** 571 双库 json search→read / 567 双库 notes search only / 570 双库 notes read only 链）。

### 证据脚本

- `mootool_evidence_print_reformat_file_drop_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F03 Compose 帧 `167` 提示。

### Compose 证据

- `ReformatFileDropCaptureTest` → `167-compose-reformat-file-drop-tab-focus.png`（非产品主窗）。

## 验证

- `ReformatWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `ReformatFileDropCaptureTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 F03/IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
