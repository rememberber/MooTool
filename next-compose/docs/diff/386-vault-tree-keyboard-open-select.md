# DIFF-386：Vault 树键盘 Enter 打开文件先 `onSelect`

## 背景

鼠标单击文件会先 `onSelect` 再 `onOpen`（目录 Enter 亦 `onSelect`，见 [DIFF-381](381-vault-tree-keyboard-directory-select.md)）。键盘 Enter 仅 `onOpen`，`vaultSelectedPath` 在 flush 失败时回滚逻辑与鼠标不一致。

## 行为

- 键盘 Enter 打开文件：先 `onSelect(entry)` 再 `onOpen(entry)`。

## 测试

- 复用 `VaultTreeKeyTest` / `openJsonVaultTreeFile` 单测；行为与鼠标一致。

## 验收

- F01/F04 Vault 键盘；`desktopTest --offline`。
