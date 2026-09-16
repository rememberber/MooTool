# DIFF-309：HTTP 响应查找按选区跳转

## 问题

Electron `HttpTool.findAround` 以响应编辑器当前选区为起点，调用 `findNextMatch`（有命中则环绕，无命中才 `toast.info`）。Compose 响应查找条此前用 `HttpResponseFind.nextIndex` 在命中列表上循环递增，与光标位置无关。

## 行为

- `HttpFindBar` 的查找/上一处/下一处/Enter 走 `RstaFindNavigation.jump(session.responseEditor, …)`（与 `FindReplace.findNext` 环绕语义一致）。
- 同步 `session.findIndex` 以保留 RSTA 命中高亮；无命中仍 `toastFindNoMatches()`（见 [DIFF-308](308-find-no-matches-toast-only.md)）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
- 既有 `RstaFindNavigationTest` / `HttpEngineTest`（`HttpResponseFind` 高亮索引）
