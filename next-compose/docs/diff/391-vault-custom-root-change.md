# DIFF-391：Vault 自定义根目录变更

## 背景

Electron `settings:update` 在 `data.directory` / `vault.quickNotePath` / `vault.jsonPath` 变化时调用 `setDataAccess({ notes: false, json: false })`，避免 MCP 仍指向旧 Vault 根。换库后 JSON/随手记 Vault 树按设置重新展开（`applyTreeExpandMode` / `jsonVaultNeedsExpandPreference`）。

## 行为

- `AppContainer.updateSettings`：上述路径字段变化时 `aiIntegration.setDataAccess(notes=false, json=false)`。
- `OnVaultEffectiveRootChanged`：监听**解析后** Vault 根（`jsonPath`/`quickNotePath` + `data.directory` 下的默认库），变化时清空会话级 `vaultTreeExpanded` / `vaultTreeScrollOffset`、干净时 `*ReloadOpenFileIfClean`（含 `ClearedMissing` 提示），并 bump 树刷新。
- JSON/随手记 `LaunchedEffect` 快照与回页重载增加 `settings.data.directory` 依赖。

## 测试

- `VaultPathAiAccessTest`
- `VaultTreeSessionTest`

## 验收

- A01 AI 接入 / F01/F04 Vault；`desktopTest --offline`。
