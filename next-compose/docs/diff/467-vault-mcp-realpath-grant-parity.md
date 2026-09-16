# DIFF-467：Vault MCP 授权路径 `realpath` 对齐 Electron

## 背景

Electron `setDataAccess` 将 `await realpath(roots[kind])` 写入 `access.json`；`VaultReadService.root` 拒绝 `realpath(path) !== path`（符号链接别名授权）。

## 变更

- `AiIntegrationService.setDataAccess`：`canonicalVaultPath` = `toRealPath()` 持久化。
- `VaultMcpReadService.root`：解析后 `real != path` 时抛出「granted vault moved」。
- 单测：`VaultMcpReadServiceTest.rejectsSymlinkVaultRootGrant`；fixture 写入路径改用 canonical `realpath`；`VaultPathAiAccessTest` 断言同步。

## 验证

```bash
./gradlew :composeApp:desktopTest --offline
```
