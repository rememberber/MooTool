# DIFF-336：随手记用户编辑清空状态栏 error

## 背景

[DIFF-335](335-quicknote-save-if-needed-clean.md) 后，干净文档不再因旧 `session.error` 阻塞 Vault/Git，但保存失败文案仍可能留在状态栏，用户继续输入后仍显示红色 error（`notice` 已在 DIFF-230 随编辑清空）。

## 行为

- `EditorBuffer.onUserDocumentChange`：清空 `session.error`（列编辑闩锁时仍保留列编辑 `notice`，error 不保留）。
- 与 JSON DIFF-227 清空过期 UI 反馈同一思路。

## 验证

- `QuickNoteNoticeTest.user_edit_clears_stale_error`
- `./gradlew :composeApp:desktopTest --offline`
