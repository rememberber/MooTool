# DIFF-462：`VaultMcpAccess` 校验 + Vault 冲突产品窗准备脚本

## 背景

Electron `readAccess` 拒绝超过 16 KB 的 `access.json`；Compose 已有实现但缺单测。产品主窗 Vault 外部冲突验收步骤需要可重复的隔离数据目录，避免污染本机默认数据。

## 行为

- `VaultMcpAccessTest`：缺失文件返回空授权；超大 access 文件抛 `Invalid MooTool access settings`。
- `scripts/prepare-vault-conflict-evidence.sh`：创建 `MOOTOOL_COMPOSE_DATA_DIR` 与 `data/vaults/json/sample.json`，打印 `runDistributable` 与外部改写命令（**不代替**产品窗截图；执行记录仍须人工登记 PNG）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（625/625）
- 脚本：`eval "$(./scripts/prepare-vault-conflict-evidence.sh)"` 后按 `docs/evidence/2026-09-17-vault-conflict-product-window/results.md` 操作
