# DIFF-447：Git pull 合并中守卫顺序 + 冲突对话框「保留编辑」点击

## 背景

Electron `VaultGitService.pull` 在检查 remote 之前先拒绝 merge/rebase 进行中的 pull。Compose 原先先报「未配置 remote」，与 Electron 不一致。冲突叠层「保留编辑」需有可点击语义回归（`MooOverlay.onDismiss` 亦绑定 `onKeep`，见 DIFF-441）。

## 行为

- `GitEngine.pull`：先 `merging` 再 `remote` 校验（对齐 Electron）。
- `GitEngineTest.pullBlockedWhileMergeInProgress`；并发提交单测改为断言 `concurrent.txt` 仅一条提交记录（消除顺序竞态）。
- `VaultConflictDialogInteractionTest.keepButtonInvokesOnKeepCallback`：语义 `contentDescription` 点击触发 `onKeep`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（591/591）
