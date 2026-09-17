# Electron `mcp/server.test.ts` 对照登记

| caseId | sourceProduct | sourceFile | Compose 验证 |
| --- | --- | --- | --- |
| json-format-spaces-zero | MooTool Next Electron | `server.test.ts` | `JsonEngineTest.formatAdvancedSpacesZeroMinifiesLikeElectronMcp`（[DIFF-459](../diff/459-ai-mcp-test-connection-json-spaces-zero.md)） |
| mcp-json-format-sort | 同上 | `negotiates MCP…` `sortKeys`/`spaces:0` | `MooToolMcpToolsTest.mirrorsElectronMcpToolHappyPaths`（[DIFF-536](../diff/536-f22-f24-history-mcp-slice.md)） |
| mcp-timestamp-13-digit-local | 同上 | `timeTools.test.ts` 13 位 + second | `MooToolMcpToolsTest.timestampToLocalDetectsThirteenDigitMillisForMcp`（MCP 层；F18 UI 见 DIFF-001） |
| mcp-tools-happy-path | 同上 | `handles UTF-8…` | `MooToolMcpToolsTest.mirrorsElectronMcpToolHappyPaths`（[DIFF-460](../diff/460-mootool-mcp-tools-server-test-parity.md)） |
| mcp-tools-validation | 同上 | `validates types, limits…` | `MooToolMcpToolsTest.mirrorsElectronMcpToolValidationFailures`（含 `hash` 非字符串/未知字段，[DIFF-470](../diff/470-mootool-mcp-strict-object-args.md)） |
| mcp-json-query-array | 同上 | `$.values[*]` | `MooToolMcpToolsTest.jsonQueryReturnsArrayMatchesForValuesPath` |
| mcp-stdio-roundtrip | 同上 | `negotiates MCP…`（子进程） | `AiIntegrationConnectionTest`（DIFF-459）；`AiIntegrationMcpStdioNegotiationTest`（[DIFF-473](../diff/473-mcp-stdio-negotiation-json-format.md)）；Vault search/read 见 `AiIntegrationVaultMcpConnectionTest`（[DIFF-463](../diff/463-vault-mcp-stdio-e2e-symlink-access.md)） |
| list-tools-11-readonly | 同上 | `negotiates MCP, discovers schemas…`（11 tools、`readOnlyHint`、`inputSchema.type===object`） | `McpToolCatalogTest` + `AiIntegrationMcpListToolsTest`（[DIFF-471](../diff/471-mcp-list-tools-catalog.md)） |
| list-tools-metadata | 同上 | `negotiates MCP, discovers schemas…` 11 tools + readOnlyHint | `AiIntegrationMcpListToolsTest` + `McpToolCatalogTest`（[DIFF-471](../diff/471-mcp-list-tools-catalog.md)） |

UI JSON 检查器仍可使用 filter/slice JSONPath；MCP `mootool_json_query` 拒绝 `?(` 过滤表达式，对齐 Electron `eval: false`。
