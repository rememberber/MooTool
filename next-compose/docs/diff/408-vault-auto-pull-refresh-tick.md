# DIFF-408：自动 pull 后刷新 Vault UI

## 背景

Electron 在 `pullJsonVault` / `pullQuickNoteVault` 成功或进入 merge/冲突态后 `broadcast('*-vault:changed')`，驱动 Vault 树与干净编辑器重载。Compose 依赖 `VaultRevisionMonitor`，但自动 pull 后需显式对齐刷新信号（与手动 Git 面板 `onVaultRefresh` / `persistFilter` 一致）。

## 行为

- `VaultGitAutoPull.shouldNotifyVaultAfterPull`：pull 成功或 pull 后 `merging`/`conflicts > 0` 时通知。
- `AppContainer.notifyJsonVaultTreeChanged()` / `notifyQuickNoteVaultTreeChanged()` 递增 tick（自动 pull 与手动 Git/刷新 Vault 共用，见 [DIFF-415](415-vault-git-manual-refresh-snapshot.md)）。
- JSON/随手记屏 `LaunchedEffect`：重抓 snapshot 并 `persistFilter()`（含干净时重载当前文件、Git 角标 rev）。

## 验收

- A03 / F01/F04；`VaultGitAutoPullTest`；`./gradlew :composeApp:desktopTest --offline`。
