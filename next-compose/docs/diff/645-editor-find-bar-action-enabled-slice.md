# DIFF-645：F01/F04/F10/F09 查找条 `EditorFindBarPresentation`

基线：DIFF-644（工作区）。

## 范围

- **P0/跨工具**：`EditorFindBarPresentation.findQueryActionEnabled` / `findStepActionEnabled`；F04 JSON、F01 随手记查找条「查找/上一处/下一处」接线；F10 Host 上一处/下一处；F09 `HttpResponsePresentation` / F10 `HostWiringPresentation` 委托同一守卫。
- **单测**：`EditorFindBarPresentationTest`（commonTest）。

**不重复** 644：644 为 Host 方案 IO；本切片为查找条步进钮与共享 Presentation。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
