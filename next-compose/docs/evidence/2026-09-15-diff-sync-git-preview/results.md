# 证据：同步滚动与 Git 前后预览（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **222/222**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 绝对偏移同步、夹紧不回拉较长一侧、同步锁跳过空操作
- 工作区已跟踪修改与未跟踪文件的 before/after
- Git 路径拒绝 `..` 与绝对路径；rename name-status 解析

## 未测

- 真实窗口内拖动两侧滚动是否无抖动
- Git 对话框内超大/二进制文件手工预览
- IME/列编辑手工、托盘权限、三平台安装、窗口截图
