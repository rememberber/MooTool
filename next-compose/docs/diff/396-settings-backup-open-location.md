# DIFF-396：设置备份路径「在文件夹中打开」

## 背景

Electron 备份行除展示路径外，提供 `openBackupLocation`（数据根、图片、Vault 等），并在打开前 `mkdir` 递归创建。DIFF-395 仅展示路径，无法直达目录。

## 行为

- `BackupOpenLocation` 与 `BackupInfo.pathForOpen`（DIFF-398 起数据库/配置分别打开 SQLite 与 `config/`）。
- `AppContainer.openBackupLocation`：创建目录后 `Desktop.open`。
- 设置备份路径行增加 `settings.backup.open` 按钮。

## 测试

- `BackupInfoTest` 补充 `pathForOpen` 断言。

## 验收

- A01/A03；`desktopTest --offline`。
