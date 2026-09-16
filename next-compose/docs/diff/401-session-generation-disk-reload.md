# DIFF-401：备份恢复后刷新磁盘数据源 UI

## 背景

DIFF-400 原地重载 SQLite 工具会话，但图片库、收藏、HTTP 集合、Vault 树快照等仍依赖 `remember` 或仅监听 `data.directory` 的 `LaunchedEffect`；备份恢复未改 `data.directory` 时界面仍显示旧列表。

## 行为

- `SessionStoreReload.kt`：`OnSessionStoreReload` 辅助（供后续页面复用）。
- 各工具页在原有 `LaunchedEffect(settings.data.directory)` 上增加 `sessionGeneration` 依赖：Image、Regex、Cron、Color、Translation、Variables、HTTP、Host、JSON Vault、随手记 Vault。

## 验收

- A03；`desktopTest --offline`。
