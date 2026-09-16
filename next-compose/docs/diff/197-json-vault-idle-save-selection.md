# DIFF-197：JSON Vault 空闲自动保存与树选中

## 背景

Electron `JsonVaultPanel`：

- 编辑脏数据 **250ms** 后静默 `persistSelected`（无 toast）。
- 树单击先 `onSelect`（文件/目录均可），目录再折叠；当前打开文件未保存时文件名旁显示标记。
- 底栏 `selectedEntry` 与 `selectedPath` 分离：仅当选中路径等于当前打开文件且脏时显示 `•`。

Compose 此前需手动点保存；目录选中不更新底栏；树行无脏标记。

## 行为

- `JsonSession.vaultSelectedPath` 写入会话快照。
- `VaultTreeList` 增加 `onSelect`、`activeFilePath`、`activeFileDirty`（随手记默认无操作）。
- F04：`LaunchedEffect` 250ms debounce 调用 `saveJsonVault(..., showToast = false)`；冲突态不自动保存。
- 底栏与树选中/脏标记按 Electron 规则展示。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
- 对照 `next/src/features/json/JsonVaultPanel.tsx`（`persistSelectedOnIdle`、`VaultNode`）
