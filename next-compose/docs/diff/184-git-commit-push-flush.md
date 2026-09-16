# DIFF-184：Vault Git 提交/推送前刷新编辑器

## 行为

- **F01/F04 / A03**：`VaultGitDialog` 在 **commit**、**push** 前调用 `onFlush()`，保证 Git 操作针对磁盘上的 Vault/笔记内容，而不是未保存缓冲区。
- 仍对齐 Electron：`pull`/`continue-operation` 刷新；`discard`/`resolve`/`abort` 不刷新；`fetch`/`init`/配置 remote 等无 `GitVaultFlushAction` 的操作不额外 flush（打开面板时 `load()` 仍会 flush 一次）。
- 策略集中在 `GitEditorFlushPolicy` + `GitVaultFlushAction`。

## 验证

- `GitEditorFlushPolicyTest`
- `./gradlew :composeApp:desktopTest --offline`（**342/342**）
