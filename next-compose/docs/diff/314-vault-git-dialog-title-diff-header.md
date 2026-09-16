# DIFF-314：Vault Git 对话框标题、入口与 Diff 区标题

## 问题

- JSON Vault 底栏 Git 入口仍为 `json.git.open` =「Git」，Electron 为「打开 Git 面板」/ `Open Git panel`。
- 对话框标题与 Electron `json.git.title` / `quickNote.git.title` 不一致（如「JSON 文档库 Git」「随手记 Git」）。
- Diff 右栏缺少 Electron `<h3>{json.git.diff}</h3>` 区标题。

## 行为

- 更新 `json.git.*`、`quickNote.git.title` 与新增 `git.diff`（zh/en/ja）。
- `VaultGitDialog` `GitFileDiffPane` 顶部展示 `git.diff` 标题。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
