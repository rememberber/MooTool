# DIFF-388：Vault 树刷新时立即刷新 Git 角标

## 背景

Electron `onJsonVaultChange` / `loadTree` 后调用 `refreshGitChangeCount()`。Compose `rememberVaultGitChangeCount` 主要 5s 轮询；`persistFilter`、磁盘监听 `handle*VaultChange` 未 bump 角标，保存/外部变更后角标可能滞后。

## 行为

- JSON/随手记 `persistFilter` 与 `VaultRevisionMonitor` 回调中 `gitCountRev++`，触发 `rememberVaultGitChangeCount` 立即重算。

## 测试

- 复用 `VaultGitBadge` / Git 相关单测；`desktopTest --offline` 全绿。

## 验收

- F01/F04 Vault Git 角标；A03 Git。
