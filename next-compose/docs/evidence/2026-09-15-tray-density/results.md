# 证据：托盘、密度与 960 折叠（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **188/188** 通过，0 skipped，0 failed
- 覆盖：AWT 托盘图标/菜单模型、不支持时原因、JSON/随手记新会话软换行默认、工具栏 token、`LayoutPolicy` <960 折叠
- Compose 场景 PNG（**不是**真实窗口截图）：`captures/compact-960-focus-ring.png`
- 未测：托盘点击手工、屏幕取色/截图权限对话框、IME 窗口手势、三平台安装/公证、Electron CSS 逐选择器皮肤
