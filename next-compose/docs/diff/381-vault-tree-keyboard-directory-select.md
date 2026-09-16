# DIFF-381：Vault 树键盘 Enter 选中目录

## 背景

鼠标单击目录会 `onSelect`（JSON/随手记用于 flush、更新 `vaultSelectedPath`、底栏新建父路径）。键盘 Enter 在目录上仅折叠/展开，未调用 `onSelect`，方向键聚焦目录后无法作为新建/导入目标。

## 行为

- `applyVaultTreeKey`：目录 Enter 返回 `selectPath`（仍切换 `expanded`）。
- `VaultTreeList`：处理 `selectPath` 时调用 `onSelect`。

## 测试

- `VaultTreeKeyTest.arrowsMoveAmongVisibleRowsAndEnterOpensFiles`

## 验收

- F01/F04 Vault 键盘；`desktopTest --offline`。
