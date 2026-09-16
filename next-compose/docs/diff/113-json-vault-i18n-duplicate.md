# DIFF-113：JSON Vault 文案与复制片段

对照 Electron `messages.ts` 中 `json.vault.*` 菜单文案。

## 变更

- 右键/底栏使用 `json.vault.rename` / `move` / `duplicate` / `moved` / `duplicated`（不再混用 `quickNote.*`）。
- 复制片段成功时 `toastSuccess`（与 Electron toast 一致）。
- `formatVaultGitBadgeCount` 单测（DIFF-111 角标 `99+` 规则）。

## 文件

- `Translator.kt`、`JsonScreen.kt`
- `VaultGitBadge.kt`、`VaultGitBadgeTest.kt`
