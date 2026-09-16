# DIFF-133：AI 安装 receipt 与 manageServer

对照 Electron `aiIntegrationService.ts` / `aiIntegrationConfig.ts`。

## 范围

- **manageServer**：安装/修复/卸载 MCP 条目；外部篡改时拒绝（conflict）；TOML 按块移除、JSON 保留 JSONC 注释。
- **Receipt**：`ai-integration/{codex|claude-code|cursor}.json`，含 `schemaVersion`、`appVersion`、`mcp`、`skills`（SHA-256）；预览写入 internal 变更，安装后校验与失败回滚。
- **状态**：`getStatus` 区分 installed / needs-repair / conflict；技能文件按 hash 判断 repair。
- **单测**：`AiClientConfigMergeTest` 增补 JSONC、TOML 卸载、外部篡改拒绝。

## 仍与 Electron 差距

- JSON 重复键检测见 [DIFF-134](134-ai-json-duplicate-keys-mcp-tool-list.md)（Jackson strict，非 visit 级）。
- 真实 Codex/Claude/Cursor 端到端安装未手工验收；打包后 `--mcp` 路径待 P7。

## 文件

- `AiClientConfigMerge.kt`、`AiIntegrationService.kt`、`AiIntegrationModels.kt`
- `AiClientConfigMergeTest.kt`
