# DIFF-631：A03 冲突期 `automaticCheckpoint` 跳过登记

基线：DIFF-630（F05/A01 运行台 Wiring）。

## 范围

- **A03**：`GitEngine.automaticCheckpoint` 在 `merging || conflicts > 0` 时成功返回跳过文案、不写新提交（对齐 Electron `performAutomaticCheckpoint`）；独立单测 `GitAutomaticCheckpointGuardTest`（与 `GitEngineTest.resolvesAndAbortsMergeConflicts` 内嵌断言互补，登记 fixture）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
