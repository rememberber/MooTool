# DIFF-600：F04 检查器 JSONPath 复制 toast 呈现层 + 帧 195

基线：DIFF-599（工作区）。

## 范围

- **F04**：`JsonInspectorPresentation.runCopyJsonPath` + `shouldToastPathCopySuccess`/`shouldToastPathCopyFailure`；`jsonInspectorCopyJsonPath` 统一经 Presentation（空路径不 toast）。
- **证据**：`JsonInspectorPathQueryCaptureTest` → `195-compose-json-inspector-path-query-tab-focus.png`（检查器路径区「查询」钮焦点，**不重复** 帧 `166` 复制路径）。

**不重复** 599：F08/F17/F22 / 帧 194。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
