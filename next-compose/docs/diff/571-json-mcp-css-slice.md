# DIFF-571：F04 run* 接线 + F04 检查器 CSS + Vault MCP 双库 search→read + 帧 166

## 背景

DIFF-570 已做 F06 `ConfigWiringPresentation.run*`、`mooConfigValidateLayout`/`mooConfigValidateActions`、Vault MCP 双库 **notes read** offset、Compose 帧 `165`；本条**不重复** 570 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F04 JSON run* 接线

- `JsonWiringPresentation.runValidate` / `runQuickFormat` / `runCompress` / `runFormatAdvanced` / `runQueryPath` / `runTransform`（`TransformOutcome` 对齐 `ConfigWiringPresentation`）。
- `JsonScreen` 工具栏格式化/压缩、检查器高级格式化、转换/JSONPath/Schema 推断与状态栏校验经 Presentation，不再分散 `runCatching` + 直接 `JsonEngine`。

### 样式（CSS 组件批次，非 570）

- `mooJsonInspectorSectionResult`（F04 `.inspector-section--result` 72dp 最小高度）。
- `mooJsonInspectorPathActions`（JSONPath 输入 +「复制」行 7dp 间距，对齐 `.inspector-action`）。

### Vault MCP stdio（双库 + search→read 余量）

- `subprocessDualVaultJsonSearchThenReadHonorsBodyOffset`：双库 access 同会话 **json search offset** 后再 **read** body offset/length（**非** 570 双库 notes read / 568 双库 json search only 链）。

### 证据脚本

- `mootool_evidence_print_json_inspector_copy_path_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F04 Compose 帧 `166` 提示。

### Compose 证据

- `JsonInspectorCopyPathCaptureTest` → `166-compose-json-inspector-copy-path-tab-focus.png`（非产品主窗）。

## 验证

- `JsonWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `JsonInspectorCopyPathCaptureTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 JSON「复制」外描边/IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
