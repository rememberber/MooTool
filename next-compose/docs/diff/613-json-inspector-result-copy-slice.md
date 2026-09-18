# DIFF-613：F04 检查器结果区复制 + 帧 208

基线：DIFF-612（工作区）。

## 范围

- **F04**：`JsonInspectorPresentation.runCopyResultText`/`resultCopyEnabled`/`shouldToastResultCopy*`；检查器结果 `MooCard` 标题行「复制」写入剪贴板（对齐弹层 `json.action.copy` + `json.notice.copied`）。
- **证据**：`JsonInspectorCopyResultCaptureTest` → `208-compose-json-inspector-copy-result-tab-focus.png`（**不重复** 166 路径复制 / 195 查询钮链）。

**不重复** 612：A03 rebase §C / 帧 207。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
