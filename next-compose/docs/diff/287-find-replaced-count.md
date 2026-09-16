# DIFF-287：JSON/随手记查找条「已替换」计数

## 问题

Electron `FindReplaceBar` 展示 `replacedCount`（`findReplace.replacedPrefix`）。Compose JSON/随手记查找条此前无对应计数与会话持久化。

## 行为

- `JsonSession` / `QuickNoteSession`：`findReplacedCount` 写入快照。
- 查找条展示 `find.replacedPrefix` + 计数。
- 打开查找、修改查找词/选项、关闭查找时计数归零。
- 单次替换成功 `+1`；全部替换成功设为替换次数。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
- `JsonSessionSnapshotTest` / `QuickNoteSessionSnapshotTest` round-trip `findReplacedCount`
