# DIFF-046：HTTP 方法/Body 下拉、响应查找与复制

- 编号：DIFF-046
- 影响：F09 HTTP、ui-spec 1080 工具栏密度、复制短暂反馈、快捷键帮助
- 日期：2026-09-15

## 原行为（Electron）

Method 与 Body MIME 是 URL 行/正文控制条上的 compact `<select>`，不是一排 chip。响应区有只查找（`showReplace={false}`）与复制；Cmd/Ctrl+F 打开查找，Escape 关闭；Cmd/Ctrl+Shift+F 在 Body 页格式化正文。

## 本产品行为

- Method 下拉与 URL、超时、发送/取消同一行。
- Body 类型改为完整 MIME 下拉（`application/json` 等 6 项）。
- 响应区查找只搜索真实 payload，不搜索空响应占位文案；上一处/下一处循环；匹配高亮当前项。
- 复制按钮 1400ms「已复制」/「复制失败」反馈；查找/复制状态不写入 `HttpSessionSnapshot`。
- 内容宽 < 1440 时另存收入「更多」，保留查找与复制。
- 快捷键帮助写明 HTTP 查找与 Body 格式化。

## 理由

对照 Electron `HttpTool.tsx` 的 URL 栏与响应查找/复制。chip 行在 1080 宽挤掉发送按钮；响应区此前只有另存。

## 证据

`HttpEngineTest.responseFindReadsTabPayloadAndWrapsIndex`。`desktopTest` **232/232**，见 `docs/evidence/2026-09-15-http-find-copy/`。真实窗口截图、外网、自签证书仍未测。

## 受影响范围

- 请求/响应正文仍是 Compose 文本，不是 `EditorHost` 语法高亮。
- 查找不做替换，与 Electron 一致。
