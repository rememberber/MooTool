# DIFF-337：Git 面板操作后立即刷新 Vault 角标

## 问题

Electron 在 `refreshAfterGitAction` / `refreshGitChangeCount` 中于 commit、pull、discard 等之后**立即**更新 Vault Git 角标。Compose 仅依赖 5s 轮询与关闭 Git 对话框时的 `gitCountRev++`（DIFF-111），面板内提交成功后角标可能仍显示旧变更数直至关窗或下一轮 poll。

## 行为

- `VaultGitDialog` 新增 `onGitStatusChanged`：仅在 `runAction()` 成功/ pull 失败后重载 status 时调用（**不**在 `load()` 打开/刷新面板时调用，对齐 Electron `load` 不触发 `refreshGitChangeCount`）。
- JSON / 随手记传入 `{ gitCountRev++; refresh() }`，与关窗逻辑一致；commit/discard 等后角标与 Vault 树立即更新。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
