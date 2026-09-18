# DIFF-642：F04 检查器 Schema/结果/重复键 `*ActionEnabled` + A03 调度单测轮询

基线：DIFF-641（工作区）。

## 范围

- **F04**：`JsonInspectorPresentation.inferSchemaActionEnabled` / `resultCopyActionEnabled` / `duplicatePathClickActionEnabled`；`JsonScreen` / `JsonInspectorStructure` 对应 `enabled` 接线。
- **A03**：`UpdateAutoCheckSchedulerTest` 用 `waitUntil` 轮询替代固定 `delay`，消除全量负载下偶发 `expected:1 but was:0`。

**不重复** 641：641 为转换九动作网格；本切片为 Schema 生成、结果复制、重复键路径行。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
