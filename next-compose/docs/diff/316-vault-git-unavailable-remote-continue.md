# DIFF-316：Vault Git 不可用态与继续合并按钮

## 问题

- Electron 在 `status.available === false` 时不渲染远程地址行与工作区，仅显示 `json.git.unavailable` 空态；Compose 仍显示远程输入行。
- Electron 仅在 `merging && conflicts === 0` 时展示「继续合并 / Rebase」；Compose 在仍有冲突时展示禁用按钮。

## 行为

- `status.available` 为 false 时隐藏远程行（保留顶栏不可用提示与居中空态）。
- 「继续」按钮仅在 `status.merging && status.conflicts == 0` 时出现。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
