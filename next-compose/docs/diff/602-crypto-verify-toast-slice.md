# DIFF-602：F14 验签失败 toast 呈现层收尾 + 帧 197

基线：DIFF-601（工作区）。

## 范围

- **F14**：非对称验签失败经 `notifyCryptoVerifyFailure`（沿用 `shouldToastVerifyFailure`）；`notifyCryptoFailure` 仍负责运算异常。
- **证据**：`CryptoVerifyTabCaptureTest` → `197-compose-crypto-verify-tab-focus.png`。

**不重复** 601：A01 设置校验 / 帧 196。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
