# DIFF-617：A03 rebase §C resolve 提示 + merge/rebase 完成后 pull 恢复

基线：DIFF-616（工作区）。

## 范围

- **A03**：`GitRebaseProductEvidenceFlowTest` 锁定自动选中 `conflict.json` 后走查提示为 `git.rebaseProductFlowResolve`（非未选中时的 `Select`）；rebase 完成后 `pullActionEnabled` 与 push 一并恢复。
- **A03**：`GitMergeProductEvidenceFlowTest` 锁定自动选中后 `git.mergeProductFlowResolve`，merge 完成后 `pullActionEnabled` 恢复（对齐 push 链 DIFF-609/616）。
- **证据脚本**：`mootool_evidence_print_git_rebase_product_hint` 文案对齐 resolve 提示。

**不重复** 616：push/pull 禁用 Compose 帧 `211` / 捕获测试链。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
