# DIFF-662：F11 网络 IPv4/Long 转换 `*ActionEnabled`

基线：DIFF-661（工作区）。

## 范围

- **F11**：IPv4↔Long 转换钮 `ipv4ToLongActionEnabled` / `longToIpv4ActionEnabled`（委托 `NetEngine` 校验）+ `NetScreen` 接线。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
