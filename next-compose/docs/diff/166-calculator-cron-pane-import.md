# DIFF-166：计算器 / Cron 分栏与 Electron 迁入

## 背景

Electron `calculator-panels`（1:1，最小 360/320）、`cron-builder`（默认 660:340，最小 460/260）。compose 此前用 `weight` 或固定 340dp 表达式列，无法持久化用户拖动结果。

## 行为

- **F21**：输入面板与结果/历史区 `VerticalPaneHandle`，写入 `layout.paneSizes[calculator]` 索引 0。
- **F16**：字段构建器与表达式侧栏可拖，写入 `layout.paneSizes[cron]` 索引 0（默认约 66% 内容宽）。
- **A03**：`ElectronPaneSizeImport` 增加上述 storage key 映射。

## 验证

- `ElectronPaneSizeImportTest.convertsCalculatorAndCronBuilderRatios`
- `./gradlew :composeApp:desktopTest --offline`
