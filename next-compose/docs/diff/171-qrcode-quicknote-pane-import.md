# DIFF-171：二维码 / 随手记分栏与 Electron 迁入

## 背景

Electron：`qrcode-generate`、`qrcode-recognize` 为 1:1 双列；随手记外层 `quick-note-{tree|no-tree}-{replace|no-replace}`，分屏编辑 `quick-note-editor-preview`。

## 行为

- **F17**：生成/识别 Tab 使用 `IoTwoPaneRow`，键 `qrcode-generate` / `qrcode-recognize`（最小 300/280）。
- **F01**：分屏模式编辑/预览可拖，键 `quick-note-editor-preview`；Vault/快速替换分栏键与 Electron 一致，并从旧 `ToolId.QuickNote` 分栏回退读取。
- **A03**：`ElectronPaneSizeImport` 增加上述键及 `quick-note-no-tree-replace`。

## 验证

- `ElectronPaneSizeImportTest.convertsQrcodeAndQuickNoteEditorPreviewRatios`
- `./gradlew :composeApp:desktopTest --offline`
