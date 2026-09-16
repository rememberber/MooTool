# DIFF-152：SQLite `t_func_content` 草稿写入工具会话

## 背景

DIFF-147 将 `t_func_content` 迁入通用 `history`（`operation = draft`），与 Electron legacy 迁移写入 `t_func_history` 一致，但 Java 版用户期望草稿仍出现在对应工具编辑区（如 `JavaConsole`、`Regex`、`JsonBeauty`）。DIFF-151 仅覆盖 Electron `settings.runtime.drafts`。

## 行为

- `ImportedLegacyHistory` 增加 `legacyFunc`（SQLite 原始 `func` / `func_type`）。
- `LegacyToolDraftApplier` 在跨产品导入确认后，将 `operation = draft` 行写入：
  - 代码运行：`java` / `JavaConsole` / `groovy` / `python` / `node`
  - 正则：`Regex` → `pattern`
  - JSON：`JsonBeauty` / `json` → 编辑器正文
- 历史记录仍经 `HistoryRepository.mergeImport` 保留。

## 证据

- `LegacyToolDraftApplierTest.appliesCodeRunRegexAndJsonDrafts`
- `LegacyToolIdMapperTest`：`JavaConsole` → `ToolId.Java`

## 仍未覆盖

- 更多 `FuncConsts` 草稿映射与 Regex `source` 修正见 [DIFF-153](153-legacy-draft-func-parity.md)。
- Electron `t_next_migration_run` 幂等与 safeStorage 未做。
