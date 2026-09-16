# DIFF-444：Vault 冲突动作提取 + Git init/持锁单测

## 背景

DIFF-440～441 的会话流单测复制了 `JsonScreen`/`QuickNoteScreen` 内联逻辑，易与 UI 漂移。Electron `vaultGitService` 的 `.DS_Store` 忽略与「持锁不隔离陈旧 index.lock」仍缺 Compose 登记。

## 行为

- 新增 `VaultConflictActions.kt`：`applyJsonVaultConflict*` / `applyQuickNoteVaultConflict*`；`JsonScreen`/`QuickNoteScreen` 与 flow 单测共用。
- `GitEngineTest.initWritesGitignoreAndIgnoresDsStore`、`doesNotQuarantineIndexLockHeldOpenByThisProcess`（本进程 `InputStream` 持锁 + 陈旧 mtime）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（579/579）
