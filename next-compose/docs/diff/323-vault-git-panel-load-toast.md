# DIFF-323：Vault Git 面板 `load` 失败 toast

## 问题

Electron `VaultGitDialog.load` 在 `getVaultGitStatus` / `listVaultGitHistory` 失败时 `toast.error` 并结束 `busy`。Compose `load()` 无 try/catch，异常时可能长期 `busy = true` 且无用户可见反馈。

## 行为

- `load()` 用 try/catch/finally：`toastError` 后仍 `busy = false`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
