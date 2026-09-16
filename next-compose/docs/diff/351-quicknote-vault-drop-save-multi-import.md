# DIFF-351：随手记 Vault 树拖放多文件导入前保存

## 问题

[DIFF-342](342-quicknote-vault-import-save-dirty.md) 仅在**单文件**将自动打开时于拖放前 `saveIfNeeded`；一次拖入多个文件只入库不切换时不保存，当前脏笔记可能仍在编辑器中但未落盘。工具栏导入（[DIFF-350](350-quicknote-vault-import-selection-path.md)）已始终先保存。

## 行为

- Vault 树区域拖放：任意数量有效文件导入前均 `saveIfNeeded`；失败则中止。
- 导入循环复用 `importQuickNoteVaultFile`（与工具栏一致）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
