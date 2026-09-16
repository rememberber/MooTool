# DIFF-425：Vault 结构性 CRUD 后 `rebaseline`

## 背景

[DIFF-424](424-vault-move-rename-note-own-write-helpers.md) 用 `noteOwnWrite` 抑制同路径写盘误报。**删除、移动、重命名**会改变路径集合，`noteOwnWrite` 无法覆盖旧路径移除；轮询仍可能触发 `handle*VaultChange` 与多余 Git 角标刷新。

## 行为

- `rebaselineAfterLocalCrud()`：包装 `VaultRevisionMonitor.rebaseline()`。
- JSON/随手记：删除确认、树拖移、Vault 对话框成功、树多文件拖放导入成功后调用。
- 树拖移不再单独 `noteOwnWrite`（由 rebaseline 覆盖）；同路径保存/单文件导入仍用 `noteOwnWrite`。

## 验证

- `VaultConflictEngineTest.rebaselineAfterLocalDeleteAvoidsSpuriousPoll`
- `./gradlew :composeApp:desktopTest --offline`（532/532）
