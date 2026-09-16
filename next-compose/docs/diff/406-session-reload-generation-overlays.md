# DIFF-406：`reloadAllToolSessionsFromStore` 单测补强

## 背景

DIFF-399～405 依赖 `sessionGeneration` 与 `resetVaultScopedOverlays` 在备份恢复/跨产品导入后通知 UI 并重置 Vault/Git 叠层。除「JSON 仍同一实例且从 store 读回正文」外，需锁定 generation 递增与 overlay 清理。

## 行为

- `SessionManagerReloadTest.reloadIncrementsSessionGeneration`：`reloadAllToolSessionsFromStore` 使 `sessionGeneration` +1。
- `SessionManagerReloadTest.reloadResetsJsonVaultScopedOverlays`：重载后 `gitDialogOpen`/`pathPickerOpen` 等为 false。

## 验收

- A03；`./gradlew :composeApp:desktopTest --offline`。
