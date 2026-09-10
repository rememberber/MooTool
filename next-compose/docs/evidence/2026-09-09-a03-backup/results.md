# 本轮验收记录

- 阶段/条目：A03 备份/恢复切片（P6）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `backupService.ts`；本产品 `docs/data-platform-release.md` §6
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **122/122**（含 BackupEngineTest 2；此前 F01 为 120/120）
- 语义：设置 → 数据：导出 zip、预览校验、恢复；包含设置/SQLite/Vault 等；排除缓存/日志/凭据；恢复前写 `data/backups/pre-restore-*`
- 差异：DIFF-022（zip+manifest+SHA-256，相对 Electron 目录复制）
- 未测：窗口截图、安装镜像、磁盘满、符号链接 zip、跨产品导入
- 下一轮：随手记预览/附件/列编辑，或 JSON/随手记 Git、更新通道
