# DIFF-019：翻译使用 OkHttp 与本产品 JSON 存储

- 编号：DIFF-019
- 影响：F20 翻译
- 日期：2026-09-09

## 原行为（Electron）

`undici` `fetch` 访问 Google `translate_a/single` 与 Bing `cn.bing.com` 会话/翻译接口。单词本与历史写入 Electron SQLite `t_translation_word` / `t_translation_history`（整数 id）。通用历史仍可声明；页面实际使用专用历史 Tab。

## 本产品行为

客户端为 OkHttp **4.12.0**，连接 5s / 正文 10s，工具总超时默认 15s。Google 按 UTF-16 长度 1800 分段、并发 3、保序拼接；Bing 全文一次 POST、会话 IG/token 缓存。失败服务冷却 10 分钟后跳过首选再 fallback。单词本/历史写入 Compose `data/translation/words.json` 与 `history.json`（UUID id，历史最多 500）。不声明通用历史，避免与专用历史重复。

## 理由

架构指定 OkHttp。产品线独立要求自有存储，不能读写 Electron 数据库。单词本与翻译历史已有各自模型。

## 证据

`TranslationEngineTest`：与 Electron 相同的分段/emoji surrogate、语言码别名、provider 顺序 fixture；本机 `HttpServer` 覆盖 Google 拼接、Bing fallback、冷却跳过、取消、超时、超长拒绝；单词本按原文+语言去重更新。`desktopTest` **112/112**。

## 受影响范围

- User-Agent 仍为 `Mozilla/5.0 (MooTool Next) …`，以便与现网接口兼容，不是 Electron 运行时。
- 设置页尚未提供代理表单；若 `NetworkSettings.proxyEnabled` 已写入则翻译请求生效。
- 未测真实 Google/Bing 联网、安装镜像、窗口截图。
