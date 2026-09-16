# DIFF-095：Host P5 工具栏、UA 分栏持久化、二维码 file-drop

- 编号：DIFF-095
- 影响：F10 Host；F12 UA；F17 二维码；`layout.paneSizes`
- 日期：2026-09-15

## 原行为（Electron）

- `.host-toolbar` 与 `.p5-tool .toolbar-button`（30px 高）
- UA `ResizableColumns`：`defaultSizes [0.9, 1.1]`、`minPaneWidths [300, 300]`、`storageKey="ua-parser"`
- 二维码 Logo 选择 + 文件名省略（与 `.file-drop-row` / `.selected-file-name` 同类）

## 本产品行为

- Host 编辑条「保存 / 应用 / 恢复」使用 `MooButton(p5Toolbar = true)`
- UA 工作区左栏宽度写入 `settings.layout.paneSizes["uaParse"]`，`VerticalPaneHandle` 拖动/双击重置，默认约 45% 宽、最小 300
- 二维码 Logo 行使用 `FileDropRow`（空名显示 `—`）

## 证据

`LayoutPaneSizesTest` 含 `uaParse`；`desktopTest` 见 `docs/acceptance.md`。

## 未做

file-drop 拖放、UA 工具栏 `p5Toolbar`、系统 IME、产品窗 Tab 帧、三平台安装仍未测。
