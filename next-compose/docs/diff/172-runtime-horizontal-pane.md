# DIFF-172：代码运行台左右分栏对齐 Electron

## 背景

Electron `RuntimeTool` 使用 `ResizableColumns` 水平布局（`runtime-editor-output`，1:1，最小 300/300）。compose 此前为「上编辑、下输出」纵向分栏，且迁入将比例误映射为输出区高度。

## 行为

- **F05**：工作区改为 `IoTwoPaneRow`（左源码 / 右输出），键 `runtime-editor-output`；输出区保留状态头、滚动正文与命令/退出码/耗时脚。
- **A03**：`ElectronPaneSizeImport` 对该键改为 `mapTwoColumnWorkspace`（宽度比例），不再写入输出高度。

## 验证

- `ElectronPaneSizeImportTest.convertsRuntimeEditorOutputRatios`
- `./gradlew :composeApp:desktopTest --offline`
