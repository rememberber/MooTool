# 证据：Host/网络/UA/翻译/环境/系统/留言板 1080 溢出（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **233/233**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- Host/网络/UA/翻译/环境/系统/留言板在内容宽 < 1440 时把低频工具栏收入「更多」
- 翻译提供商、留言板对齐改为 compact 下拉
- 本切片无新增引擎单测

## 未测

- 「更多」菜单与下拉的真实窗口手势/截图
- IME/列编辑手工、托盘权限、三平台安装
