# DIFF-469：Vault MCP 参数 Zod 边界 + Git push 非快进失败

## 变更

- `VaultMcpTools`：`search`/`read` 参数对齐 Electron `vaultTools.ts` Zod（`limit` 1–50、`offset` 0–2000、`query` ≤200、`path` 1–1000、`length` 1–50000 等），越界或类型错误返回 `isError`，不再 `coerceIn` 静默钳制。
- `VaultMcpToolsTest`：越界/合法参数用例。
- `GitEngineTest.pushFailsWhenRemoteIsAheadWithoutPull`：双 clone 分叉后 push 应失败并带 Git 拒绝文案。
- `VaultMcpReadServiceTest.revokesAccessWhenPolicyCleared`：`length` 改为 50000 以内。

## 验证

```bash
./gradlew :composeApp:desktopTest --offline
```
