# DIFF-322：Vault Git 加载 Diff 对齐 Electron

## 问题

Electron `showWorkingDiff` / `showCommitDiff` 在请求前清空 `diff`，失败时 `toast.error` 并结束 `busy`。Compose `loadFileDiffs` 失败路径未 toast，且加载期间仍显示上一份 diff。

## 行为

- 开始加载时清空 `fileDiffs` / `selectedDiffPath`。
- `GitEngine.fileDiffs` 异常时 `toastError`，`finally` 恢复 `busy`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
