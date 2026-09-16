# DIFF-476：Vault 树「全部折叠」保留选中祖先

## 背景

Electron `resolveExpandedPaths(directories, 'collapseAll', selectedPath)` 在折叠全部目录时仍展开当前选中路径的祖先目录（见 `next/src/shared/vaultTreeExpand.ts` / `JsonVaultPanel.applyTreeExpandMode`）。Compose `vaultTreeExpandForMode("collapseAll")` 曾把所有目录设为 `false`，仅靠后续 `expandVaultPathForSelection` 补救，与 Electron 单步语义不一致，且在模式切换瞬间可能把选中项藏进折叠树。

## 变更

- 新增 `VaultTreeExpand.kt`：`vaultAncestorDirectoryPaths`、`vaultTreeExpandForMode(..., selectedPath)`。
- `VaultTreeList` 切换展开模式时传入 `selectedPath`；`expandVaultPathForSelection` 复用祖先路径辅助函数。
- `VaultTreeTest` 对照 Electron `vaultTreeExpand.test.ts`。

## 验证

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :composeApp:desktopTest --offline
```
