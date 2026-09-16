# DIFF-163：Electron 分栏比例迁入 P5 工作区

## 背景

DIFF-162 仅处理 JSON。Electron 设置里还有 `http-workspace`、`host-workspace`、`quick-note-tree-*`、`runtime-editor-output` 等键，均为归一化比例；compose 用各 `ToolId` 的 dp 分栏。

## 变更

扩展 `ElectronPaneSizeImport`：
- HTTP / Host / 图片库 / 翻译单词本 / UA：首列宽 → index 0，保留 compose 已有 index 1（HTTP 响应区等）。
- 代码运行 `runtime-editor-output`：首行比例 → 输出区高度 index 0。
- 随手记 `quick-note-tree-replace`（Vault + 快速替换）、`quick-note-tree-no-replace`（仅 Vault）。

## 测试

- `ElectronPaneSizeImportTest.convertsHttpHostAndQuickNoteWorkspaceRatios`

## 未覆盖

- 随手记 `no-tree` 组合键、Net/文本对比等未持久化分栏的工具。
