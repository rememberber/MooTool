# DIFF-335：随手记 `saveIfNeeded` 干净文档不因旧 error 阻塞

## 问题

`saveIfNeeded` 在无脏内容时返回 `session.error.isBlank()`。保存失败后若用户撤销/重载回到与磁盘一致的内容，状态栏仍显示旧 error，Vault 拖放/移动/重命名（DIFF-333/334）与 Git `onFlush` 会被误拒。

Electron 以 `dirty` 为准；无未保存改动时不应再挡路径操作。

## 行为

- 提取 `quickNoteSaveIfNeeded(dirty, save)`：仅 `dirty` 时调用 `saveCurrent`。
- 干净文档恒返回 `true`（不再读 `session.error`）。

## 验证

- `QuickNoteSaveIfNeededTest`
- `./gradlew :composeApp:desktopTest --offline`
