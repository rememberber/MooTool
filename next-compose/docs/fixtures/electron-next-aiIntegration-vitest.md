# Electron `aiIntegrationService.test.ts` 对照登记

| caseId | sourceProduct | sourceFile | Compose 验证 |
| --- | --- | --- | --- |
| mcp-install-roundtrip | MooTool Next Electron | `next/electron/main/aiIntegrationService.test.ts` | `AiIntegrationServiceTest.mcpInstallPreviewAndUninstallRoundTrip` |
| preview-expired | 同上 | 同上 | `AiIntegrationServiceTest.installRejectsExpiredPreview` |
| user-mcp-conflict | 同上 | `it('detects user changes and refuses repair...')` | `AiIntegrationServiceTest.detectsUserMcpConfigEditsAsConflict` |
| preview-no-write | 同上 | `previews without writing` | `previewDoesNotWriteClientConfiguration`（[DIFF-449](../diff/449-ai-install-stale-symlink-skill-parity.md)）；Codex 隐私见 `codexBothPreviewOmitsPrivateTomlAndInstallBacksUp`（[DIFF-453](../diff/453-ai-preview-privacy-install-backup.md)） |
| codex-backup-idempotent | 同上 | `backs up and is idempotent` | `codexBothPreviewOmitsPrivateTomlAndInstallBacksUp` |
| claude-backup-existing | 同上 | 更新已有 JSON | `mcpInstallBacksUpExistingClientConfig` |
| stale-on-install | 同上 | `preview became stale` | `installRejectsStaleConfigurationSincePreview` |
| connection-failed | 同上 | `connection failed` | `installPropagatesConnectionVerifierFailure` |
| symlink-config | 同上 | `rejects symlink files` | `rejectsSymlinkConfigurationFile` |
| cursor-skill-only | 同上 | `unsupported modes` | `cursorRejectsSkillOnlyPreview` |
| claude-skill-only | 同上 | `standalone Skills` | `claudeSkillOnlyInstallDoesNotCreateMcpConfig` |
| install-rollback | 同上 | `rolls back earlier writes` | `AiIntegrationServiceTest.installRollsBackEarlierWritesWhenLaterFileFails`（[DIFF-450](../diff/450-ai-install-rollback-hook.md)；`installPutVerifier` 注入） |
| codex-toml-mcp | 同上 | `previews without writing` / Codex install | `codexMcpInstallPreservesTomlAndIdempotentPreview`（[DIFF-451](../diff/451-ai-codex-toml-skill-install.md)） |
| codex-both-skill | 同上 | `installs both modes` | `codexBothInstallsMcpTomlAndSkill` |
| needs-repair-mcp | 同上 | `repairs a moved runtime` | `mcpNeedsRepairWhenRuntimeLaunchChangesAndReinstallFixes`（[DIFF-452](../diff/452-ai-needs-repair-runtime-skill-uninstall.md)；`testLaunch`） |
| needs-repair-skill | 同上 | 缺 `SKILL.md` | `codexSkillNeedsRepairWhenManagedFileMissing` |
| uninstall-skill-user-files | 同上 | `removes only managed content` | `uninstallPreservesUserFilesInSkillDirectory` |

| concurrent-install | 同上 | 并行 `install` | `installRejectsConcurrentInstallation`（[DIFF-454](../diff/454-json-inspector-path-tree-limit-ai-install-guard.md)） |

| test-connection-mcp | 同上 | `testConnection` 子进程 + `mootool_json_format` | `AiIntegrationConnectionTest.testConnectionListsToolsAndVerifiesJsonFormat`（[DIFF-459](../diff/459-ai-mcp-test-connection-json-spaces-zero.md)） |

未镜像：Electron 独立 `entry` 文件路径（Compose 使用 `MainKt --mcp`）。
