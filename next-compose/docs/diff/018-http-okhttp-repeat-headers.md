# DIFF-018：HTTP 使用 OkHttp 并保留重复 Header

- 编号：DIFF-018
- 影响：F09 HTTP 请求
- 日期：2026-09-09

## 原行为（Electron）

`undici` `fetch` 发请求。`buildHeaders` 用 `Object.fromEntries`，同名 Header **后者覆盖前者**。响应 JSON 用 `JSON.stringify(..., null, 2)` 美化。方案存在 Electron SQLite `t_msg_http`。

## 本产品行为

客户端为 OkHttp **4.12.0**，跟随重定向，默认校验 TLS。Params/Headers 按列表顺序发送，同名 Header **全部保留**。Cookies 表若有启用项，会替换已有 Cookie 头（与 Electron 覆盖 Cookie 的方向一致）。集合写入 Compose `data/http/requests.json`。cURL 只做文本解析/生成，不执行命令。二进制或含 NUL 的响应用 `[binary n bytes]` 加十六进制预览，不按 UTF-8 强解。

## 理由

架构指定 OkHttp。规格禁止用 Map 吞掉重复键，并要求声明重复 Header 策略。产品线独立要求自有集合存储。

## 证据

`HttpEngineTest`：与 Electron 相同的 cURL fixture；GET 追加 Params 且保留原 query/重复键；POST 无正文时表单编码、有正文时不把 Params 塞进 URL；本机 `HttpServer` 覆盖 200/404、重定向、超时、取消、10 MiB 超限。`desktopTest` **109/109**。

## 受影响范围

- 同名 Header 比 Electron 更完整，对依赖 last-wins 的服务可能不同。
- 正文格式化目前只对 JSON 美化；XML/HTML 不高亮（未接 EditorHost）。
- 设置页尚未提供代理表单；若 `NetworkSettings.proxyEnabled` 已写入则发送时生效。
- 响应文件另存未做，可选中复制。
- 未测真实外网、HTTPS 自签证书拒绝、带认证的代理、安装镜像。
