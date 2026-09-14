# 本轮验收记录

- 阶段/条目：JSON/随手记 Vault 外部修改冲突切片（P6 / A03）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `fs.watch` + dirty 时静默跳过重载；`docs/data-platform-release.md` §4
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **143/143**（含 VaultConflictEngineTest 4，0 skipped；此前 Git 检查点为 139/139）
- 语义：内容 SHA-256；干净则自动重载；dirty 且磁盘偏离则对话框；保存前拒绝覆盖；自写哈希忽略；400ms 轮询
- 差异：DIFF-026
- 未测：窗口截图与冲突对话框手工操作、三方合并、>8 MiB 文件、安装镜像、Windows/Linux
- 下一轮：更新通道，或 JSON 完整检查器弹层，或 Git pull/push
