# DIFF-114：JSON Vault 更多菜单与 Git 溢出文案

对照 `JsonVaultPanel.tsx` 的 `vault-more-menu` 与工具栏溢出 Git 项。

## 行为

- Vault 侧栏增加 **「更多 Vault 操作」** `MooMenu`：重命名/移动/复制片段/刷新/打开目录（无选中文件时前三项禁用）。
- 窄屏工具栏溢出菜单中 Git 项显示变更数 `(n)` / `(99+)`；随手记溢出菜单同步。

## 文件

- `JsonScreen.kt`、`QuickNoteScreen.kt`
- `VaultGitBadge.kt`：`gitActionMenuLabel`
- `Translator.kt`：`json.vault.more`
- `VaultGitBadgeTest.kt`
