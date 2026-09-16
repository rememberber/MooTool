# DIFF-459：AI `testConnection` 真 MCP 子进程 + `spaces: 0` 紧凑 JSON

## 背景

`docs/fixtures/electron-next-aiIntegration-vitest.md` 标「未镜像：`testConnection` 真子进程」。生产 `AiIntegrationService.testConnection` 会拉起当前 JVM `--mcp`，并调用 `mootool_json_format`（`spaces: 0`）校验工具可用性；Electron 同参期望 `{"moo":true}`（见 `aiIntegrationService.ts` / `mcp/server.test.ts`）。

Compose `JsonEngine.formatAdvanced` 在 `spaces == 0` 仍走 pretty printer，输出带换行，导致 MCP 校验失败。

## 行为

- `JsonEngine.formatAdvanced`：`spaces > 0` 才 pretty；`spaces == 0` 与 Electron `JSON.stringify(value, null, 0)` 一样单行紧凑（含 `sortKeys`）。
- `JsonEngineTest.formatAdvancedSpacesZeroMinifiesLikeElectronMcp`：对照 `server.test.ts` 样本。
- `AiIntegrationConnectionTest.testConnectionListsToolsAndVerifiesJsonFormat`：`desktopTestMcpLaunch` 用测试 classpath 启动 `MainKt --mcp`，断言 11 个工具名与 `testConnection()` 成功。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（618/618）
