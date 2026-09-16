# DIFF-326：Vault Git `runAction` 失败时不盲目刷新状态

## 问题

Electron `runAction` 在 `result.success === false` 时：仅 **pull** 会 `load()` 并 `onVaultChange`；其它动作 `busy = false` 后直接 `return`，不重新拉 status/history。Compose 在任意失败后仍执行 `GitEngine.status` / `history`，与 Electron 不一致（可能掩盖用户正在查看的 diff/选中态，且无必要 IO）。

## 行为

- 失败且非 `Pull`：toast 后 `return`（`finally` 仍 `busy = false`）。
- 失败且为 `Pull`：toast、`onVaultRefresh()` 后仍刷新 status/history（对齐冲突/半成品 merge 状态）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
