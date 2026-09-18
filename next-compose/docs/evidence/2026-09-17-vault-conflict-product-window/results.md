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
| §B 证据脚本等价链 + 面板 Presentation | `GitMergeProductEvidenceFlowTest` + `preferredConflictSelectionPath`（DIFF-604） |
| §B merge 冲突期 push/pull 禁用、完成后 push 恢复 | `GitVaultRemotePresentation.pushActionEnabled` + `GitMergeProductEvidenceFlowTest`（DIFF-609） |
| merge continue 钮 Compose 焦点帧 | `199-compose-git-merge-continue-tab-focus.png`（DIFF-604） |
| merge 进行中 push 禁用态 Compose 帧 | `204-compose-git-merge-push-disabled-tab-focus.png`（DIFF-609） |
| §C 证据脚本等价链 + rebase 走查提示 | `GitRebaseProductEvidenceFlowTest` + `git.rebaseProductFlow*`（DIFF-612）；自动选中 `conflict.json` → `git.rebaseProductFlowResolve`、完成后 pull 恢复（DIFF-617） |
| rebase continue 钮 Compose 焦点帧 | `207-compose-git-rebase-continue-tab-focus.png`（DIFF-612） |
| rebase §C 走查 hint + 刷新钮 Compose 帧 | `209-compose-git-rebase-flow-hint-tab-focus.png`（DIFF-614） |
| rebase §C ours/theirs 解析钮 Compose 帧 | `210-compose-git-rebase-resolve-tab-focus.png`（DIFF-615） |
| rebase 冲突期 push/pull 禁用 Compose 帧 | `211-compose-git-rebase-push-pull-disabled-tab-focus.png`（DIFF-616） |
| merge/rebase 冲突期提交禁用 Compose 帧 | `212`/`213`（DIFF-618；`GitOperationPresentation.commitEnabled`） |
| rebase 冲突期 fetch 可用 / pull 禁用 Compose 帧 | `214`（DIFF-619） |
| Compose Tab 焦点帧 | `141`–`144`（DIFF-436～439） |
| JSON Vault 冲突 Overlay 完整链（非主窗） | `147-compose-json-vault-conflict-overlay-keep-tab-focus.png`（`VaultConflictOverlayCaptureTest`，DIFF-542/543；**不能**代替 §A 产品主窗 PNG） |
| §A 证据脚本 `sample.json` 监视器→冲突链 | `VaultConflictProductEvidencePresentation` + `VaultConflictProductEvidenceFlowTest`（DIFF-605） |
| §A 证据 sample 冲突 reload 帧 | `200-compose-vault-external-conflict-sample-reload-tab-focus.png`（DIFF-605） |
| §A 证据 `sample.json` 外部删除→冲突链 | `VaultConflictDeletedProductEvidenceFlowTest`（DIFF-610） |
| §A 证据 sample 外部删除 saveCopy 帧 | `205-compose-vault-external-conflict-sample-deleted-savecopy-tab-focus.png`（DIFF-610） |
| §A F01 `sample-external.md` 监视器→冲突链 | `QuickNoteVaultConflictProductEvidenceFlowTest`（DIFF-607） |
| §A F01 sample 冲突 keep 帧 | `202-compose-quicknote-external-conflict-keep-tab-focus.png`（DIFF-607） |
| §A F01 `sample-external.md` 外部删除→冲突链 | `QuickNoteVaultConflictDeletedProductEvidenceFlowTest`（DIFF-611） |
| §A F01 sample 外部删除 saveCopy 帧 | `206-compose-quicknote-external-conflict-deleted-savecopy-tab-focus.png`（DIFF-611） |

## 待本机产品窗（通过后才可标 F04/F01 冲突 UI 已验收）

### A. JSON Vault 外部冲突叠层

0. 隔离数据目录（不污染本机默认 `~/Library/Application Support/...`）。总览见 [`scripts/README-product-evidence.md`](../../../scripts/README-product-evidence.md)；外部冲突脚本 [DIFF-462](../../diff/462-vault-mcp-access-evidence-prep-script.md)：

```bash
cd /path/to/next-compose
export MOOTOOL_COMPOSE_DATA_DIR="$(mktemp -d /tmp/mootool-compose-evidence-XXXX)"
./scripts/prepare-vault-conflict-evidence.sh   # 末尾自检 sample.json；勿 eval 整段输出
./gradlew :composeApp:runDistributable --offline
# 无 GUI 时先跑: ./scripts/verify-product-evidence-prep.sh
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
export MOOTOOL_COMPOSE_DATA_DIR="$(mktemp -d /tmp/mootool-compose-git-evidence-XXXX)"
./scripts/prepare-git-merge-conflict-evidence.sh   # 会重建 Vault 内 .git；末尾断言 MERGE_HEAD + conflict.json 未合并
./gradlew :composeApp:runDistributable --offline
```

（§A 外部冲突与 §B merge 冲突请用**不同**隔离目录；勿在同一目录先后跑两个 prepare 脚本。）

脚本会在 `$MOOTOOL_COMPOSE_DATA_DIR/data/vaults/json` 内留下 **merge 中** 的 `conflict.json`（逻辑同 `GitEngineTest.pullLeavesMergeConflictWhenHistoriesDiverge`）。

1. 若未用脚本：在 JSON Vault 目录 `git init`，配置本地 bare remote，制造分叉后 **pull** 进入 merge 冲突（见 `GitEngineTest` 步骤）。
2. 主窗打开 **Git 面板**，选中冲突文件，应显示「使用本地版本 / 使用远端版本」。
3. 分别 resolve 后 **继续合并**，工作区内容与 Electron 行为一致。
4. 产品窗截图：`NNN-json-vault-git-merge-conflict-product.png`。

### C. Vault Git rebase 冲突面板

0. 使用**独立**隔离目录（勿与 §A/§B 共用；勿 `eval` 整段脚本输出）：

```bash
export MOOTOOL_COMPOSE_DATA_DIR="$(mktemp -d /tmp/mootool-compose-git-rebase-evidence-XXXX)"
./scripts/prepare-git-rebase-conflict-evidence.sh   # 断言 rebase-merge + conflict.json 未合并
./gradlew :composeApp:runDistributable --offline
```

1. 主窗打开 **Git 面板**，应显示变基进行中（`git.operationRebase`）与 §C 走查提示（`git.rebaseProductFlow*`）。
2. 变更列表应自动选中 `conflict.json`；resolve 后冲突计数归零，点「继续合并 / Rebase」完成变基。
3. 变基冲突期 push/pull 应禁用（与 merge 相同 `GitVaultRemotePresentation` 逻辑；Compose 帧 `204` 为 merge 态示意）。
4. 产品窗截图：`NNN-json-vault-git-rebase-conflict-product.png`。

## 执行记录

- 本机 2026-09-17（DIFF-513）：已跑 `./scripts/verify-product-evidence-prep.sh` 与 `ProductEvidencePrepScriptTest`（隔离目录下三套 prepare 脚本退出码 0，merge 仓库 `conflict.json` 为 `U` 状态）；**未执行** `runDistributable` 与产品窗 PNG（需人工 GUI + 系统输入法）。
- 产品窗截图仍 **未执行**；`acceptance.md` 仍标记 F04/F01 冲突 UI 待验收。
