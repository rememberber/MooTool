# Electron `netTools.test.ts` / `systemService.test.ts` ↔ Compose 对照

| caseId | Electron vitest | Compose |
| --- | --- | --- |
| ipv4-long | `netTools.test.ts` `127.0.0.1` ↔ `2130706433` | `NetEngineTest.convertsIpv4AndUnsignedLongFixtures` |
| port-scan-history | 命令 `port-scan` + 端口规格持久化 | `NetHistoryMetadataTest` / `NetHistoryRestoreTest`（wire `operation` + `options` 端口 JSON） |
| env-file | `systemService.test.ts` 环境文件更新 | `EnvEngineTest.updatesEnvironmentFileLikeElectronSystemServiceFixture`（见 [electron-next-systemService-vitest.md](electron-next-systemService-vitest.md)） |

登记：[DIFF-533](../diff/533-f11-f20-net-history-mcp-timestamp-slice.md)
