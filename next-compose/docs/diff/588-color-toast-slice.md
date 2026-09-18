# DIFF-588：F22 调色板失败 toast + 帧 183

基线：DIFF-587（工作区）。

## 范围

- **F22**：`ColorWiringPresentation.shouldToastOperationFailure` + `notifyColorFailure`；屏幕取色失败、色码解析、历史无效色、收藏夹文件夹重复/失败 error toast（对齐 Electron `reportError` / `toast.error`）。
- **证据**：`ColorScreenPickCaptureTest` → `183-compose-color-screen-pick-tab-focus.png`。

**不重复** 587：F08/F03 toast / 帧 182。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
