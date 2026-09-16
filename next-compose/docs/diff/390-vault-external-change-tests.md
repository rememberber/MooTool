# DIFF-390：Vault 外部变更删除/重载单测补全

## 背景

DIFF-389 落地 `*ApplyExternalChange` 干净时总重载当前文件；需固定随手记与「打开文件已从盘删除」的回归。

## 行为

无产品行为变更；补单测：

- `QuickNoteVaultExternalChangeTest`：干净时路径不在 `paths` 仍重载；干净时文件已删仍清空会话（对齐 Electron `reloadCurrentNoteFromDisk` catch）。
- `JsonVaultExternalChangeTest.cleanOpenFileClearsWhenDeletedRegardlessOfPathsList`：同上 JSON。
- `QuickNoteReloadOpenFileTest`：缺失文件时断言 `vaultSelectedPath` 清空。

## 验收

- F01/F04 Vault；`desktopTest --offline`。
