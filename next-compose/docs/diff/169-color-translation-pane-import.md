# DIFF-169：调色板 / 翻译编辑分栏与 Electron 迁入

## 背景

Electron `color-board`（0.34:0.66，最小 240/420）、`translation-editor`（1:1，最小 280）。compose 调色板用固定 `weight(0.34/0.66)`；翻译 Tab 源/目标等分 `weight`，且迁入未覆盖 `translation-editor`（单词本仍用 `translation-words` → `ToolId.Translation`）。

## 行为

- **F22**：当前色/对比色区与色板区可拖，写入 `layout.paneSizes[colorBoard]` 索引 0。
- **F20**：翻译 Tab 源文列宽可拖，写入 `layout.paneSizes["translation-editor"]` 索引 0。
- **A03**：`ElectronPaneSizeImport` 增加上述键映射。

## 验证

- `ElectronPaneSizeImportTest.convertsColorBoardAndTranslationEditorRatios`
- `./gradlew :composeApp:desktopTest --offline`
