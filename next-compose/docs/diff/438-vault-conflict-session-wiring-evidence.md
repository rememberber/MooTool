# DIFF-438：Vault 外部冲突会话 wiring +「保存副本」Tab 帧

## 背景

DIFF-436/437 覆盖存储旁路副本与 Compose 对话框单钮焦点帧，但 `jsonVaultApplyExternalChange` / `saveJsonVault` 在脏编辑 + 磁盘分叉时是否弹出 `VaultConflictState` 缺会话级单测；随手记对称路径亦需证明。

## 行为

- `JsonVaultConflictSessionTest`：脏 `open.json` + 外部改写 → `jsonVaultApplyExternalChange` 回调 `Conflict`；`saveJsonVault` 在 `canOverwrite` 失败时同样回调。
- `QuickNoteVaultConflictSessionTest`：脏笔记 + 磁盘 frontmatter/正文变更 → `quickNoteApplyExternalChange` 回调 `Conflict`。
- `VaultConflictCaptureTest`：`143-compose-vault-conflict-savecopy-tab-focus.png`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（555/555；`CryptoEngineTest.generatesRsaAndSm2RoundTrips` 曾偶发失败，隔离重跑通过）
