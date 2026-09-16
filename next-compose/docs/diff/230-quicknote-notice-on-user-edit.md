# DIFF-230：随手记用户编辑清空状态栏 notice

## 背景

[DIFF-227](227-json-editor-clear-inspector-notice.md) 已为 JSON 工具在用户改文档时清空 `notice`（列编辑闩锁保留提示）。随手记状态栏右侧同样展示 `session.notice`，此前复制/保存等提示会在用户继续输入后残留。

## 行为

- `EditorBuffer.onUserDocumentChange`：非列编辑闩锁时清空 `notice`；闩锁开启时恢复 `quickNote.columnEdit.hint`。
- `openFile` 加载 Vault 笔记时清空 `notice`（外部冲突重载等仍可在之后写入专用提示）。

## 验证

- `QuickNoteNoticeTest`
- `./gradlew :composeApp:desktopTest --offline`
