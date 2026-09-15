# 证据：编码/加解密 compact 下拉（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **232/232**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 编码 URL charset / ASCII 进制由按钮改为下拉，引擎路径未改
- 加解密四类算法由按钮改为下拉，选项仍是既有 enum
- 内容宽 < 1440 时历史/清空/分离收入「更多」

## 未测

- 下拉与「更多」真实窗口手势
- 加解密超大文件与哈希中途取消
- IME/列编辑手工、托盘权限、三平台安装
