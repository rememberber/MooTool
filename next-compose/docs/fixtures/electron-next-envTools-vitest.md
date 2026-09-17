# Electron 环境变量 ↔ Compose 对照

| caseId | sourceProduct | sourceFile | Compose 验证 |
| --- | --- | --- | --- |
| export-sections | MooTool Next Electron | `next/src/features/variables/VariablesTool.tsx` `formatEnvironment` | `EnvEngineTest.formatExportUsesElectronSectionHeaders`（`EnvExportSections`，运行时区为 Compose 产品名） |
| preview-diff | 同上编辑对话框 | `variables.diff` + 保存前摘要 | `EnvEngineTest.previewDiffDescribesScopeFileAndDelete` |
| env-file-update | 同上 | `next/src/shared/systemService.test.ts` | `EnvEngineTest.updatesEnvironmentFileLikeElectronSystemServiceFixture`（见 [electron-next-systemService-vitest.md](electron-next-systemService-vitest.md)） |
| permission-deny | Compose 行为 | — | `EnvEngineTest.systemWriteWithoutElevationKeepsOriginalFile` |

| session-restore | Compose 会话 | tab/scope/query wire | `EnvSessionMetadataTest` / `VariablesSessionRestoreTest`（[DIFF-535](../diff/535-f08-session-f-tools-history-git-slice.md)） |

登记：[DIFF-534](../diff/534-f08-env-export-git-push-slice.md)、[DIFF-535](../diff/535-f08-session-f-tools-history-git-slice.md)
