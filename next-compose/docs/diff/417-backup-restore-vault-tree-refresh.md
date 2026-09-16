# DIFF-417：备份恢复后刷新 JSON/随手记 Vault 树

## 背景

`restoreBackup` 会 `reloadAllToolSessionsFromStore()` 并递增 `sessionGeneration`，但用户若正停留在 JSON/随手记页，仅靠 `tick`/`sessionGeneration` 的 `LaunchedEffect` 重抓 `snapshot` 时机与 Electron 全量 `loadTree()` 不完全一致；且与 [DIFF-415](415-vault-git-manual-refresh-snapshot.md) 统一的 `*-vault:changed` 信号应对齐。

## 行为

- `AppContainer.restoreBackup` 经 `reloadToolSessionsFromStore()` 刷新 Vault 树（notify 逻辑已集中到 [DIFF-418](418-reload-sessions-vault-tree-notify.md)）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
