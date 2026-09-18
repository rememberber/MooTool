# DIFF-668：F11 瞬时网络操作 / F16 解析钮 `*ActionEnabled`

基线：DIFF-667。

## 范围

- **F11**：`NetWiringPresentation.flushDnsActionEnabled` / `refreshLocalAddressesActionEnabled`（在途任务时禁用 flush DNS 与刷新本机地址）+ `NetScreen` 接线。
- **F16**：`CronWiringPresentation.parseActionEnabled`（委托 `canParse`）+ 解析钮接线。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
