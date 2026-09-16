# DIFF-472：26 工具注册 Available + testConnection 11 工具

## 变更

- `ToolRegistry` 私有 `tool()` 默认 `ToolStatus.Available`（不再误默认 NotImplemented）。
- `ToolRegistryTest`：26 项均为 Available。
- `AiIntegrationConnectionTest`：断言 `testConnection` 返回恰好 11 个 MCP 工具名（对齐 Electron `server.test.ts`）。

## 验证

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :composeApp:desktopTest --offline
```
