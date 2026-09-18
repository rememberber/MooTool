# DIFF-603：F04 检查器 JSONPath 查询失败 toast 呈现层 + 帧 198

基线：DIFF-602（工作区）。

## 范围

- **F04**：`JsonInspectorPresentation.shouldToastPathQueryFailure`（对齐 Electron `showError`）；`notifyJsonInspectorPathQueryFailure` 用于检查器「查询」钮与内联路径树双击失败；路径复制 toast 提取为 `notifyJsonInspectorPathCopySuccess`/`notifyJsonInspectorPathCopyFailure`。
- **证据**：`JsonInspectorInferSchemaCaptureTest` → `198-compose-json-inspector-infer-schema-tab-focus.png`（结构区 Schema 钮焦点，**不重复** 帧 `195` 查询钮）。

**不重复** 602：F14 验签 / 帧 197。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
