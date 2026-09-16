# DIFF-463：Vault MCP stdio 子进程端到端 + `access.json` 符号链接拒绝

## 背景

DIFF-459 已验证 MCP 子进程 `testConnection`；Vault 只读工具此前仅经 `VaultMcpReadServiceTest` 进程内调用。Electron `readAccess` 拒绝符号链接形式的 `access.json`。

## 行为

- `AiIntegrationVaultMcpConnectionTest.subprocessSearchAndReadNotesVault`：`--mcp` 子进程上调用 `mootool_notes_search` / `mootool_notes_read`。
- `VaultMcpAccess.read`：拒绝符号链接 access 文件；`VaultMcpAccessTest.readRejectsSymlinkAccessFile`（本机不支持 symlink 时跳过）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（627/627）
