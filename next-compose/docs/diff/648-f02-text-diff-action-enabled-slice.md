# DIFF-648：F02 文本对比工具栏 `*ActionEnabled`

基线：DIFF-647（工作区）。

## 范围

- **F02**：`TextDiffPresentation.manualCompareActionEnabled` / `navigateDiffActionEnabled` / `copyPatchActionEnabled`；`TextDiffScreen` 比较、上/下差异、复制补丁（工具栏与溢出菜单）`enabled` 接线。
- **单测**：`TextDiffPresentationTest.toolbarActionEnabledMatchesGuards`。

**说明**：复制钮在无 unified 时禁用（Compose 侧 UX）；Electron 仍可点并显示 `noCopy` 状态。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
