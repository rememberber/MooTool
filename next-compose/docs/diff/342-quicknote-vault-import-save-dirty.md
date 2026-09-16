# DIFF-342：随手记 Vault 导入/拖放打开前保存脏笔记

## 问题

[DIFF-341](341-json-vault-import-flush-dirty.md) 已为 JSON 拖入 Vault 补 flush。随手记工具栏 **导入** 与 Vault 树拖放（[DIFF-118](118-vault-drop-toast-quicknote.md)）在 `openFile` 切换笔记前未 `saveIfNeeded`，可能丢失当前未保存编辑。

## 行为

- 工具栏导入：导入并打开前先 `saveIfNeeded`。
- Vault 树拖放：仅当**单文件**将自动打开时，在导入循环前先 `saveIfNeeded`；多文件只入库不切换时不强制保存。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
