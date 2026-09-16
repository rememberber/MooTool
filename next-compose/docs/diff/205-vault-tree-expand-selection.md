# DIFF-205：Vault 树按选中路径展开祖先目录

## 背景

Electron JSON/随手记 Vault 在创建文件夹后会把新目录加入 `expanded`，并选中该节点；树刷新后仍能看到新节点。

Compose `VaultTreeList` 在 `items` 变化时会按 `expandMode` 重置展开状态（smart 模式仅展开根级目录）。DIFF-202/204 建夹后虽更新了 `vaultSelectedPath`，嵌套目录在 smart 模式下父级仍折叠，新节点不可见。

## 行为

- 新增 `expandVaultPathForSelection`：对选中路径展开全部祖先目录；若选中的是目录则展开该目录。
- `VaultTreeList` 在 `expandMode`/`items` 初始化后，根据 `selectedPath` 应用上述展开（JSON 与随手记共用）。

## 验证

- `VaultTreeKeyTest.expandSelectionOpensAncestorsAndSelectedDirectory`
- `./gradlew :composeApp:desktopTest --offline`
