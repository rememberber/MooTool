# DIFF-664：F16 Cron 下次运行复制 `copyRunsActionEnabled`

基线：DIFF-663（工作区）。

## 范围

- **F16**：「复制」下次运行列表钮常显、`copyRunsActionEnabled`/`canCopyRuns` 接线（未解析前禁用，对齐其它工具 `*ActionEnabled` 模式）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
