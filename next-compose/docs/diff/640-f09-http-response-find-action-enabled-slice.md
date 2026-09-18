# DIFF-640：F09 HTTP 响应查找 `*ActionEnabled`

基线：DIFF-639（工作区）。

## 范围

- **F09**：`HttpResponsePresentation.responseFindOpenActionEnabled`（无可见响应/发送中禁用打开；`findOpen` 时仍可关闭）、`findQueryActionEnabled` / `findStepActionEnabled`；`HttpScreen` 响应区「查找」钮与查找条「查找/上一处/下一处」`enabled` 接线。
- **单测**：`HttpResponsePresentationTest.responseFindAndFindBarActionEnabled`。

**不重复** 637：637 为复制/另存；本切片为响应查找。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
