# DIFF-621：A03 `UpdateAutoCheckSchedulerTest` 时序稳定

基线：DIFF-620（工作区）。

## 问题

全量 `desktopTest` 在负载下 `passesLatestAutoDownloadFlagOnEachCheck` / `runsCheckAfterStartupDelayWhenEnabled` 可能因固定 `delay` 略小于 `startupDelayMs`/`intervalMs` 而偶发失败。

## 行为

- 测试等待改为相对 `startupDelayMs`/`intervalMs` 的缓冲（+25/+30/+40 ms），不断言产品调度常量。

**不重复** 620：F04 重复键路径 UI。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`
