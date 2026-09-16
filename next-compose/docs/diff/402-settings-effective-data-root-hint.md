# DIFF-402：设置页展示有效数据目录

## 背景

Electron 数据目录输入在留空时展示 `userData` 实际路径。Compose 仅有「留空使用默认」说明，运行时切换 `data.directory` 或从备份恢复设置后，用户不易确认当前 SQLite/Vault 使用的根。

## 行为

- 数据分类在目录输入下增加 `settings.dataPath.effectiveHint`（`dataDirectories().dataRoot`，等宽 11sp）。
- 备份路径预览 `LaunchedEffect` 增加 `sessionGeneration`，恢复备份后刷新。

## 验收

- A01；`desktopTest --offline`。
