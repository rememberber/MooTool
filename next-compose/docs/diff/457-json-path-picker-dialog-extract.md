# DIFF-457：JSONPath 弹层 `JsonPathPickerDialog` 提取与「使用」交互单测

## 背景

`JsonScreen` 内联的 `PathPickerDialog` 体量较大，不利于单测与 DIFF-319「使用」仅选路径、不自动查询的回归。Electron `JsonPathPicker` 的「使用」与双击共用 `onChoose(path)`。

## 行为

- 新增 `JsonPathPickerDialog.kt`：`internal fun JsonPathPickerDialog(container, session, onChanged)`，由 `JsonScreen` 在 `pathPickerOpen` 时调用。
- 「使用」按钮增加 `contentDescription = json.pathPicker.use`，便于 Compose 测试与无障碍语义对齐冲突对话框可点击控件。
- `JsonPathPickerDialogInteractionTest.useButtonAppliesPathWithoutQuery`：预置 `pathPickerSelection`，点击「使用」后关闭弹层、写入 `jsonPath`、清空 `pathResult`、仅 `json.notice.pathApplied`，不触发查询（DIFF-319）。
- `JsonPathPickerCaptureTest.capturePathPickerUseButtonFocusRing`：写出 Compose 场景帧 `148-compose-json-path-picker-use-tab-focus.png`（非产品主窗）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（615/615）
- 证据：`docs/evidence/2026-09-15-inspector-screencapture/windows/148-compose-json-path-picker-use-tab-focus.png`
