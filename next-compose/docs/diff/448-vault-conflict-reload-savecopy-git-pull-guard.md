# DIFF-448：冲突对话框 reload/另存副本点击 + Git pull 守卫独立回归

## 背景

DIFF-447 已锁定 merge 中 `pull` 顺序与「保留编辑」点击。本切片补全冲突叠层其余主按钮的 Compose 点击语义，并将 pull 守卫从 `GitEngineTest` 拆出为独立用例，避免与大量 Git 集成测试耦合。

## 行为

- `VaultConflictDialogInteractionTest.reloadAndSaveCopyButtonsInvokeCallbacks`：按 `contentDescription` 点击「重新加载」「另存副本」，分别触发 `onReload` / `onSaveCopy`。
- `GitPullGuardTest.mergeInProgressRejectedBeforeMissingRemoteMessage`：merge 未完成且无 remote 时，`pull` 失败信息含 `merge`、不含 `remote`（与 Electron 先拒 merge 一致）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（593/593）
