# DIFF-360：随手记单击目录先保存再切换选中

## 问题

Electron `selectNode` 在选中**目录**时先 `saveCurrent(false)`，再更新 `selectedPath` 并清空 `note/content`。Compose 先写 `vaultSelectedPath` 再用手写 `quickNoteDirty` + `saveCurrent`；保存失败时选中已落到目录而编辑器仍绑定旧笔记，与 Electron 不一致。

## 行为

- 树 **单击目录**：`saveIfNeeded`（复用 [DIFF-335](335-quicknote-save-if-needed-clean.md) 干净跳过）；失败则提示、**不**改选中、不清空编辑器。
- 成功后再设 `vaultSelectedPath` 并清空编辑区/metadata（与原先目录语义一致）。
- **单击文件**仍只更新 `vaultSelectedPath`；双击打开走 `onOpen` + `saveIfNeeded`。

## 验证

- 对照 `next/src/features/quickNote/QuickNoteTool.tsx` `selectNode`
- `./gradlew :composeApp:desktopTest --offline`
