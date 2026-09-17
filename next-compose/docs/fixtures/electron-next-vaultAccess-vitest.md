# Electron `vaultAccess.test.ts` 对照登记

| caseId | sourceProduct | sourceFile | Compose 验证 |
| --- | --- | --- | --- |
| search-read-paging | MooTool Next Electron | `next/electron/mcp/vaultAccess.test.ts` | `VaultMcpReadServiceTest.searchReadAndPaging`（含读后不改动磁盘） |
| revoke-access | 同上 | `revokes access…` | `VaultMcpReadServiceTest.revokesAccessWhenPolicyCleared` |
| unsafe-paths | 同上 | `rejects traversal…` | `VaultMcpReadServiceTest.rejectsUnsafePathsAndGitignoredFiles`（含符号链接探测） |
| unsafe-gitignore | 同上 | `refuses document access when ignore rules…` | `VaultMcpReadServiceTest.refusesDocumentAccessWhenGitignoreCannotBeReadSafely` |
| search-paging | 同上 | `pages search results…` | `VaultMcpReadServiceTest.pagesSearchResultsWithoutDroppingMatches` |
| canonical-grant | 同上 | `setDataAccess`/`realpath(path) !== path` | `VaultMcpReadServiceTest.rejectsSymlinkVaultRootGrant` + `AiIntegrationService` canonical 写入（[DIFF-467](../diff/467-vault-mcp-realpath-grant-parity.md)） |
| absolute-grant | 同上 | `isAbsolute(path)` | `VaultMcpReadServiceTest.rejectsRelativeVaultRootGrant`（[DIFF-468](../diff/468-vault-mcp-absolute-grant-path.md)） |
| mcp-stdio-vault | 同上 | MCP 子进程调用 vault 工具 | `AiIntegrationVaultMcpConnectionTest` notes/json（[DIFF-463](../diff/463-vault-mcp-stdio-e2e-symlink-access.md)、[DIFF-464](../diff/464-vault-access-schema-editor-ime-prep.md)）；notes read 分页 + list/read 联调见 [DIFF-565](../diff/565-f15-regex-worker-css-vault-mcp-p7-slice.md)；双库 json read offset 见 [DIFF-569](../diff/569-time-host-mcp-css-slice.md) |
| vault-tool-args | 同上 | `searchSchema`/`readSchema` | `VaultMcpToolsTest`（[DIFF-469](../diff/469-vault-mcp-zod-args-git-push-reject.md)） |

实现：`VaultMcpReadService` / `VaultMcpTools`（[DIFF-132](../diff/132-vault-mcp-read-tools.md)）；`.gitignore` 严格 UTF-8 见 [DIFF-461](../diff/461-vault-mcp-vaultaccess-vitest-parity.md)；`access.json` ≤16 KB / 非符号链接见 `VaultMcpAccessTest`（[DIFF-462](../diff/462-vault-mcp-access-evidence-prep-script.md)、DIFF-463）。
