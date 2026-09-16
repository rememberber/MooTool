# DIFF-245：切页清理与分离守卫复用

## 行为

- `ToolModalLifecycle.kt` 增加 `shouldRunToolLeaveCleanup`、`OnToolLeaveUnlessDetached`。
- Net / HTTP / 翻译 / 正则 / 代码运行 / 系统信息 的「离开工具页取消在途任务」改为使用该 helper，与 `DismissModalOverlaysOnDispose` 同一 `!isDetached` 语义。

## 验证

- `ToolModalLifecycleTest`
- `./gradlew :composeApp:desktopTest --offline`
