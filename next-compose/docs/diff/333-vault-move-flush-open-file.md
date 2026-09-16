# DIFF-333：Vault 移动前保存当前打开文件

## 问题

Electron 随手记 `moveTreeEntry` 在 `affectsSelection && dirty` 时先 `saveCurrent(false)` 再移动。Compose JSON/随手记 Vault 树拖放与「移动」对话框可直接 `move`，当前打开且未保存时磁盘仍是旧内容，路径 retarget 后易丢编辑或冲突。

JSON Electron `moveSpecificEntry` 未显式 flush，但移动**正在编辑**的文件时 Compose 应与随手记同一 `affectsSelection` 语义先写盘。

## 行为

- `VaultMove.moveAffectsOpenPath(openPath, movedPath)`：移动条目等于当前打开路径，或其目录前缀包含当前打开路径。
- JSON：拖放与 `json-move` 对话框在 affects 时 `flushJsonVaultEditorIfDirty`；失败则中止移动并提示。
- 随手记：拖放与 `move` 对话框在 affects 时 `saveIfNeeded`；失败则中止。

## 验证

- `VaultMoveTest.moveAffectsOpenPathMatchesElectronAffectsSelection`
- `./gradlew :composeApp:desktopTest --offline` → **453/453**
