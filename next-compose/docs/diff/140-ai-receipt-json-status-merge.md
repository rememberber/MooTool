# DIFF-140：AI receipt 路径、预览 JSON 与 MCP 状态比对

对照 Electron `aiIntegrationService.ts`（`${client}.json` receipt、`getStatus` 幂等 `manageServer`）。

## 范围

- Receipt 文件改为 `ai-integration/{codex|claude-code|cursor}.json`（此前缺少 `.json` 后缀）。
- `jsonConfiguration` 使用 `AiClientConfigMerge.mergeJson`，修复 `Map<String, Any>` 序列化崩溃。
- `serverEquals` / `mergeJson` 对 Claude JSON MCP 以 `toMap()` 语义比对；`JsonNode` 经 `claudeLaunchFromMap` 解析。
- `mergeJson` 幂等分支优先 `serverEquals`，避免 `getStatus` 误报 Conflict。
- 单测：`AiIntegrationServiceTest`（MCP 安装/卸载闭环）、`JsonEngineTest` 转义往返、`AiClientConfigMergeTest` 外部篡改拒绝。

## 仍与 Electron 差距

- 真实 Codex/Claude/Cursor 客户端端到端安装未手工验收；打包 `--mcp` 路径见 P7。

## 文件

- `AiIntegrationService.kt`、`AiClientConfigMerge.kt`
- `AiIntegrationServiceTest.kt`、`AiClientConfigMergeTest.kt`、`JsonEngineTest.kt`
