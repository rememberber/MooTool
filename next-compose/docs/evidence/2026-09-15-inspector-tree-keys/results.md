# 证据：JSON 检查器结果区与文档库方向键（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **206/206**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 折叠目录不出现在可见行；← 折叠、→ 展开、Enter 打开文件不打开目录
- 检查器结果区与独立 `quickNoteTreeExpandMode` 为代码落地，无窗口截图

## 未测

- 真实窗口内方向键手势
- IME/列编辑手工、托盘权限对话框、三平台安装
