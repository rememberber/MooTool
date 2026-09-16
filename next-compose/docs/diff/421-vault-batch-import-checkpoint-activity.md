# DIFF-421：Vault 树多文件拖放导入记录 Git 检查点活动

## 背景

单文件导入并打开已在 DIFF-414（随手记）/JSON 树拖放路径记录 `UPDATE_*`。[DIFF-412](412-vault-git-checkpoint-activity.md) 对齐 Electron `recordActivity` 以驱动空闲自动提交计时。一次拖入多个文件仅入库、不切换当前文档时此前未 `recordVaultActivity`，自动检查点不知道 Vault 有批量落盘。

## 行为

- JSON / 随手记 `handle*VaultTreeFileDrop`：`importedCount > 1` 时各记一次 `UPDATE_JSON_SNIPPET` / `UPDATE_QUICK_NOTE`（与单文件打开分支互斥）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
