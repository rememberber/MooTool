# DIFF-450：AI 安装中途失败回滚

## 背景

Electron `aiIntegrationService.test.ts` 在 `rename` 写 SKILL 失败时要求已写入的 MCP 配置回滚。Compose `AiIntegrationService.install` 已有 `written` 逆序恢复逻辑，但缺可注入的失败点单测。

## 行为

- `AiIntegrationService.installPutVerifier`：每次安装写盘前回调（仅测试注入）。
- `AiIntegrationServiceTest.installRollsBackEarlierWritesWhenLaterFileFails`：Claude `Both` 预览后，在 `SKILL.md` 写盘前抛错 → `.claude.json` 与 skill 均不存在。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（600/600）
