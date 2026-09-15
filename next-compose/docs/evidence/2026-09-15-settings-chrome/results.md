# 证据：设置页分组/分段/色板与二维码时间溢出（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **235/235**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 设置分组/分段/下拉/开关/强调色色板（Compose token，非 CSS 逐选择器）
- Electron 强调色 id：yellow/coral/blue/green/red/purple；`orange`→`yellow`、`teal`→`green`
- 二维码/时间/调色板/JSON/随手记/格式化标题栏分离在 < 1440 收入「更多」

## 未测

- 设置页与 1080 工具栏真实窗口截图
- IME/列编辑手工、托盘权限、三平台安装
