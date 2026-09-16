# DIFF-324：Vault Git 面板 `runAction` 异常 toast

## 问题

Electron `runAction` 在 `runVaultGitAction` 抛错时 `toast.error` 并 `busy = false`。Compose `runAction` 无 try/catch，IO 或 `GitEngine.status` 异常时可能长期 `busy = true`。

## 行为

- `runAction` 整体 try/catch/finally：失败 `toastError`，`finally` 保证 `busy = false`。
- flush 失败仍提前 return，由 `finally` 结束 busy。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
