# DIFF-451：Codex TOML MCP / Both+Skill 安装闭环单测

## 背景

Electron `aiIntegrationService.test.ts` 对 Codex `config.toml` 合并、备份与 `both` 模式（MCP + Agents Skill）有集成覆盖。Compose 已有 `AiClientConfigMergeTest` TOML 合并，但缺 `AiIntegrationService` 端到端写盘验收。

## 行为

- `codexMcpInstallPreservesTomlAndIdempotentPreview`：保留用户 TOML 前缀 → 安装 MCP → 二次预览 `Unchanged` → 卸载后移除 `mootool` 段且 `getStatus` 为未安装。
- `codexBothInstallsMcpTomlAndSkill`：`Both` 预览含 `config.toml` 与 `SKILL.md`，安装后 MCP/Skill 均为 `Installed`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（602/602）
