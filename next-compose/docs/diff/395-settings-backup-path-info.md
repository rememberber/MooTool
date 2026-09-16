# DIFF-395：设置备份路径预览

## 背景

Electron 设置「数据」分类在 `settings.data.directory` 变化时调用 `getBackupInfo()`，展示数据库、配置、图片与 Vault 实际路径。Compose 仅有 zip 导出/恢复，用户看不到有效根目录下的真实路径。

## 行为

- `BackupInfo` / `BackupInfoResolver` / `AppContainer.backupInfo()`：解析有效 `dataRoot`、SQLite、设置文件、`images/` 与当前 JSON/随手记 Vault 根。
- 设置页「备份」分组：`LaunchedEffect(data.directory, vault.*)` 刷新路径行（对照 Electron `useEffect(..., [settings.data.directory])`）。
- i18n：`settings.backup.database/settings/images/pathsHint` 等。

## 测试

- `BackupInfoTest`

## 验收

- A01 数据/备份、A03；`desktopTest --offline`。
