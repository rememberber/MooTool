# DIFF-414：随手记 Vault 导入记录 Git 检查点活动

## 背景

Electron `quick-note:save` 在落盘后记 `Update Quick Note`。JSON 导入打开单文件已在 Compose 记 `Update JSON snippet`（DIFF-412 链）。随手记工具栏/树拖放导入成功并打开笔记后此前未 `recordVaultActivity`，空闲自动提交计时与提交说明与 Electron 不一致。

## 行为

- `runQuickNoteVaultImport` 成功打开导入笔记后 `UPDATE_QUICK_NOTE`。
- 树拖放仅当单文件导入并打开当前笔记时同样记录（与 JSON 多文件拖放策略一致）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
