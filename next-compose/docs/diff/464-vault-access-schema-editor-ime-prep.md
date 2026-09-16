# DIFF-464：`access.json` schema 版本 + JSON Vault MCP stdio + IME 手工准备脚本

## 背景

Electron `accessSchema` 要求 `version: 1` 字面量；Compose 此前可接受 `version: 2`。Vault MCP 子进程端到端仅覆盖 notes；JSON 库 search/read 缺 stdio 回归。F01/F04 系统 IME 产品窗验收需要与冲突脚本类似的隔离数据准备。

## 行为

- `VaultMcpAccess.read`：`version != 1` 视为 `Invalid MooTool access settings`；畸形 JSON 仍为 `Cannot read MooTool access settings…`。
- `VaultMcpAccessTest`：`readRejectsUnsupportedSchemaVersion`、`readRejectsMalformedJson`。
- `AiIntegrationVaultMcpConnectionTest.subprocessSearchAndReadJsonVault`：`mootool_json_documents_search` / `read` 经 `--mcp` 子进程。
- `scripts/prepare-editor-ime-evidence.sh`：隔离目录下 `ime-sample.json` / `ime-sample.md` 与手工步骤提示（**不代替** IME 产品窗截图）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（630/630）
- 文档：`docs/evidence/2026-09-16-editor-manual-acceptance/results.md` 引用 IME 准备脚本
