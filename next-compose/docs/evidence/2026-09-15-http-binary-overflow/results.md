# 证据：HTTP 二进制另存与其余工具 1080 溢出（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **233/233**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- `image/png` 含 NUL 判定为二进制，保留原始字节供 `downloadBytes`
- `application/json` 文本不提供原始字节下载
- `application/pdf` 按类型视为二进制
- 1080 溢出为 UI 结构，无新增引擎单测

## 未测

- 二进制另存真实文件对话框与窗口手势
- 代码运行/正则/Cron 等「更多」菜单窗口手势
- 二维码纠错与时间单位下拉窗口手势
- IME/列编辑手工、托盘权限、三平台安装
