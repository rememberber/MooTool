# DIFF-254：窗口激活时聚焦编辑器

## 对照 Electron

`useFocusOnWindowActivate` 在工具页激活且未打开查找/历史/路径选择等遮罩时，将焦点还给主编辑器。

## 行为

- `FocusEditorOnWindowActivate` 经 `LocalAwtWindow`（主窗/分离窗根部注入）监听 AWT `windowGainedFocus`。
- `jsonEditorAutoFocusEnabled` / `quickNoteEditorAutoFocusEnabled` 对照 Electron 守卫条件。
- `JsonEditorWindowFocus`、`QuickNoteEditorWindowFocus` 挂到 F04/F01 屏幕。

## 验证

- `JsonEditorFocusPolicyTest`、`QuickNoteEditorAutoFocusPolicyTest`
- `./gradlew :composeApp:desktopTest --offline`

系统 IME 预编辑仍须手工验收，见 `docs/evidence/2026-09-16-editor-manual-acceptance/`。
