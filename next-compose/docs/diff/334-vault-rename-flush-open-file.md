# DIFF-334：Vault 重命名前保存当前打开文件

## 问题

DIFF-333 已在拖放/移动前 flush。重命名当前打开文件（或其所处目录）时若编辑器仍脏，`prepareJsonVaultContext` 对「目标即当前文件」直接放行，提交重命名会把**旧内容**文件改路径，编辑器未保存编辑会丢失或与磁盘不一致。

随手记 Electron `moveTreeEntry` 在 affects 时先保存；重命名对话框应对同一 `affectsSelection` 语义。

## 行为

- JSON Vault 文本对话框 `json-rename`：若 `VaultMove.moveAffectsOpenPath(currentFile, target)` 则先 `flushJsonVaultEditorIfDirty`，失败则中止。
- 随手记 `rename` 对话框：同样条件下 `saveIfNeeded`，失败则中止。

## 验证

- 复用 `VaultMoveTest.moveAffectsOpenPathMatchesElectronAffectsSelection`
- `./gradlew :composeApp:desktopTest --offline`
