# DIFF-367：JSON Vault Git flush 提取与单测

## 背景

[DIFF-332](332-json-git-flush-skip-clean.md) 已在 `JsonScreen` 内联跳过干净片段；[DIFF-366](366-quicknote-git-flush-skip-clean.md) 将随手记 Git `onFlush` 提取为 `quickNoteGitFlushBeforeAction` 并加单测。JSON 仍内联，失败文案曾混用 `quickNote.saveFailed`。

## 变更

- 新增 `jsonGitFlushBeforeAction`（`JsonVaultSession.kt`）：未命名守卫、`isVaultEditorDirty()` 跳过写盘、脏时 `saveJsonVault(..., showToast = false)`，失败用 `json.notice.failed`。
- `JsonScreen` Git `onFlush` 委托该函数。
- 新增 `JsonGitFlushTest`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
