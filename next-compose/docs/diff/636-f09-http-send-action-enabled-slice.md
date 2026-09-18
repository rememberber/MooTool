# DIFF-636：F09 HTTP 发送钮 `sendActionEnabled`

基线：DIFF-635（工作区）。

## 范围

- **F09**：`HttpRequestPresentation.sendActionEnabled`（委托既有 `canSend`）；`HttpScreen` URL 栏发送钮 `enabled` 接线（空 URL / 发送中禁用；`send()` 仍保留 `http.urlRequired` 守卫）。
- **单测**：`HttpRequestPresentationTest` 补充 `sendActionEnabled`。

**不重复** 547：547 已引入 `canSend` 与在途 requestId；本切片补 UI 禁用态。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
