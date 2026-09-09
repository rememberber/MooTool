# DIFF-022：本产品备份为带清单的 zip

- 编号：DIFF-022
- 影响：A03 备份/恢复
- 日期：2026-09-09

## 原行为（Electron）

`backupService.export` 把数据库文件（含 wal/shm）、settings、images、quick-notes、json-vault **复制到一个目录** `MooTool-backup-{timestamp}`，不是压缩包。无文件清单、无 SHA-256。恢复走独立迁移适配器，不是对称 unzip。凭据不在该目录方案里显式导出。

## 本产品行为

导出为 `MooTool-Next-Compose-backup-{timestamp}.zip`。`manifest.json` 含 `productId`、`appVersion`、`schemaVersion`、时间与每个文件的路径/大小/SHA-256。内容为设置、SQLite（checkpoint 后）、`data/` 下 Vault/图片/收藏/环境/Host/HTTP/翻译等；**排除** `data/backups`、cache、日志。凭据不打包。预览先拒绝 `..` 路径穿越，再校验 productId 与哈希。恢复前把当前快照写到 `data/backups/pre-restore-*`；失败回滚该快照。

## 理由

数据文档要求清单/hash、路径穿越防护、失败保留旧数据。zip 比目录复制更便于单文件交接，且能在解压前做哈希校验。

## 证据

`BackupEngineTest`：设置语言、历史记录、随手记 `hello.md` 导出后改写再恢复；`../secret.txt` zip 为 `PATH_ESCAPE`；缺 manifest 为 `INVALID_ARCHIVE`。`desktopTest` **122/122**。

## 受影响范围

- 恢复会关闭并重开 SQLite；内存中未保存的工具会话可能仍在，界面提示退出重开。
- 未做跨产品 Electron/Java 导入适配器、备份进度条、磁盘空间预检、符号链接条目的完整拒绝矩阵。
- 未测安装镜像内的备份路径。
