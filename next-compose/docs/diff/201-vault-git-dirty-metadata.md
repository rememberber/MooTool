# DIFF-201：Vault Git 自动检查点/拉取识别 metadata 脏状态

## 背景

Electron 通过 `setQuickNoteEditorDirty` / `setJsonVaultEditorDirty` 阻止在编辑器未落盘时自动 Git 检查点与自动 pull。

Compose `VaultGitCheckpointScheduler` / `VaultGitPullScheduler` 原先仅比较随手记 **正文** `editor.text != savedText`，未包含 **metadata**（标题/字体/语法等）变更，可能在仅改元数据时仍自动 commit/pull。

## 行为

- `JsonSession.isVaultEditorDirty()`：已打开 Vault 文件且正文未保存。
- `QuickNoteSession.isVaultEditorDirty()`：已打开笔记且正文或 `metadata != savedMetadata`。
- `AppContainer` 四个调度器统一调用上述方法。
- 顺手修复 F01 文档库 `onSelect` 在保存失败时误用 `return@VaultTreeList` 的流程控制。

## 验证

- `VaultEditorDirtyTest`
- `./gradlew :composeApp:desktopTest --offline`
