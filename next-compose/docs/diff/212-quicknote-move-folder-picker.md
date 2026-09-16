# DIFF-212：随手记「移动」目标文件夹下拉

## 背景

Electron `ActionDialog` 在 `move` 模式用 `<select>` 列出 Vault 根目录与各文件夹，并排除被移动路径及其子目录；初始值为当前父目录。

Compose 此前用自由文本「空为根」，与 JSON Vault 移动下拉（DIFF-115）及 Electron 不一致。

## 行为

- `quickNoteMoveFolderOptions`：根目录选项 + 过滤后的目录列表。
- 移动对话框使用 `VaultSortMenu`；打开时 `dialogValue` 为 `VaultSelectionPath.parentDirectory`。
- 提交时 `vault.move(source, dialogValue)` 并更新 `vaultSelectedPath`。

## 验证

- `QuickNoteMoveFolderOptionsTest`：移动**文件**时祖先目录仍可选（与 Electron `selectedPath` 为条目路径一致）；移动**目录**时排除自身及子目录。
- `./gradlew :composeApp:desktopTest --offline`（与 DIFF-213 后合计 372/372）
