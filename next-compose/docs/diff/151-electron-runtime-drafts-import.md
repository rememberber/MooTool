# DIFF-151：Electron `runtime.drafts` / `runtime.options` → 代码运行会话

## 背景

Electron `mootool-next.json` 的 `settings.runtime` 除可执行路径外，还在 `drafts` 与 `options`（`arguments` / `workingDirectory`）中保存四语言运行台草稿。DIFF-150 仅合并路径类 `RuntimeSettings` 字段；草稿仍留在 Electron 存储中，compose 代码运行会话（`CodeRunSessionSnapshot` / `ToolId.Java` 会话存储）未迁入。

## 行为

- `ElectronNextSettingsImport.loadCodeRunPatchFromStore` 从 Electron 商店 JSON 解析 `runtime.drafts` 与 `runtime.options`。
- `mergeCodeRunSnapshots`：导入侧非空字段覆盖当前会话对应字段；代码长度受 `CodeRunEngine.MAX_CODE_BYTES` 截断。
- 跨产品导入确认后，在设置合并之后调用 `AppContainer.mergeElectronCodeRunFromStore(electronStorePath)` 并 `persistCodeRun()`。

## 证据

- `ElectronNextSettingsImportTest.loadsRuntimeDraftsAndOptionsIntoCodeRunPatch`

## 仍未覆盖

- SQLite `t_func_content` 写入工具会话见 [DIFF-152](152-legacy-tool-draft-sessions.md)；仍同时写入 history。
- `t_next_migration_run` 逐行幂等与 safeStorage 密文迁移未做。
