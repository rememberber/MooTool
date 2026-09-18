# DIFF-644：F10 Host 方案/查找/对话框 `*ActionEnabled`

基线：DIFF-643（工作区）。

## 范围

- **F10**：`HostWiringPresentation.saveProfileActionEnabled` / `exportProfileActionEnabled` / `copyProfileActionEnabled` / `deleteProfileActionEnabled` / `findQueryActionEnabled` / `renameProfileActionEnabled` / `copySystemHostsActionEnabled`；`HostScreen` 工具栏保存、方案侧栏 IO、查找条、重命名/删除确认、系统 hosts 复制 `enabled` 接线（对齐 Electron `disabled={!dirty}` / `!renameValue.trim()` / `!systemHosts`）。
- **单测**：`HostWiringPresentationTest.profileAndFindActionEnabled`。

**不重复** 643：643 为 F09 HTTP 集合/cURL/停止。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
