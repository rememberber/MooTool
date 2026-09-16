# DIFF-471：MCP `listTools` 元数据对齐 Electron

## 背景

Electron `createMooToolServer` 通过 `listMooToolTools()` + `listVaultTools()` 暴露 **11** 个工具，每项含真实 `description`、Draft-7 `inputSchema` 与 `annotations.readOnlyHint`（Vault 工具另含 `idempotentHint`）。Compose `McpBootstrap` 此前仅用工具名作描述、空 `{"type":"object"}` schema。

## 变更

- 新增 `McpToolCatalog.kt`：7 个 MooTool + 4 个 Vault 工具注册（文案与 schema 对齐 `tools.ts` / `vaultTools.ts`）。
- `McpBootstrap.kt`：按 catalog 注册 `description`、`inputSchema`、`ToolAnnotations`（readOnly / 非 destructive / 非 openWorld；Vault 只读工具 `idempotentHint=true`）。
- `McpToolCatalogTest`：11 项、名称与 handler 一致、schema 与 annotations。
- `AiIntegrationMcpListToolsTest`：stdio 子进程 `listTools` 端到端（对照 `server.test.ts` 首条用例）。

## 验证

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"   # macOS
./gradlew :composeApp:desktopTest --offline
```

未镜像：Electron 将 schema 经 `z.toJSONSchema` 生成（Compose 为等价手写 JSON）；MCP SDK `outputSchema` 未使用（Electron 同样未设）。
