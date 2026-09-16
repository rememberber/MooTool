# DIFF-473：MCP stdio 协商 + json_format 成功/失败往返

## 变更

- 新增 `AiIntegrationMcpStdioNegotiationTest`：对照 Electron `mcp/server.test.ts` 首条用例，经 `MainKt --mcp` 子进程断言 `initialize` 服务名为 MooTool、`listTools` 11 项、`mootool_json_format` 排序紧凑输出与非法 JSON 返回 `isError`，且二次 `listTools` 仍为 11。

## 验证

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :composeApp:desktopTest --offline
```
