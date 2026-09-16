# DIFF-343：随手记「更多」菜单 Vault 导入前保存

## 问题

[DIFF-342](342-quicknote-vault-import-save-dirty.md) 已为工具栏导入与 Vault 树拖放补 `saveIfNeeded`。窄屏下导入收入「更多」菜单，该入口仍直接 `importFile` + `openFile`，与工具栏行为不一致，可能丢失当前脏笔记。

## 行为

「更多」→ 导入：在选文件并成功导入、打开前与工具栏相同，先 `saveIfNeeded`；失败或冲突时中止导入。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
