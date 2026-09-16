# DIFF-115：JSON Vault 移动目标文件夹选择

对照 `JsonVaultPanel.tsx` 移动对话框（`json.vault.move` / `moveTo` / 目录下拉）。

## 行为

- 移动弹层用 `VaultSortMenu` 选择目标文件夹（含「Vault 根目录」），不再手输路径。
- 打开移动时默认选中当前文件父目录。
- 成功提示 `json.vault.moved` + toast；重命名/新建文件夹标题改用 `json.vault.*` 文案。

## 文件

- `JsonScreen.kt`（`VaultPane` 弹层）
- `Translator.kt`：`json.vault.moveTo`、`json.vault.root`
