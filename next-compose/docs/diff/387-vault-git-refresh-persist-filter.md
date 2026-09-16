# DIFF-387：Vault Git `onVaultRefresh` 重建索引并重载打开文件

## 背景

Electron `refreshAfterGitAction` 调用 `loadTree()` + `reloadCurrentNoteFromDisk()`。Compose Git 成功/ pull 失败仅 `handle*VaultChange` 针对当前路径，未 `persistFilter` 重建树索引与 [DIFF-382](382-json-vault-refresh-reload-open.md)/[DIFF-383](383-quicknote-vault-refresh-reload-open.md) 干净重载。

## 行为

- JSON/随手记 `VaultGitDialog.onVaultRefresh` 调用 `notify*VaultTreeChanged()`（[DIFF-415](415-vault-git-manual-refresh-snapshot.md) 重载 `snapshot` + `persistFilter`），再保留既有 `handle*VaultChange` 冲突检测。

## 测试

- 复用 `JsonVaultReloadOpenFileTest` / `QuickNoteReloadOpenFileTest` 与 Git 集成单测。

## 验收

- F01/F04 Git + Vault；`desktopTest --offline`。
