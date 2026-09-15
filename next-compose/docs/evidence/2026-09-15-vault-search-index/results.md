# 证据：Vault 全文检索内存索引（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **204/204**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 正文命中保留祖先目录；`includeContent=false` 不搜正文
- 目录名命中保留该目录及后代
- JSON/随手记页面：关键字与搜正文开关不再触发 `snapshot()`

## 未测

- 超大 Vault 的索引耗时窗口观感
- 真实窗口截图、IME 手工、三平台安装
