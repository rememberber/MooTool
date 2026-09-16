# DIFF-164：文本对比 / 网络分栏持久化与 Electron 迁入

## 背景

Electron `TextDiffTool` 使用 `storageKey="text-diff"`（1:1 列比例，最小 240dp）；`NetTool` 使用 `network-workspace`（默认 1.15:0.85，最小 340/300）。DIFF-163 已迁入 HTTP/Host 等键，但未覆盖上述两项；compose 侧此前用固定 `weight` 分栏，拖动宽度无法写入 `layout.paneSizes`。

## 行为

- **F02**：并排模式下左栏宽度可拖动，写入 `ToolId.TextDiff` 索引 0；双击手柄恢复默认 50%。统一三栏模式仍用等比 `weight`（与 Electron 仅两栏可拖一致）。
- **F11**：输出区宽度可拖动，写入 `ToolId.Net` 索引 0；默认按 1.15:0.85 相对内容区计算；最小列宽 340/300dp。
- **A03**：`ElectronPaneSizeImport` 增加 `text-diff`、`network-workspace` 比例 → compose dp 映射（参考宽 1320dp）。

## 验证

- `ElectronPaneSizeImportTest.convertsTextDiffAndNetworkWorkspaceRatios`
- `./gradlew :composeApp:desktopTest --offline`
