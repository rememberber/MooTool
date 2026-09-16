# DIFF-398：备份路径打开区分 config 与 data

## 背景

本产品设置落在 `configRoot/config/`，SQLite 在有效 `dataRoot/`（见 `data-platform-release.md`）。DIFF-396 对「配置文件」行也打开 data 根，用户找不到 `mootool-compose.settings.json`。

Electron 的 `userData` 合并了配置与数据，其 `location: data` 合理；Compose 需按真实路径打开。

## 行为

- `BackupOpenLocation.DatabaseFile`：`revealInFileManager` 指向 SQLite 文件（父目录不存在则创建）。
- `BackupOpenLocation.SettingsConfig`：打开设置文件所在 `config/` 目录。
- 图片与 Vault 行不变。

## 测试

- `BackupInfoTest` 更新 `pathForOpen` 断言。

## 验收

- A01/A03；`desktopTest --offline`。
