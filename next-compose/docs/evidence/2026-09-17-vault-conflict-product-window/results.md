# Vault 外部冲突 / Git merge 冲突 — 产品主窗验收说明（2026-09-17）

## 已自动化（不能代替产品主窗截图）

| 项 | 证据 |
| --- | --- |
| 冲突决策引擎 | `VaultConflictEngineTest` |
| 脏编辑 + 磁盘分叉 → `VaultConflictState` | `JsonVaultConflictSessionTest`、`QuickNoteVaultConflictSessionTest`（DIFF-438） |
| 另存副本后编辑器回灌磁盘 + 外部删除 `deleted` | `JsonVaultConflictSaveCopyFlowTest`、`QuickNoteVaultConflictSaveCopyFlowTest`（DIFF-440） |
| 重新加载 / 保留编辑三条分支（会话态） | `JsonVaultConflictReloadKeepFlowTest`、`QuickNoteVaultConflictReloadKeepFlowTest`（DIFF-441；实现见 `VaultConflictActions` DIFF-444） |
| 删除态对话框（无 reload 钮）Tab 帧 | `146-compose-vault-conflict-deleted-savecopy-tab-focus.png`（DIFF-441） |
| 保存副本不写覆盖原文件 | `JsonVaultConflictCopyTest`、`QuickNoteVaultConflictCopyTest`（DIFF-436/437） |
| 监视器轮询 → 冲突回调 | `JsonVaultMonitorConflictTest`、`QuickNoteVaultMonitorConflictTest`（DIFF-439） |
| 冲突叠层打开时禁编辑器窗口激活聚焦 | `JsonEditorFocusPolicyTest`、`QuickNoteEditorFocusPolicyTest`（DIFF-445～446） |
| `VaultConflictActions` reload/save/keep | `VaultConflictActionsTest`（DIFF-444～446） |
| 冲突对话框「保留编辑」可点击 | `VaultConflictDialogInteractionTest`（DIFF-447） |
| 冲突对话框「重新加载」「另存副本」可点击 | `VaultConflictDialogInteractionTest.reloadAndSaveCopyButtonsInvokeCallbacks`（DIFF-448） |
| merge 中 pull 错误优先于 remote | `GitPullGuardTest`（DIFF-448） |
| Git pull merge 冲突 resolve/continue | `GitEngineTest.pullLeavesMergeConflictWhenHistoriesDiverge`（DIFF-137） |
| Compose Tab 焦点帧 | `141`–`144`（DIFF-436～439） |

## 待本机产品窗（通过后才可标 F04/F01 冲突 UI 已验收）

### A. JSON Vault 外部冲突叠层

0. 隔离数据目录（不污染本机默认 `~/Library/Application Support/...`）。总览见 [`scripts/README-product-evidence.md`](../../../scripts/README-product-evidence.md)；外部冲突脚本 [DIFF-462](../../diff/462-vault-mcp-access-evidence-prep-script.md)：

```bash
cd /path/to/next-compose
export MOOTOOL_COMPOSE_DATA_DIR="$(mktemp -d /tmp/mootool-compose-evidence-XXXX)"
./scripts/prepare-vault-conflict-evidence.sh
./gradlew :composeApp:runDistributable
```

Vault 根为 `$MOOTOOL_COMPOSE_DATA_DIR/data/vaults/json/`（脚本会写入 `sample.json`）。

1. `./gradlew :composeApp:runDistributable`（或已安装镜像）启动主窗，打开 **JSON**，Vault 中打开 `sample.json`。
2. 在编辑器内修改内容**不要保存**（状态栏应显示未保存 `•`）。
3. 用 Finder/编辑器直接改写该产品 `data/vaults/json/` 下同一 `sample.json`（或另开终端 `echo … > …/sample.json`）。
4. 等待 ≤1s 监视器刷新，应弹出 **外部修改冲突** 对话框（重新加载 / 保存副本 / 保留编辑），状态栏可有 `vault.conflict.banner`。
5. 分别验证三条分支：重新加载恢复磁盘、保存副本生成 `*.local-<epoch>.json`、保留编辑关闭叠层且编辑器内容不变。
6. 截图保存为 `docs/evidence/2026-09-15-inspector-screencapture/windows/NNN-json-vault-external-conflict-product.png`（编号接续现有序列），并在本文件「执行记录」登记。

### B. Vault Git merge 冲突面板

0. 推荐与 §A 共用隔离目录（勿 `eval` 整段脚本输出，注释行会导致 shell 报错）：

```bash
export MOOTOOL_COMPOSE_DATA_DIR="$(mktemp -d /tmp/mootool-compose-evidence-XXXX)"
./scripts/prepare-vault-conflict-evidence.sh    # 可选：外部冲突样本
./scripts/prepare-git-merge-conflict-evidence.sh
./gradlew :composeApp:runDistributable
```

脚本会在 `$MOOTOOL_COMPOSE_DATA_DIR/data/vaults/json` 内留下 **merge 中** 的 `conflict.json`（逻辑同 `GitEngineTest.pullLeavesMergeConflictWhenHistoriesDiverge`）。

1. 若未用脚本：在 JSON Vault 目录 `git init`，配置本地 bare remote，制造分叉后 **pull** 进入 merge 冲突（见 `GitEngineTest` 步骤）。
2. 主窗打开 **Git 面板**，选中冲突文件，应显示「使用本地版本 / 使用远端版本」。
3. 分别 resolve 后 **继续合并**，工作区内容与 Electron 行为一致。
4. 产品窗截图：`NNN-json-vault-git-merge-conflict-product.png`。

## 执行记录

- 本机 2026-09-17：**未执行**上述产品窗步骤；`acceptance.md` 仍标记冲突 UI 待验收。
