# DIFF-128：正则/Cron/UA/网络/代码运行 toast

对照 Electron 在解析、匹配与命令完成时的 toast 反馈。

## 范围

- **F15 正则**：`runMatch` 成功/失败 toast。
- **F16 Cron**：`parseRuns` 成功/失败 toast。
- **F12 UA**：`parseSource` 成功/失败 toast。
- **F11 网络/IP**：网络命令结束成功/失败 toast；完成提示使用本地化动作标签（ifconfig/netstat 等）。
- **F05 代码运行**：进程结束成功（退出码）/失败 toast。

## 文件

- `RegexScreen.kt`、`CronScreen.kt`、`UaParseScreen.kt`、`NetScreen.kt`、`CodeRunScreen.kt`
