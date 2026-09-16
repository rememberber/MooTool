# DIFF-165：正则 / 格式化文件分栏与 Electron 迁入

## 背景

Electron 正则测试区 `storageKey="regex-test"`（默认 710:290，最小 320/220）；格式化文件 Tab `reformat-file`（1:1，最小 260）。compose 此前固定 290dp 结果列或等比 `weight`，拖动宽度无法持久化，设置迁入也未覆盖。

## 行为

- **F15**：测试 Tab 源码/结果区 `VerticalPaneHandle`，宽度写入 `layout.paneSizes[regex]` 索引 0。
- **F03**：文件 Tab 原文/结果 `VerticalPaneHandle`，写入 `layout.paneSizes[reformat]` 索引 0。
- **A03**：`ElectronPaneSizeImport` 增加 `regex-test`、`reformat-file` 比例映射（参考宽 1320dp；regex 左栏上限 900dp）。

## 验证

- `ElectronPaneSizeImportTest.convertsRegexAndReformatFileRatios`
- `./gradlew :composeApp:desktopTest --offline`
