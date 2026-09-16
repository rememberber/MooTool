# DIFF-292：HTTP 响应查找索引重置

## 问题

HTTP 响应查找用 `findIndex` 在命中列表间循环。打开查找、新响应到达、从历史恢复响应或切换 Body/Headers/Cookies 后若保留旧索引，可能高亮错误的命中（`coerceIn` 会夹紧但仍非从首条开始）。

Electron 无会话索引，每次 `findAround` 从编辑器选区/光标起算；Compose 需在上下文变化时归零。

## 行为

- 上下文变化时 `findIndex = HttpResponseFind.FIND_INDEX_UNSET`（见 [DIFF-310](310-http-find-open-no-auto-select.md)）：打开查找、新响应、历史恢复、切 Tab、关条、改查找词/选项。
- 用户导航后 `findIndex >= 0` 才选中当前命中。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
