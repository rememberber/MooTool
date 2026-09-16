# DIFF-327：Vault Git 远程输入框与 `load` 同步规则

## 问题

Electron `VaultGitDialog.load` 每次刷新后设置 `remote: status.remote || settings.vault.gitRemote`。Compose 仅在 `status.remote` 非空时覆盖，或当前输入为空时才回填设置，删除 origin 后输入框可能仍显示旧 URL，与仓库/设置不一致。

## 行为

- `syncRemoteField`：`remote = status.remote.ifBlank { settings.vault.gitRemote }`
- `load()` 与 `runAction` 成功/pull 失败后的 status 刷新均调用。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
