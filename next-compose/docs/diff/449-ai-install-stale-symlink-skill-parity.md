# DIFF-449：AI 安装预览/失败路径对齐 Electron vitest

## 背景

Electron `aiIntegrationService.test.ts` 覆盖预览不写盘、安装前配置被改、连接失败、符号链接配置、Cursor 仅 MCP、Claude 独立 Skills 等路径。Compose 此前仅有安装往返、用户改配置冲突与过期 preview。

## 行为

- `AiIntegrationServiceTest.previewDoesNotWriteClientConfiguration`：预览不覆盖已有 `.claude.json`。
- `installRejectsStaleConfigurationSincePreview`：安装前用户改配置 → `Configuration changed since preview`。
- `installPropagatesConnectionVerifierFailure`：连接校验失败不落盘（Cursor `mcp.json`）。
- `rejectsSymlinkConfigurationFile`：符号链接配置拒绝预览。
- `cursorRejectsSkillOnlyPreview`：Cursor 不支持仅 Skill。
- `claudeSkillOnlyInstallDoesNotCreateMcpConfig`：仅 Skill 不写 MCP JSON、写入 `SKILL.md`。
- 测试辅助 `aiService(...)` 减少重复构造。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（599/599）
