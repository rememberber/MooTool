# DIFF-198：Git 面板打开/刷新不再隐式 flush

## 背景

Electron `VaultGitDialog.load` 仅拉取 status/history，不调用 `prepareGitAction` 或保存编辑器。

Compose 曾在 `load()`（打开面板、点刷新）时无条件 `onFlush()`，与 Electron 不一致，且会在用户只想查看变更时触发保存/toast。

## 行为

- `VaultGitDialog.load()` 去掉 `onFlush()`。
- 仍通过 `GitEditorFlushPolicy` 在 **pull / continue-operation / commit / push** 等动作前 flush（commit/push 为 compose 在 DIFF-184 上的增强，保证提交内容与缓冲区一致）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
- 对照 `next/src/features/json/VaultGitDialog.tsx` `load` 与 `JsonVaultPanel.prepareGitAction`
