# DIFF-397：恢复备份后按新设置重绑数据根

## 背景

zip 恢复会写回 `settings` 与 `data/`。若备份内 `data.directory` 与恢复前不同，`BackupEngine.restore` 仍按恢复前的有效根写盘，随后从磁盘加载的设置可能指向另一 `dataRoot`，但 `AppDatabase` 仍连接旧 SQLite 路径。

## 行为

- `restoreBackup` 在 `settingsRepository.load()` 后：`ensureDataRootExists` + `database.rebindDataDirectories(dataDirectories())`。
- 撤销 MCP Vault 只读授权（与 `updateSettings` 改 `data.directory` 一致）。

## 验收

- A03；`desktopTest --offline`。
