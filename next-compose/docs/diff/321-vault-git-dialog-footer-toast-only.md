# DIFF-321：Vault Git 对话框底部状态行移除

## 问题

Compose `VaultGitDialog` 在关闭按钮下方展示 `error` / `notice` / `git.busy` 行文案；Electron `VaultGitDialog` 仅通过 toast 反馈成功/失败，对话框内无对应状态行（[DIFF-318](318-vault-git-success-toast-only.md) 已去掉成功 notice）。

## 行为

- 移除面板内 `error`/`notice` 状态与底部三行文案；失败/flush 拦截仍 `toastError`，成功仍 `toastSuccess(git.done)`。
- `busy` 仍驱动按钮 `enabled` 与加载逻辑。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
