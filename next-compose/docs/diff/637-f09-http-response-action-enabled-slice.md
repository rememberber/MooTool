# DIFF-637：F09 HTTP 响应复制/另存 `*ActionEnabled`

基线：DIFF-636（工作区）。

## 范围

- **F09**：`HttpResponsePresentation.copyResponseActionEnabled` / `saveResponseActionEnabled`（无可见响应或发送中禁用）；`HttpScreen` 响应区复制、另存与溢出菜单项 `enabled` 接线。
- **单测**：`HttpResponsePresentationTest.copyAndSaveResponseActionEnabledRequireVisibleAndNotSending`。

**不重复** 636：636 为 URL 栏发送钮；本切片为响应工具栏 IO。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
