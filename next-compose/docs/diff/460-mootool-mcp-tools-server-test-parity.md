# DIFF-460：`MooToolMcpTools` 对齐 Electron `mcp/server.test.ts`

## 背景

Electron MCP 工具层（`callMooTool` / `server.test.ts`）对输入长度、UUID 数量、Base64、JSONPath 过滤与重复键有严格校验；Compose `MooToolMcpTools` 此前部分边界与 UI 共用 `JsonEngine.queryPath`（允许 filter），且 `uuid` 对 `count > 100` 静默 `coerceIn`。

## 行为

- `mootool_json_query`：拒绝含 `?(` 的过滤路径（MCP `eval: false` 语义；UI 检查器不受影响）。
- `mootool_encode` Base64 解码：无效字符拒绝（对齐 Electron 正则）。
- `mootool_diff`：单侧最长 8000 字符。
- `mootool_uuid`：`count` 须在 1–100，超出抛错而非截断。
- `MooToolMcpToolsTest`：对照 `server.test.ts` 成功/失败路径与 `$.values[*]` 查询。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（621/621）
- 登记：`docs/fixtures/electron-next-mcp-server-vitest.md`
