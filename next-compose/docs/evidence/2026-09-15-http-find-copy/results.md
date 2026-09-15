# 证据：HTTP 方法/类型下拉、响应查找与复制（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **232/232**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- `HttpResponseFind.payload` 按 Body/Headers/Cookies 读真实响应
- `nextIndex` 空列表为 0，有匹配时正反向循环
- `spans` 标记当前匹配；空匹配不产生 span
- 查找不写入 HTTP 会话快照

## 未测

- 响应查找栏真实窗口手势与高亮观感
- Cmd/Ctrl+F / Escape / 复制 1400ms 按钮反馈
- 1080「更多」另存菜单
- 外网、自签证书、代理对话框、IME/列编辑手工、三平台安装
