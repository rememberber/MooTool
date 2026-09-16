# DIFF-288：Host 查找条已替换计数 + 查找条「共找到」文案

## 问题

Electron `HostTool` 与 `FindReplaceBar` 使用 `replacedCount` 与 `findReplace.foundPrefix`（「共找到:」）。Compose Host 仅有替换能力、无计数；JSON/随手记/HTTP/Host 命中数文案为 `{count} 处匹配`，与 Electron 不一致。

## 行为

- `HostSessionSnapshot` / `HostSession`：`findReplacedCount` 随 `snapshotState`/`restore` 往返。
- Host 查找条：展示 `find.replacedPrefix`；打开/关闭/改查找词或选项时归零；单次替换 `+1`、全部替换设为 `count`。
- JSON/随手记/Host/HTTP 查找条命中数改为 `find.foundPrefix` + 数字；全部/单次替换成功不写 `json.find.matches`（见 [DIFF-301](301-host-quicknote-replace-all-notice.md)、[DIFF-306](306-host-replace-notice.md)）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
- `HostSessionSnapshotTest`
