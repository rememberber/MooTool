# 证据：跟尾滚动、HTTP 上次响应与工具栏溢出（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **210/210**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 距底部 48px 内视为跟尾；空内容视为钉住；新请求 ID / 非空 reset key 才重新钉住
- 内容宽 1439 及以下溢出工具栏，1440 及以上不溢出
- HTTP 取消/超时显示上次可用响应；超限响应仍作为当前结果

## 未测

- 真实窗口内上翻暂停跟尾的手势
- IME/列编辑手工、托盘权限对话框、三平台安装
