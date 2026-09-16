# DIFF-315：Vault Git 面板布局对齐 Electron

## 问题

Compose `VaultGitDialog` 在 Electron `VaultGitDialog.tsx` 之外额外展示 Vault 绝对路径、`git.counts`、Git 版本与面板内 `git.authHint`；提交区为整行输入 + 下方按钮，缺少「提交说明」标签与同一行提交按钮。

## 行为

- 标题下不再显示仓库根路径；移除 counts / `status.version` / 面板 `authHint`（HTTPS 说明仍在设置「文档库」`git.later` / `git.authHint`）。
- 「变更」Tab 底栏改为标签 + 输入 +「提交全部变更」同一行（对齐 `git-commit-row`）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
