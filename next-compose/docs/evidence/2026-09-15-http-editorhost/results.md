# 证据：HTTP EditorHost 语法高亮（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **234/234**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- `syntaxForMime`：JSON/XML/HTML/JavaScript/plain
- 响应 Body 按 `Content-Type` 高亮；Headers/Cookies 无语法
- 查找仍只读 tab payload，不包含空状态占位文案（既有 `responseFindReadsTabPayloadAndWrapsIndex`）

## 未测

- 请求/响应编辑器真实窗口语法色与查找手势
- IME/列编辑手工、托盘权限、三平台安装
