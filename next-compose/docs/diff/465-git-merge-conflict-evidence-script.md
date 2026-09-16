# DIFF-465：Git merge 冲突产品窗准备脚本

## 背景

`docs/evidence/2026-09-17-vault-conflict-product-window/results.md` §B 要求本机 JSON Vault 处于 merge 冲突后再拍产品窗；手工 `git` 步骤易错且难复现。

## 行为

- `scripts/prepare-git-merge-conflict-evidence.sh`：在 `MOOTOOL_COMPOSE_DATA_DIR/data/vaults/json` 初始化仓库、bare `origin`、远端/本地分叉提交后 `git pull` 进入 merge 冲突（与 `GitEngineTest.pullLeavesMergeConflictWhenHistoriesDiverge` 一致）。
- 冲突产品窗文档 §B 引用该脚本；**不代替**产品窗 PNG。

## 验证

- 本机需 `git`；脚本退出码 0 后 `git -C "$VAULT" status` 应显示 merging + 冲突文件。
- `./gradlew :composeApp:desktopTest --offline` 仍 **630/630**（无新增单测）。
