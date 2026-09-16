# DIFF-437：Git 冲突按钮焦点帧 + 随手记冲突副本单测

## 背景

Git pull/merge 冲突引擎已有 `GitEngineTest`（DIFF-135～137），但 Vault Git 面板冲突操作钮缺少键盘焦点回归证据；随手记 Vault 外部冲突「保存副本」与 JSON 共用 `VaultConflictEngine.conflictCopyName`，需对称存储单测。

## 行为

- `QuickNoteVaultConflictCopyTest`：随手记 Vault 旁路副本不覆盖原笔记。
- `GitConflictActionsCaptureTest`：`142-compose-git-conflict-ours-tab-focus.png`（`git.ours` / `git.theirs` `p5Toolbar` 样式与 `VaultGitDialog` 一致）。
- Fixture：[electron-next-vaultGitService-vitest.md](../fixtures/electron-next-vaultGitService-vitest.md)。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（551/551）
