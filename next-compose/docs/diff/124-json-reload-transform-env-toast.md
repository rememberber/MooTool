# DIFF-124：JSON 转换 toast、工具激活 Vault 重载、环境变量 toast

## JSON（F04）

- `transform` / `showResult` 成功与失败对齐 Electron `runTransform` / `showError`：`toastSuccess` / `toastError`。
- 切回 JSON 页（或分离窗）、Vault 路径/隐藏 gitignore 变更时：刷新树；若当前文件无未保存编辑且磁盘内容变化，从盘重载并提示 `vault.conflict.reloaded`（对照 `JsonVaultPanel` `reloadSelectedFromDisk`）。

## F01 随手记

- 同上：切回工具/路径变更时刷新树并在干净状态下重载当前笔记。

## F08 环境变量

- 保存编辑值成功 → `variables.saved` toast（对照 `VariablesTool.tsx`）。

## 文件

- `JsonScreen.kt`、`QuickNoteScreen.kt`、`VariablesScreen.kt`
