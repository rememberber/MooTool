# DIFF-458：JSONPath 弹层双击选路径（对齐 Electron `onDoubleClick`）

## 背景

Electron `JsonPathPicker` 双击路径行调用 `onChoose(path)`，与「使用此路径」相同（不写 `pathResult`、不查询）。DIFF-457 已提取 `JsonPathPickerDialog` 并覆盖「使用」按钮；弹层双击仍缺 Compose UI 回归。

## 行为

- `JsonPathListRow` 为路径行设置 `contentDescription = entry.path`（`mergeDescendants`），供弹层与内联树测试定位。
- `JsonPathPickerDialogInteractionTest.doubleTapRowAppliesPathWithoutQuery`：双击 `$.store.books[0].title` 后关闭弹层、写入 `jsonPath`、清空 `pathResult`、仅 `json.notice.pathApplied`（DIFF-319）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（616/616）
