# DIFF-623：A03 Vault Git 未解决冲突期 push UI 禁用

基线：DIFF-622（工作区）。

## 范围

- **A03**：`GitVaultRemotePresentation.pushEnabled`/`pushActionEnabled` 在 `conflicts > 0` 时禁用 push（补 DIFF-516 文档与 `GitEngine.push` 引擎守卫；顶栏 `VaultGitDialog` 传入 `status.conflicts`）。
- **单测**：`GitVaultRemotePresentationTest.pushDisabledWhileUnresolvedConflicts`；merge/rebase 产品证据流传入 `conflicts`。
- **证据帧**：`216`（`GitVaultPushDisabledConflictsCaptureTest`，`merging=false` + `conflicts=2`）。

**不重复** 609/618 链：609 锁定 merge 进行中 push 禁用；618 锁定提交禁用；本切片覆盖「有冲突计数但非 merging」与 UI 显式 `conflicts` 参数。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
