# 证据：Vault 排序与查找/格式化/发送快捷键（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **202/202**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21

## 覆盖

- 目录始终在前；修改/创建时间新→旧；嵌套目录兄弟节点同样排序
- JSON 不允许 `created` 排序值，回退 `name`
- 随手记 `list()` 带 frontmatter 或文件 `createdAt`/`modifiedAt`
- RSTA `InputMap` 菜单键+F / Shift+F / S 回调

## 未测

- 真实窗口内编辑器焦点下敲快捷键
- 拆出窗口/960 布局截图
- 三平台安装/升级/卸载
