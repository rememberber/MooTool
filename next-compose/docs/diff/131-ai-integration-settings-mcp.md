# DIFF-131：设置 AI 接入与 MCP 运行时

对照 Electron `AiIntegrationSettings` 与 `aiIntegrationService` 在 compose 桌面版落地首版能力。

## 范围

- **A01 设置**：新增 **AI 接入** 导航分类（位于运行环境与工具之间），含客户端/模式、安装预览、测试连接、随手记与 JSON 只读授权开关。
- **MCP**：`--mcp` 子进程入口（`Main.kt` → `McpBootstrap`），注册 `mootool_json_format` 等 7 个工具（复用现有 `JsonEngine`/`EncodeEngine`/`TimeEngine`/`DiffEngine`）。
- **安装**：向 Codex TOML / Claude·Cursor `mcp.json` 合并 `mootool` 服务器配置；Skill 文件写入 `SKILL.md`（来自 `resources/mcp/SKILL.md`）与 `runtime.md`。
- **依赖**：`io.modelcontextprotocol.sdk:mcp`、`org.tomlj:tomlj`。

## 与 Electron 差异（本阶段）

- 安装 receipt 与 `manageServer` 见 [DIFF-133](133-ai-receipt-manage-server.md)；Vault MCP 见 [DIFF-132](132-vault-mcp-read-tools.md)。
- 打包后 MCP 启动命令为当前进程可执行文件 + `--mcp`（开发时为 `java`），与 Electron `ELECTRON_RUN_AS_NODE` + `mcp.js` 路径不同但协议一致。
- 未在本机对真实 Codex/Claude/Cursor 客户端做端到端安装验收。

## 文件

- `ai/*`、`features/settings/AiIntegrationSettingsPanel.kt`、`Main.kt`、`AppContainer.kt`、`Translator.kt`
- `resources/mcp/SKILL.md`
- `AiClientConfigMergeTest.kt`
