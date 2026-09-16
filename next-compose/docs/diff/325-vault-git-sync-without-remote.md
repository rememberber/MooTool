# DIFF-325：Vault Git 顶栏 ahead/behind 与 Electron 一致

## 问题

Electron `VaultGitDialog` 在 `status.repository` 为真时始终展示 `json.git.sync`（↑ahead ↓behind），不依赖是否已配置 remote。Compose 仅在 `status.remote.isNotBlank()` 时显示，本地仓库未配远程时缺少同步计数行。

## 行为

- 已初始化仓库时顶栏在分支名下方始终显示 `git.sync`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
