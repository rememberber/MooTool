# DIFF-394：`data.directory` 变更后工具页与监视器重载

## 背景

DIFF-393 使 `data.directory` 运行时切换 `dataRoot` 与 SQLite。Host/HTTP/图片/环境变量等页面仍缓存旧 `EnvStoreConfig`/`HostApplyConfig` 或列表，JSON Vault 磁盘监视未随有效根重启。

## 行为

- 迁移指纹/草稿键路径到 `dataDirectories()`。
- JSON `VaultRevisionMonitor` 依赖 `jsonVaultRootKey`（含 `data.directory`）。
- Host/HTTP/图片/环境变量/翻译：`remember(settings.data.directory)` 重建 store 配置；`LaunchedEffect(data.directory)` 重载列表/快照（Host 选中方案缺失时清空）。
- 正则/Cron/调色板收藏：`data.directory` 变更时重载 favorites 数据。
- 侧栏状态栏展示有效 `dataRoot`。
- 单测：`SettingsDataDirectorySanitizeTest`、`AppDatabaseRebindTest`。

## 验收

- A01/F08/F09/F10/F23；`desktopTest --offline`。
