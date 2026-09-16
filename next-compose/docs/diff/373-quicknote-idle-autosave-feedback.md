# DIFF-373：随手记空闲自动保存失败刷新 UI

## 背景

JSON [DIFF-370](370-json-vault-idle-autosave-notice.md) 在 idle 保存后始终 `refresh()`，冲突/失败时状态栏 `notice` 立即可见。

随手记 idle 协程在 `quickNoteSaveCurrent` 失败时虽写入 `session.error`，但仅成功路径调用 `persistFilter()`，失败时未 `bump`，状态栏可能迟迟不更新。

## 行为

- 新增 `quickNoteIdleAutosaveAttempt`（封装 `showToast = false` 的 `quickNoteSaveCurrent`）。
- idle `LaunchedEffect`：成功仍 `persistFilter()`；失败 `sessionManager.bump()` 以刷新 `error` 展示。

## 测试

- `QuickNoteIdleAutosaveTest`

## 验收

- F01 自动保存反馈；`desktopTest --offline`。
