# 证据：格式化下拉、调色板溢出与复制反馈（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **231/231**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 复制按钮 idle 文案可按工具覆盖（`reformat.copy`），成功/失败仍用统一 copied/failed 键
- 1400ms 复位间隔与 Electron JSON `copyState` 一致

## 未测

- 格式化下拉与 1080「更多」真实窗口截图
- 调色板溢出菜单窗口手势
- 复制按钮 1400ms 观感与焦点保留
- IME/列编辑手工、托盘权限、三平台安装
