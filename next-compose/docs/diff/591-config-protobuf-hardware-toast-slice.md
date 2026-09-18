# DIFF-591：F06/F07/F25 失败 toast 呈现层 + 帧 186

基线：DIFF-590（工作区）。

## 范围

- **F06**：`ConfigWiringPresentation.shouldToastConvertFailure` / `shouldToastIoFailure` + `notifyConfigFailure`（转换/格式化/导入/导出失败）。
- **F07**：`ProtobufWiringPresentation.shouldToastOperationFailure` + `notifyProtobufFailure`。
- **F25**：`HardwareWiringPresentation.shouldToastCollectFailure`（跳过 `CancellationException`）/ `shouldToastLocalFailure` + `notifyHardwareFailure`（采集失败、复制报告失败）。
- **证据**：`HardwareRefreshTabCaptureTest` → `186-compose-hardware-refresh-tab-focus.png`。

**不重复** 590：F11 网络 / 帧 185。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
