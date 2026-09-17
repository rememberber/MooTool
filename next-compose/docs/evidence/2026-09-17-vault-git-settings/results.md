# Vault / Git 设置与 JSON 面板产品走查（2026-09-17）

## 准备

```bash
export MOOTOOL_COMPOSE_DATA_DIR="$(mktemp -d /tmp/mootool-compose-vault-git-settings-XXXX)"
./scripts/prepare-vault-git-settings-evidence.sh
# 按脚本 stdout 中的 export MOOTOOL_COMPOSE_DATA_DIR=... 执行
cd /path/to/next-compose
./gradlew :composeApp:runDistributable --offline
```

## 手工步骤（PNG 仅登记本目录，勿提交无关 evidence 帧）

1. **设置 → Vault**：确认 `gitRemote` / `gitUsername` / `gitToken` 仅在失焦时写盘；输入 `ftp://bad` 失焦应 toast 且字段恢复为已保存合法值（DIFF-509）。
2. **设置 → Vault**：填入脚本输出的 `file://` bare remote，保存后重开设置页 remote 仍在。
3. **JSON → Vault Git**：fetch/sync 识别 `origin`；与 DIFF-508 加载 sanitize 一致。
4. 截图命名建议：`vault-git-settings-remote.png`、`vault-git-panel-fetch.png`（可选，未拍不冒充验收）。

## 自动化（本目录外）

- `SettingsVaultGitNormalizeTest` / `GitEngineTest.settingsVaultGitSanitizeAcceptsSameRemotesAsNormalizeGitRemote`
- `desktopTest --offline` 见 `docs/acceptance.md` DIFF-509 行

## 未测

- 三平台安装包、HTTPS token 真 push、分离窗 Vault Git 全 Tab 走查。
