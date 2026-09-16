# DIFF-289：查找条「查找」按钮 + JSON 替换清空 notice

## 问题

Electron `FindReplaceBar` 在查找输入框旁有独立的「查找」按钮（`findReplace.find`），Enter 与其行为一致。Compose 仅有上一处/下一处与 Enter。JSON 替换成功后 Electron 清空 `notice`，Compose 仍可能保留旧状态栏/检查器提示。

## 行为

- JSON/随手记/Host/HTTP 查找条：查找词非空时启用 `find.find`，点击执行与 Enter/下一处相同的向前跳转。
- JSON 单次替换、全部替换成功：`notice` 置空（仍 `clearJsonPathQueryResult()`）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
