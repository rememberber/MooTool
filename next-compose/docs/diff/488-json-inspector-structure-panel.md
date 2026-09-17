# DIFF-488：JSON 检查器结构面板（键统计、重复键、UTF-8）

## 背景

[DIFF-487](487-json-validate-structure-summary.md) 已在状态栏展示结构摘要。Tauri `JsonToolSurface` 检查器顶部的 `json-analysis` 区块还提供根类型、节点/键/深度、重复键计数与路径列表、UTF-8 字节长度。Compose 检查器此前仅有格式化/转换/JSONPath，缺少该结构面板。

## 行为

- `JsonAnalysis.bytes`：有效 JSON 时按 UTF-8 编码字节数（对齐 Tauri `analyzeJson.bytes`）。
- 检查器新增「结构」卡片（位于关闭钮之下、格式化选项之上）：展示根类型、节点、键、最大深度、重复键数量（>0 时强调色）、`{bytes} B`；有重复键时列出 JSONPath 风格路径。
- 重复键检测使用 `JsonEngine.findDuplicateKeys`，`ignoreCase` 与检查器「排序和重复检查忽略大小写」开关一致。

## 验证

- `JsonEngineTest.analyzeStructure_matchesTauriFixture`（含 `bytes: 15`）
- `JsonInspectorStructureTest`
- `./gradlew :composeApp:desktopTest --offline`
