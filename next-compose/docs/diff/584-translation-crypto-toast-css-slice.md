# DIFF-584：F20 翻译失败/保存 toast + F12 密钥生成失败 toast + 词书 footer CSS + 帧 179

基线：DIFF-583（工作区）。

## 范围

- **F20**：`TranslationWiringPresentation.runSaveWord` + `failureMessage`；翻译 API 失败、词书重译失败、保存生词/词书条目 error toast（`ABORTED` 除外）。
- **F12**：非对称密钥生成失败 `toastError`（对齐已有「恢复公钥」路径）。
- **CSS**：`mooTranslationWordBookAsideFooter`（Electron `.translation-record-layout > aside > footer`）。
- **证据**：`TranslationNowButtonCaptureTest` → `179-compose-translation-now-tab-focus.png`。

**不重复** 583：F23 图片导出 toast / 帧 178。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
