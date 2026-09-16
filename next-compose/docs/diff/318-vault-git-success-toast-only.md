# DIFF-318：Vault Git 成功反馈对齐 Electron toast

## 问题

Compose 成功时将 `GitActionResult.message`（如 `Done`、pull 输出）写入面板底部绿色 `notice` 并用于 toast；Electron 固定 `toast.success(json.git.done)`，面板内无成功行文案。

## 行为

Git 操作成功：清空 `error`/`notice`，仅 `toastSuccess(git.done)`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
