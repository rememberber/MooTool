# DIFF-597：F01/F09 失败 toast 呈现层收尾 + 帧 192

基线：DIFF-596（工作区）。

## 范围

- **F01**：`notifyQuickNoteIoFailure` / `notifyQuickNoteSaveGuardFailure` + `QuickNoteWiringPresentation.shouldToast*`；导出/导入与 Vault 保存守卫失败统一 helper（`notifyQuickNoteOperationFailure` 经 `shouldToastOperationFailure`）。
- **F09**：`notifyHttpResponseFailure` / `notifyHttpWriteFailure` / `notifyHttpCopyFailure` + `HttpRequestPresentation.shouldToastCopyFailure` / `HttpResponsePresentation.shouldToastWriteFailure`（复制、另存响应、网络错误 toast）。
- **证据**：`HttpResponseSaveCaptureTest` → `192-compose-http-response-save-tab-focus.png`。

**不重复** 596：F23/F24 / 帧 191。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
