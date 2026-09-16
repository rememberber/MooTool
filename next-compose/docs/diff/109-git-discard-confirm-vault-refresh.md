# DIFF-109：Git 丢弃确认与工作区刷新

对照 `next/src/features/json/VaultGitDialog.tsx`。

## 行为

- 丢弃变更前弹出 overlay 确认（`git.confirmDiscard`），确认后才 `GitEngine.discard`。
- `pull` / `discard` / `abort-merge` / `resolve-conflict` / `continue-operation` 成功后调用 `onVaultRefresh`，JSON 与随手记重新加载当前文件并刷新 Vault 树（与 Electron `refreshAfterGitAction` 一致）。
- `pull` 失败时同样 `onVaultRefresh`，对齐 Electron 在 pull 失败后 `load` + `onVaultChange`。

## 文件

- `VaultGitDialog.kt`：`GitWorkingTreeAction`、`confirmDiscardPath`、`onVaultRefresh`
- `JsonScreen.kt` / `QuickNoteScreen.kt`：传入 `onVaultRefresh`
- `Translator.kt`：`git.confirmDiscard`
