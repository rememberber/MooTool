# 证据：文档库右键、Host 菜单、HTTP 分栏（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **212/212**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 目录右键不出现仅文件动作（复制/导出）
- 重命名/移动后当前打开路径跟随
- 分隔条左右/上下键盘步进 16

## 未测

- 真实窗口内右键手势与文件管理器打开
- IME/列编辑手工、托盘权限对话框、三平台安装
