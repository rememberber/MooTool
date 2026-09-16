# DIFF-296：JSON 结果弹层复制同步工具栏复制反馈

## 问题

Electron `JsonToolDialogs` 结果弹层「复制」调用 `copyValue`，除 notice/toast 外还将工具栏复制按钮置为 `copied` 约 1400ms（`copyState`）。Compose DIFF-295 仅同步 `session.notice`，工具栏按钮文案不变。

## 行为

- `ResultDialog` 复制成功/失败：`CopyFeedbackPolicy.afterCopy` + `copyGeneration`，与主工具栏「复制」一致。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
