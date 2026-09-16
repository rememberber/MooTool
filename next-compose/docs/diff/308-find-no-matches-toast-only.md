# DIFF-308：查找无匹配仅 info toast

## 问题

Electron `findAround` / `replaceCurrent` 在无命中时调用 `toast.info(findReplace.noMatches)`，**不**更新工具 `notice`。Compose 在 [DIFF-283](283-json-find-no-matches-notice.md) 起将无匹配写入状态栏，随手记列编辑闩锁时会顶掉 `quickNote.columnEdit.hint`。

## 行为

- `AppContainer.toastInfo` / `toastFindNoMatches()`（文案 `find.noMatches`，见 [DIFF-307](307-find-no-matches-i18n.md)）。
- JSON / 随手记 / Host / HTTP 响应查找条：无匹配只 toast，不写 `session.notice`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
