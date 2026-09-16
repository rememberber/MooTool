# DIFF-310：HTTP 响应打开查找不自动选中首条

## 问题

Electron `HttpTool.openFind` 仅显示查找条与 `searchQuery` 全文高亮，**不**调用 `findAround`，光标/选区保持原位。Compose 在 `findOpen` 且 `findIndex == 0` 时 `LaunchedEffect` 会立刻 `select` 第一条命中。

## 行为

- `HttpResponseFind.FIND_INDEX_UNSET = -1`：查找条打开、改词/选项、新响应、切 Tab、关条等重置为该值。
- `spans(..., FIND_INDEX_UNSET)` 仅高亮全部命中，无「当前」条。
- 用户点「查找/上一处/下一处」后由 [DIFF-309](309-http-response-find-caret-navigation.md) 写入有效 `findIndex` 并选中。

## 验证

- `HttpEngineTest.responseFindReadsTabPayloadAndWrapsIndex`
- `./gradlew :composeApp:desktopTest --offline`
