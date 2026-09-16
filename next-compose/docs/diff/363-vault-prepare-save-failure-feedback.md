# DIFF-363：Vault prepare / saveIfNeeded 失败统一状态栏反馈

## 背景

随手记 Vault 右键/底栏经 `prepareQuickNoteVaultContext` 保存失败时仅 `refresh()`，状态栏可能无 `error`（与 DIFF-362 打开/拖移路径不一致）。JSON Vault 上下文经 `prepareJsonVaultContext` flush 失败时未写 `notice`，与树 `onOpen`/`onMove` 的 flush 失败处理不一致。

## 变更

- `saveIfNeeded`：返回 `false` 时 `session.error = session.error.ifBlank { quickNote.saveFailed }`（覆盖右键/底栏/导入/拖放等所有调用点）。
- `prepareJsonVaultContext`：`saveJsonVault` 失败时写入 `session.notice`（与 `json.notice.failed` / 冲突文案一致）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
