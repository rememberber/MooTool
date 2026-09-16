# DIFF-407：Vault 自动 pull 对齐 Electron 工作区前置条件

## 背景

Electron `pullJsonVault` / `pullQuickNoteVault` 在编辑器干净之外，还要求 `status.changes` 为空、无 merge/冲突、已配置 remote 才执行自动 pull。Compose `pullVault` 此前仅跳过不可用仓库、无 remote 与 `merging`，可能在本地有未提交变更时仍自动 pull，与 Electron 不一致。

## 行为

- 新增 `VaultGitAutoPull.mayPullCleanWorkingTree`：`conflicts > 0`、`changes` 非空、`merging`、无 remote 等条件下不 pull。
- `AppContainer.pullVault`（定时 `VaultGitPullScheduler` 与 JSON/随手记 Vault 共用）调用上述判断；编辑器脏仍由 scheduler 的 `hasUnsavedEditorChanges` 拦截。

## 验收

- A03 / F01/F04 Vault Git；`VaultGitAutoPullTest`；`./gradlew :composeApp:desktopTest --offline`。
