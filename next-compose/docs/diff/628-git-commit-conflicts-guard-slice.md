# DIFF-628：A03 未解决冲突期 commit 引擎守卫登记

基线：DIFF-627（F05 格式化接线）。

## 范围

- **A03**：`GitEngine.commit` 在 `merging || conflicts > 0` 时拒绝（已有 `commitLocked`）；本切片补 **独立单测** 与 fixture 登记，与 `GitPushGuardTest` / `GitPullGuardTest` 同构 merge 冲突夹具。
- **Presentation**：`GitOperationPresentationTest` 断言 `conflicts > 0` 时 `commitEnabled` 为 false（对齐 Electron `VaultGitDialog` commit 按钮）。

**不重复** 618 帧链：618 为 merge/rebase 冲突期 commit **UI** 捕获；本切片为引擎 + Presentation 单测闭环。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
