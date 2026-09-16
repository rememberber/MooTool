# DIFF-313：Vault Git 面板文案对齐 Electron `json.git.*`

## 问题

Compose `git.*` 与 Electron `json.git.*` 在面板状态、Tab、冲突按钮、Diff 空态等处措辞不一致（如 `git.unavailable` 长说明 vs「未检测到 Git 命令」、`git.changes`「变更」vs「未提交变更」等）。设置页能力说明仍用 `git.later` / `git.authHint`，不受本次缩短 `git.unavailable` 影响。

## 行为

- zh/en（及 ja 主要项）按 Electron `messages.ts` 更新 `git.done`、`git.unavailable`、`git.noRepo`、`git.init`、`git.changes`/`history`/空列表、`git.ours`/`theirs`、`git.remotePlaceholder`/`saveRemote`、`git.discard`、`git.diffEmpty` 及二进制/过大提示等。
- 延续 [DIFF-311](311-vault-git-default-commit-message-i18n.md)、[DIFF-312](312-vault-git-panel-button-i18n.md)。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
