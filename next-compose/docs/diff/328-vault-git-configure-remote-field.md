# DIFF-328：Vault Git 保存/删除远程后输入框与 Electron 一致

## 问题

Electron `configure-remote` 成功时：`updateSettings` → `load()` → `update({ remote: configuredRemote })`，保证输入框与用户提交的 URL（含空串删除）一致。Compose 在 `afterSuccess` 里改 `remote`，但随后 `syncRemoteField` 会按 status/设置覆盖，可能偏离用户刚保存的值。

## 行为

- `runAction` 增加可选 `remoteOverrideAfterReload`：在 `syncRemoteField` 之后写入（对齐 Electron `load` 后的 `remote` 赋值）。
- 远程行保存/删除使用 `remoteOverrideAfterReload = nextRemote`；`afterSuccess` 仅更新设置。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
