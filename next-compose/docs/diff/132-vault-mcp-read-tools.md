# DIFF-132：Vault MCP 只读搜索/读取

对照 Electron `electron/mcp/vaultAccess.ts` 与 `vaultTools.ts`。

## 范围

- MCP 工具：`mootool_notes_search`、`mootool_notes_read`、`mootool_json_documents_search`、`mootool_json_documents_read`。
- 通过 `--access-file` 读取 `ai-integration/access.json` 中的绝对路径授权；未授权时调用失败。
- 搜索：目录遍历上限、`.gitignore` 过滤、随手记 frontmatter 剥离、JSON 仅 `.json`、2 MiB 读取上限、分页 `offset`/`limit`/`nextOffset`。
- 单测：`VaultMcpReadServiceTest` 覆盖搜索/分页/撤销授权/路径与 gitignore 拒绝。

## 文件

- `VaultMcpAccess.kt`、`VaultMcpReadService.kt`、`VaultMcpTools.kt`、`McpBootstrap.kt`
- `VaultMcpReadServiceTest.kt`
