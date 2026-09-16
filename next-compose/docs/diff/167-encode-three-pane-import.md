# DIFF-167：编码解码三列分栏与 Electron 迁入

## 背景

Electron `EncodeTool` 使用 `storageKey="encode-panes"`（三列比例 1 : 0.32 : 1，最小 240 / 120 / 240）。compose 此前左/右 `weight`、中间固定 132dp，无法持久化拖动结果，设置迁入也未覆盖。

## 行为

- **F13**：左输入 / 中操作条 / 右输出各列可拖（双 `VerticalPaneHandle`），宽度写入 `layout.paneSizes[encode]` 索引 0（左）与 1（中）。
- **A03**：`ElectronPaneSizeImport.mapIoThreePane` 处理 `encode-panes` 三列比例 → compose dp。

## 验证

- `ElectronPaneSizeImportTest.convertsEncodeThreePaneRatios`
- `./gradlew :composeApp:desktopTest --offline`
