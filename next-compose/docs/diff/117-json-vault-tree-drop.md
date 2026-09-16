# DIFF-117：JSON Vault 树区域拖入 .json

补充 DIFF-116（编辑器拖放）：将 `.json` 文件拖到 Vault 树区域导入。

## 行为

- 导入到当前打开文件的父目录；无选中文件时导入到 Vault 根目录。
- 仅处理 `.json`；单文件导入后自动打开；多文件只刷新树并提示已导入。
- 路径辅助：`jsonVaultImportRelativePath` / `jsonVaultEntryRelativePath`（单测）。

## 文件

- `JsonScreen.kt`、`JsonVault.kt`
- `JsonVaultImportPathTest.kt`
