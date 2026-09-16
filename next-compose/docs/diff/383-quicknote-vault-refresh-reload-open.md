# DIFF-383：随手记文档库刷新重载当前笔记

## 背景

Electron `loadTree` / `onQuickNoteVaultChange` 在编辑器干净时调用 `reloadCurrentNoteFromDisk`。Compose 仅有切回工具页时的重载；筛选索引刷新不重读磁盘。JSON 已在 [DIFF-382](382-json-vault-refresh-reload-open.md) 补齐。

## 行为

- `quickNoteReloadOpenFileIfClean`：干净时 `readNote` 并 `quickNoteOpenVaultFile`；缺失则 `clearQuickNoteOpenSession`。
- `persistFilter`（搜索/排序/含正文筛选）与「刷新文档库」按钮调用该逻辑；切回随手记且干净时复用并写 `vault.conflict.reloaded` notice。

## 测试

- `QuickNoteReloadOpenFileTest`

## 验收

- F01 Vault；`desktopTest --offline`。
