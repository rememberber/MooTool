# 证据：窗口夹紧、JSON 复制反馈、图片溢出与 Git 多文件（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **231/231**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 已保存坐标在屏内保持；第二屏不拉回主屏；完全越界夹到最近工作区；标题在屏幕上方夹回；无屏幕信息时保留原坐标
- 复制成功/失败按钮文案键与 1400ms 复位间隔
- Git 提交含两个变更文件时 `fileDiffs` 返回全部，并可按路径选择
- 重命名差异标签为 `status  old → new`

## 未测

- 真实多显示器插拔后启动窗口位置
- JSON 复制按钮在运行窗口中的 1400ms 观感与焦点保留
- 图片 1080 工具栏「更多」窗口截图
- Git 对话框内多文件下拉手工切换
- IME/列编辑手工、托盘权限、三平台安装
