# DIFF-439：Vault 监视器 → 冲突叠层 wiring + Git/Vault 冲突 Tab 帧

## 背景

DIFF-438 证明 `jsonVaultApplyExternalChange` 在脏编辑时产出 `VaultConflictState`，但未覆盖 `JsonScreen` 实际使用的 `VaultRevisionMonitor` 轮询路径。Git 冲突「使用远端版本」钮缺对称 Tab 证据。

## 行为

- `JsonVaultMonitorConflictTest` / `QuickNoteVaultMonitorConflictTest`：监视器检测到外部改写后，经 `SwingUtilities.invokeLater` 调用 `jsonVaultApplyExternalChange` / `quickNoteApplyExternalChange` 并得到 `Conflict`。
- `GitConflictActionsCaptureTest`：`144-compose-git-conflict-theirs-tab-focus.png`。
- `VaultConflictCaptureTest`：`145-compose-vault-conflict-keep-tab-focus.png`（「保留编辑」）。
- 产品主窗手工步骤：[docs/evidence/2026-09-17-vault-conflict-product-window/results.md](../evidence/2026-09-17-vault-conflict-product-window/results.md)。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（559/559）
