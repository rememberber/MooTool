# DIFF-622：F04 检查器重复键路径 Tab 焦点帧 + 端到端单测

基线：DIFF-621（工作区）。

## 范围

- **F04**：Compose 场景结构面板重复键路径 `mooJsonInspectorDuplicatePath` + `mooFocusClickable` Tab 焦点帧 `215`（`JsonInspectorDuplicatePathCaptureTest`）。
- **单测**：`JsonInspectorDuplicatePathTest` 改走 `jsonInspectorDuplicatePathClick` + 真实 `AppContainer` 剪贴板链。

**不重复** 620：重复键同步 JSONPath/预览逻辑已在 DIFF-620；本切片只补证据帧与 handler 端到端断言。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
