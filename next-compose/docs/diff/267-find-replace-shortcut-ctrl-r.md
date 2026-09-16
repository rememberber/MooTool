# DIFF-267：Cmd/Ctrl+R 打开查找替换

## 对照 Electron

JSON / 随手记 / Host 在 `metaKey/ctrlKey` 且非 Shift/Alt 时，`f` 与 `r` 均触发 `openFindReplace`（Host 此前 compose 已支持 F/R，JSON/随手记仅 F）。

## 行为

- `FindReplaceShortcutPolicy.opensFindReplace` 统一判断
- JSON / 随手记 增加 `Cmd/Ctrl+R`；Host 改为同一策略（排除 Shift/Alt，避免与格式化等冲突）

## 验证

- `FindReplaceShortcutPolicyTest`
- `LegacyMigrationHintPolicyTest` 补 `groupManagerOpen`
- `./gradlew :composeApp:desktopTest --offline`
