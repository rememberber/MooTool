# DIFF-332：JSON Vault Git flush 跳过已保存文件

## 问题

Electron `prepareGitAction` 在 `!dirty` 时直接放行，不在 pull/continue 前写盘。Compose JSON `onFlush` 每次 Git flush 都调用 `saveJsonVault`（含 toast），即使 `editor.text == savedText`（250ms 自动保存后常见），多余 IO 且可能打断用户。

随手记 Git 已用 `saveIfNeeded`/`quickNoteDirty`；JSON 未对齐。

## 行为

- 已命名文件且 `editor.text == savedText`：`onFlush` 返回 `null`（无需写盘）。
- 仍有脏内容时 `saveJsonVault(..., showToast = false)`，避免 Git 前重复 success toast。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
