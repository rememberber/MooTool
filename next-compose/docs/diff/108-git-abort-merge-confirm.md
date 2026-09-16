# DIFF-108：Vault Git 中止合并确认

- 编号：DIFF-108
- 影响：F01/F04 Vault Git；A03 Git
- 日期：2026-09-15

## 原行为（Electron）

- 合并/冲突时显示「中止合并 / Rebase」，点击后 `desktopDialog.confirm` 二次确认再 `abort-merge`
- 存在冲突但未标 merging 时仍可中止（`merging || conflicts > 0`）

## 本产品行为

- 新增 `git.confirmAbort`、`git.abortMerge` 文案
- `VaultGitDialog`：合并或冲突 > 0 时显示危险中止钮，弹出 overlay 确认后再 `GitEngine.abortMerge`；成功后清空选中与 diff 预览（对齐 Electron 丢弃 diff 状态）

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

产品窗合并冲突手工走查、真实 pull 引发冲突场景、三平台安装仍未测。
