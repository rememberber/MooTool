# DIFF-370：JSON Vault 空闲自动保存失败 `notice`

## 背景

随手记 250ms idle 自动保存失败会经 `quickNoteSaveCurrent` 写入 `session.error`（[DIFF-199](199-quicknote-idle-autosave.md)）。JSON 同类 `LaunchedEffect` 调用 `saveJsonVault(..., showToast = false)` 但 **失败时静默**，与 [DIFF-363](363-vault-prepare-save-failure-feedback.md) 的 Vault 保存反馈不一致。

## 行为

- 新增 `jsonVaultIdleAutosaveAttempt`：成功返回 `true`；失败写入 `session.notice` 并返回 `false`。
- `JsonScreen` 空闲保存协程改用该辅助函数。

## 测试

- `JsonVaultIdleAutosaveTest`（冲突 notice + 成功落盘）

## 验收

- F04 Vault 自动保存；`desktopTest --offline`。
