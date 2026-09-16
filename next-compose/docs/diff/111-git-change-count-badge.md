# DIFF-111：Vault Git 变更数角标

对照 Electron `JsonVaultPanel.tsx` / `QuickNoteTool.tsx` 与 `.vault-git-badge`。

## 行为

- `GitEngine.status` 轮询变更数（5s），`tick` / 关闭 Git 弹层时立即刷新。
- JSON 工具栏与 Vault 操作行、随手记工具栏 Git 按钮显示角标（>99 显示 `99+`）。

## 文件

- `VaultGitBadge.kt`：`rememberVaultGitChangeCount`、`GitActionButton`
- `JsonScreen.kt`、`QuickNoteScreen.kt`
- `Translator.kt`：`json.git.open`、`quickNote.git`
