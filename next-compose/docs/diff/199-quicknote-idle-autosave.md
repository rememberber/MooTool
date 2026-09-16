# DIFF-199：随手记 250ms 空闲自动保存

## 背景

Electron `QuickNoteTool` 在 `dirty && selectedNotePath` 时 **250ms** debounce 调用 `persistSnapshot(..., false)`（无 toast），与 JSON Vault 空闲保存一致。

Compose F01 此前仅在切换文件、Git flush、手动保存时落盘。

## 行为

- `QuickNoteScreen` 增加与 F04 相同的 250ms debounce；冲突对话框打开时不自动保存。
- `saveCurrent(..., showToast = false)` 仍写入 Vault/frontmatter/附件清理与 `recordVaultActivity`，但不 toast、不写通用历史、不改 `notice`。
- 成功后 `persistFilter()` 刷新文档库索引。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
- 对照 `next/src/features/quickNote/QuickNoteTool.tsx` `persistSnapshotOnIdle`
