# DIFF-513：产品窗冲突/Git merge/IME 准备脚本校验

## 背景

DIFF-512「未做」仍列 Vault 外部冲突、Git merge 冲突与系统 IME **产品主窗**截图验收。已有 `prepare-*-evidence.sh` 与 `results.md` 步骤，但缺少可重复的非交互校验，且 `prepare-vault-conflict-evidence.sh` 曾输出裸 `export` 行（与 README「勿 eval 整段输出」不一致）。

## 行为

- `scripts/lib/product-evidence-common.sh`：隔离 `MOOTOOL_COMPOSE_DATA_DIR`、runDistributable 提示、Vault 样本与 Git merge 冲突后置断言（`MERGE_HEAD` + `diff-filter=U`）。
- 三个 `prepare-*-evidence.sh`：统一 `#` 注释输出、脚本末尾自检、对齐 `docs/evidence/…/results.md` 登记路径。
- `scripts/verify-product-evidence-prep.sh`：本机无 GUI 一键跑通三套准备逻辑（**不**启动 `runDistributable`）。
- `ProductEvidencePrepScriptTest`：desktopTest 内调用上述 bash 脚本，锁定与 `GitEngineTest.pullLeavesMergeConflictWhenHistoriesDiverge` 一致的 merge 冲突树。

## 验证

- `./scripts/verify-product-evidence-prep.sh`
- `ProductEvidencePrepScriptTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗 `runDistributable` + 截图（§A/B 冲突叠层、§B Git 面板、IME 预编辑）；六套 CSS 皮肤、P7 Win/Linux 安装、设置行内控件像素级差、其余 F-tool 引擎大切片。
