# Electron `systemService.test.ts` 对照登记

| caseId | sourceProduct | sourceFile | Compose 验证 |
| --- | --- | --- | --- |
| hosts-normalize | MooTool Next Electron | `next/src/shared/systemService.test.ts` `normalizes hosts line endings` | `HostEngineTest.normalizesCrlfAndRejectsNul` |
| ipv4-long | 同上 | `converts IPv4 and unsigned long` | `NetEngineTest.convertsIpv4AndUnsignedLongFixtures`（含 127.0.0.1；Electron 192.168.1.1 同算法） |
| ipv4-range | 同上 | `expands a /24-style IPv4 prefix` | `NetEngineTest.parsesIpv4RangeAndPortSpec` |
| port-spec | 同上 | `parses custom port lists` | `NetEngineTest.parsesIpv4RangeAndPortSpec`（`3306,80-82,22`） |
| env-update | 同上 | `parses and safely updates persistent environment files` | `EnvEngineTest.updatesEnvironmentFileLikeElectronSystemServiceFixture`（F08 导出/preview 见 [electron-next-envTools-vitest.md](electron-next-envTools-vitest.md)） |

HTTP/翻译代理运行时映射见 [DIFF-515](../diff/515-git-proxy-runtime-wiring.md) `NetworkRuntimeTest`（设置加载链见 [DIFF-514](../diff/514-settings-network-proxy-git-remote-commit.md)）。
