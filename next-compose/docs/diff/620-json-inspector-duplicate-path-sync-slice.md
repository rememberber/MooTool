# DIFF-620：F04 检查器重复键路径同步 JSONPath + 预览 + 复制

基线：DIFF-619（工作区）。

## 范围

- **F04**：结构面板重复键路径单击：`jsonInspectorDuplicatePathClick` 写入 `jsonPath`、结果区 `pathResult` 预览（对齐内联路径树单击 / `applyInlinePathTreePreview`），并保留 DIFF-489 剪贴板复制 + toast。
- **单测**：`JsonInspectorDuplicatePathTest` 锁定预览与复制链。

**不重复** 619：A03 Git fetch 帧 `214`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
