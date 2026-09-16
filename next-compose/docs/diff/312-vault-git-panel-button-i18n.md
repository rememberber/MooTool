# DIFF-312：Vault Git 面板「继续 / 提交」按钮文案

## 问题

Electron `json.git.continueOperation` / `json.git.commit` 为「继续合并 / Rebase」「提交全部变更」。Compose `git.continue` / `git.commit` 过短（「继续」「提交」/ “Continue”“Commit”），与面板按钮语义不一致。

## 行为

- `git.continue`：zh `继续合并 / Rebase`、en `Continue merge / rebase`、ja `マージ / リベースを続行`。
- `git.commit`：zh `提交全部变更`、en `Commit all changes`、ja `すべての変更をコミット`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
