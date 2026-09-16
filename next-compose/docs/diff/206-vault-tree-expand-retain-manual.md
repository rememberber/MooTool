# DIFF-206：Vault 树选中展开不重置手动折叠状态

## 背景

DIFF-205 在 `LaunchedEffect(expandMode, items, selectedPath)` 中每次 `selectedPath` 变化都会 `expanded.clear()` 并按 `expandMode` 重建，用户手动展开的其它分支会被清掉。

## 行为

- 仅在 `expandMode` 或 `items` 变化时重置展开状态。
- `selectedPath` 变化时只调用 `expandVaultPathForSelection`，保留用户已展开的分支。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
