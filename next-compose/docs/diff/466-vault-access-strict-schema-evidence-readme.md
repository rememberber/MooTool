# DIFF-466：`access.json` strict schema + 产品窗证据 README

## 变更

- `VaultMcpAccess` 解码改用 `ignoreUnknownKeys = false`，对齐 Electron `accessSchema` strictObject；未知字段抛 `IllegalArgumentException`（包装为既有「Cannot read…」文案）。
- `VaultMcpAccessTest.readRejectsUnknownJsonProperties`。
- `scripts/prepare-git-merge-conflict-evidence.sh`：`init.defaultBranch=main`、`git init -b main`。
- `scripts/README-product-evidence.md`：§A/§B/IME 脚本用法与「勿 eval 带注释输出」说明。

## 验证

```bash
./gradlew :composeApp:desktopTest --offline --tests 'com.rememberber.mootool.next.compose.ai.VaultMcpAccessTest'
./gradlew :composeApp:desktopTest --offline
```

产品窗 PNG 仍须人工：`docs/evidence/2026-09-17-vault-conflict-product-window/results.md`。
