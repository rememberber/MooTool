# DIFF-435：AI MCP 用户篡改冲突（对齐 Electron vitest）

## 背景

Electron `aiIntegrationService.test.ts` 在安装 Claude MCP 后若用户改 `.claude.json` 中 `mootool` 参数，应报告 `conflict`，且 install/uninstall 预览拒绝并保留用户文件。Compose `AiIntegrationService.getStatus` 已有 `Conflict` 分支，缺回归用例。

## 行为

- `AiIntegrationServiceTest.detectsUserMcpConfigEditsAsConflict`：安装后向 `args` 追加 `--user-option`，断言 `getStatus().mcp == Conflict`，`preview(install|uninstall)` 抛错且配置未变。
- 登记 `docs/fixtures/electron-next-aiIntegration-vitest.md`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（547/547）
