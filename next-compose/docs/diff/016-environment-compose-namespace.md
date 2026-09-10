# DIFF-016：环境变量使用本产品文件与 JVM 运行时

- 编号：DIFF-016
- 影响：F08 环境变量
- 日期：2026-09-09

## 原行为（Electron）

用户作用域写入 `~/.MooTool/environment`，并在 `~/.zshenv` / `~/.profile` 插入 `# >>> MooTool environment >>>` 钩子。当前进程 Tab 在保存后改写 Electron `process.env`。Runtime Tab 显示 `process.versions.electron/chrome/node`。

## 本产品行为

用户文件为 Compose 数据目录下的 `data/environment`，不读写 `~/.MooTool`。Shell 钩子使用 `# >>> MooTool Next Compose environment >>>`，与 Electron 标记隔离。当前进程 Tab 只读 `System.getenv()`，保存用户/系统变量**不会**改写本 JVM 的环境 Map。Runtime Tab 显示本产品名/版本与 Java/OS 属性。改写前展示 diff，并在 `data/backups/environment/` 留下备份。系统作用域在无权限时按需提权；拒绝时保持原文件。

## 理由

产品线独立要求自有路径与标记。规格禁止用进程 Map 冒充系统设置。Electron 运行时字段在 JVM 桌面产品中没有对应物。

## 证据

`EnvEngineTest`：解析保留注释/未知行；用户文件增删与备份；`System.getenv` 不被改写；无提权时系统写入失败且不创建目标文件。`desktopTest` **100/100**。

## 受影响范围

- 新终端不会自动看到用户变量，除非已加载 Compose shell 钩子；macOS 另尝试 `launchctl setenv`（失败忽略）。
- 系统文件在 macOS 为 `/etc/zshenv`，Linux 为 `/etc/environment`；不是所有发行版的唯一全局环境。
- Windows 用户/系统走 PowerShell 注册表目标，本机未测。
- 不把变量值写入会话 JSON 或通用历史，避免密钥进 SQLite。
