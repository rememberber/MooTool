# DIFF-110：Vault Git 面板 P5 与 flush 语义

对照 `next/src/features/json/VaultGitDialog.tsx` 与 `global.css` 中 `.git-panel` / `.git-workspace` / `.git-tab` / `.git-list-item`。

## 功能

- `discard` / `abort-merge` / `resolve-conflict` 执行前**不再**调用 `onFlush`（与 Electron `prepareGitAction` 一致）；`pull` / `continue-operation` 与其它 Git 操作仍先 flush。
- 弹层宽度 **920dp**；顶栏分支 13sp + ahead/behind 11sp muted；操作钮 `p5Toolbar`（30dp / 9dp 边距）。

## 样式

- `.git-remote-row`：`git.remote` 标签 + 输入 + 保存/删除 remote。
- `.git-workspace`：430dp 高、6dp 圆角、`borderSoft` 边框；左栏 280dp + `GitPanelTabs`（toolbar 底、30dp 分段 Tab）；变更/历史列表 11sp 行、状态码 10sp 等宽 accent。
- 底栏「关闭」右对齐。

## 文件

- `VaultGitDialog.kt`
- `Translator.kt`：`git.remote`
