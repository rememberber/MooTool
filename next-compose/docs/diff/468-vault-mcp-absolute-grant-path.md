# DIFF-468：Vault MCP 拒绝相对路径授权

## 背景

Electron `VaultReadService.root` 要求 `isAbsolute(path)`；Compose 曾先 `toAbsolutePath()` 再校验，会把 `access.json` 中的相对路径解析为进程 CWD 下的目录，存在误授权风险。

## 变更

- `VaultMcpReadService.root`：在 `normalize()` 后、`toRealPath()` 前校验 `path.isAbsolute`。
- `VaultMcpReadServiceTest.rejectsRelativeVaultRootGrant`。

## 验证

```bash
./gradlew :composeApp:desktopTest --offline
```
