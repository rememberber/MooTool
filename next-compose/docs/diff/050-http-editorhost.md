# DIFF-050：HTTP 请求/响应 EditorHost 语法高亮

- 编号：DIFF-050
- 影响：F09 HTTP 正文语法高亮与查找接到 RSTA
- 日期：2026-09-15

## 原行为（Electron）

请求 Body 与响应正文按 MIME 语法高亮；响应区只查找，不搜空状态占位文案。

## 本产品行为

- 请求 Body 与响应正文改用会话级 `EditorHost`（RSTA）。
- 标题栏历史/分离在内容宽 < 1440 时收入「更多」。
- MIME 映射：JSON / XML / HTML / JavaScript 高亮，`text/plain` 与 Headers/Cookies 无语法。
- 响应编辑器只读；空响应仍用 Compose 占位文案覆盖，查找只扫真实 payload。
- 查找命中在 RSTA 高亮并选中当前项；Cmd/Ctrl+F 仍打开只查找，Cmd/Ctrl+Shift+F 格式化请求 Body。

## 理由

feature-parity 要求正文格式化与语法高亮。原先 Compose `Text`/`MooTextField` 无 token 色，查找也无法落到编辑器选区。

## 证据

`HttpEngineTest.bodyAndResponseSyntaxFollowMime`。`desktopTest` **234/234**，见 `docs/evidence/2026-09-15-http-editorhost/`。真实窗口截图未取。

## 受影响范围

- 集合 JSON 仍只存正文文本，不存编辑器 undo。
- 仍非 Electron 六套 CSS 逐选择器移植。
- IME/列编辑手工窗口手势、三平台安装未测。
