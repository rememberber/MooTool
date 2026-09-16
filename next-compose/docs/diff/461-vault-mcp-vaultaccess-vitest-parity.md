# DIFF-461：Vault MCP 只读工具对齐 Electron `vaultAccess.test.ts`

## 背景

`VaultMcpReadServiceTest` 已覆盖搜索/分页/撤销/路径与 gitignore 基础场景；Electron `vaultAccess.test.ts` 还要求：读后不改动 Vault、策略撤销后 JSON 大 length 读取失败、遍历/盘符路径、符号链接、超大 `.gitignore` 与非法 UTF-8 `.gitignore`、搜索分页不丢命中。

## 行为

- 扩充 `VaultMcpReadServiceTest` 对照上述用例（符号链接在本机不支持时跳过）。
- `VaultMcpReadService.matcher`：`.gitignore` 使用 UTF-8 **REPORT** 解码（对齐 Electron `fatal: true`），非法字节拒绝暴露文档。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（623/623）
- 登记：`docs/fixtures/electron-next-vaultAccess-vitest.md`
