# DIFF-017：Host 方案存本产品数据并做并发校验

- 编号：DIFF-017
- 影响：F10 Host
- 日期：2026-09-09

## 原行为（Electron）

方案存在 Electron SQLite 表 `t_host`。默认模板注释为 `# MooTool hosts profile`。`writeHosts` 规范化后直接写系统文件，权限不足再提权；**不**检查系统 hosts 是否已被其他程序改过。

## 本产品行为

方案写入 Compose 数据目录 `data/hosts/profiles.json`，不读写 Electron 数据库。默认模板注释为 `# MooTool Next Compose hosts profile`。应用到系统前展示 unified diff，把当前系统文件备份到 `data/backups/hosts/`，并用上次读取的 SHA-256 指纹拒绝并发改写。保存方案与应用到系统分开；关闭应用不会自动把系统 hosts 改回上一份方案。

## 理由

产品线独立要求自有存储。规格要求提交时校验系统文件未被别人改动，以及备份/恢复真实可用。模板注释隔离避免两套产品互相误认。

## 证据

`HostEngineTest`：CRLF 规范化与 NUL 拒绝；无效条目拒绝；方案 CRUD 在临时目录；应用到临时「系统」文件并备份；指纹冲突时保留被改过的文件；无提权时权限失败不改原文件。`desktopTest` **105/105**。

## 受影响范围

- 不会自动导入 Electron `t_host` 方案。
- 条目校验比 Electron 更严：非注释行必须是 IP + 至少一个名称。
- macOS 直接写入后的 `dscacheutil -flushcache` 常需权限；失败时仍把系统文件写入标为成功，并提示 DNS 未刷新。提权写入时 flush 包含在同一 osascript 命令中。
- 真实 `/etc/hosts`、Windows hosts、管理员对话框未在本机单测中执行。
