# DIFF-338：Git 角标刷新不挂钩面板 load

## 问题

DIFF-337 初版在 `VaultGitDialog.load()` 末尾也调用 `onGitStatusChanged`，打开 Git 面板或点「刷新」会 `gitCountRev++` 并重扫 Vault 树。Electron `VaultGitDialog.load` 只更新面板内 status/history，角标由 `refreshGitChangeCount` 在 Git **动作**与轮询时更新。

## 行为

- `onGitStatusChanged` 仅从 `runAction()` 在 status/history 重载后触发。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
