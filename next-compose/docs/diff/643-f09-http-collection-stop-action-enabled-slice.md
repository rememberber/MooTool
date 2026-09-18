# DIFF-643：F09 HTTP 集合/对话框/停止 `*ActionEnabled`

基线：DIFF-642（工作区）。

## 范围

- **F09**：`HttpCollectionPresentation.deleteSavedActionEnabled` / `saveCollectionActionEnabled` / `curlImportActionEnabled`；集合底栏删除、保存/删除确认与 cURL 导入对话框 `enabled` 接线（对齐 Electron `disabled={!saveName.trim()}` / `!curlValue.trim()`）。
- **F09**：`HttpRequestPresentation.stopSendActionEnabled`；发送中「停止」与 `cancelInFlightSend` 守卫一致。
- **单测**：`HttpCollectionPresentationTest`、`HttpRequestPresentationTest.stopSendActionEnabledRequiresSendingAndRequestId`。

**不重复** 636–640：636 为发送钮；637–640 为响应区 IO/查找。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
